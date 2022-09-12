package com.revealprecision.revealserver.messaging.listener;

import static com.revealprecision.revealserver.constants.FormConstants.BOTTLES_EMPTY;
import static com.revealprecision.revealserver.constants.FormConstants.BUSINESS_STATUS;
import static com.revealprecision.revealserver.constants.FormConstants.COLLECTION_DATE;
import static com.revealprecision.revealserver.constants.FormConstants.COMPOUNDHEADNAME;
import static com.revealprecision.revealserver.constants.FormConstants.DAILY_SUMMARY;
import static com.revealprecision.revealserver.constants.FormConstants.ELIGIBILITY;
import static com.revealprecision.revealserver.constants.FormConstants.ELIGIBLE;
import static com.revealprecision.revealserver.constants.FormConstants.HOH_PHONE;
import static com.revealprecision.revealserver.constants.FormConstants.IRS_LITE_VERIFICATION;
import static com.revealprecision.revealserver.constants.FormConstants.IRS_SA_DECISION;
import static com.revealprecision.revealserver.constants.FormConstants.LOCATION_PARENT;
import static com.revealprecision.revealserver.constants.FormConstants.MOBILIZATION;
import static com.revealprecision.revealserver.constants.FormConstants.MOBILIZATION_DATE;
import static com.revealprecision.revealserver.constants.FormConstants.MOBILIZED;
import static com.revealprecision.revealserver.constants.FormConstants.NAME_HO_H;
import static com.revealprecision.revealserver.constants.FormConstants.NOTSPRAYED_REASON;
import static com.revealprecision.revealserver.constants.FormConstants.REGISTER_STRUCTURE;
import static com.revealprecision.revealserver.constants.FormConstants.ROOMS_SPRAYED;
import static com.revealprecision.revealserver.constants.FormConstants.SPRAY;
import static com.revealprecision.revealserver.constants.FormConstants.SPRAYED_FEMALES;
import static com.revealprecision.revealserver.constants.FormConstants.SPRAYED_MALES;
import static com.revealprecision.revealserver.constants.FormConstants.SPRAYED_PREGWOMEN;
import static com.revealprecision.revealserver.constants.FormConstants.SPRAY_DATE;
import static com.revealprecision.revealserver.constants.FormConstants.STRUCTURE_SPRAYED;
import static com.revealprecision.revealserver.constants.FormConstants.YES;

