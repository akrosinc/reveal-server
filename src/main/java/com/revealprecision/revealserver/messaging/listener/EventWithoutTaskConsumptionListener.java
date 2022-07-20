package com.revealprecision.revealserver.messaging.listener;

import com.revealprecision.revealserver.api.v1.dto.factory.EntityTagEventFactory;
import com.revealprecision.revealserver.messaging.KafkaConstants;
import com.revealprecision.revealserver.messaging.message.EntityTagEvent;
import com.revealprecision.revealserver.messaging.message.FormDataEntityTagEvent;
import com.revealprecision.revealserver.messaging.message.FormDataEntityTagValueEvent;
import com.revealprecision.revealserver.messaging.message.FormFieldEvent;
import com.revealprecision.revealserver.messaging.message.Message;
import com.revealprecision.revealserver.persistence.domain.EntityTag;
import com.revealprecision.revealserver.persistence.domain.EventTracker;
import com.revealprecision.revealserver.persistence.domain.Location;
import com.revealprecision.revealserver.persistence.domain.Plan;
import com.revealprecision.revealserver.persistence.domain.metadata.LocationMetadata;
import com.revealprecision.revealserver.persistence.domain.metadata.infra.MetadataObj;
import com.revealprecision.revealserver.persistence.repository.EventTrackerRepository;
import com.revealprecision.revealserver.props.KafkaProperties;
import com.revealprecision.revealserver.service.EntityTagService;
import com.revealprecision.revealserver.service.FormFieldService;
import com.revealprecision.revealserver.service.LocationService;
import com.revealprecision.revealserver.service.MetadataExpressionEvaluationService;
import com.revealprecision.revealserver.service.MetadataService;
import com.revealprecision.revealserver.service.PersonService;
import com.revealprecision.revealserver.service.PlanService;
import com.revealprecision.revealserver.service.TaskService;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
@Slf4j
public class EventWithoutTaskConsumptionListener extends Listener {

  private final MetadataService metadataService;
  private final PersonService personService;
  private final PlanService planService;
  private final FormFieldService formFieldService;
  private final EntityTagService entityTagService;
  private final TaskService taskService;
  private final LocationService locationService;
  private final MetadataExpressionEvaluationService metadataExpressionEvaluationService;
  private final EventTrackerRepository eventTrackerRepository;
  private final KafkaTemplate<String, Message> kafkaTemplate;
  private final KafkaProperties kafkaProperties;

