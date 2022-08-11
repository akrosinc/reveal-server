package com.revealprecision.revealserver.service;

import static com.revealprecision.revealserver.constants.EntityTagDataTypes.BOOLEAN;
import static com.revealprecision.revealserver.constants.EntityTagDataTypes.DATE;
import static com.revealprecision.revealserver.constants.EntityTagDataTypes.DOUBLE;
import static com.revealprecision.revealserver.constants.EntityTagDataTypes.INTEGER;
import static com.revealprecision.revealserver.constants.EntityTagDataTypes.OBJECT;
import static com.revealprecision.revealserver.constants.EntityTagDataTypes.STRING;

import com.revealprecision.revealserver.api.v1.dto.factory.FormDataEntityTagValueEventFactory;
import com.revealprecision.revealserver.api.v1.dto.factory.LocationMetadataEventFactory;
import com.revealprecision.revealserver.api.v1.dto.factory.LocationMetadataImportFactory;
import com.revealprecision.revealserver.api.v1.dto.factory.MetadataImportResponseFactory;
import com.revealprecision.revealserver.api.v1.dto.factory.PersonMetadataEventFactory;
import com.revealprecision.revealserver.api.v1.dto.response.LocationMetadataImport;
import com.revealprecision.revealserver.api.v1.dto.response.MetadataFileImportResponse;
import com.revealprecision.revealserver.constants.EntityTagScopes;
import com.revealprecision.revealserver.constants.KafkaConstants;
import com.revealprecision.revealserver.enums.BulkEntryStatus;
import com.revealprecision.revealserver.enums.EntityStatus;
import com.revealprecision.revealserver.exceptions.FileFormatException;
import com.revealprecision.revealserver.exceptions.NotFoundException;
import com.revealprecision.revealserver.messaging.message.EntityTagEvent;
import com.revealprecision.revealserver.messaging.message.FormDataEntityTagValueEvent;
import com.revealprecision.revealserver.messaging.message.LocationMetadataEvent;
import com.revealprecision.revealserver.messaging.message.Message;
import com.revealprecision.revealserver.messaging.message.PersonMetadataEvent;
import com.revealprecision.revealserver.persistence.domain.EntityTag;
import com.revealprecision.revealserver.persistence.domain.Location;
import com.revealprecision.revealserver.persistence.domain.MetadataImport;
import com.revealprecision.revealserver.persistence.domain.Person;
import com.revealprecision.revealserver.persistence.domain.Plan;
import com.revealprecision.revealserver.persistence.domain.User;
import com.revealprecision.revealserver.persistence.domain.metadata.LocationMetadata;
import com.revealprecision.revealserver.persistence.domain.metadata.PersonMetadata;
import com.revealprecision.revealserver.persistence.domain.metadata.infra.Metadata;
import com.revealprecision.revealserver.persistence.domain.metadata.infra.MetadataList;
import com.revealprecision.revealserver.persistence.domain.metadata.infra.MetadataObj;
import com.revealprecision.revealserver.persistence.domain.metadata.infra.TagData;
import com.revealprecision.revealserver.persistence.domain.metadata.infra.TagValue;
import com.revealprecision.revealserver.persistence.domain.metadata.metadataImport.MetaImportDTO;
import com.revealprecision.revealserver.persistence.domain.metadata.metadataImport.fieldMapper.MetaFieldSetMapper;
import com.revealprecision.revealserver.persistence.repository.LocationMetadataRepository;
import com.revealprecision.revealserver.persistence.repository.MetadataImportRepository;
import com.revealprecision.revealserver.persistence.repository.PersonMetadataRepository;
import com.revealprecision.revealserver.persistence.repository.PersonRepository;
import com.revealprecision.revealserver.props.KafkaProperties;
import com.revealprecision.revealserver.util.UserUtils;
import java.io.IOException;
import java.security.Principal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import javax.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.SerializationUtils;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.keycloak.KeycloakPrincipal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.util.Pair;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class MetadataService {

  private final LocationMetadataRepository locationMetadataRepository;
  private final PersonMetadataRepository personMetadataRepository;
  private final PersonRepository personRepository;
  private final KafkaTemplate<String, LocationMetadataEvent> locationMetadataKafkaTemplate;
  private final KafkaTemplate<String, PersonMetadataEvent> personMetadataKafkaTemplate;
  private final KafkaProperties kafkaProperties;
  private final LocationService locationService;
  private final MetadataImportRepository metadataImportRepository;
  private final UserService userService;
  private final StorageService storageService;
  private final EntityTagService entityTagService;
  private final KafkaTemplate<String, Message> kafkaTemplate;
  private final MetaFieldSetMapper metaFieldSetMapper;

  public LocationMetadata getLocationMetadataByLocation(UUID locationIdentifier) {
    //TODO fix this
    return locationMetadataRepository.findLocationMetadataByLocation_Identifier(locationIdentifier)
        .orElse(null);
  }

  public PersonMetadata getPersonMetadataByPerson(UUID personIdentifier) {
    return personMetadataRepository.findPersonMetadataByPerson_Identifier(personIdentifier)
        .orElse(null);
  }

  public Object updateMetaData(UUID identifier, Object tagValue,
      Plan plan, UUID taskIdentifier,
      String user, String dataType, EntityTagEvent tag, String type, Object entity, String taskType,
      Class<?> aClass, String tagKey, String finalDateForScopeDateFields1) {
    if (aClass == Person.class) {
      return updatePersonMetadata(identifier, tagValue, plan, taskIdentifier, user, dataType, tag,
          type, (Person) entity, taskType, tagKey, finalDateForScopeDateFields1);
    } else if (aClass == Location.class) {
      return updateLocationMetadata(identifier, tagValue, plan, taskIdentifier, user, dataType, tag,
          type, (Location) entity, taskType, tagKey, finalDateForScopeDateFields1);
    }
    return null;
  }

  @Transactional
  public PersonMetadata updatePersonMetadata(UUID personIdentifier, Object tagValue,
      Plan plan, UUID taskIdentifier,
      String user, String dataType, EntityTagEvent tag, String type, Person person, String taskType,
      String tagKey, String dateForScopeDateFields) {

    PersonMetadata personMetadata;

    Optional<PersonMetadata> optionalPersonMetadata = personMetadataRepository.findPersonMetadataByPerson_Identifier(
        personIdentifier);
    if (optionalPersonMetadata.isPresent()) {

      OptionalInt optionalArrIndex = IntStream.range(0,
          optionalPersonMetadata.get().getEntityValue().getMetadataObjs().size()).filter(i ->
          optionalPersonMetadata.get().getEntityValue().getMetadataObjs().get(i).getTag()
              .equals(tag.getTag())
      ).findFirst();

      if (optionalArrIndex.isPresent()) {
        personMetadata = optionalPersonMetadata.get();

        int arrIndex = optionalArrIndex.getAsInt();
        //TODO: Add history

        personMetadata.getEntityValue().getMetadataObjs().get(arrIndex).getCurrent().setValue(
            getTagValue(tagValue, dataType,
                personMetadata.getEntityValue().getMetadataObjs().get(arrIndex).getCurrent()
                    .getValue()));
        personMetadata.getEntityValue().getMetadataObjs().get(arrIndex).getCurrent().getMeta()
            .setUpdateDateTime(LocalDateTime.now());
        personMetadata.getEntityValue().getMetadataObjs().get(arrIndex).getCurrent().getMeta()
            .setUserId(user);
        personMetadata.getEntityValue().getMetadataObjs().get(arrIndex).getCurrent().getMeta()
            .setTaskType(taskType);

        //TODO: Add history

      } else {
        // tag does not exist in list
        MetadataObj metadataObj = getMetadataObj(tagValue,
            plan == null ? null : plan.getIdentifier(), taskIdentifier,
            user, dataType, tag, type, taskType, tagKey, dateForScopeDateFields);

        personMetadata = optionalPersonMetadata.get();
        List<MetadataObj> metadataObjs = new ArrayList<>(
            personMetadata.getEntityValue().getMetadataObjs());
        metadataObjs.add(metadataObj);
        personMetadata.getEntityValue().setMetadataObjs(metadataObjs);

      }
    } else {
      //person metadata does not exist
      personMetadata = new PersonMetadata();
      //TODO: check this
      personMetadata.setPerson(person);

      MetadataObj metadataObj = getMetadataObj(tagValue, plan == null ? null : plan.getIdentifier(),
          taskIdentifier, user,
          dataType, tag, type, taskType, tagKey, dateForScopeDateFields);

      MetadataList metadataList = new MetadataList();
      metadataList.setMetadataObjs(List.of(metadataObj));
      personMetadata.setEntityValue(metadataList);
      personMetadata.setEntityStatus(EntityStatus.ACTIVE);
    }

    PersonMetadata savedPersonMetadata = personMetadataRepository.save(personMetadata);

    List<UUID> locationList = locationService.getLocationsByPeople(person.getIdentifier())
        .stream()
        .map(Location::getIdentifier).collect(Collectors.toList());

    PersonMetadataEvent personMetadataEvent = PersonMetadataEventFactory.getPersonMetadataEvent(
        plan, locationList, savedPersonMetadata);

    personMetadataKafkaTemplate.send(
        kafkaProperties.getTopicMap().get(KafkaConstants.PERSON_METADATA_UPDATE),
        personMetadataEvent);

    return savedPersonMetadata;
  }


  public LocationMetadata updateLocationMetadata(UUID locationIdentifier, Object tagValue,
      Plan plan, UUID taskIdentifier,
      String user, String dataType, EntityTagEvent locationEntityTag, String type,
      Location location,
      String taskType, String tagKey, String dateForScopeDateFields) {

    LocationMetadata locationMetadata;

    Optional<LocationMetadata> locationMetadataOptional = locationMetadataRepository.findLocationMetadataByLocation_Identifier(
        locationIdentifier);
    if (locationMetadataOptional.isPresent()) {

      OptionalInt optionalArrIndex = OptionalInt.empty();
      if (locationEntityTag.getScope() == null || (locationEntityTag.getScope() != null
          && !locationEntityTag.getScope().equals("Date"))) {
        optionalArrIndex = IntStream.range(0,
                locationMetadataOptional.get().getEntityValue().getMetadataObjs().size())
            .filter(
                i -> locationMetadataOptional.get().getEntityValue().getMetadataObjs().get(i)
                    .getTag()
                    .equals(locationEntityTag.getTag()))
            .findFirst();
      } else {
        optionalArrIndex = IntStream.range(0,
                locationMetadataOptional.get().getEntityValue().getMetadataObjs().size())
            .filter(
                i -> locationMetadataOptional.get().getEntityValue().getMetadataObjs().get(i)
                    .getTag()
                    .equals(locationEntityTag.getTag())
                    && (!locationMetadataOptional.get().getEntityValue().getMetadataObjs().get(i)
                    .isDateScope() ||
                    locationMetadataOptional.get().getEntityValue().getMetadataObjs().get(i)
                        .getDateForDateScope().equals(dateForScopeDateFields)))
            .findFirst();
      }
      if (optionalArrIndex.isPresent()) {
        //tag exists
        locationMetadata = locationMetadataOptional.get();

        int arrIndex = optionalArrIndex.getAsInt();
        //TODO: Add history

        locationMetadata.getEntityValue().getMetadataObjs().get(arrIndex).getCurrent().setValue(
            getTagValue(tagValue, dataType,
                locationMetadata.getEntityValue().getMetadataObjs().get(arrIndex).getCurrent()
                    .getValue()));
        locationMetadata.getEntityValue().getMetadataObjs().get(arrIndex).getCurrent().getMeta()
            .setUpdateDateTime(LocalDateTime.now());
        locationMetadata.getEntityValue().getMetadataObjs().get(arrIndex).getCurrent().getMeta()
            .setUserId(user);
        locationMetadata.getEntityValue().getMetadataObjs().get(arrIndex).getCurrent().getMeta()
            .setTaskType(taskType);

        locationMetadata.getEntityValue().getMetadataObjs().get(arrIndex).setActive(true);
        if (locationEntityTag.getScope().equals("Date")) {
          if (dateForScopeDateFields != null) {
            locationMetadata.getEntityValue().getMetadataObjs().get(arrIndex)
                .setDateForDateScope(dateForScopeDateFields);
            locationMetadata.getEntityValue().getMetadataObjs().get(arrIndex).setDateScope(true);
            locationMetadata.getEntityValue().getMetadataObjs().get(arrIndex).setCaptureNumber(
                LocalDate.parse(dateForScopeDateFields, DateTimeFormatter.ISO_LOCAL_DATE)
                    .toEpochDay());
          }
        }

        //TODO: Add history

      } else {
        // tag does not exist in list
        MetadataObj metadataObj = getMetadataObj(tagValue,
            plan == null ? null : plan.getIdentifier(), taskIdentifier,
            user,
            dataType, locationEntityTag, type, taskType, tagKey, dateForScopeDateFields);

        locationMetadata = locationMetadataOptional.get();
        List<MetadataObj> temp = new ArrayList<>(
            locationMetadata.getEntityValue().getMetadataObjs());
        temp.add(metadataObj);
        locationMetadata.getEntityValue().setMetadataObjs(temp);

      }
    } else {
      //location metadata does not exist
      locationMetadata = new LocationMetadata();
      //TODO: check this

      location.setIdentifier(locationIdentifier);
      locationMetadata.setLocation(location);

      MetadataObj metadataObj = getMetadataObj(tagValue, plan == null ? null : plan.getIdentifier(),
          taskIdentifier, user,
          dataType, locationEntityTag, type, taskType, tagKey, dateForScopeDateFields);

      MetadataList metadataList = new MetadataList();
      metadataList.setMetadataObjs(List.of(metadataObj));
      locationMetadata.setEntityValue(metadataList);

      locationMetadata.setEntityStatus(EntityStatus.ACTIVE);
    }
    LocationMetadata savedLocationMetadata = locationMetadataRepository.save(locationMetadata);

    LocationMetadataEvent locationMetadataEvent = LocationMetadataEventFactory.
        getLocationMetadataEvent(
            plan, location, savedLocationMetadata);

    locationMetadataKafkaTemplate.send(
        kafkaProperties.getTopicMap().get(KafkaConstants.LOCATION_METADATA_UPDATE),
        locationMetadataEvent);
    return savedLocationMetadata;
  }

  public LocationMetadata deactivateLocationMetadata(UUID locationIdentifier, EntityTag tag,
      Plan plan) {

    LocationMetadata locationMetadata;

    Optional<LocationMetadata> locationMetadataOptional = locationMetadataRepository.findLocationMetadataByLocation_Identifier(
        locationIdentifier);
    if (locationMetadataOptional.isPresent()) {

      OptionalInt optionalArrIndex = IntStream.range(0,
              locationMetadataOptional.get().getEntityValue().getMetadataObjs().size())
          .filter(
              i -> locationMetadataOptional.get().getEntityValue().getMetadataObjs().get(i).getTag()
                  .equals(tag.getTag()))
          .findFirst();

      if (optionalArrIndex.isPresent()) {
        //tag exists
        locationMetadata = locationMetadataOptional.get();

        int arrIndex = optionalArrIndex.getAsInt();
        TagData oldObj = SerializationUtils.clone(
            locationMetadataOptional.get().getEntityValue().getMetadataObjs().get(arrIndex)
                .getCurrent());

        locationMetadata.getEntityValue().getMetadataObjs().get(arrIndex).setActive(false);

        if (locationMetadata.getEntityValue().getMetadataObjs().get(arrIndex).getHistory()
            != null) {
          locationMetadata.getEntityValue().getMetadataObjs().get(arrIndex).getHistory()
              .add(oldObj);
        } else {
          locationMetadata.getEntityValue().getMetadataObjs().get(arrIndex)
              .setHistory(List.of(oldObj));
        }

        LocationMetadata savedLocationMetadata = locationMetadataRepository.save(locationMetadata);

        LocationMetadataEvent locationMetadataEvent = LocationMetadataEventFactory.
            getLocationMetadataEvent(
                plan, null, savedLocationMetadata);

        locationMetadataKafkaTemplate.send(
            kafkaProperties.getTopicMap().get(KafkaConstants.LOCATION_METADATA_UPDATE),
            locationMetadataEvent);
        return savedLocationMetadata;
      } else {
        // tag does not exist in list
        log.info("tag {} not present...ignoring ", tag);
      }
    } else {
      //location metadata does not exist
      log.info("no metadata for entity {} not present...ignoring ", locationIdentifier);
    }

    return null;
  }


  public PersonMetadata deactivatePersonMetadata(UUID locationIdentifier, EntityTag tag,
      Plan plan) {

    PersonMetadata personMetadata;

    Optional<PersonMetadata> personMetadataOptional = personMetadataRepository.findPersonMetadataByPerson_Identifier(
        locationIdentifier);
    if (personMetadataOptional.isPresent()) {

      OptionalInt optionalArrIndex = IntStream.range(0,
              personMetadataOptional.get().getEntityValue().getMetadataObjs().size())
          .filter(
              i -> personMetadataOptional.get().getEntityValue().getMetadataObjs().get(i).getTag()
                  .equals(tag.getTag()))
          .findFirst();

      if (optionalArrIndex.isPresent()) {
        //tag exists
        personMetadata = personMetadataOptional.get();

        int arrIndex = optionalArrIndex.getAsInt();
        TagData oldObj = SerializationUtils.clone(
            personMetadataOptional.get().getEntityValue().getMetadataObjs().get(arrIndex)
                .getCurrent());

        personMetadata.getEntityValue().getMetadataObjs().get(arrIndex).setActive(false);

        if (personMetadata.getEntityValue().getMetadataObjs().get(arrIndex).getHistory()
            != null) {
          personMetadata.getEntityValue().getMetadataObjs().get(arrIndex).getHistory()
              .add(oldObj);
        } else {
          personMetadata.getEntityValue().getMetadataObjs().get(arrIndex)
              .setHistory(List.of(oldObj));
        }

        PersonMetadata savedPersonMetadata = personMetadataRepository.save(personMetadata);

        List<Location> locationsByPeople = locationService.getLocationsByPeople(
            personMetadata.getPerson().getIdentifier());

        PersonMetadataEvent personMetadataEvent = PersonMetadataEventFactory.getPersonMetadataEvent(
            plan,
            locationsByPeople.stream().map(Location::getIdentifier).collect(Collectors.toList()),
            savedPersonMetadata);

        personMetadataKafkaTemplate.send(
            kafkaProperties.getTopicMap().get(KafkaConstants.PERSON_METADATA_UPDATE),
            personMetadataEvent);
        return savedPersonMetadata;
      } else {
        // tag does not exist in list
        log.info("tag {} not present...ignoring ", tag);
      }
    } else {
      //location metadata does not exist
      log.info("no metadata for entity {} not present...ignoring ", locationIdentifier);
    }

    return null;
  }

  private MetadataObj getMetadataObj(Object tagValue, UUID planIdentifier, UUID taskIdentifier,
      String user,
      String dataType, EntityTagEvent tag, String type, String taskType, String tagKey,
      String dateForScopeDateFields) {
    Metadata metadata = new Metadata();
    metadata.setPlanId(planIdentifier);
    metadata.setTaskId(taskIdentifier);
    metadata.setTaskType(taskType);
    metadata.setCreateDateTime(LocalDateTime.now());
    metadata.setUpdateDateTime(LocalDateTime.now());
    metadata.setUserId(user);

    TagValue value = getTagValue(tagValue, dataType, new TagValue());

    TagData tagData = new TagData();
    tagData.setMeta(metadata);
    tagData.setValue(value);

    MetadataObj metadataObj = new MetadataObj();
    metadataObj.setDataType(dataType);
    metadataObj.setTag(tag.getTag());
    metadataObj.setType(type);
    metadataObj.setEntityTagId(tag.getIdentifier());
    metadataObj.setCurrent(tagData);
    metadataObj.setActive(true);
    metadataObj.setTagKey(tagKey);

    if (tag.getScope().equals(EntityTagScopes.DATE)) {
      if (dateForScopeDateFields != null) {
        metadataObj.setDateForDateScope(dateForScopeDateFields);
        metadataObj.setDateScope(true);
        metadataObj.setCaptureNumber(
            LocalDate.parse(dateForScopeDateFields, DateTimeFormatter.ISO_LOCAL_DATE).toEpochDay());
      }
    }

    return metadataObj;
  }

  public static Pair<Class, Object> getValueFromValueObject(MetadataObj metadataObj) {
    switch (metadataObj.getDataType()) {
      case STRING:
        return Pair.of(String.class, metadataObj.getCurrent().getValue().getValueString());

      case INTEGER:
        return Pair.of(Integer.class, metadataObj.getCurrent().getValue().getValueInteger());

      case DATE:
        return Pair.of(LocalDateTime.class, metadataObj.getCurrent().getValue().getValueDate());

      case DOUBLE:
        return Pair.of(Double.class, metadataObj.getCurrent().getValue().getValueDouble());

      case BOOLEAN:
        return Pair.of(Boolean.class, metadataObj.getCurrent().getValue().getValueBoolean());

      default:
        return Pair.of(String.class, metadataObj.getCurrent().getValue().getValueString());

    }
  }


  private TagValue getTagValue(Object tagValue, String dataType, TagValue value) {
    switch (dataType) {
      case STRING:
        value.setValueString((String) tagValue);
        break;
      case INTEGER:
        value.setValueInteger((Integer) tagValue);
        break;
      case DATE:
        value.setValueDate((LocalDateTime) tagValue);
        break;
      case DOUBLE:
        value.setValueDouble((Double) tagValue);
        break;
      case BOOLEAN:
        value.setValueBoolean((Boolean) tagValue);
        break;
      case OBJECT:
        value.getValueObjects().add(tagValue);
        break;
      default:
        value.setValueString((String) tagValue);
        break;
    }
    return value;
  }

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
      List<MetaImportDTO> metaImportDTOS = metaFieldSetMapper.mapMetaFields(sheet);

      if (metaImportDTOS.stream().map(MetaImportDTO::getErrors).map(Map::size).reduce(0,
          Integer::sum) >1){
        throw new FileFormatException("Invalid file errors with: "+
            metaImportDTOS.stream().map(MetaImportDTO::getErrors).flatMap(error->error.entrySet().stream()).map(entry->
                "tag: "+entry.getKey().getTag() + "value: "+entry.getValue()
            ).collect(Collectors.joining("\r\n"))
            );
      }

      //send data to kafka listener
      if (!metaImportDTOS.isEmpty()) {

        metaImportDTOS.forEach(metaImportDTO ->
            metaImportDTO.getConvertedEntityData().forEach(
                (key, value) -> update(user, currentMetaImport, metaImportDTO,
                    metaImportDTO.getLocation(),
                    value, key)));
      }
      storageService.deleteFile(file);
      currentMetaImport.setStatus(BulkEntryStatus.SUCCESSFUL);
      return metadataImportRepository.save(currentMetaImport).getIdentifier();
    } catch (Exception e) {
      log.error(e.getMessage(), e);
      storageService.deleteFile(file);
      currentMetaImport.setStatus(BulkEntryStatus.FAILED);
      metadataImportRepository.save(currentMetaImport);
      throw new FileFormatException(e.getMessage());
    }

  }

  private void update(User user, MetadataImport currentMetaImport, MetaImportDTO metaImportDTO,
      Location loc, Object importEntityTagValue, EntityTagEvent entityTagEvent) {
    try {

      LocationMetadata locationMetadata = (LocationMetadata) updateMetaData(
          metaImportDTO.getLocation().getIdentifier(), importEntityTagValue,
          null,
          null, user.getIdentifier().toString(), entityTagEvent.getValueType(),
          entityTagEvent, "ImportData",
          loc,
          "File import", Location.class,
          entityTagEvent.getTag(), null);

      LocationMetadataEvent locationMetadataEvent =
          LocationMetadataEventFactory.getLocationMetadataEvent(null,
              locationMetadata.getLocation(),
              locationMetadata);

      FormDataEntityTagValueEvent entity = FormDataEntityTagValueEventFactory.getEntity(null,
          metaImportDTO.getLocationHierarchy().getIdentifier(),
          loc.getGeographicLevel().getName(),
          null, null, null, loc, entityTagEvent, importEntityTagValue, null);

      kafkaTemplate.send(
          kafkaProperties.getTopicMap()
              .get(KafkaConstants.FORM_EVENT_CONSUMPTION),
          entity);

      // check if there is an existing metadata event already created
      // than just update the metaDataEvent list instead of creating a duplicate
      boolean shouldAdd = true;
      for (LocationMetadataEvent l : currentMetaImport.getLocationMetadataEvents()) {
        if (l.getIdentifier().equals(locationMetadataEvent.getIdentifier())) {
          shouldAdd = false;
          l.setMetaDataEvents(locationMetadataEvent.getMetaDataEvents());
        }
      }

      if (shouldAdd) {
        currentMetaImport.getLocationMetadataEvents().add(locationMetadataEvent);
      }

      metadataImportRepository.save(currentMetaImport);
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