import com.revealprecision.revealserver.api.v1.facade.models.EventFacade;
import com.revealprecision.revealserver.api.v1.facade.models.Obs;
import com.revealprecision.revealserver.constants.KafkaConstants;
import com.revealprecision.revealserver.exceptions.NotFoundException;
import com.revealprecision.revealserver.messaging.message.FormCaptureEvent;
import com.revealprecision.revealserver.persistence.domain.Location;
import com.revealprecision.revealserver.persistence.domain.Plan;
import com.revealprecision.revealserver.persistence.domain.Report;
import com.revealprecision.revealserver.persistence.domain.ReportIndicators;
import com.revealprecision.revealserver.persistence.repository.ReportRepository;
import com.revealprecision.revealserver.props.KafkaProperties;
import com.revealprecision.revealserver.service.LocationService;
import com.revealprecision.revealserver.service.PlanService;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
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

  private final KafkaTemplate<String, FormCaptureEvent> formCaptureEventKafkaTemplate;

  private final KafkaProperties kafkaProperties;

  // 1. Added 2 listeners here for the "original" events and the events generated for each parent in the location hierarchy
  // 2. The events generated for the location parents will go into the FORM_SUBMISSIONS_PARENT topic
  // 3. In the event that a replay of the topic is needed the offset of FORM_SUBMISSIONS should be set to beginning
  //      and the offset on topic FORM_SUBMISSIONS_PARENT should be set to the end

  @KafkaListener(topics = "#{kafkaConfigProperties.topicMap.get('FORM_SUBMISSIONS')}", groupId = "reveal_server_group")
  public void etl(FormCaptureEvent formCaptureEvent) {
    handleEvent(formCaptureEvent);
  }

  //TODO: Opportunity here for topic events for FORM_SUBMISSIONS_PARENT to have a short expiry as a disk space optimization
  @KafkaListener(topics = "#{kafkaConfigProperties.topicMap.get('FORM_SUBMISSIONS_PARENT')}", groupId = "reveal_server_group")
  public void etlForParent(FormCaptureEvent formCaptureEvent) {
    handleEvent(formCaptureEvent);
  }

  private void handleEvent(FormCaptureEvent formCaptureEvent) {
    log.debug("Received Message {}, ", formCaptureEvent);
    Plan plan = planService.findPlanByIdentifier(formCaptureEvent.getPlanId());
    Location location;
    if (formCaptureEvent.getLocationId() != null) {
      location = locationService.findByIdentifier(formCaptureEvent.getLocationId());
    } else if (formCaptureEvent.getRawFormEvent().getLocationId() != null
        && !formCaptureEvent.getRawFormEvent().getLocationId().isEmpty()) {
      location = locationService.findByIdentifier(
          UUID.fromString(formCaptureEvent.getRawFormEvent().getLocationId()));
    } else {
      Location locationFromBaseEntityId = null;
      try {
        locationFromBaseEntityId = locationService.findByIdentifier(
            UUID.fromString(formCaptureEvent.getRawFormEvent().getBaseEntityId()));
      } catch (NotFoundException notFoundException) {
        log.warn("Unable to find associated location for this event: {}",
            formCaptureEvent.getSavedEventId());
      }

      if (locationFromBaseEntityId != null) {
        location = locationFromBaseEntityId;
      } else {
        location = locationService.findByIdentifier(
            UUID.fromString(formCaptureEvent.getRawFormEvent().getDetails().get(LOCATION_PARENT)));
      }
    }
    Report reportEntry = getOrInstantiateReportEntry(plan, location);

    List<Obs> observations = formCaptureEvent.getRawFormEvent().getObs();
    ReportIndicators reportIndicators = reportEntry.getReportIndicators();

    EventFacade rawFormEvent = formCaptureEvent.getRawFormEvent();
    if (rawFormEvent.getEventType().equals(SPRAY)) {
      extractIRSSprayedLocationIndicators(observations, reportIndicators);
    } else if (rawFormEvent.getEventType().equals(IRS_LITE_VERIFICATION)) {
      extractVillageVisitationIndicators(observations, reportIndicators);
      extractMobilizationIndicators(observations, reportIndicators);
    } else if (rawFormEvent.getEventType().equals(DAILY_SUMMARY)) {
      extractDailySupervisionIndicators(observations, reportIndicators);
    } else if (rawFormEvent.getEventType().equals(IRS_SA_DECISION)) {
      reportIndicators.setIrsDecisionFormFilled(true);
    } else if (rawFormEvent.getEventType().equals(REGISTER_STRUCTURE)) {
      extractStructureRegistrationIndicators(reportIndicators);
    }

    reportIndicators.setBusinessStatus(getObservation(observations, BUSINESS_STATUS));
    reportRepository.save(reportEntry);
    Location parentLocation = locationService.getLocationParent(location,
        plan.getLocationHierarchy());
    if (parentLocation != null) {
      publishForParent(formCaptureEvent, plan, rawFormEvent, parentLocation);
    }
  }

  private void extractStructureRegistrationIndicators(ReportIndicators reportIndicators) {
    reportIndicators.setRegisteredStructures(
        (reportIndicators.getRegisteredStructures() == null ? 0
            : reportIndicators.getRegisteredStructures()) + 1);
  }

  private void extractVillageVisitationIndicators(List<Obs> observations,
      ReportIndicators reportIndicators) {
    reportIndicators.setDateSprayed(
        getObservation(observations, SPRAY_DATE));
    reportIndicators.setMobilizationDate(
        getObservation(observations, MOBILIZATION_DATE));
  }

  private void extractMobilizationIndicators(List<Obs> observations,
      ReportIndicators reportIndicators) {
    reportIndicators.setMobilized(
        getObservation(observations, MOBILIZED));
  }

  private void extractDailySupervisionIndicators(List<Obs> observations,
      ReportIndicators reportIndicators) {
    Set<String> currentDates = reportIndicators.getUniqueSupervisionDates();
    currentDates.add(getObservation(observations, COLLECTION_DATE));
    reportIndicators.setUniqueSupervisionDates(currentDates);
    reportIndicators.setSupervisorFormSubmissionCount(
        reportIndicators.getSupervisorFormSubmissionCount() == null ? 1
            : reportIndicators.getSupervisorFormSubmissionCount() + 1);
    reportIndicators.setInsecticidesUsed(
        (reportIndicators.getInsecticidesUsed() != null ? reportIndicators.getInsecticidesUsed()
            : 0) + NumberValue(getObservation(observations, BOTTLES_EMPTY), 0));
  }

  private void publishForParent(FormCaptureEvent formCaptureEvent, Plan plan,
      EventFacade rawFormEvent,
      Location parentLocation) {
    FormCaptureEvent parentEvent = FormCaptureEvent.builder().rawFormEvent(rawFormEvent)
        .locationId(parentLocation.getIdentifier())
        .savedEventId(formCaptureEvent.getSavedEventId()).planId(formCaptureEvent.getPlanId())
        .taskId(formCaptureEvent.getTaskId()).build();
    formCaptureEventKafkaTemplate.send(
        kafkaProperties.getTopicMap().get(KafkaConstants.FORM_SUBMISSIONS_PARENT),
        plan.getIdentifier().toString(), parentEvent);
  }

  private void extractIRSSprayedLocationIndicators(List<Obs> observations,
      ReportIndicators reportIndicators) {
    reportIndicators.setPregnantWomen(
        NumberValue(getObservation(observations, SPRAYED_PREGWOMEN), 0));
    reportIndicators.setFemales(NumberValue(getObservation(observations, SPRAYED_MALES), 0));
    reportIndicators.setMales(
        NumberValue(getObservation(observations, SPRAYED_FEMALES), 0));
    reportIndicators.setSprayedRooms(
        NumberValue(getObservation(observations, ROOMS_SPRAYED), 0));
    reportIndicators.setPhoneNumber(
        getObservation(observations, HOH_PHONE));
    reportIndicators.setNotSprayedReason(
        getObservation(observations, NOTSPRAYED_REASON));
    if (YES.equals(getObservation(observations, STRUCTURE_SPRAYED))) {
      reportIndicators.setSprayedStructures((reportIndicators.getSprayedStructures() == null ? 0
          : reportIndicators.getSprayedStructures()) + 1);
    }
    if (ELIGIBLE.equals(getObservation(observations, ELIGIBILITY))) {
      reportIndicators.setFoundStructures((reportIndicators.getFoundStructures() == null ? 0
          : reportIndicators.getFoundStructures()) + 1);
    }
    String houseHoldHead = getObservation(observations, COMPOUNDHEADNAME);
    if (StringUtils.isBlank(houseHoldHead)) {
      houseHoldHead = getObservation(observations, NAME_HO_H);
    }
    reportIndicators.setHouseholdHead(houseHoldHead);
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
    return StringUtils.isNotBlank(value) ? Integer.valueOf(value) : defaultValue;
  }
}
