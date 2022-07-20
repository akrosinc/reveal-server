package com.revealprecision.revealserver.messaging.listener;

import com.revealprecision.revealserver.api.v1.dto.factory.LocationMetadataEventFactory;
import com.revealprecision.revealserver.api.v1.facade.models.Obs;
import com.revealprecision.revealserver.enums.EntityStatus;
import com.revealprecision.revealserver.messaging.KafkaConstants;
import com.revealprecision.revealserver.messaging.message.EventMetadata;
import com.revealprecision.revealserver.messaging.message.LocationMetadataEvent;
import com.revealprecision.revealserver.messaging.message.Message;
import com.revealprecision.revealserver.messaging.message.MetadataObjEvent;
import com.revealprecision.revealserver.messaging.message.TMetadataEvent;
import com.revealprecision.revealserver.persistence.domain.EntityTag;
import com.revealprecision.revealserver.persistence.domain.EventTracker;
import com.revealprecision.revealserver.persistence.domain.FormField;
import com.revealprecision.revealserver.persistence.domain.Location;
import com.revealprecision.revealserver.persistence.domain.Person;
import com.revealprecision.revealserver.persistence.domain.Task;
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
import com.revealprecision.revealserver.service.TaskService;
import com.revealprecision.revealserver.util.ActionUtils;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
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
  private final PersonService personService;
  private final FormFieldService formFieldService;
  private final EntityTagService entityTagService;
  private final TaskService taskService;
  private final LocationService locationService;
  private final MetadataExpressionEvaluationService metadataExpressionEvaluationService;
  private final EventTrackerRepository eventTrackerRepository;
  private final KafkaTemplate<String, Message> kafkaTemplate;
  private final KafkaProperties kafkaProperties;

