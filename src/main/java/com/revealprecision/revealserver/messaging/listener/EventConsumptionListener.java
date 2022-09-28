package com.revealprecision.revealserver.messaging.listener;

import static com.revealprecision.revealserver.constants.EntityTagDataTypes.INTEGER;
import static com.revealprecision.revealserver.constants.EntityTagScopes.DATE;
import static com.revealprecision.revealserver.constants.EntityTagScopes.EVENT;
import static com.revealprecision.revealserver.constants.EntityTagScopes.GLOBAL;
import static com.revealprecision.revealserver.constants.EntityTagScopes.PLAN;

import com.revealprecision.revealserver.api.v1.dto.factory.EntityTagEventFactory;
import com.revealprecision.revealserver.api.v1.dto.factory.FormDataEntityTagValueEventFactory;
import com.revealprecision.revealserver.constants.KafkaConstants;
import com.revealprecision.revealserver.exceptions.NotFoundException;
import com.revealprecision.revealserver.messaging.message.EntityTagEvent;
import com.revealprecision.revealserver.messaging.message.FormDataEntityTagEvent;
import com.revealprecision.revealserver.messaging.message.FormDataEntityTagValueEvent;
import com.revealprecision.revealserver.messaging.message.FormFieldEvent;
import com.revealprecision.revealserver.messaging.message.Message;
import com.revealprecision.revealserver.persistence.domain.EntityTag;
import com.revealprecision.revealserver.persistence.domain.Location;
import com.revealprecision.revealserver.persistence.domain.Person;
import com.revealprecision.revealserver.persistence.domain.Plan;
import com.revealprecision.revealserver.persistence.domain.Task;
import com.revealprecision.revealserver.persistence.domain.metadata.LocationMetadata;
import com.revealprecision.revealserver.persistence.domain.metadata.infra.MetadataObj;
import com.revealprecision.revealserver.props.KafkaProperties;
import com.revealprecision.revealserver.service.EntityTagService;
import com.revealprecision.revealserver.service.LocationService;
import com.revealprecision.revealserver.service.MetadataExpressionEvaluationService;
import com.revealprecision.revealserver.service.MetadataService;
import com.revealprecision.revealserver.service.PersonService;
import com.revealprecision.revealserver.service.PlanService;
import com.revealprecision.revealserver.service.TaskService;
import com.revealprecision.revealserver.util.ActionUtils;
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
public class EventConsumptionListener extends Listener {


  private final MetadataService metadataService;
  private final PlanService planService;
  private final EntityTagService entityTagService;
  private final LocationService locationService;
  private final MetadataExpressionEvaluationService metadataExpressionEvaluationService;
  private final KafkaTemplate<String, Message> kafkaTemplate;
  private final KafkaProperties kafkaProperties;
  private final TaskService taskService;
  private final PersonService personService;


  @KafkaListener(topics = "#{kafkaConfigProperties.topicMap.get('EVENT_CONSUMPTION')}", groupId = "reveal_server_group")
  public void eventConsumption(FormDataEntityTagEvent eventMetadata) {
    log.info("Received Message in group foo: {}", eventMetadata.toString());
    init();
    Task task = null;
    UUID eventId = UUID.fromString(eventMetadata.getEventId());
    Plan plan = planService.findPlanByIdentifier(eventMetadata.getPlanIdentifier());
    String metadataTitle = eventMetadata.getEventType();
    if (eventMetadata.getTaskIdentifier() != null) {
      try {
        task = taskService.getTaskByIdentifier(eventMetadata.getTaskIdentifier());
        metadataTitle = task.getAction().getTitle();
      }catch (NotFoundException e){
        log.warn("Task not found {} - it may still need to be synced!",eventMetadata.getTaskIdentifier());
      }
    }
    List<FormDataEntityTagValueEvent> formDataEntityTagValueEvents = eventMetadata.getFormDataEntityTagValueEvents();

    String dateForScopeDateFields = eventMetadata.getDate();

    for (FormDataEntityTagValueEvent formDataEntityTagValueEvent : formDataEntityTagValueEvents) {
      switch (formDataEntityTagValueEvent.getEntityTagEvent().getLookupEntityType().getCode()) {
        case "Location":
          List<FormDataEntityTagValueEvent> formDataEntityTagValueEvents1 = new ArrayList<>(
              formDataEntityTagValueEvents);
          List<Location> locations = getLocations(eventMetadata, task);

          for (Location location : locations) {
            LocationMetadata locationMetadataByLocation = metadataService.getLocationMetadataByLocation(
                location.getIdentifier());

            List<FormDataEntityTagValueEvent> existingMetadata = getFormDataEntityTagValueEvents(
                formDataEntityTagValueEvent, locationMetadataByLocation);

            formDataEntityTagValueEvents1.addAll(existingMetadata);

            if (formDataEntityTagValueEvent.getEntityTagEvent().isAddToMetadata()) {
              updateMetadata(task, eventMetadata, dateForScopeDateFields, plan,
                  formDataEntityTagValueEvent.getEntityTagEvent(), formDataEntityTagValueEvent,
                  location.getIdentifier(), location, metadataTitle);
            }

            Set<EntityTag> referencedTags = entityTagService.findEntityTagsByReferencedTags(
                formDataEntityTagValueEvent.getEntityTagEvent().getTag());

            for (EntityTag referencedTag : referencedTags) {

              EntityTagEvent referencedTagEvent = EntityTagEventFactory.getEntityTagEvent(
                  referencedTag);

              if (referencedTagEvent.isGenerated()) {
                processReferencedEntityTagEvent(eventMetadata, plan, metadataTitle, task,
                    dateForScopeDateFields,
                    formDataEntityTagValueEvents1, location, referencedTagEvent, eventId,
                    formDataEntityTagValueEvent.getSelectedformField());
              }
            }
          }
          break;
        case "Person":
          //TODO: Person Entity Type Metadata
          break;
        case "Plan":
          //TODO: Plan Entity Type Metadata
          break;
      }
    }
  }

