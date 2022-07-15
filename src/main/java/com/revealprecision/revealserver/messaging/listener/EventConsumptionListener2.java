package com.revealprecision.revealserver.messaging.listener;

import com.revealprecision.revealserver.api.v1.dto.factory.EntityTagEventFactory;
import com.revealprecision.revealserver.api.v1.facade.models.Obs;
import com.revealprecision.revealserver.enums.EntityStatus;
import com.revealprecision.revealserver.messaging.KafkaConstants;
import com.revealprecision.revealserver.messaging.message.EntityTagEvent;
import com.revealprecision.revealserver.messaging.message.EventMetadata;
import com.revealprecision.revealserver.messaging.message.FormDataEntityTagEvent;
import com.revealprecision.revealserver.messaging.message.FormDataEntityTagValueEvent;
import com.revealprecision.revealserver.messaging.message.FormFieldEvent;
import com.revealprecision.revealserver.messaging.message.Message;
import com.revealprecision.revealserver.messaging.message.TMetadataEvent;
import com.revealprecision.revealserver.persistence.domain.EntityTag;
import com.revealprecision.revealserver.persistence.domain.EventTracker;
import com.revealprecision.revealserver.persistence.domain.Location;
import com.revealprecision.revealserver.persistence.domain.Person;
import com.revealprecision.revealserver.persistence.domain.Task;
import com.revealprecision.revealserver.persistence.repository.EventTrackerRepository;
import com.revealprecision.revealserver.props.KafkaProperties;
import com.revealprecision.revealserver.service.EntityTagService;
import com.revealprecision.revealserver.service.FormFieldService;
import com.revealprecision.revealserver.service.LocationService;
import com.revealprecision.revealserver.service.MetadataExpressionEvaluationService;
import com.revealprecision.revealserver.service.MetadataService;
import com.revealprecision.revealserver.service.PersonService;
import com.revealprecision.revealserver.service.TaskService;
import com.revealprecision.revealserver.util.ActionUtils;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
@Slf4j
public class EventConsumptionListener2 extends Listener {

  private final MetadataService metadataService;
  private final PersonService personService;
  private final FormFieldService formFieldService;
  private final EntityTagService entityTagService;
  private final TaskService taskService;
  private final LocationService locationService;
  private final MetadataExpressionEvaluationService metadataExpressionEvaluationService;
  private final EventTrackerRepository eventTrackerRepository;
  private final KafkaTemplate<String, Message> kafkaTemplate;
  private final KafkaProperties kafkaProperties;

