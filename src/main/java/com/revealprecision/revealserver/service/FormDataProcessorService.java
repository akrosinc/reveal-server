package com.revealprecision.revealserver.service;

import static com.revealprecision.revealserver.constants.FormConstants.CDD_DRUG_ALLOCATION_CDD_NAME_FIELD;
import static com.revealprecision.revealserver.constants.FormConstants.CDD_DRUG_ALLOCATION_DATE_FIELD;
import static com.revealprecision.revealserver.constants.FormConstants.CDD_DRUG_ALLOCATION_FORM;
import static com.revealprecision.revealserver.constants.FormConstants.CDD_DRUG_ALLOCATION_HEALTH_WORKER_SUPERVISOR_FIELD;
import static com.revealprecision.revealserver.constants.FormConstants.CDD_DRUG_ALLOCATION_LOCATION_FIELD;
import static com.revealprecision.revealserver.constants.FormConstants.CDD_SUPERVISOR_DAILY_SUMMARY_CDD_NAME_FIELD;
import static com.revealprecision.revealserver.constants.FormConstants.CDD_SUPERVISOR_DAILY_SUMMARY_DATE_FIELD;
import static com.revealprecision.revealserver.constants.FormConstants.CDD_SUPERVISOR_DAILY_SUMMARY_FORM;
import static com.revealprecision.revealserver.constants.FormConstants.CDD_SUPERVISOR_DAILY_SUMMARY_HEALTH_WORKER_SUPERVISOR_FIELD;
import static com.revealprecision.revealserver.constants.FormConstants.IRS_ELIGIBLE;
import static com.revealprecision.revealserver.constants.FormConstants.IRS_FOUND;
import static com.revealprecision.revealserver.constants.FormConstants.IRS_LITE_ELIGIBLE;
import static com.revealprecision.revealserver.constants.FormConstants.IRS_LITE_FOUND;
import static com.revealprecision.revealserver.constants.FormConstants.IRS_LITE_NOT_SPRAYED;
import static com.revealprecision.revealserver.constants.FormConstants.IRS_LITE_SPRAYED;
import static com.revealprecision.revealserver.constants.FormConstants.IRS_LITE_VERIFICATION_FORM;
import static com.revealprecision.revealserver.constants.FormConstants.IRS_LITE_VERIFICATION_FORM_BUSINESS_STATUS_FIELD;
import static com.revealprecision.revealserver.constants.FormConstants.IRS_LITE_VERIFICATION_FORM_SUPERVISOR;
import static com.revealprecision.revealserver.constants.FormConstants.IRS_NOT_SPRAYED;
import static com.revealprecision.revealserver.constants.FormConstants.IRS_SACHET_COUNT;
import static com.revealprecision.revealserver.constants.FormConstants.IRS_SPRAYED;
import static com.revealprecision.revealserver.constants.FormConstants.SPRAY_FORM;
import static com.revealprecision.revealserver.constants.FormConstants.SPRAY_FORM_BUSINESS_STATUS_FIELD;
import static com.revealprecision.revealserver.constants.FormConstants.SPRAY_FORM_SACHET_COUNT_FIELD;
import static com.revealprecision.revealserver.constants.FormConstants.SPRAY_FORM_SPRAY_OPERATOR_FIELD;
import static com.revealprecision.revealserver.constants.FormConstants.TABLET_ACCOUNTABILITY_CDD_NAME_FIELD;
import static com.revealprecision.revealserver.constants.FormConstants.TABLET_ACCOUNTABILITY_FORM;
import static com.revealprecision.revealserver.constants.FormConstants.TABLET_ACCOUNTABILITY_HEALTH_WORKER_SUPERVISOR_FIELD;
import static com.revealprecision.revealserver.constants.FormConstants.TABLET_ACCOUNTABILITY_LOCATION_FIELD;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.revealprecision.revealserver.api.v1.dto.factory.EntityTagEventFactory;
import com.revealprecision.revealserver.api.v1.dto.factory.FormDataEntityTagEventFactory;
import com.revealprecision.revealserver.api.v1.dto.factory.FormDataEntityTagValueEventFactory;
import com.revealprecision.revealserver.api.v1.facade.models.EventFacade;
import com.revealprecision.revealserver.api.v1.facade.models.Obs;
import com.revealprecision.revealserver.constants.KafkaConstants;
import com.revealprecision.revealserver.enums.PlanInterventionTypeEnum;
import com.revealprecision.revealserver.messaging.message.DeviceUser;
import com.revealprecision.revealserver.messaging.message.FormCaptureEvent;
import com.revealprecision.revealserver.messaging.message.FormDataEntityTagEvent;
import com.revealprecision.revealserver.messaging.message.FormDataEntityTagValueEvent;
import com.revealprecision.revealserver.messaging.message.OrgLevel;
import com.revealprecision.revealserver.messaging.message.UserData;
import com.revealprecision.revealserver.messaging.message.mdalite.MDALiteLocationSupervisorCddEvent;
import com.revealprecision.revealserver.persistence.domain.EntityTag;
import com.revealprecision.revealserver.persistence.domain.Event;
import com.revealprecision.revealserver.persistence.domain.FormField;
import com.revealprecision.revealserver.persistence.domain.Location;
import com.revealprecision.revealserver.persistence.domain.Organization;
import com.revealprecision.revealserver.persistence.domain.Plan;
import com.revealprecision.revealserver.persistence.domain.User;
import com.revealprecision.revealserver.props.KafkaProperties;
import com.revealprecision.revealserver.util.FormDataUtil;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class FormDataProcessorService {


  private final ObjectMapper objectMapper;
  private final KafkaProperties kafkaProperties;
  private final PlanService planService;
  private final KafkaTemplate<String, FormDataEntityTagEvent> eventConsumptionTemplate;


  private final FormFieldService formFieldService;
  private final EntityTagService entityTagService;
  private final LocationService locationService;

  private final LocationRelationshipService locationRelationshipService;

  private final KafkaTemplate<String, UserData> userDataTemplate;
  private final KafkaTemplate<String, MDALiteLocationSupervisorCddEvent> mdaliteSupervisorTemplate;

  private final KafkaTemplate<String, FormCaptureEvent> formObservationsEventKafkaTemplate;


  public void processFormDataAndSubmitToMessaging(Event savedEvent,EventFacade eventFacade) throws IOException {

    JsonNode obsList = savedEvent.getAdditionalInformation().get("obs");
    FormCaptureEvent formCaptureEvent = FormCaptureEvent.builder()
        .locationId(savedEvent.getLocationIdentifier()).savedEventId(savedEvent.getIdentifier()).planId(savedEvent.getPlanIdentifier()).rawFormEvent(eventFacade).build();
    publishFormObservations(formCaptureEvent);

    if (obsList.isArray()) {

      Plan plan = planService.getPlanByIdentifier(savedEvent.getPlanIdentifier());

      ObjectReader reader = objectMapper.readerFor(new TypeReference<List<Obs>>() {
      });

      List<Obs> obsJavaList = reader.readValue(obsList);

      String dateString = null;
      String supervisorName = null;
      String cdd = null;
      UUID baseEntityIdentifier = savedEvent.getBaseEntityIdentifier();

      if (plan != null) {
        if (plan.getInterventionType().getCode().equals(PlanInterventionTypeEnum.MDA_LITE.name())) {
          if (savedEvent.getEventType().equals(CDD_SUPERVISOR_DAILY_SUMMARY_FORM)) {

            dateString = getFormValue(obsJavaList, CDD_SUPERVISOR_DAILY_SUMMARY_DATE_FIELD);

            supervisorName = getFormValue(obsJavaList,
                CDD_SUPERVISOR_DAILY_SUMMARY_HEALTH_WORKER_SUPERVISOR_FIELD);

            cdd = getFormValue(obsJavaList, CDD_SUPERVISOR_DAILY_SUMMARY_CDD_NAME_FIELD);

            submitSupervisorCddToMessaging(supervisorName, cdd, baseEntityIdentifier, plan);
          }
          if (savedEvent.getEventType().equals(TABLET_ACCOUNTABILITY_FORM)) {

            baseEntityIdentifier = getBaseEntityIdentifierFromLocationFormData(obsJavaList,
                TABLET_ACCOUNTABILITY_LOCATION_FIELD);

            supervisorName = getFormValue(obsJavaList,
                TABLET_ACCOUNTABILITY_HEALTH_WORKER_SUPERVISOR_FIELD);

            cdd = getFormValue(obsJavaList, TABLET_ACCOUNTABILITY_CDD_NAME_FIELD);

            submitSupervisorCddToMessaging(supervisorName, cdd, baseEntityIdentifier, plan);

          }
          if (savedEvent.getEventType().equals(CDD_DRUG_ALLOCATION_FORM)) {

            dateString = getFormValue(obsJavaList, CDD_DRUG_ALLOCATION_DATE_FIELD);

            supervisorName = getFormValue(obsJavaList,
                CDD_DRUG_ALLOCATION_HEALTH_WORKER_SUPERVISOR_FIELD);

            cdd = getFormValue(obsJavaList, CDD_DRUG_ALLOCATION_CDD_NAME_FIELD);

            baseEntityIdentifier = getBaseEntityIdentifierFromLocationFormData(obsJavaList,
                CDD_DRUG_ALLOCATION_LOCATION_FIELD);

            submitSupervisorCddToMessaging(supervisorName, cdd, baseEntityIdentifier, plan);
          }
        }

        List<FormDataEntityTagValueEvent> formDataEntityTagValueEvents = obsJavaList.stream()
            .flatMap(obs -> {
              Object value = FormDataUtil.extractData(obs).get(obs.getFieldCode());
              FormField formField = formFieldService.findByNameAndFormTitle(obs.getFieldCode(),
                  savedEvent.getEventType());
              if (formField != null) {
                Set<EntityTag> entityTagsByFieldName = entityTagService.findEntityTagsByFormField(
                    formField);
                return entityTagsByFieldName.stream().map(EntityTagEventFactory::getEntityTagEvent)
                    .map(entityTagEvent -> FormDataEntityTagValueEventFactory.getEntity(value,
                        formField, entityTagEvent));
              } else {
                return null;
              }
            }).filter(Objects::nonNull).collect(Collectors.toList());

        FormDataEntityTagEvent entityTagEvent = FormDataEntityTagEventFactory.getEntity(savedEvent,
            formDataEntityTagValueEvents, plan, baseEntityIdentifier, dateString, cdd,
            supervisorName);

        eventConsumptionTemplate.send(
            kafkaProperties.getTopicMap().get(KafkaConstants.EVENT_CONSUMPTION), entityTagEvent);

        User deviceUser = savedEvent.getUser();
        String fieldWorker = null;
        String fieldWorkerLabel = null;
        String userLabel = null;
        String orgLabel = null;
        boolean sprayed = false;
        boolean notSprayed = false;
        boolean isEligible = true;
        List<List<OrgLevel>> collect = new ArrayList<>();
        Map<String, Object> fields = new HashMap<>();
        if (savedEvent.getEventType().equals(SPRAY_FORM)) {
          fieldWorker = getFormValue(obsJavaList, SPRAY_FORM_SPRAY_OPERATOR_FIELD);

          String businessStatus = getFormValue(obsJavaList, SPRAY_FORM_BUSINESS_STATUS_FIELD);

          if (businessStatus.equals("Complete")) {
            sprayed = true;
          }
          if (businessStatus.equals("Not Eligible")) {
            isEligible = false;
          }
          if (businessStatus.equals("Not Sprayed")) {
            notSprayed = true;
          }
          boolean found = true;

          String sachetCount = getFormValue(obsJavaList, SPRAY_FORM_SACHET_COUNT_FIELD);
          Integer sachetCountInt = 0;
          try {
            sachetCountInt = Integer.parseInt(sachetCount);
          } catch (ClassCastException e) {
            log.warn("Could not cast form data: {}, of value: {}", SPRAY_FORM_SACHET_COUNT_FIELD,
                sachetCount);
          }

          fields.put(IRS_FOUND, found);
          fields.put(IRS_SPRAYED, sprayed);
          fields.put(IRS_NOT_SPRAYED, notSprayed);
          fields.put(IRS_ELIGIBLE, isEligible);
          fields.put(IRS_SACHET_COUNT, sachetCountInt);

          collect = deviceUser.getOrganizations().stream()
              .map(this::getFlattenedOrganizationalHierarchy).collect(Collectors.toList());

          fieldWorkerLabel = "Spray Operator";
          userLabel = "Supervisor";
          orgLabel = "Team";
        }
        if (savedEvent.getEventType().equals(IRS_LITE_VERIFICATION_FORM)) {
          fieldWorker = getFormValue(obsJavaList, IRS_LITE_VERIFICATION_FORM_SUPERVISOR);

          String businessStatus = getFormValue(obsJavaList,
              IRS_LITE_VERIFICATION_FORM_BUSINESS_STATUS_FIELD);

          if (businessStatus.equals("Sprayed")) {
            sprayed = true;
          }
          if (businessStatus.equals("Not Sprayed")) {
            notSprayed = true;
          }
          boolean found = true;

          fields.put(IRS_LITE_FOUND, found);
          fields.put(IRS_LITE_SPRAYED, sprayed);
          fields.put(IRS_LITE_NOT_SPRAYED, notSprayed);
          fields.put(IRS_LITE_ELIGIBLE, isEligible);

          collect = deviceUser.getOrganizations().stream()
              .map(this::getFlattenedOrganizationalHierarchy).collect(Collectors.toList());

          fieldWorkerLabel = "Supervisor";
          userLabel = "Field Officer";
          orgLabel = "Team";
        }

        userDataTemplate.send(kafkaProperties.getTopicMap().get(KafkaConstants.USER_DATA),
            new UserData(savedEvent.getPlanIdentifier(),
                new DeviceUser(deviceUser.getIdentifier(), deviceUser.getUsername()), userLabel,
                fieldWorker, fieldWorkerLabel, savedEvent.getCaptureDatetime(), collect, orgLabel,
                fields));

      }
    }
  }

  private void publishFormObservations(FormCaptureEvent event) {
    formObservationsEventKafkaTemplate.send(
        kafkaProperties.getTopicMap().get(KafkaConstants.FORM_SUBMISSIONS), event.getPlanId().toString(),
        event);
  }


  private List<OrgLevel> getFlattenedOrganizationalHierarchy(Organization organization) {

    List<OrgLevel> orgHierarchy = new ArrayList<>();
    int levelCounter = 0;

    orgHierarchy.add(new OrgLevel(organization.getIdentifier().toString(), organization.getName(),
        levelCounter));
    Organization loopOrg = organization;

    while (loopOrg.getParent() != null) {
      loopOrg = loopOrg.getParent();
      orgHierarchy.add(
          new OrgLevel(loopOrg.getIdentifier().toString(), loopOrg.getName(), levelCounter));
      levelCounter++;
    }
    return orgHierarchy;
  }

  private void submitSupervisorCddToMessaging(String supervisorName, String cdd,
      UUID baseEntityIdentifier, Plan plan) {
    if (supervisorName != null && cdd != null) {
      mdaliteSupervisorTemplate.send(
          kafkaProperties.getTopicMap().get(KafkaConstants.LOCATION_SUPERVISOR_CDD),
          MDALiteLocationSupervisorCddEvent.builder().cddName(cdd).supervisorName(supervisorName)
              .locationIdentifier(baseEntityIdentifier)
              .locationHierarchyIdentifier(plan.getLocationHierarchy().getIdentifier())
              .planIdentifier(plan.getIdentifier()).build());
    }
  }


  private UUID getBaseEntityIdentifierFromLocationFormData(List<Obs> obsJavaList,
      String formfield) {
    String locationName = getFormValue(obsJavaList, formfield);

    if (locationName != null) {
      List<Location> locations = locationService.getAllByNames(List.of(locationName));
      return locations.get(0).getIdentifier();
    }
    return null;
  }

  private String getFormValue(List<Obs> obsJavaList, String tabletAccountabilityCddNameField) {
    Optional<Obs> cddName = obsJavaList.stream()
        .filter(obs -> obs.getFieldCode().equals(tabletAccountabilityCddNameField)).findFirst();
    return cddName.map(obs -> (String) FormDataUtil.extractData(obs).get(obs.getFieldCode()))
        .orElse(null);
  }

}