  private void processReferencedEntityTagEvent(FormDataEntityTagEvent eventMetadata, Plan plan,
      String metadataTitle,
      Task task, String dateForScopeDateFields,
      List<FormDataEntityTagValueEvent> formDataEntityTagValueEvents1, Location location,
      EntityTagEvent referencedTagEvent, UUID eventId, FormFieldEvent formFieldEvent) {
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

          String referencedTagResultExpression = referencedTagEvent.getResultExpression();
          Object value = metadataExpressionEvaluationService.evaluateExpression(
              referencedTagResultExpression, FormDataEntityTagEvent.class,
              entityTagEvent1, Object.class,
              dateForScopeDateFields);
          log.debug("generated value for referencedTagEvent: {} expression: {} = {}",
              referencedTagEvent.getTag(), referencedTagResultExpression, value);
          if (value != null) {

            FormDataEntityTagValueEvent formDataEntityTagValueEvent1 = FormDataEntityTagValueEventFactory.getEntity(
                eventMetadata, dateForScopeDateFields, location, referencedTagEvent,
                value, eventId);
            formDataEntityTagValueEvent1.setSelectedformField(formFieldEvent);
            formDataEntityTagValueEvents1.add(formDataEntityTagValueEvent1);

            kafkaTemplate.send(
                kafkaProperties.getTopicMap()
                    .get(KafkaConstants.FORM_EVENT_CONSUMPTION),
                formDataEntityTagValueEvent1);

            if (referencedTagEvent.isAddToMetadata()) {

              updateMetadata(task, eventMetadata, dateForScopeDateFields, plan,
                  referencedTagEvent, formDataEntityTagValueEvent1,
                  formDataEntityTagValueEvent1.getEntityIdentifier(), location,
                  metadataTitle);
            }
          }
        }

      } catch (NoSuchMethodException e) {
        log.error("spel method execution error: {}", e.getMessage(), e);
      }
    }
  }

  private void updateMetadata(Task task, FormDataEntityTagEvent eventMetadata,
      String dateForScopeDateFields, Plan plan, EntityTagEvent formDataEntityTagValueEvent,
      FormDataEntityTagValueEvent formDataEntityTagValueEvent1, UUID location, Location location1,
      String metadataTitle) {
    String tag = getTagKey(task, eventMetadata, dateForScopeDateFields, plan,
        formDataEntityTagValueEvent);

    Object tagValue = formDataEntityTagValueEvent1.getValue();

    Object tagValueCasted = getCastedValue(
        formDataEntityTagValueEvent1.getSelectedformField(),
        tagValue);

    metadataService.updateMetaData(
        location,
        tagValueCasted,
        plan,
        eventMetadata.getTaskIdentifier(),
        eventMetadata.getUser(),
        formDataEntityTagValueEvent1.getEntityTagEvent().getValueType(),
        formDataEntityTagValueEvent,
        "FormData",
        location1,
        metadataTitle,
        Location.class,
        tag,
        dateForScopeDateFields);
  }

  private String getTagKey(Task task, FormDataEntityTagEvent eventMetadata,
      String dateForScopeDateFields, Plan plan, EntityTagEvent formDataEntityTagValueEvent) {
    String tag;
    if (task == null) {
      tag = getTagKeyWithOutTask(
          eventMetadata, dateForScopeDateFields,
          plan.getIdentifier(),
          formDataEntityTagValueEvent.getScope(),
          formDataEntityTagValueEvent.getTag());
    } else {
      tag = getTagKeyWithTask(
          eventMetadata, dateForScopeDateFields,
          plan.getIdentifier(), task.getIdentifier(),
          formDataEntityTagValueEvent.getScope(),
          formDataEntityTagValueEvent.getTag());
    }
    return tag;
  }

  private List<FormDataEntityTagValueEvent> getFormDataEntityTagValueEvents(
      FormDataEntityTagValueEvent formDataEntityTagValueEvent,
      LocationMetadata locationMetadataByLocation) {
    if (locationMetadataByLocation != null) {
      Set<UUID> entityTagIdentifierList = locationMetadataByLocation.getEntityValue()
          .getMetadataObjs()
          .stream().map(MetadataObj::getEntityTagId).collect(
              Collectors.toSet());

      Map<UUID, EntityTag> entityTagMap = entityTagService.findEntityTagsByIdList(
              entityTagIdentifierList).stream().map(entityTag -> new SimpleEntry<>(
              entityTag.getIdentifier(), entityTag))
          .collect(Collectors.toMap(Entry::getKey, Entry::getValue));

      return locationMetadataByLocation.getEntityValue()
          .getMetadataObjs().stream().map(metadataObj -> FormDataEntityTagValueEventFactory
              .getEntity(MetadataService.getValueFromValueObject(metadataObj).getSecond(),
                  formDataEntityTagValueEvent.getSelectedformField(),
                  EntityTagEventFactory.getEntityTagEvent(
                      entityTagMap.get(metadataObj.getEntityTagId())))
          ).collect(Collectors.toList());

    }
    return new ArrayList<>();
  }

  private List<Location> getLocations(FormDataEntityTagEvent eventMetadata, Task task) {
    List<Location> locations = new ArrayList<>();
    if (task == null) {
      if (eventMetadata.getEntityId() != null) {
        Optional<Location> location = locationService.findNullableByIdentifier(eventMetadata.getEntityId());
        location.ifPresent(locations::add);
      }
    } else {
      if (ActionUtils.isActionForPerson(task.getAction())) {
        Person person = personService.getPersonByIdentifier(eventMetadata.getEntityId());
        locations.addAll(person.getLocations());
      } else if (ActionUtils.isActionForLocation(task.getAction())) {
        Location location = locationService.findByIdentifier(eventMetadata.getEntityId());
        locations.add(location);
      }
    }
    return locations;
  }


  private Object getCastedValue(FormFieldEvent formField, Object tagValue) {
    Object tagValueCasted;
    if (formField.getDataType().equals(INTEGER)) {
      tagValueCasted =
          (tagValue instanceof Integer) ? tagValue : Integer.valueOf((String) tagValue);
    } else {
      tagValueCasted = tagValue;
    }
    return tagValueCasted;
  }

  private String getTagKeyWithTask(FormDataEntityTagEvent eventMetadata,
      String dateForScopeDateFields, UUID planIdentifier,
      UUID taskIdentifier, String scope, String tag) {
    String referencedTagKey = null;
    switch (scope) {
      case GLOBAL:
        referencedTagKey = tag;
        break;
      case PLAN:
        referencedTagKey =
            tag + "_" + planIdentifier + "_"
                + taskIdentifier;
        break;
      case EVENT:
        referencedTagKey =
            tag + "_" + planIdentifier + "_"
                + taskIdentifier + "_" + eventMetadata.getEventId();
        break;
      case DATE:
        referencedTagKey =
            tag + "_" + planIdentifier + "_"
                + taskIdentifier + "_" + dateForScopeDateFields; // + Date;

        break;
    }
    return referencedTagKey;
  }

  private String getTagKeyWithOutTask(FormDataEntityTagEvent eventMetadata,
      String dateForScopeDateFields, UUID planIdentifier, String scope, String tag) {
    String referencedTagKey = null;
    switch (scope) {
      case GLOBAL:
        referencedTagKey = tag;
        break;
      case PLAN:
        referencedTagKey =
            tag + "_" + planIdentifier;
        break;
      case EVENT:
        referencedTagKey =
            tag + "_" + planIdentifier + "_" + eventMetadata.getEventId();
        break;
      case DATE:
        referencedTagKey =
            tag + "_" + planIdentifier + "_" + dateForScopeDateFields; // + Date;

        break;
    }
    return referencedTagKey;
  }
}
