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
import com.revealprecision.revealserver.api.v1.facade.models.Obs;
import com.revealprecision.revealserver.constants.KafkaConstants;
import com.revealprecision.revealserver.enums.PlanInterventionTypeEnum;
import com.revealprecision.revealserver.messaging.message.FormDataEntityTagEvent;
import com.revealprecision.revealserver.messaging.message.FormDataEntityTagValueEvent;
import com.revealprecision.revealserver.messaging.message.mdalite.MDALiteLocationSupervisorCddEvent;
import com.revealprecision.revealserver.persistence.domain.EntityTag;
import com.revealprecision.revealserver.persistence.domain.Event;
import com.revealprecision.revealserver.persistence.domain.FormField;
import com.revealprecision.revealserver.persistence.domain.Location;
import com.revealprecision.revealserver.persistence.domain.Plan;
import com.revealprecision.revealserver.props.KafkaProperties;
import com.revealprecision.revealserver.util.FormDataUtil;
import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class FormDataProcessorService {

  private final ObjectMapper objectMapper;
  private final KafkaProperties kafkaProperties;
  private final PlanService planService;
  private final KafkaTemplate<String, FormDataEntityTagEvent> eventConsumptionTemplate;
  private final FormFieldService formFieldService;
  private final EntityTagService entityTagService;
  private final LocationService locationService;

  private final KafkaTemplate<String, MDALiteLocationSupervisorCddEvent> mdaliteSupervisorTemplate;

  public void processFormDataAndSubmitToMessaging(Event savedEvent) throws IOException {

    JsonNode obsList = savedEvent.getAdditionalInformation().get("obs");

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
        if (plan.getInterventionType()
            .getCode()
            .equals(PlanInterventionTypeEnum.MDA_LITE.name())) {
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
              FormField formField = formFieldService.findByNameAndFormTitle(
                  obs.getFieldCode(), savedEvent.getEventType());
              if (formField != null) {
                Set<EntityTag> entityTagsByFieldName = entityTagService.findEntityTagsByFormField(
                    formField);
                return entityTagsByFieldName.stream()
                    .map(EntityTagEventFactory::getEntityTagEvent)
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
            kafkaProperties.getTopicMap().get(KafkaConstants.EVENT_CONSUMPTION),
            entityTagEvent);

      }
    }
  }

  private void submitSupervisorCddToMessaging(String supervisorName, String cdd, UUID baseEntityIdentifier,
      Plan plan) {
    if (supervisorName != null && cdd != null) {
      mdaliteSupervisorTemplate.send(
          kafkaProperties.getTopicMap().get(KafkaConstants.LOCATION_SUPERVISOR_CDD),
          MDALiteLocationSupervisorCddEvent.builder()
              .cddName(cdd)
              .supervisorName(supervisorName)
              .locationIdentifier(baseEntityIdentifier)
              .locationHierarchyIdentifier(plan.getLocationHierarchy().getIdentifier())
              .planIdentifier(plan.getIdentifier())
              .build());
    }
  }


  private UUID getBaseEntityIdentifierFromLocationFormData(List<Obs> obsJavaList,
      String formfield) {
    String locationName = getFormValue(obsJavaList,
        formfield);

    if (locationName != null) {
      List<Location> locations = locationService.getAllByNames(List.of(locationName));
      return locations.get(0).getIdentifier();
    }
    return null;
  }

  private String getFormValue(List<Obs> obsJavaList, String tabletAccountabilityCddNameField) {
    Optional<Obs> cddName = obsJavaList.stream()
        .filter(obs -> obs.getFieldCode().equals(tabletAccountabilityCddNameField))
        .findFirst();
    return cddName.map(obs -> (String) FormDataUtil.extractData(obs)
        .get(obs.getFieldCode())).orElse(null);
  }

}