  @KafkaListener(topics = "#{kafkaConfigProperties.topicMap.get('EVENT_WITHOUT_TASK_CONSUMPTION')}", groupId = "reveal_server_group")
  public void eventConsumption(FormDataEntityTagEvent eventMetadata) {
    log.info("Received Message in group foo: {}" , eventMetadata.toString());

    init();

    Plan plan = planService.findPlanByIdentifier(eventMetadata.getPlanIdentifier());

    List<FormDataEntityTagValueEvent> formDataEntityTagValueEvents = eventMetadata.getFormDataEntityTagValueEvents();

    String dateForScopeDateFields = eventMetadata.getDate();

//    FormDataEntityTagEvent entityTagEvent = FormDataEntityTagEvent
//        .builder()
//        .user(eventMetadata.getUser())
//        .entityId(eventMetadata.getEntityId())
//        .eventId(eventMetadata.getEventId())
//        .date(eventMetadata.getDate())
//        .eventType(eventMetadata.getEventType())
//        .taskIdentifier(eventMetadata.getTaskIdentifier())
//        .geographicalLevelName(eventMetadata.getGeographicalLevelName())
//        .supervisor(eventMetadata.getSupervisor())
//        .cddName(eventMetadata.getCddName())
//        .locationIdentifier(eventMetadata.getLocationIdentifier())
//        .locationHierarchyIdentifier(eventMetadata.getLocationHierarchyIdentifier())
//        .planIdentifier(eventMetadata.getPlanIdentifier())
//        .build();

    for (FormDataEntityTagValueEvent formDataEntityTagValueEvent : formDataEntityTagValueEvents) {

      switch (formDataEntityTagValueEvent.getEntityTagEvent().getLookupEntityType().getCode()) {
        case "Location":

          Location location = locationService.findByIdentifier(eventMetadata.getEntityId());

          List<FormDataEntityTagValueEvent> formDataEntityTagValueEvents1 = new ArrayList<>(
              formDataEntityTagValueEvents);

          LocationMetadata locationMetadataByLocation = metadataService.getLocationMetadataByLocation(
              location.getIdentifier());

          Set<UUID> entityTagIdentifierList = locationMetadataByLocation.getEntityValue()
              .getMetadataObjs()
              .stream().map(MetadataObj::getEntityTagId).collect(
                  Collectors.toSet());

          Map<UUID, EntityTag> entityTagMap = entityTagService.findEntityTagsByIdList(
                  entityTagIdentifierList).stream().map(entityTag -> new SimpleEntry<>(
                  entityTag.getIdentifier(), entityTag))
              .collect(Collectors.toMap(Entry::getKey, Entry::getValue));

          List<FormDataEntityTagValueEvent> existingMetadata = locationMetadataByLocation.getEntityValue()
              .getMetadataObjs().stream().map(metadataObj -> FormDataEntityTagValueEvent
                  .builder()
                  .selectedformField(formDataEntityTagValueEvent.getSelectedformField())
                  .entityTagEvent(EntityTagEventFactory.getEntityTagEvent(
                      entityTagMap.get(metadataObj.getEntityTagId())))
                  .value(MetadataService.getValueFromValueObject(metadataObj).getSecond())
                  .build()).collect(Collectors.toList());

          formDataEntityTagValueEvents1.addAll(existingMetadata);

          if (formDataEntityTagValueEvent.getEntityTagEvent().isAddToMetadata()) {
            String tag = getTagKey(plan, formDataEntityTagValueEvent.getEntityTagEvent(),
                eventMetadata, dateForScopeDateFields);

            Object tagValue = formDataEntityTagValueEvent.getValue();

            Object tagValueCasted = getCastedValue(
                formDataEntityTagValueEvent.getSelectedformField(),
                tagValue);

            metadataService.updateMetaData(
                location.getIdentifier(),
                tagValueCasted,
                plan,
                eventMetadata.getTaskIdentifier(),
                eventMetadata.getUser(),
                formDataEntityTagValueEvent.getEntityTagEvent().getValueType(),
                formDataEntityTagValueEvent.getEntityTagEvent(),
                "FormData",
                location,
                eventMetadata.getEventType(),
                Location.class,
                tag,
                dateForScopeDateFields);

          }

          Set<EntityTag> referencedTags = entityTagService.findEntityTagsByReferencedTags(
              formDataEntityTagValueEvent.getEntityTagEvent().getTag());

          for (EntityTag referencedTag : referencedTags) {

            EntityTagEvent referencedTagEvent = EntityTagEventFactory.getEntityTagEvent(
                referencedTag);

            if (referencedTag.isGenerated()) {
              String generationFormula = referencedTagEvent.getGenerationFormula();
              if (generationFormula != null) {

                try {
                  FormDataEntityTagEvent entityTagEvent1 = new FormDataEntityTagEvent();
                  entityTagEvent1.setFormDataEntityTagValueEvents(formDataEntityTagValueEvents1);

                  entityTagEvent1.getFormDataEntityTagValueEvents().forEach(
                      formDataEntityTagValueEvent1 -> log.trace("current meta object: {} = {}",
                          formDataEntityTagValueEvent1.getEntityTagEvent().getTag(),
                          formDataEntityTagValueEvent1.getValue()));

                  if ((Boolean) metadataExpressionEvaluationService.evaluateExpression(
                      generationFormula, FormDataEntityTagEvent.class,
                      entityTagEvent1, Boolean.class,
                      dateForScopeDateFields)) {

                    Optional<EventTracker> eventTrackerRetrievedOptional;

//                      if (referencedTagEvent.getScope().equals("Date")) {
//                        eventTrackerRetrievedOptional = eventTrackerRepository.findFirstEventTrackerByEventIdentifierAndEntityTagIdentifierAndDate(
//                            eventIdentifier,
//                            referencedTagEvent.getIdentifier(), dateForScopeDateFields);
//                      } else {
//                        eventTrackerRetrievedOptional = eventTrackerRepository.findFirstEventTrackerByEventIdentifierAndEntityTagIdentifier(
//                            eventIdentifier,
//                            referencedTagEvent.getIdentifier());
//                      }
//
//                      if (eventTrackerRetrievedOptional.isEmpty()) {

                    String referencedTagResultExpression = referencedTagEvent.getResultExpression();
                    Object o = metadataExpressionEvaluationService.evaluateExpression(
                        referencedTagResultExpression, FormDataEntityTagEvent.class,
                        entityTagEvent1, Object.class,
                        dateForScopeDateFields);
                    log.debug("generated value for referencedTagEvent: {} expression: {} = {}",
                        referencedTagEvent.getTag(), referencedTagResultExpression, o);
                    if (o != null) {
                      Object value = o;

//                          EventTracker eventTracker = EventTracker.builder()
//                              .eventIdentifier(eventIdentifier)
//                              .date(dateForScopeDateFields)
//                              .entityTagIdentifier(referencedTagEvent.getIdentifier())
//                              .scope(referencedTagEvent.getScope())
//                              .build();
//                          eventTracker.setEntityStatus(EntityStatus.ACTIVE);
//                          eventTrackerRepository.save(eventTracker);


                        FormDataEntityTagValueEvent formDataEntityTagValueEvent1 = FormDataEntityTagValueEvent
                            .builder()
                            .entityTagEvent(referencedTagEvent)
                            .planIdentifier(eventMetadata.getPlanIdentifier())
                            .locationHierarchyIdentifier(
                                eventMetadata.getLocationHierarchyIdentifier())
                            .geographicLevelName(eventMetadata.getGeographicalLevelName())
                            .entityIdentifier(location.getIdentifier())
                            .dateForScopeDate(dateForScopeDateFields)
                            .cddName(eventMetadata.getCddName())
                            .supervisor(eventMetadata.getSupervisor())
                            .value(value)
                            .build();

                        formDataEntityTagValueEvents1.add(formDataEntityTagValueEvent1);

                        kafkaTemplate.send(
                            kafkaProperties.getTopicMap()
                                .get(KafkaConstants.FORM_EVENT_CONSUMPTION),
                            formDataEntityTagValueEvent1);


                      if (referencedTagEvent.isAddToMetadata()) {

                        String referencedTagKey = getTagKey(plan, referencedTagEvent, eventMetadata,
                            dateForScopeDateFields);

                        Object generatedValue = formDataEntityTagValueEvent1.getValue();

                        Object tagValueCasted = getCastedValue(
                            formDataEntityTagValueEvent1.getSelectedformField(),
                            generatedValue);

                        metadataService.updateMetaData(
                            formDataEntityTagValueEvent1.getEntityIdentifier(),
                            tagValueCasted,
                            plan,
                            eventMetadata.getTaskIdentifier(),
                            eventMetadata.getUser(),
                            formDataEntityTagValueEvent1.getEntityTagEvent().getValueType(),
                            referencedTagEvent,
                            "FormData",
                            location,
                            eventMetadata.getEventType(),
                            Location.class,
                            referencedTagKey,
                            dateForScopeDateFields);
//                          }
                      }
                    }
                  }

                } catch (NoSuchMethodException e) {
                  e.printStackTrace();
                }
              }
            }
          }
          break;
        case "Person":
          //TODO: Plan Entity Type Metadata
          break;
        case "Plan":
          //TODO: Plan Entity Type Metadata
          break;
      }
    }
  }