  @KafkaListener(topics = "#{kafkaConfigProperties.topicMap.get('EVENT_CONSUMPTION')}", groupId = "reveal_server_group")
  public void eventConsumption(FormDataEntityTagEvent eventMetadata) {
    init();
    Task task = taskService.getTaskByIdentifier(eventMetadata.getTaskIdentifier());

//    FormField dateFormField = formFieldService.findByNameAndFormTitle("date", eventMetadata.getEventType());

    UUID eventIdentifier = UUID.fromString(eventMetadata.getEventId());

    List<FormDataEntityTagValueEvent> formDataEntityTagValueEvents = eventMetadata.getFormDataEntityTagValueEvents();

    List<FormDataEntityTagValueEvent> formDataEntityTagValueEvents1 = new ArrayList<>(
        formDataEntityTagValueEvents);
    String dateForScopeDateFields = eventMetadata.getDate();
    for (FormDataEntityTagValueEvent formDataEntityTagValueEvent : formDataEntityTagValueEvents) {

      //TODO: we need to generalise this somehow

      switch (formDataEntityTagValueEvent.getEntityTagEvent().getLookupEntityType().getCode()) {
        case "Location":

          List<Location> locations = new ArrayList<>();
          if (ActionUtils.isActionForPerson(task.getAction())) {
            Person person = personService.getPersonByIdentifier(eventMetadata.getEntityId());
            locations.addAll(person.getLocations());
          } else if (ActionUtils.isActionForLocation(task.getAction())) {
            Location location = locationService.findByIdentifier(eventMetadata.getEntityId());
            locations.add(location);
          }

          for (Location location : locations) {
            String tag = getTagKey(task, formDataEntityTagValueEvent.getEntityTagEvent(),
                eventMetadata, dateForScopeDateFields);

            Object tagValue = formDataEntityTagValueEvent.getValue();

            Object tagValueCasted = getCastedValue(
                formDataEntityTagValueEvent.getEntityTagEvent().getFormFields().stream().filter(
                    formFieldEvent -> formFieldEvent.getName()
                        .equals(formDataEntityTagValueEvent.getFormField())).findFirst().get(),
                tagValue);

            Set<EntityTag> referencedTags = entityTagService.findEntityTagsByReferencedTags(
                formDataEntityTagValueEvent.getEntityTagEvent().getTag());

            for (EntityTag referencedTag : referencedTags) {

              EntityTagEvent entityTagEvent = EntityTagEventFactory.getEntityTagEvent(
                  referencedTag);

              if (referencedTag.isGenerated()) {
                String generationFormula = referencedTag.getGenerationFormula();
                if (generationFormula != null) {

                  try {

                    FormDataEntityTagEvent entityTagEvent1 = new FormDataEntityTagEvent();
                    entityTagEvent1.setFormDataEntityTagValueEvents(formDataEntityTagValueEvents1);
                    if ((Boolean) metadataExpressionEvaluationService.evaluateExpression(
                        generationFormula, FormDataEntityTagEvent.class,
                        entityTagEvent1, Boolean.class,
                        dateForScopeDateFields)) {

                      Optional<EventTracker> eventTrackerRetrievedOptional;

                      if (referencedTag.getScope().equals("Date")) {
                        eventTrackerRetrievedOptional = eventTrackerRepository.findFirstEventTrackerByEventIdentifierAndEntityTagIdentifierAndDate(
                            eventIdentifier,
                            referencedTag.getIdentifier(), dateForScopeDateFields);
                      } else {
                        eventTrackerRetrievedOptional = eventTrackerRepository.findFirstEventTrackerByEventIdentifierAndEntityTagIdentifier(
                            eventIdentifier,
                            referencedTag.getIdentifier());
                      }
                      if (eventTrackerRetrievedOptional.isEmpty()) {

                        String referencedTagResultExpression = referencedTag.getResultExpression();
                        Object o = metadataExpressionEvaluationService.evaluateExpression(
                            referencedTagResultExpression, FormDataEntityTagEvent.class,
                            entityTagEvent1, Object.class,
                            dateForScopeDateFields);
                        if (o != null) {
                          Object value = o;

                          EventTracker eventTracker = EventTracker.builder()
                              .eventIdentifier(eventIdentifier)
                              .date(dateForScopeDateFields)
                              .entityTagIdentifier(referencedTag.getIdentifier())
                              .scope(referencedTag.getScope())
                              .build();
                          eventTracker.setEntityStatus(EntityStatus.ACTIVE);
                          eventTrackerRepository.save(eventTracker);

                          FormDataEntityTagValueEvent formDataEntityTagValueEvent1 = FormDataEntityTagValueEvent
                              .builder()
                              .entityTagEvent(entityTagEvent)
                              .planIdentifier(eventMetadata.getPlanIdentifier())
                              .locationHierarchyIdentifier(
                                  eventMetadata.getLocationHierarchyIdentifier())
                              .geographicLevelName(eventMetadata.getGeographicalLevelName())
                              .locationIdentifier(location.getIdentifier())
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
                        }
                      }
                    }


                  } catch (NoSuchMethodException e) {
                    e.printStackTrace();
                  }
                }
              }
            }

          }
          break;
        case "Person":
//            List<Person> people = new ArrayList<>();
//            if (ActionUtils.isActionForPerson(task.getAction())) {
//              Person person = personService.getPersonByIdentifier(
//                  eventMetadata.getBaseEntityId());
//              people.add(person);
//            } else if (ActionUtils.isActionForLocation(task.getAction())) {
//              Location location = locationService.findByIdentifier(eventMetadata.getBaseEntityId());
//              List<Person> peopleByLocations = personService.getPeopleByLocations(
//                  List.of(location));
//              people.addAll(peopleByLocations);
//            }
//
//            String finalDateForScopeDateFields1 = dateForScopeDateFields;
//            people.forEach(person -> {
//              String tag = getTagKey(task, entityTag, eventMetadata, finalDateForScopeDateFields1);
//              Object tagValue = metadata.get(eventMetadata.getObs().getFieldCode());
//
//              Object tagValueCasted = getCastedValue(formField, tagValue);
//
//              PersonMetadata personMetadata = (PersonMetadata) metadataService.updateMetaData(
//                  eventMetadata.getBaseEntityId(),
//                  tagValueCasted,
//                  task.getAction().getGoal().getPlan(),
//                  eventMetadata.getTaskIdentifier(),
//                  eventMetadata.getUser(),
//                  entityTag.getValueType(),
//                  entityTag,
//                  "FormData",
//                  person,
//                  task.getAction().getTitle(), Person.class,
//                  tag, finalDateForScopeDateFields1);
//
//              Set<EntityTag> referencedTags = entityTagService.findEntityTagsByReferencedTags(
//                  entityTag.getTag());
//
//              PersonMetadata referenceMetadata = personMetadata;
//
//              for (EntityTag referencedTag : referencedTags) {
//                if (referencedTag.isGenerated()) {
//
//                  String generationFormula = referencedTag.getGenerationFormula();
//
//                  if (generationFormula != null) {
//
//                    PersonMetadataEvent personMetadataEvent = PersonMetadataEventFactory.getPersonMetadataEvent(
//                        task.getAction().getGoal().getPlan(), person.getLocations().stream().map(
//                            Location::getIdentifier).collect(
//                            Collectors.toList()), referenceMetadata);
//
//                    try {
//                      referenceMetadata = (PersonMetadata) updateMetaDataForGeneratedTags(referencedTag, personMetadataEvent,
//                          eventMetadata, task, person,finalDateForScopeDateFields1,referenceMetadata);
//                    } catch (NoSuchMethodException e) {
//                      e.printStackTrace();
//                    }
//                  }
//                }
//              }
//            });
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
      tagValueCasted = Integer.valueOf((String) tagValue);
    } else {
      tagValueCasted = tagValue;
    }
    return tagValueCasted;
  }

  public Object updateMetaDataForGeneratedTags(EntityTag referencedTag,
      TMetadataEvent metadataEvent,
      EventMetadata eventMetadata, Task task, Object entity, String dateForScopeDateFields,
      Object referenceMetadata, String referencedTagKey)
      throws NoSuchMethodException {
    if (referencedTag.isGenerated()) {

      String generationFormula = referencedTag.getGenerationFormula();

      log.trace("#############referenced tag {} generation formula: {} metadata: {}",
          referencedTag.getTag(), generationFormula, metadataEvent);

      if ((Boolean) metadataExpressionEvaluationService.evaluateExpression(
          generationFormula, TMetadataEvent.class, metadataEvent,
          Boolean.class, dateForScopeDateFields)) {

        log.trace("###########referenced tag {} successfully evaluated with date: {} ",
            referencedTag.getTag(), dateForScopeDateFields);

        Object generatedValue = getGeneratedValue(referencedTag, metadataEvent,
            dateForScopeDateFields);

        log.trace("###########referenced tag {} generatedValue {} ", referencedTag.getTag(),
            generatedValue);

        Object o = metadataService.updateMetaData(
            eventMetadata.getBaseEntityId(),
            generatedValue,
            task.getAction().getGoal().getPlan(),
            eventMetadata.getTaskIdentifier(),
            eventMetadata.getUser(),
            referencedTag.getValueType(),
            referencedTag,
            "FormData",
            entity,
            task.getAction().getTitle(),
            Location.class,
            referencedTagKey,
            dateForScopeDateFields);

        log.trace("################referencedTag: {} locationMetadata: {}", referencedTag.getTag(),
            o);

        return o;
      }
    }
    return null;
  }

  private Object getGeneratedValue(EntityTag referencedTag, TMetadataEvent metadataEvent,
      String dateForScopeDateFields)
      throws NoSuchMethodException {
    Object generatedValue;
    String valueType = referencedTag.getValueType();
    String resultExpression = referencedTag.getResultExpression();
    if (!referencedTag.isResultLiteral()) {

      generatedValue = getGeneratedResult(metadataEvent, valueType,
          resultExpression, dateForScopeDateFields);

    } else {
      generatedValue = resultExpression;
    }
    return generatedValue;
  }

  private String getTagKey(Task task, EntityTagEvent referencedTag,
      FormDataEntityTagEvent eventMetadata, String dateForScopeDateFields) {
    String referencedTagKey = null;
    if (referencedTag.getScope().equals("Global")) {
      referencedTagKey = referencedTag.getTag();
    } else if (referencedTag.getScope().equals("Plan")) {
      referencedTagKey =
          referencedTag.getTag() + "_" + task.getAction().getGoal().getPlan().getIdentifier() + "_"
              + task.getIdentifier();
    } else if (referencedTag.getScope().equals("Event")) {
      referencedTagKey =
          referencedTag.getTag() + "_" + task.getAction().getGoal().getPlan().getIdentifier() + "_"
              + task.getIdentifier() + "_" + eventMetadata.getEventId();
    } else if (referencedTag.getScope().equals("Date")) {
      referencedTagKey =
          referencedTag.getTag() + "_" + task.getAction().getGoal().getPlan().getIdentifier() + "_"
              + task.getIdentifier() + "_" + dateForScopeDateFields; // + Date;
    }
    return referencedTagKey;
  }

  private Object getGeneratedResult(TMetadataEvent tMetadataEvent, String valueType,

      String resultExpression, String dateForDateScoped) throws NoSuchMethodException {

    log.trace("resultExpression: {} ", resultExpression);
    Object generatedValue;
    switch (valueType) {
      case "string":
        generatedValue = metadataExpressionEvaluationService.evaluateExpression(
            resultExpression, TMetadataEvent.class, tMetadataEvent,
            String.class, dateForDateScoped);
        break;
      case "date":
        generatedValue = metadataExpressionEvaluationService.evaluateExpression(
            resultExpression, TMetadataEvent.class, tMetadataEvent,
            LocalDateTime.class, dateForDateScoped);
        break;
      case "boolean":
        generatedValue = metadataExpressionEvaluationService.evaluateExpression(
            resultExpression, TMetadataEvent.class, tMetadataEvent,
            Boolean.class, dateForDateScoped);
        break;
      case "double":
        generatedValue = metadataExpressionEvaluationService.evaluateExpression(
            resultExpression, TMetadataEvent.class, tMetadataEvent,
            Double.class, dateForDateScoped);
        break;
      case "integer":
        generatedValue = metadataExpressionEvaluationService.evaluateExpression(
            resultExpression, TMetadataEvent.class, tMetadataEvent,
            Integer.class, dateForDateScoped);
        break;
      default:
        generatedValue = metadataExpressionEvaluationService.evaluateExpression(
            resultExpression, TMetadataEvent.class, tMetadataEvent,
            Integer.class, dateForDateScoped);
    }
    return generatedValue;
  }

  public static Map<String, Object> extractData(Obs obs) { //TODO:
    Map<String, Object> response = new HashMap<>();
    if (obs.getKeyValPairs() == null) {
      response.put(obs.getFieldCode(), obs.getValues().get(0));
    } else {
      response.put(obs.getFieldCode(), obs.getKeyValPairs().get(obs.getValues().get(0)));
    }
    return response;
  }


}
