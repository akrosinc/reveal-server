package com.revealprecision.revealserver.messaging.listener;

import com.revealprecision.revealserver.api.v1.facade.models.EventFacade;
import com.revealprecision.revealserver.api.v1.facade.models.Obs;
import com.revealprecision.revealserver.constants.KafkaConstants;
import com.revealprecision.revealserver.messaging.message.FormCaptureEvent;
import com.revealprecision.revealserver.persistence.domain.Location;
import com.revealprecision.revealserver.persistence.domain.Plan;
import com.revealprecision.revealserver.persistence.domain.Report;
import com.revealprecision.revealserver.persistence.domain.ReportIndicators;
import com.revealprecision.revealserver.persistence.domain.Task;
import com.revealprecision.revealserver.persistence.repository.ReportRepository;
import com.revealprecision.revealserver.props.KafkaProperties;
import com.revealprecision.revealserver.service.LocationRelationshipService;
import com.revealprecision.revealserver.service.LocationService;
import com.revealprecision.revealserver.service.PlanService;
import com.revealprecision.revealserver.service.TaskService;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class RawFormSubmissionListener extends Listener {

  private final ReportRepository reportRepository;
  private final PlanService planService;
  private final LocationService locationService;
  private final TaskService taskService;


  private final KafkaProperties kafkaProperties;

  private final LocationRelationshipService locationRelationshipService;

  private final KafkaTemplate<String, FormCaptureEvent> formCaptureEventKafkaTemplate;

  @KafkaListener(topics = "#{kafkaConfigProperties.topicMap.get('FORM_SUBMISSIONS')}", groupId = "reveal_server_group")
  public void etl(FormCaptureEvent formCaptureEvent) {
    Plan plan = planService.findPlanByIdentifier(formCaptureEvent.getPlanId());
    Task eventTask = null;
    if (formCaptureEvent.getTaskId() != null) {
      eventTask = taskService.getTaskByIdentifier(formCaptureEvent.getTaskId());
    }
    Location location;
    if (formCaptureEvent.getLocationId() != null) {
      location = locationService.findByIdentifier(formCaptureEvent.getLocationId());
    } else {
      location = locationService.findByIdentifier(
          UUID.fromString(formCaptureEvent.getRawFormEvent().getDetails().get("locationParent")));

    }
    Report reportEntry = getOrInstantiateReportEntry(plan, location);

    List<Obs> observations = formCaptureEvent.getRawFormEvent().getObs();
    ReportIndicators reportIndicators = reportEntry.getReportIndicators();

    EventFacade rawFormEvent = formCaptureEvent.getRawFormEvent();
    if (rawFormEvent.getEventType().equals("Spray")) {
      extractIRSFullSprayedLocationObservations(observations, reportIndicators);
    } else if (rawFormEvent.getEventType().equals("mobilization")) {
      reportIndicators.setMobilized(
          getObservation(observations, "mobilized"));
    } else if (rawFormEvent.getEventType().equals("irs_lite_verification")) {
      reportIndicators.setDateSprayed(
          getObservation(observations, "sprayDate"));
      reportIndicators.setMobilizationDate(
          getObservation(observations, "mobilization_date"));
    } else if (rawFormEvent.getEventType().equals("daily_summary")) {
      Set<String> currentDates = reportIndicators.getUniqueSupervisionDates();
      currentDates.add(getObservation(observations, "collection_date"));
      reportIndicators.setUniqueSupervisionDates(currentDates);
      reportIndicators.setSupervisorFormSubmissionCount(
          reportIndicators.getSupervisorFormSubmissionCount() + 1);
    } else if (rawFormEvent.getEventType().equals("irs_sa_decision")) {
      reportIndicators.setIrsDecisionFormFilled(true);
    } else if (rawFormEvent.getEventType().equals("Register_Structure")) {
      reportIndicators.setDiscoveredStructures((reportIndicators.getDiscoveredStructures() == null ? 0 : reportIndicators.getDiscoveredStructures() ) + 1);
    }
    if (eventTask != null) {
      reportIndicators.setBusinessStatus(eventTask.getBusinessStatus());
    }
    reportRepository.save(reportEntry);
  }

  private void extractIRSFullSprayedLocationObservations(List<Obs> observations,
      ReportIndicators reportIndicators) {
    reportIndicators.setPregnantWomen(
        NumberValue(getObservation(observations, "sprayed_pregwomen"), 0));
    reportIndicators.setFemales(NumberValue(getObservation(observations, "sprayed_males"), 0));
    reportIndicators.setMales(
        NumberValue(getObservation(observations, "sprayed_females"), 0));
    reportIndicators.setSprayedRooms(
        NumberValue(getObservation(observations, "rooms_sprayed"), 0));
    reportIndicators.setPhoneNumber(
        getObservation(observations, "hoh_phone"));
    reportIndicators.setNotSprayedReason(
        getObservation(observations, "notsprayed_reason"));
  }

  private Report getOrInstantiateReportEntry(Plan plan, Location location) {
    return reportRepository.findByPlanAndLocation(plan, location)
        .orElse(
            Report.builder().location(location).plan(plan).reportIndicators(new ReportIndicators())
                .build());
  }

  private String getObservation(List<Obs> observations, String fieldCode) {
    Obs observation = observations.stream()
        .filter(obs -> obs.getFieldCode().equals(fieldCode)).findFirst().orElse(null);
    if (observation != null) {
      return (String) observation.getValues().get(0);
    }
    return null;
  }

  private Integer NumberValue(String value, Integer defaultValue) {
    return value != null ? Integer.valueOf(value) : defaultValue;
  }
}