//  @KafkaListener(topics = "#{kafkaConfigProperties.topicMap.get('EVENT_CONSUMPTION')}", groupId = "reveal_server_group")
//  public void eventConsumption(EventMetadata eventMetadata)  {
//    Obs obs = eventMetadata.getObs();
//    Task task = taskService.getTaskByIdentifier(eventMetadata.getTaskIdentifier());
//    init();
//
//    log.debug("Obs Field: {}",obs.getFieldCode());
//
//
//    FormField formField = formFieldService.findByNameAndFormTitle(obs.getFieldCode(), eventMetadata.getEventType());
//
//    if (formField != null && formField.isAddToMetadata()) {
//      Map<String, Object> metadata = extractData(obs);
//
//      List<EntityTag> entityTagsByFieldName = entityTagService.findEntityTagsByFormField(formField);
//      log.debug("###############Obs Field: {} - entityTagsByFieldName: {}",obs.getFieldCode(),entityTagsByFieldName.stream().map(EntityTag::getTag).collect(Collectors.joining(",")));
//
//      for (EntityTag entityTag: entityTagsByFieldName) {
//        String dateForScopeDateFields = null;
//        //TODO: we need to generalise this somehow
//
//          FormField dateFormField = formFieldService.findByNameAndFormTitle("date", eventMetadata.getEventType());
//
//        Optional<Map<String, Object>> dateMap = eventMetadata.getFullObs().stream()
//            .filter(ob -> ob.getFieldCode().equals("date")).findFirst().map(EventConsumptionListener::extractData);
//
//        if (dateMap.isPresent()) {
//          dateForScopeDateFields = (String) dateMap.get().get("date");
//        }
//        switch (entityTag.getLookupEntityType().getCode()) {
//          case "Location":
//
//            List<Location> locations = new ArrayList<>();
//            if (ActionUtils.isActionForPerson(task.getAction())) {
//              Person person = personService.getPersonByIdentifier(eventMetadata.getBaseEntityId());
//              locations.addAll(person.getLocations());
//            } else if (ActionUtils.isActionForLocation(task.getAction())) {
//              Location location = locationService.findByIdentifier(eventMetadata.getBaseEntityId());
//              locations.add(location);
//            }
//            String finalDateForScopeDateFields;
//            if (entityTag.getScope().equals("Date")){
//              finalDateForScopeDateFields = dateForScopeDateFields;
//            } else {
//              finalDateForScopeDateFields = null;
//            }
//
//            for(Location location : locations) {
//              String tag = getTagKey(task, entityTag,eventMetadata, finalDateForScopeDateFields);
//
//              Object tagValue = metadata.get(eventMetadata.getObs().getFieldCode());
//
//              Object tagValueCasted = getCastedValue(formField, tagValue);
//
//              LocationMetadata locationMetadata = (LocationMetadata) metadataService.updateMetaData(
//                  eventMetadata.getBaseEntityId(), tagValueCasted,
//                  task.getAction().getGoal().getPlan(), eventMetadata.getTaskIdentifier(),
//                  eventMetadata.getUser(), entityTag.getValueType(), entityTag, "FormData",
//                  location, task.getAction().getTitle(), Location.class, tag,finalDateForScopeDateFields);
//
//              log.trace("###############Obs Field: {} entityTag: {} locationMetadata: {}",obs.getFieldCode(),entityTag.getTag(),locationMetadata.toString());
//
//
//              Set<EntityTag> referencedTags = entityTagService.findEntityTagsByReferencedTags(
//                  entityTag.getTag());
//
//              log.debug("###############Obs Field: {} referencedentityTag: {}",obs.getFieldCode(),
//                  referencedTags.stream().map(EntityTag::getTag).collect(Collectors.joining(",")));
//
//              LocationMetadata referenceMetadata = locationMetadata;
//              log.trace("is metadata obj null (outside loop)? {}",referenceMetadata==null);
//
//              for (EntityTag referencedTag : referencedTags) {
//                if (referencedTag.isGenerated()) {
//
//                  String generationFormula = referencedTag.getGenerationFormula();
//
//                  if (generationFormula != null) {
//
//                    LocationMetadataEvent locationMetadataEvent = LocationMetadataEventFactory.getLocationMetadataEvent(
//                        task.getAction().getGoal().getPlan(), location, referenceMetadata);
//
//
//                    log.trace("###############Obs Field: {} referencedentityTagItem: {} scope: {} locationMetadataEvent:{}",obs.getFieldCode(),
//                        referencedTag.getTag(),referencedTag.getScope(),locationMetadataEvent);
//
//                    String finalDateForReferencedScopeDateFields;
//                    if (referencedTag.getScope().equals("Date")){
//                      finalDateForReferencedScopeDateFields = dateForScopeDateFields;
//                    } else {
//                      finalDateForReferencedScopeDateFields = null;
//                    }
//
//                    log.trace("###############Obs Field: {} referencedentityTagItem: {} finalDateForReferencedScopeDateFields: {}",obs.getFieldCode(),
//                        referencedTag.getTag(),finalDateForReferencedScopeDateFields);
//
//                    log.trace("###############Obs Field: {} referencedentityTagItem: {}",obs.getFieldCode(),
//                        referencedTag.getTag());
//
//                    try {
//                      if (!referencedTag.getScope().equals("Date")
//                          || finalDateForReferencedScopeDateFields != null) {
//
//                        log.trace("###############Obs Field: success!!");
//
//                        Optional<EventTracker> eventTrackerRetrievedOptional;
//
//                        if (referencedTag.getScope().equals("Date")){
//                          eventTrackerRetrievedOptional = eventTrackerRepository.findFirstEventTrackerByEventIdentifierAndEntityTagIdentifierAndDate(eventMetadata.getEventId(),
//                              referencedTag.getIdentifier(), dateForScopeDateFields);
//                        } else{
//                          eventTrackerRetrievedOptional = eventTrackerRepository.findFirstEventTrackerByEventIdentifierAndEntityTagIdentifier(eventMetadata.getEventId(),
//                              referencedTag.getIdentifier());
//                        }
//                        String referencedTagKey = getTagKey(task, referencedTag, eventMetadata,dateForScopeDateFields);
//                        if (eventTrackerRetrievedOptional.isEmpty()) {
//
//                          Object o = updateMetaDataForGeneratedTags(
//                              referencedTag, locationMetadataEvent,
//                              eventMetadata, task, location, finalDateForReferencedScopeDateFields,
//                              referenceMetadata, referencedTagKey);
//
//                          if (o != null) {
//                            referenceMetadata = (LocationMetadata) o;
//                            log.trace("is metadata obj null ? {}", referenceMetadata == null);
//
//                            EventTracker eventTracker = EventTracker.builder()
//                                .eventIdentifier(eventMetadata.getEventId())
//                                .date(dateForScopeDateFields)
//                                .entityTagIdentifier(referencedTag.getIdentifier())
//                                .scope(referencedTag.getScope())
//                                .build();
//                            eventTracker.setEntityStatus(EntityStatus.ACTIVE);
//                            eventTrackerRepository.save(eventTracker);
//
//                            Optional<MetadataObj> metadataObjOptional = referenceMetadata.getEntityValue()
//                                .getMetadataObjs().stream().filter(
//                                    metadataObj -> metadataObj.getTagKey().equals(referencedTagKey))
//                                .findFirst();
//
//                            LocationMetadata finalReferenceMetadata = referenceMetadata;
//                            metadataObjOptional.ifPresent(metadataObj -> {
//                                  MetadataObjEvent metadataObjEvent = new MetadataObjEvent();
//                                  metadataObjEvent.setEntityId(
//                                      finalReferenceMetadata.getLocation().getIdentifier());
//                                  metadataObjEvent.setMetadataObj(metadataObj);
//                                  metadataObjEvent.setLocationGeographicLevel(
//                                      location.getGeographicLevel().getName());
//                                  metadataObjEvent.setLocationHierarchy(
//                                      task.getAction().getGoal().getPlan().getLocationHierarchy()
//                                          .getIdentifier());
//
//                                  kafkaTemplate.send(
//                                      kafkaProperties.getTopicMap()
//                                          .get(KafkaConstants.FORM_EVENT_CONSUMPTION),
//                                      metadataObjEvent);
//                                }
//                            );
//
//                          } else {
//                            log.debug("No change in metadata");
//                          }
//                        } else {
//                          log.debug("Not saving this metadata as it has been saved for this event");
//                        }
//
//                      }
//                    } catch (NoSuchMethodException e) {
//                      e.printStackTrace();
//                    }
//                  }
//                }
//              }
//
//            }
//            break;
//          case "Person":
////            List<Person> people = new ArrayList<>();
////            if (ActionUtils.isActionForPerson(task.getAction())) {
////              Person person = personService.getPersonByIdentifier(
////                  eventMetadata.getBaseEntityId());
////              people.add(person);
////            } else if (ActionUtils.isActionForLocation(task.getAction())) {
////              Location location = locationService.findByIdentifier(eventMetadata.getBaseEntityId());
////              List<Person> peopleByLocations = personService.getPeopleByLocations(
////                  List.of(location));
////              people.addAll(peopleByLocations);
////            }
////
////            String finalDateForScopeDateFields1 = dateForScopeDateFields;
////            people.forEach(person -> {
////              String tag = getTagKey(task, entityTag, eventMetadata, finalDateForScopeDateFields1);
////              Object tagValue = metadata.get(eventMetadata.getObs().getFieldCode());
////
////              Object tagValueCasted = getCastedValue(formField, tagValue);
////
////              PersonMetadata personMetadata = (PersonMetadata) metadataService.updateMetaData(
////                  eventMetadata.getBaseEntityId(),
////                  tagValueCasted,
////                  task.getAction().getGoal().getPlan(),
////                  eventMetadata.getTaskIdentifier(),
////                  eventMetadata.getUser(),
////                  entityTag.getValueType(),
////                  entityTag,
////                  "FormData",
////                  person,
////                  task.getAction().getTitle(), Person.class,
////                  tag, finalDateForScopeDateFields1);
////
////              Set<EntityTag> referencedTags = entityTagService.findEntityTagsByReferencedTags(
////                  entityTag.getTag());
////
////              PersonMetadata referenceMetadata = personMetadata;
////
////              for (EntityTag referencedTag : referencedTags) {
////                if (referencedTag.isGenerated()) {
////
////                  String generationFormula = referencedTag.getGenerationFormula();
////
////                  if (generationFormula != null) {
////
////                    PersonMetadataEvent personMetadataEvent = PersonMetadataEventFactory.getPersonMetadataEvent(
////                        task.getAction().getGoal().getPlan(), person.getLocations().stream().map(
////                            Location::getIdentifier).collect(
////                            Collectors.toList()), referenceMetadata);
////
////                    try {
////                      referenceMetadata = (PersonMetadata) updateMetaDataForGeneratedTags(referencedTag, personMetadataEvent,
////                          eventMetadata, task, person,finalDateForScopeDateFields1,referenceMetadata);
////                    } catch (NoSuchMethodException e) {
////                      e.printStackTrace();
////                    }
////                  }
////                }
////              }
////            });
//            break;
//          case "Plan":
//            //TODO: Plan Entity Type Metadata
//            break;
//        }
//      }
//    }
//  }
//
//  private Object getCastedValue(FormField formField, Object tagValue) {
//    Object tagValueCasted;
//    if (formField.getDataType().equals("integer")) {
//      tagValueCasted = Integer.valueOf((String) tagValue);
//    } else {
//      tagValueCasted = tagValue;
//    }
//    return tagValueCasted;
//  }
//
//  public Object updateMetaDataForGeneratedTags(EntityTag referencedTag, TMetadataEvent metadataEvent,
//      EventMetadata eventMetadata, Task task, Object entity, String dateForScopeDateFields, Object referenceMetadata, String referencedTagKey)
//      throws NoSuchMethodException {
//    if (referencedTag.isGenerated()) {
//
//      String generationFormula = referencedTag.getGenerationFormula();
//
//      log.trace("#############referenced tag {} generation formula: {} metadata: {}",referencedTag.getTag(), generationFormula, metadataEvent);
//
//      if ((Boolean) metadataExpressionEvaluationService.evaluateExpression(
//          generationFormula, TMetadataEvent.class, metadataEvent,
//          Boolean.class, dateForScopeDateFields)) {
//
//        log.trace("###########referenced tag {} successfully evaluated with date: {} ",referencedTag.getTag(),dateForScopeDateFields);
//
//        Object generatedValue = getGeneratedValue(referencedTag, metadataEvent, dateForScopeDateFields);
//
//        log.trace("###########referenced tag {} generatedValue {} ",referencedTag.getTag(), generatedValue);
//
//        Object o = metadataService.updateMetaData(
//            eventMetadata.getBaseEntityId(),
//            generatedValue,
//            task.getAction().getGoal().getPlan(),
//            eventMetadata.getTaskIdentifier(),
//            eventMetadata.getUser(),
//            referencedTag.getValueType(),
//            referencedTag,
//            "FormData",
//            entity,
//            task.getAction().getTitle(),
//            Location.class,
//            referencedTagKey,
//            dateForScopeDateFields);
//
//        log.trace("################referencedTag: {} locationMetadata: {}",referencedTag.getTag(),o);
//
//        return o;
//      }
//    }
//    return null;
//  }
//
//  private Object getGeneratedValue(EntityTag referencedTag, TMetadataEvent metadataEvent, String dateForScopeDateFields)
//      throws NoSuchMethodException {
//    Object generatedValue;
//    String valueType = referencedTag.getValueType();
//    String resultExpression = referencedTag.getResultExpression();
//    if (!referencedTag.isResultLiteral()) {
//
//      generatedValue = getGeneratedResult(metadataEvent, valueType,
//          resultExpression,dateForScopeDateFields);
//
//    } else {
//      generatedValue = resultExpression;
//    }
//    return generatedValue;
//  }
//
//  private String getTagKey(Task task, EntityTag referencedTag, EventMetadata eventMetadata, String dateForScopeDateFields) {
//    String referencedTagKey = null;
//    if (referencedTag.getScope().equals("Global")) {
//      referencedTagKey = referencedTag.getTag();
//    } else if (referencedTag.getScope().equals("Plan")) {
//      referencedTagKey =
//          referencedTag.getTag() + "_" + task.getAction().getGoal().getPlan().getIdentifier() + "_"
//              + task.getIdentifier();
//    } else if (referencedTag.getScope().equals("Event")){
//      referencedTagKey =
//          referencedTag.getTag() + "_" + task.getAction().getGoal().getPlan().getIdentifier() + "_"
//              + task.getIdentifier()+"_"+eventMetadata.getEventId();
//    } else if(referencedTag.getScope().equals("Date")){
//      referencedTagKey =
//          referencedTag.getTag() + "_" + task.getAction().getGoal().getPlan().getIdentifier() + "_"
//              + task.getIdentifier() + "_"+ dateForScopeDateFields; // + Date;
//    }
//    return referencedTagKey;
//  }
//
//  private Object getGeneratedResult(TMetadataEvent tMetadataEvent, String valueType,
//
//      String resultExpression, String dateForDateScoped) throws NoSuchMethodException {
//
//    log.trace("resultExpression: {} ",resultExpression);
//    Object generatedValue;
//    switch (valueType) {
//      case "string":
//        generatedValue =  metadataExpressionEvaluationService.evaluateExpression(
//            resultExpression, TMetadataEvent.class, tMetadataEvent,
//            String.class,dateForDateScoped);
//        break;
//      case "date":
//        generatedValue =  metadataExpressionEvaluationService.evaluateExpression(
//            resultExpression, TMetadataEvent.class, tMetadataEvent,
//            LocalDateTime.class,dateForDateScoped);
//        break;
//      case "boolean":
//        generatedValue =  metadataExpressionEvaluationService.evaluateExpression(
//            resultExpression, TMetadataEvent.class, tMetadataEvent,
//            Boolean.class,dateForDateScoped);
//        break;
//      case "double":
//        generatedValue =  metadataExpressionEvaluationService.evaluateExpression(
//            resultExpression, TMetadataEvent.class, tMetadataEvent,
//            Double.class,dateForDateScoped);
//        break;
//      case "integer":
//        generatedValue =  metadataExpressionEvaluationService.evaluateExpression(
//            resultExpression, TMetadataEvent.class, tMetadataEvent,
//            Integer.class,dateForDateScoped);
//        break;
//      default:
//        generatedValue = metadataExpressionEvaluationService.evaluateExpression(
//            resultExpression, TMetadataEvent.class, tMetadataEvent,
//            Integer.class,dateForDateScoped);
//    }
//    return generatedValue;
//  }
//
//  public static Map<String, Object> extractData(Obs obs) { //TODO:
//    Map<String, Object> response = new HashMap<>();
//    if (obs.getKeyValPairs() == null) {
//      response.put(obs.getFieldCode(), obs.getValues().get(0));
//    } else {
//      response.put(obs.getFieldCode(), obs.getKeyValPairs().get(obs.getValues().get(0)));
//    }
//    return response;
//  }


}
