package com.revealprecision.revealserver.service;

import static com.revealprecision.revealserver.constants.EntityTagDataTypes.BOOLEAN;
import static com.revealprecision.revealserver.constants.EntityTagDataTypes.DOUBLE;
import static com.revealprecision.revealserver.constants.EntityTagDataTypes.INTEGER;
import static com.revealprecision.revealserver.constants.EntityTagDataTypes.STRING;

import com.revealprecision.revealserver.api.v1.dto.factory.LocationMetadataImportFactory;
import com.revealprecision.revealserver.api.v1.dto.factory.MetadataImportResponseFactory;
import com.revealprecision.revealserver.api.v1.dto.response.LocationMetadataImport;
import com.revealprecision.revealserver.api.v1.dto.response.MetadataFileImportResponse;
import com.revealprecision.revealserver.constants.EntityTagScopes;
import com.revealprecision.revealserver.constants.KafkaConstants;
import com.revealprecision.revealserver.enums.BulkEntryStatus;
import com.revealprecision.revealserver.enums.EntityStatus;
import com.revealprecision.revealserver.exceptions.FileFormatException;
import com.revealprecision.revealserver.exceptions.NotFoundException;
import com.revealprecision.revealserver.messaging.message.EntityTagEvent;
import com.revealprecision.revealserver.messaging.message.LocationIdEvent;
import com.revealprecision.revealserver.persistence.domain.EntityTag;
import com.revealprecision.revealserver.persistence.domain.LookupEntityType;
import com.revealprecision.revealserver.persistence.domain.MetadataImport;
import com.revealprecision.revealserver.persistence.domain.User;
import com.revealprecision.revealserver.persistence.domain.aggregation.ImportAggregationNumeric;
import com.revealprecision.revealserver.persistence.domain.aggregation.ImportAggregationString;
import com.revealprecision.revealserver.persistence.domain.metadata.LocationMetadata;
import com.revealprecision.revealserver.persistence.domain.metadata.PersonMetadata;
import com.revealprecision.revealserver.persistence.domain.metadata.SaveHierarchyMetadata;
import com.revealprecision.revealserver.persistence.domain.metadata.metadataImport.MetaImportDTO;
import com.revealprecision.revealserver.persistence.domain.metadata.metadataImport.fieldMapper.MetaFieldSetMapper;
import com.revealprecision.revealserver.persistence.repository.ImportAggregationNumericRepository;
import com.revealprecision.revealserver.persistence.repository.ImportAggregationStringRepository;
import com.revealprecision.revealserver.persistence.repository.LocationMetadataRepository;
import com.revealprecision.revealserver.persistence.repository.MetadataImportRepository;
import com.revealprecision.revealserver.persistence.repository.PersonMetadataRepository;
import com.revealprecision.revealserver.props.ImportAggregationProperties;
import com.revealprecision.revealserver.props.KafkaProperties;
import com.revealprecision.revealserver.util.UserUtils;
import java.io.IOException;
import java.security.Principal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.keycloak.KeycloakPrincipal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.util.Pair;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class MetadataService {

  private final LocationMetadataRepository locationMetadataRepository;
  private final PersonMetadataRepository personMetadataRepository;
  private final PublisherService publisherService;
  private final KafkaProperties kafkaProperties;
  private final LocationService locationService;
  private final MetadataImportRepository metadataImportRepository;
  private final UserService userService;

  private final MetaFieldSetMapper metaFieldSetMapper;
  private final LocationRelationshipService locationRelationshipService;

  private final ImportAggregationNumericRepository importAggregationNumericRepository;
  private final ImportAggregationStringRepository importAggregationStringRepository;

  private final EntityTagService entityTagService;
  private final ImportAggregationProperties importAggregationProperties;

  private final LookupEntityTypeService lookupEntityTypeService;

  public LocationMetadata getLocationMetadataByLocation(UUID locationIdentifier) {
    //TODO fix this
    return locationMetadataRepository.findLocationMetadataByLocation_Identifier(locationIdentifier)
        .orElse(null);
  }

  public PersonMetadata getPersonMetadataByPerson(UUID personIdentifier) {
    return personMetadataRepository.findPersonMetadataByPerson_Identifier(personIdentifier)
        .orElse(null);
  }

//  public Object updateMetaData(UUID identifier, Object tagValue, Plan plan, UUID taskIdentifier,
//      String user, String dataType, EntityTagEvent tag, String type, Object entity, String taskType,
//      Class<?> aClass, String tagKey, String finalDateForScopeDateFields1) {
//    if (aClass == Person.class) {
//      return updatePersonMetadata(identifier, tagValue, plan, taskIdentifier, user, dataType, tag,
//          type, (Person) entity, taskType, tagKey, finalDateForScopeDateFields1);
//    } else if (aClass == Location.class) {
//      return updateLocationMetadata(identifier, tagValue, plan, taskIdentifier, user, dataType, tag,
//          type, (Location) entity, taskType, tagKey, finalDateForScopeDateFields1);
//    }
//    return null;
//  }

//  @Transactional
//  public PersonMetadata updatePersonMetadata(UUID personIdentifier, Object tagValue, Plan plan,
//      UUID taskIdentifier, String user, String dataType, EntityTagEvent tag, String type,
//      Person person, String taskType, String tagKey, String dateForScopeDateFields) {
//
//    PersonMetadata personMetadata;
//
//    Optional<PersonMetadata> optionalPersonMetadata = personMetadataRepository.findPersonMetadataByPerson_Identifier(
//        personIdentifier);
//    if (optionalPersonMetadata.isPresent()) {
//
//      OptionalInt optionalArrIndex = IntStream.range(0,
//          optionalPersonMetadata.get().getEntityValue().getMetadataObjs().size()).filter(
//          i -> optionalPersonMetadata.get().getEntityValue().getMetadataObjs().get(i).getTag()
//              .equals(tag.getTag())).findFirst();
//
//      if (optionalArrIndex.isPresent()) {
//        personMetadata = optionalPersonMetadata.get();
//
//        int arrIndex = optionalArrIndex.getAsInt();
//        //TODO: Add history
//
//        personMetadata.getEntityValue().getMetadataObjs().get(arrIndex).getCurrent().setValue(
//            getTagValue(tagValue, dataType,
//                personMetadata.getEntityValue().getMetadataObjs().get(arrIndex).getCurrent()
//                    .getValue()));
//        personMetadata.getEntityValue().getMetadataObjs().get(arrIndex).getCurrent().getMeta()
//            .setUpdateDateTime(LocalDateTime.now());
//        personMetadata.getEntityValue().getMetadataObjs().get(arrIndex).getCurrent().getMeta()
//            .setUserId(user);
//        personMetadata.getEntityValue().getMetadataObjs().get(arrIndex).getCurrent().getMeta()
//            .setTaskType(taskType);
//
//        //TODO: Add history
//
//      } else {
//        // tag does not exist in list
//        MetadataObj metadataObj = getMetadataObj(tagValue,
//            plan == null ? null : plan.getIdentifier(), taskIdentifier, user, dataType, tag, type,
//            taskType, tagKey, dateForScopeDateFields);
//
//        personMetadata = optionalPersonMetadata.get();
//        List<MetadataObj> metadataObjs = new ArrayList<>(
//            personMetadata.getEntityValue().getMetadataObjs());
//        metadataObjs.add(metadataObj);
//        personMetadata.getEntityValue().setMetadataObjs(metadataObjs);
//
//      }
//    } else {
//      //person metadata does not exist
//      personMetadata = new PersonMetadata();
//      //TODO: check this
//      personMetadata.setPerson(person);
//
//      MetadataObj metadataObj = getMetadataObj(tagValue, plan == null ? null : plan.getIdentifier(),
//          taskIdentifier, user, dataType, tag, type, taskType, tagKey, dateForScopeDateFields);
//
//      MetadataList metadataList = new MetadataList();
//      metadataList.setMetadataObjs(List.of(metadataObj));
//      personMetadata.setEntityValue(metadataList);
//      personMetadata.setEntityStatus(EntityStatus.ACTIVE);
//    }
//
//    PersonMetadata savedPersonMetadata = personMetadataRepository.save(personMetadata);
//
//    List<UUID> locationList = locationService.getLocationsByPeople(person.getIdentifier()).stream()
//        .map(Location::getIdentifier).collect(Collectors.toList());
//
//    PersonMetadataEvent personMetadataEvent = PersonMetadataEventFactory.getPersonMetadataEvent(
//        plan, locationList, savedPersonMetadata);
//
//    publisherService.send(
//        kafkaProperties.getTopicMap().get(KafkaConstants.PERSON_METADATA_UPDATE),
//        personMetadataEvent);
//
//    return savedPersonMetadata;
//  }
//
//
//  public LocationMetadata updateLocationMetadata(UUID locationIdentifier, Object tagValue,
//      Plan plan, UUID taskIdentifier, String user, String dataType,
//      EntityTagEvent locationEntityTag, String type, Location location, String taskType,
//      String tagKey, String dateForScopeDateFields) {
//
//    LocationMetadata locationMetadata;
//
//    Optional<LocationMetadata> locationMetadataOptional = locationMetadataRepository.findLocationMetadataByLocation_Identifier(
//        locationIdentifier);
//    if (locationMetadataOptional.isPresent()) {
//
//      OptionalInt optionalArrIndex = OptionalInt.empty();
//      if (locationEntityTag.getScope() == null || (locationEntityTag.getScope() != null
//          && !locationEntityTag.getScope().equals("Date"))) {
//        optionalArrIndex = IntStream.range(0,
//            locationMetadataOptional.get().getEntityValue().getMetadataObjs().size()).filter(
//            i -> locationMetadataOptional.get().getEntityValue().getMetadataObjs().get(i).getTag()
//                .equals(locationEntityTag.getTag())).findFirst();
//      } else {
//        optionalArrIndex = IntStream.range(0,
//                locationMetadataOptional.get().getEntityValue().getMetadataObjs().size()).filter(i ->
//                locationMetadataOptional.get().getEntityValue().getMetadataObjs().get(i).getTag()
//                    .equals(locationEntityTag.getTag()) && (
//                    !locationMetadataOptional.get().getEntityValue().getMetadataObjs().get(i)
//                        .isDateScope() || locationMetadataOptional.get().getEntityValue()
//                        .getMetadataObjs().get(i).getDateForDateScope().equals(dateForScopeDateFields)))
//            .findFirst();
//      }
//      if (optionalArrIndex.isPresent()) {
//        //tag exists
//        locationMetadata = locationMetadataOptional.get();
//
//        int arrIndex = optionalArrIndex.getAsInt();
//        //TODO: Add history
//
//        locationMetadata.getEntityValue().getMetadataObjs().get(arrIndex).getCurrent().setValue(
//            getTagValue(tagValue, dataType,
//                locationMetadata.getEntityValue().getMetadataObjs().get(arrIndex).getCurrent()
//                    .getValue()));
//        locationMetadata.getEntityValue().getMetadataObjs().get(arrIndex).getCurrent().getMeta()
//            .setUpdateDateTime(LocalDateTime.now());
//        locationMetadata.getEntityValue().getMetadataObjs().get(arrIndex).getCurrent().getMeta()
//            .setUserId(user);
//        locationMetadata.getEntityValue().getMetadataObjs().get(arrIndex).getCurrent().getMeta()
//            .setTaskType(taskType);
//
//        locationMetadata.getEntityValue().getMetadataObjs().get(arrIndex).setActive(true);
//        if (locationEntityTag.getScope().equals("Date")) {
//          if (dateForScopeDateFields != null) {
//            locationMetadata.getEntityValue().getMetadataObjs().get(arrIndex)
//                .setDateForDateScope(dateForScopeDateFields);
//            locationMetadata.getEntityValue().getMetadataObjs().get(arrIndex).setDateScope(true);
//            locationMetadata.getEntityValue().getMetadataObjs().get(arrIndex).setCaptureNumber(
//                LocalDate.parse(dateForScopeDateFields, DateTimeFormatter.ISO_LOCAL_DATE)
//                    .toEpochDay());
//          }
//        }
//
//        //TODO: Add history
//
//      } else {
//        // tag does not exist in list
//        MetadataObj metadataObj = getMetadataObj(tagValue,
//            plan == null ? null : plan.getIdentifier(), taskIdentifier, user, dataType,
//            locationEntityTag, type, taskType, tagKey, dateForScopeDateFields);
//
//        locationMetadata = locationMetadataOptional.get();
//        List<MetadataObj> temp = new ArrayList<>(
//            locationMetadata.getEntityValue().getMetadataObjs());
//        temp.add(metadataObj);
//        locationMetadata.getEntityValue().setMetadataObjs(temp);
//
//      }
//    } else {
//      //location metadata does not exist
//      locationMetadata = new LocationMetadata();
//      //TODO: check this
//
//      location.setIdentifier(locationIdentifier);
//      locationMetadata.setLocation(location);
//
//      MetadataObj metadataObj = getMetadataObj(tagValue, plan == null ? null : plan.getIdentifier(),
//          taskIdentifier, user, dataType, locationEntityTag, type, taskType, tagKey,
//          dateForScopeDateFields);
//
//      MetadataList metadataList = new MetadataList();
//      metadataList.setMetadataObjs(List.of(metadataObj));
//      locationMetadata.setEntityValue(metadataList);
//
//      locationMetadata.setEntityStatus(EntityStatus.ACTIVE);
//    }
//    LocationMetadata savedLocationMetadata = locationMetadataRepository.save(locationMetadata);
//
//    LocationMetadataEvent locationMetadataEvent = LocationMetadataEventFactory.getLocationMetadataEvent(
//        plan, location, savedLocationMetadata);
//
//    publisherService.send(
//        kafkaProperties.getTopicMap().get(KafkaConstants.LOCATION_METADATA_UPDATE),
//        locationMetadataEvent);
//    return savedLocationMetadata;
//  }

//  public LocationMetadata activateOrDeactivateLocationMetadata(UUID locationIdentifier,
//      EntityTag tag, Plan plan, boolean activate) {
//
//    LocationMetadata locationMetadata;
//
//    Optional<LocationMetadata> locationMetadataOptional = locationMetadataRepository.findLocationMetadataByLocation_Identifier(
//        locationIdentifier);
//    if (locationMetadataOptional.isPresent()) {
//
//      OptionalInt optionalArrIndex = IntStream.range(0,
//          locationMetadataOptional.get().getEntityValue().getMetadataObjs().size()).filter(
//          i -> locationMetadataOptional.get().getEntityValue().getMetadataObjs().get(i).getTag()
//              .equals(tag.getTag())).findFirst();
//
//      if (optionalArrIndex.isPresent()) {
//        //tag exists
//        locationMetadata = locationMetadataOptional.get();
//
//        int arrIndex = optionalArrIndex.getAsInt();
//        TagData oldObj = SerializationUtils.clone(
//            locationMetadataOptional.get().getEntityValue().getMetadataObjs().get(arrIndex)
//                .getCurrent());
//
//        locationMetadata.getEntityValue().getMetadataObjs().get(arrIndex).setActive(activate);
//
//        if (locationMetadata.getEntityValue().getMetadataObjs().get(arrIndex).getHistory()
//            != null) {
//          locationMetadata.getEntityValue().getMetadataObjs().get(arrIndex).getHistory()
//              .add(oldObj);
//        } else {
//          locationMetadata.getEntityValue().getMetadataObjs().get(arrIndex)
//              .setHistory(List.of(oldObj));
//        }
//
//        LocationMetadata savedLocationMetadata = locationMetadataRepository.save(locationMetadata);
//
//        LocationMetadataEvent locationMetadataEvent = LocationMetadataEventFactory.getLocationMetadataEvent(
//            plan, null, savedLocationMetadata);
//
//        publisherService.send(
//            kafkaProperties.getTopicMap().get(KafkaConstants.LOCATION_METADATA_UPDATE),
//            locationMetadataEvent);
//        return savedLocationMetadata;
//      } else {
//        // tag does not exist in list
//        log.info("tag {} not present...ignoring ", tag);
//      }
//    } else {
//      //location metadata does not exist
//      log.info("no metadata for entity {} not present...ignoring ", locationIdentifier);
//    }
//
//    return null;
//  }
//
//
//  public PersonMetadata activateOrDeactivatePersonMetadata(UUID locationIdentifier, EntityTag tag,
//      Plan plan, boolean activate) {
//
//    PersonMetadata personMetadata;
//
//    Optional<PersonMetadata> personMetadataOptional = personMetadataRepository.findPersonMetadataByPerson_Identifier(
//        locationIdentifier);
//    if (personMetadataOptional.isPresent()) {
//
//      OptionalInt optionalArrIndex = IntStream.range(0,
//          personMetadataOptional.get().getEntityValue().getMetadataObjs().size()).filter(
//          i -> personMetadataOptional.get().getEntityValue().getMetadataObjs().get(i).getTag()
//              .equals(tag.getTag())).findFirst();
//
//      if (optionalArrIndex.isPresent()) {
//        //tag exists
//        personMetadata = personMetadataOptional.get();
//
//        int arrIndex = optionalArrIndex.getAsInt();
//        TagData oldObj = SerializationUtils.clone(
//            personMetadataOptional.get().getEntityValue().getMetadataObjs().get(arrIndex)
//                .getCurrent());
//
//        personMetadata.getEntityValue().getMetadataObjs().get(arrIndex).setActive(activate);
//
//        if (personMetadata.getEntityValue().getMetadataObjs().get(arrIndex).getHistory() != null) {
//          personMetadata.getEntityValue().getMetadataObjs().get(arrIndex).getHistory().add(oldObj);
//        } else {
//          personMetadata.getEntityValue().getMetadataObjs().get(arrIndex)
//              .setHistory(List.of(oldObj));
//        }
//
//        PersonMetadata savedPersonMetadata = personMetadataRepository.save(personMetadata);
//
//        List<Location> locationsByPeople = locationService.getLocationsByPeople(
//            personMetadata.getPerson().getIdentifier());
//
//        PersonMetadataEvent personMetadataEvent = PersonMetadataEventFactory.getPersonMetadataEvent(
//            plan,
//            locationsByPeople.stream().map(Location::getIdentifier).collect(Collectors.toList()),
//            savedPersonMetadata);
//
//        publisherService.send(
//            kafkaProperties.getTopicMap().get(KafkaConstants.PERSON_METADATA_UPDATE),
//            personMetadataEvent);
//        return savedPersonMetadata;
//      } else {
//        // tag does not exist in list
//        log.info("tag {} not present...ignoring ", tag);
//      }
//    } else {
//      //location metadata does not exist
//      log.info("no metadata for entity {} not present...ignoring ", locationIdentifier);
//    }
//
//    return null;
//  }

//  private MetadataObj getMetadataObj(Object tagValue, UUID planIdentifier, UUID taskIdentifier,
//      String user, String dataType, EntityTagEvent tag, String type, String taskType, String tagKey,
//      String dateForScopeDateFields) {
//    Metadata metadata = new Metadata();
//    metadata.setPlanId(planIdentifier);
//    metadata.setTaskId(taskIdentifier);
//    metadata.setTaskType(taskType);
//    metadata.setCreateDateTime(LocalDateTime.now());
//    metadata.setUpdateDateTime(LocalDateTime.now());
//    metadata.setUserId(user);
//
//    TagValue value = getTagValue(tagValue, dataType, new TagValue());
//
//    TagData tagData = new TagData();
//    tagData.setMeta(metadata);
//    tagData.setValue(value);
//
//    MetadataObj metadataObj = new MetadataObj();
//    metadataObj.setDataType(dataType);
//    metadataObj.setTag(tag.getTag());
//    metadataObj.setType(type);
//    metadataObj.setEntityTagId(tag.getIdentifier());
//    metadataObj.setCurrent(tagData);
//    metadataObj.setActive(true);
//    metadataObj.setTagKey(tagKey);
//
//    if (tag.getScope().equals(EntityTagScopes.DATE)) {
//      if (dateForScopeDateFields != null) {
//        metadataObj.setDateForDateScope(dateForScopeDateFields);
//        metadataObj.setDateScope(true);
//        metadataObj.setCaptureNumber(
//            LocalDate.parse(dateForScopeDateFields, DateTimeFormatter.ISO_LOCAL_DATE).toEpochDay());
//      }
//    }
//
//    return metadataObj;
//  }
//
//  public static Pair<Class, Object> getValueFromValueObject(MetadataObj metadataObj) {
//    switch (metadataObj.getDataType()) {
//      case STRING:
//        return Pair.of(String.class, metadataObj.getCurrent().getValue().getValueString());
//
//      case INTEGER:
//        return Pair.of(Integer.class, metadataObj.getCurrent().getValue().getValueInteger());
//
//      case DATE:
//        return Pair.of(LocalDateTime.class, metadataObj.getCurrent().getValue().getValueDate());
//
//      case DOUBLE:
//        return Pair.of(Double.class, metadataObj.getCurrent().getValue().getValueDouble());
//
//      case BOOLEAN:
//        return Pair.of(Boolean.class, metadataObj.getCurrent().getValue().getValueBoolean());
//
//      default:
//        return Pair.of(String.class, metadataObj.getCurrent().getValue().getValueString());
//
//    }
//  }
//
//
//  private TagValue getTagValue(Object tagValue, String dataType, TagValue value) {
//    switch (dataType) {
//      case STRING:
//        value.setValueString((String) tagValue);
//        break;
//      case INTEGER:
//        value.setValueInteger((Integer) tagValue);
//        break;
//      case DATE:
//        value.setValueDate((LocalDateTime) tagValue);
//        break;
//      case DOUBLE:
//        value.setValueDouble(tagValue instanceof String ? Double.valueOf((String) tagValue)
//            : tagValue instanceof Integer ? ((Integer) tagValue).doubleValue() : (Double) tagValue);
//        break;
//      case BOOLEAN:
//        value.setValueBoolean((Boolean) tagValue);
//        break;
//      case OBJECT:
//        value.getValueObjects().add(tagValue);
//        break;
//      default:
//        value.setValueString((String) tagValue);
//        break;
//    }
//    return value;
//  }

  public UUID saveImportFile(String file, String fileName) throws FileFormatException, IOException {

    MetadataImport metadataImport = new MetadataImport();
    metadataImport.setFilename(fileName);
    metadataImport.setEntityStatus(EntityStatus.ACTIVE);
    metadataImport.setUploadedDatetime(LocalDateTime.now());

    Principal principal = UserUtils.getCurrentPrinciple();
    User user;
    UUID keycloakId = null;
    if (principal instanceof KeycloakPrincipal) {
      keycloakId = UUID.fromString(principal.getName());
    }
    user = userService.getByKeycloakId(keycloakId);
    metadataImport.setUploadedBy(user.getUsername());
    MetadataImport currentMetaImport = metadataImportRepository.save(metadataImport);

    try (XSSFWorkbook workbook = new XSSFWorkbook(file)) {
      XSSFSheet sheet = workbook.getSheetAt(0);

      List<MetaImportDTO> metaImportDTOS = metaFieldSetMapper.mapMetaFieldsDB(sheet);

      if (metaImportDTOS.stream().map(metaImportDTO -> metaImportDTO.getSheetData().getErrors())
          .map(Map::size).reduce(0, Integer::sum) > 1) {
        throw new FileFormatException("Invalid file errors with: " + metaImportDTOS.stream()
            .map(metaImportDTO -> metaImportDTO.getSheetData().getErrors())
            .flatMap(error -> error.entrySet().stream())
            .map(entry -> "tag: " + entry.getKey().getTag() + "value: " + entry.getValue())
            .collect(Collectors.joining("\r\n")));
      }
      Map<String, List<String>> ancestryMap = locationRelationshipService.getAncestryMap(
          metaImportDTOS.stream()
              .map(metaImportDTO -> metaImportDTO.getLocation().getIdentifier().toString())
              .collect(Collectors.toList()));

      currentMetaImport.setStatus(BulkEntryStatus.BUSY);
      UUID identifier = metadataImportRepository.save(currentMetaImport).getIdentifier();
      //send data to kafka listener
      if (!metaImportDTOS.isEmpty()) {
        saveToDB(metaImportDTOS, ancestryMap, currentMetaImport);
        publishToMessagingGen(metaImportDTOS.stream().map(
                metaImportDTO -> new SaveHierarchyMetadata(
                    metaImportDTO.getLocation().getIdentifier().toString(),
                    metaImportDTO.getLocationHierarchy().getIdentifier().toString(),
                    metaImportDTO.getLocationHierarchy().getNodeOrder())).collect(Collectors.toList()),
            ancestryMap);
      }

      return identifier;
    } catch (Exception e) {
      log.error(e.getMessage(), e);
      currentMetaImport.setStatus(BulkEntryStatus.FAILED);
      metadataImportRepository.save(currentMetaImport);
      throw new FileFormatException(e.getMessage());
    }

  }

//  private void publishToMessaging(List<MetaImportDTO> metaImportDTOS,
//      Map<String, List<String>> ancestryMap) {
//    metaImportDTOS.forEach(metaImportDTO ->
//        ancestryMap.get(metaImportDTO.getLocation().getIdentifier().toString())
//            .forEach(ancestor -> publisherService.send(
//                kafkaProperties.getTopicMap().get(KafkaConstants.AGGREGATION_STAGING),
//                LocationIdEvent.builder()
//                    .hierarchyIdentifier(metaImportDTO.getLocationHierarchy().getIdentifier())
//                    .nodeOrder(metaImportDTO.getLocationHierarchy().getNodeOrder().stream().collect(
//                        Collectors.joining(",")))
//                    .uuids(List.of(UUID.fromString(ancestor)))
//                    .build())
//            ));
//  }

  public void publishToMessagingGen(List<SaveHierarchyMetadata> saveHierarchyMetadatas,
      Map<String, List<String>> ancestryMap) {
    saveHierarchyMetadatas.forEach(saveHierarchyMetadata ->
        ancestryMap.get(saveHierarchyMetadata.getLocationIdentifier())
            .forEach(ancestor -> publisherService.send(
                kafkaProperties.getTopicMap().get(KafkaConstants.AGGREGATION_STAGING),
                LocationIdEvent.builder()
                    .hierarchyIdentifier(saveHierarchyMetadata.getHierarchyIdentifier())
                    .nodeOrder(String.join(",", saveHierarchyMetadata.getNodeOrder()))
                    .uuids(List.of(UUID.fromString(ancestor)))
                    .build())
            ));
  }

  @Async
  void saveToDB(List<MetaImportDTO> metaImportDTOS,
      Map<String, List<String>> ancestryMap, MetadataImport currentMetaImport) {
    currentMetaImport.setStatus(BulkEntryStatus.BUSY);
    metadataImportRepository.save(currentMetaImport);

    metaImportDTOS.forEach(metaImportDTO ->
        ancestryMap.get(metaImportDTO.getLocation().getIdentifier().toString())
            .forEach(ancestor ->
                metaImportDTO.getSheetData().getConvertedEntityData().forEach(
                    (key, value) ->
                        updateDB(metaImportDTO.getLocation().getName(), ancestor, value,
                            key.getTag(), key.getValueType(),
                            metaImportDTO.getLocationHierarchy().getIdentifier().toString())
                )));

    List<Entry<EntityTagEvent, Object>> string = metaImportDTOS.stream().flatMap(
            metaImportDTO -> metaImportDTO.getSheetData().getConvertedEntityData().entrySet().stream()
                .filter(entry -> entry.getKey().getValueType().equals("string") || entry.getKey()
                    .getValueType().equals("boolean")))
        .collect(Collectors.toList());

    Set<EntityTag> collect1 = getStringOrBooleanTagsGeneratedFromImportData(
        string);

    entityTagService.saveEntityTags(collect1);

    currentMetaImport.setStatus(BulkEntryStatus.SUCCESSFUL);
    metadataImportRepository.save(currentMetaImport);
  }

  private Set<EntityTag> getStringOrBooleanTagsGeneratedFromImportData(
      List<Entry<EntityTagEvent, Object>> string) {
    Map<LookupEntityType, List<String>> collect = string.stream()
        .flatMap(entityTagEventObjectEntry ->
            entityTagEventObjectEntry.getKey().getAggregationMethod().stream().map(method ->
                Pair.of(entityTagEventObjectEntry.getKey().getTag()
                        .concat(importAggregationProperties.getDelim())
                        .concat(String.valueOf(entityTagEventObjectEntry.getValue()))
                        .concat(importAggregationProperties.getDelim())
                        .concat(method),
                    lookupEntityTypeService.getAllLookupEntities()
                        .get(entityTagEventObjectEntry.getKey().getLookupEntityType().getCode()))
            ))
        .collect(Collectors.groupingBy(Pair::getSecond,
            Collectors.mapping(Pair::getFirst, Collectors.toList())));

    Set<EntityTag> collect1 = collect.entrySet().stream().flatMap(
        entry -> {

          List<String> entityTagsByTagNameAndLookupEntityType = entityTagService.getEntityTagsByTagNameAndLookupEntityType(
              entry.getValue(),
              entry.getKey()).stream().map(EntityTag::getTag).collect(Collectors.toList());
          return entry.getValue().stream()
              .filter(tag -> !entityTagsByTagNameAndLookupEntityType.contains(tag))
              .map(s -> {
                EntityTag number = EntityTag.builder()
                    .addToMetadata(true)
                    .isAggregate(true)
                    .scope(EntityTagScopes.GLOBAL)
                    .simulationDisplay(false)
                    .valueType(DOUBLE)
                    .lookupEntityType(entry.getKey())
                    .tag(s)
                    .build();
                number.setEntityStatus(EntityStatus.ACTIVE);
                return number;
              });
        }).collect(
        Collectors.toCollection(() -> new TreeSet<>(Comparator.comparing(EntityTag::getTag))));
    return collect1;
  }

//  private void update(User user, MetadataImport currentMetaImport, MetaImportDTO metaImportDTO,
//      Location loc, Object importEntityTagValue, EntityTagEvent entityTagEvent) {
//    try {
//
//      LocationMetadata locationMetadata = (LocationMetadata) updateMetaData(
//          metaImportDTO.getLocation().getIdentifier(), importEntityTagValue, null, null,
//          user.getIdentifier().toString(), entityTagEvent.getValueType(), entityTagEvent,
//          "ImportData", loc, "File import", Location.class, entityTagEvent.getTag(), null);
//
//      LocationMetadataEvent locationMetadataEvent = LocationMetadataEventFactory.getLocationMetadataEvent(
//          null, locationMetadata.getLocation(), locationMetadata);
//
//      FormDataEntityTagValueEvent entity = FormDataEntityTagValueEventFactory.getEntity(null,
//          metaImportDTO.getLocationHierarchy().getIdentifier(), loc.getGeographicLevel().getName(),
//          null, null, null, loc, entityTagEvent, importEntityTagValue, null);
//
//      publisherService.send(
//          kafkaProperties.getTopicMap().get(KafkaConstants.FORM_EVENT_CONSUMPTION),
//          entity);
//
//      // check if there is an existing metadata event already created
//      // than just update the metaDataEvent list instead of creating a duplicate
//      boolean shouldAdd = true;
//      for (LocationMetadataEvent l : currentMetaImport.getLocationMetadataEvents()) {
//        if (l.getIdentifier().equals(locationMetadataEvent.getIdentifier())) {
//          shouldAdd = false;
//          l.setMetaDataEvents(locationMetadataEvent.getMetaDataEvents());
//        }
//      }
//
//      if (shouldAdd) {
//        currentMetaImport.getLocationMetadataEvents().add(locationMetadataEvent);
//      }
//
//      metadataImportRepository.save(currentMetaImport);
//    } catch (Exception e) {
//      //TODO: Need to handle import exceptions here and save them to the table
//      log.error(e.getMessage(), e);
//    }
//  }

  public void updateDB(String name, String locId, Object importEntityTagValue, String tag,
      String type, String hierarchyIdentifier) {
    try {
      switch (type) {
        case STRING:
        case BOOLEAN:
          Optional<ImportAggregationString> importAggregationStringOptional = importAggregationStringRepository.findByNameAndAncestorAndFieldCodeAndHierarchyIdentifier(
              name, locId, tag, hierarchyIdentifier);
          ImportAggregationString importAggregationString;
          if (importAggregationStringOptional.isPresent()) {
            importAggregationString = importAggregationStringOptional.get();
            importAggregationString.setVal((String) importEntityTagValue);
          } else {
            importAggregationString = ImportAggregationString.builder()
                .val((String) importEntityTagValue).ancestor(locId).eventType("Import")
                .fieldCode(tag)
                .hierarchyIdentifier(hierarchyIdentifier)
                .name(name).planIdentifier(null).build();
          }
          importAggregationStringRepository.save(importAggregationString);

          break;
        case DOUBLE:
        case INTEGER:
          Optional<ImportAggregationNumeric> importAggregationNumericOptional = importAggregationNumericRepository.findByNameAndAncestorAndFieldCodeAndHierarchyIdentifier(
              name, locId, tag, hierarchyIdentifier);
          ImportAggregationNumeric importAggregationNumeric;
          if (importAggregationNumericOptional.isPresent()) {
            importAggregationNumeric = importAggregationNumericOptional.get();
            importAggregationNumeric.setVal((Double) importEntityTagValue);
          } else {
            importAggregationNumeric = ImportAggregationNumeric.builder()
                .val((Double) importEntityTagValue).ancestor(locId)
                .hierarchyIdentifier(hierarchyIdentifier)
                .eventType("Import").fieldCode(tag).name(name).planIdentifier(null).build();
          }

          importAggregationNumericRepository.save(importAggregationNumeric);
          break;
      }
    } catch (Exception e) {
      //TODO: Need to handle import exceptions here and save them to the table
      log.error(e.getMessage(), e);
    }
  }

  public Page<MetadataFileImportResponse> getMetadataImportList(Pageable pageable) {
    return MetadataImportResponseFactory.fromEntityPage(metadataImportRepository.findAll(pageable),
        pageable);
  }

  public List<LocationMetadataImport> getMetadataImportDetails(UUID metaImportIdentifier) {
    Optional<MetadataImport> metadataImport = metadataImportRepository.findById(
        metaImportIdentifier);
    if (metadataImport.isPresent()) {
      List<LocationMetadataImport> locationMetadataImports = new ArrayList<>();
      metadataImport.get().getLocationMetadataEvents().forEach(el -> {
        //TODO: create a custom DTO for MetaDataEvent
        locationMetadataImports.add(LocationMetadataImportFactory.fromEntity(el));
      });
      return locationMetadataImports;
    } else {
      throw new NotFoundException("MetaImport not found.");
    }
  }


}