  private Object getCastedValue(FormFieldEvent formField, Object tagValue) {
    Object tagValueCasted;
    if (formField.getDataType().equals("integer")) {
      tagValueCasted = ( tagValue instanceof Integer ) ? tagValue : Integer.valueOf((String) tagValue);
    } else {
      tagValueCasted = tagValue;
    }
    return tagValueCasted;
  }


  private String getTagKey(Plan plan, EntityTagEvent referencedTag,
      FormDataEntityTagEvent eventMetadata, String dateForScopeDateFields) {
    String referencedTagKey = null;
    if (referencedTag.getScope().equals("Global")) {
      referencedTagKey = referencedTag.getTag();
    } else if (referencedTag.getScope().equals("Plan")) {
      referencedTagKey =
          referencedTag.getTag() + "_" + plan.getIdentifier();
    } else if (referencedTag.getScope().equals("Event")) {
      referencedTagKey =
          referencedTag.getTag() + "_" + plan.getIdentifier() + "_"
              + "_" + eventMetadata.getEventId();
    } else if (referencedTag.getScope().equals("Date")) {
      referencedTagKey =
          referencedTag.getTag() + "_" + plan.getIdentifier() + "_"
              + dateForScopeDateFields; // + Date;
    }
    return referencedTagKey;
  }


}
