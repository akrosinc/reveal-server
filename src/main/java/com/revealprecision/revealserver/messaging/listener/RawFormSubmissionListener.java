package com.revealprecision.revealserver.messaging.listener;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.revealprecision.revealserver.api.v1.facade.models.EventFacade;
import com.revealprecision.revealserver.api.v1.facade.models.Obs;
import com.revealprecision.revealserver.messaging.message.FormCaptureEvent;
import com.revealprecision.revealserver.persistence.domain.Location;
import com.revealprecision.revealserver.persistence.domain.Plan;
import com.revealprecision.revealserver.persistence.domain.Report;
import com.revealprecision.revealserver.persistence.domain.ReportIndicators;
import com.revealprecision.revealserver.persistence.repository.ReportRepository;
import com.revealprecision.revealserver.service.LocationService;
import com.revealprecision.revealserver.service.PlanService;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class RawFormSubmissionListener extends Listener {

  private final ReportRepository reportRepository;
  private final PlanService planService;
  private final LocationService locationService;
  private final ObjectMapper objectMapper;

  @KafkaListener(topics = "#{kafkaConfigProperties.topicMap.get('FORM_SUBMISSIONS')}", groupId = "reveal_server_group")
  public void etl(FormCaptureEvent formCaptureEvent) {
    Plan plan = planService.findPlanByIdentifier(formCaptureEvent.getPlanId());
    Location location = locationService.findByIdentifier(formCaptureEvent.getLocationId());
    Report reportEntry = getOrInstantiateReportEntry(plan, location);

    ObjectReader reader = objectMapper.readerFor(new TypeReference<List<Obs>>() {
    });

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
    }

    reportRepository.save(reportEntry);
  }

  private void extractIRSFullSprayedLocationObservations(List<Obs> observations,
      ReportIndicators reportIndicators) {
    reportIndicators.setPregnantWomen(
        Integer.valueOf(getObservation(observations, "sprayed_pregwomen")));
    reportIndicators.setFemales(Integer.valueOf(
        getObservation(observations, "sprayed_males")));
    reportIndicators.setMales(
        Integer.valueOf(getObservation(observations, "sprayed_females")));
    reportIndicators.setSprayedRooms(
        Integer.valueOf(getObservation(observations, "rooms_sprayed")));
    reportIndicators.setPhoneNumber(
        getObservation(observations, "hoh_phone"));
    reportIndicators.setNotSprayedReason(
        getObservation(observations, "notsprayed_reason"));
  }

  private Report getOrInstantiateReportEntry(Plan plan, Location location) {
    return reportRepository.findByPlanAndLocation(plan, location)
        .orElse(Report.builder().location(location).plan(plan).reportIndicators(new ReportIndicators()).build());
  }

  private String getObservation(List<Obs> observations, String fieldCode) {
    Obs observation = observations.stream()
        .filter(obs -> obs.getFieldCode().equals(fieldCode)).findFirst().orElse(null);
    if (observation != null) {
      return (String) observation.getValues().get(0);
    }
    return null;
  }
}
