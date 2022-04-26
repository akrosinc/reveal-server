package com.revealprecision.revealserver.service;

import com.revealprecision.revealserver.enums.EntityStatus;
import com.revealprecision.revealserver.persistence.domain.Location;
import com.revealprecision.revealserver.persistence.domain.Person;
import com.revealprecision.revealserver.persistence.domain.metadata.LocationMetadata;
import com.revealprecision.revealserver.persistence.domain.metadata.PersonMetadata;
import com.revealprecision.revealserver.persistence.domain.metadata.infra.Metadata;
import com.revealprecision.revealserver.persistence.domain.metadata.infra.MetadataList;
import com.revealprecision.revealserver.persistence.domain.metadata.infra.MetadataObj;
import com.revealprecision.revealserver.persistence.domain.metadata.infra.TagData;
import com.revealprecision.revealserver.persistence.domain.metadata.infra.TagValue;
import com.revealprecision.revealserver.persistence.repository.LocationMetadataRepository;
import com.revealprecision.revealserver.persistence.repository.PersonMetadataRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.UUID;
import java.util.stream.IntStream;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.SerializationUtils;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MetadataService {

  private final LocationMetadataRepository locationMetadataRepository;
  private final PersonMetadataRepository personMetadataRepository;

  public LocationMetadata getLocationMetadataByLocation(UUID locationIdentifier) {
    //TODO fix this
    return locationMetadataRepository.findLocationMetadataByLocation_Identifier(locationIdentifier)
        .orElse(null);
  }

  public PersonMetadata getPersonMetadataByPerson(UUID personIdentifier) {
    return personMetadataRepository.findPersonMetadataByPerson_Identifier(personIdentifier)
        .orElse(null);
  }

  public PersonMetadata updatePersonMetadata(UUID personIdentifier, Object tagValue,
      UUID planIdentifier, UUID taskIdentifier,
      String user, String dataType, String tag, String type) {

    PersonMetadata personMetadata;

    Optional<PersonMetadata> optionalPersonMetadata = personMetadataRepository.findPersonMetadataByPerson_Identifier(
        personIdentifier);
    if (optionalPersonMetadata.isPresent()) {

      OptionalInt optionalArrIndex = IntStream.range(0,
          optionalPersonMetadata.get().getEntityValue().getMetadataObjs().size()).filter(i ->
          optionalPersonMetadata.get().getEntityValue().getMetadataObjs().get(i).getTag()
              .equals(tag)
      ).findFirst();

      if (optionalArrIndex.isPresent()) {
        personMetadata = optionalPersonMetadata.get();

        int arrIndex = optionalArrIndex.getAsInt();
        TagData oldObj = SerializationUtils.clone(
            optionalPersonMetadata.get().getEntityValue().getMetadataObjs().get(arrIndex)
                .getCurrent());

        personMetadata.getEntityValue().getMetadataObjs().get(arrIndex).getCurrent().setValue(
            getTagValue(tagValue, dataType,
                personMetadata.getEntityValue().getMetadataObjs().get(arrIndex).getCurrent()
                    .getValue()));
        personMetadata.getEntityValue().getMetadataObjs().get(arrIndex).getCurrent().getMeta()
            .setUpdateDateTime(LocalDateTime.now());
        personMetadata.getEntityValue().getMetadataObjs().get(arrIndex).getCurrent().getMeta()
            .setUserId(user);

        if (personMetadata.getEntityValue().getMetadataObjs().get(arrIndex).getHistory() != null) {
          personMetadata.getEntityValue().getMetadataObjs().get(arrIndex).getHistory().add(oldObj);
        } else {
          personMetadata.getEntityValue().getMetadataObjs().get(arrIndex)
              .setHistory(List.of(oldObj));
        }

      } else {
        // tag does not exist in list
        MetadataObj metadataObj = getMetadataObj(tagValue, planIdentifier, taskIdentifier, user,
            dataType, tag, type);

        personMetadata = optionalPersonMetadata.get();
        personMetadata.getEntityValue().getMetadataObjs().add(metadataObj);

      }
    } else {
      //location metadata does not exist
      personMetadata = new PersonMetadata();

      //TODO: check this
      Person person = new Person();
      person.setIdentifier(personIdentifier);
      personMetadata.setPerson(person);

      MetadataObj metadataObj = getMetadataObj(tagValue, planIdentifier, taskIdentifier, user,
          dataType, tag, type);

      MetadataList metadataList = new MetadataList();
      metadataList.setMetadataObjs(List.of(metadataObj));
      personMetadata.setEntityValue(metadataList);

      personMetadata.setEntityStatus(EntityStatus.ACTIVE);
    }

    return personMetadataRepository.save(personMetadata);
  }


  public LocationMetadata updateLocationMetadata(UUID locationIdentifier, Object tagValue,
      UUID planIdentifier, UUID taskIdentifier,
      String user, String dataType, String tag, String type) {

    LocationMetadata locationMetadata;

    Optional<LocationMetadata> locationMetadataOptional = locationMetadataRepository.findLocationMetadataByLocation_Identifier(
        locationIdentifier);
    if (locationMetadataOptional.isPresent()) {

      OptionalInt optionalArrIndex = IntStream.range(0,
          locationMetadataOptional.get().getEntityValue().getMetadataObjs().size()).filter(i ->
          locationMetadataOptional.get().getEntityValue().getMetadataObjs().get(i).getTag()
              .equals(tag)
      ).findFirst();

      if (optionalArrIndex.isPresent()) {
        locationMetadata = locationMetadataOptional.get();

        int arrIndex = optionalArrIndex.getAsInt();
        TagData oldObj = SerializationUtils.clone(
            locationMetadataOptional.get().getEntityValue().getMetadataObjs().get(arrIndex)
                .getCurrent());

        locationMetadata.getEntityValue().getMetadataObjs().get(arrIndex).getCurrent().setValue(
            getTagValue(tagValue, dataType,
                locationMetadata.getEntityValue().getMetadataObjs().get(arrIndex).getCurrent()
                    .getValue()));
        locationMetadata.getEntityValue().getMetadataObjs().get(arrIndex).getCurrent().getMeta()
            .setUpdateDateTime(LocalDateTime.now());
        locationMetadata.getEntityValue().getMetadataObjs().get(arrIndex).getCurrent().getMeta()
            .setUserId(user);

        if (locationMetadata.getEntityValue().getMetadataObjs().get(arrIndex).getHistory()
            != null) {
          locationMetadata.getEntityValue().getMetadataObjs().get(arrIndex).getHistory()
              .add(oldObj);
        } else {
          locationMetadata.getEntityValue().getMetadataObjs().get(arrIndex)
              .setHistory(List.of(oldObj));
        }

      } else {
        // tag does not exist in list
        MetadataObj metadataObj = getMetadataObj(tagValue, planIdentifier, taskIdentifier, user,
            dataType, tag, type);

        locationMetadata = locationMetadataOptional.get();
        locationMetadata.getEntityValue().getMetadataObjs().add(metadataObj);

      }
    } else {
      //location metadata does not exist
      locationMetadata = new LocationMetadata();
      //TODO: check this
      Location location = new Location();
      location.setIdentifier(locationIdentifier);
      locationMetadata.setLocation(location);

      MetadataObj metadataObj = getMetadataObj(tagValue, planIdentifier, taskIdentifier, user,
          dataType, tag, type);

      MetadataList metadataList = new MetadataList();
      metadataList.setMetadataObjs(List.of(metadataObj));
      locationMetadata.setEntityValue(metadataList);

      locationMetadata.setEntityStatus(EntityStatus.ACTIVE);
    }
    return locationMetadataRepository.save(locationMetadata);
  }

  private MetadataObj getMetadataObj(Object tagValue, UUID planIdentifier, UUID taskIdentifier,
      String user,
      String dataType, String tag, String type) {
    Metadata metadata = new Metadata();
    metadata.setPlanId(planIdentifier);
    metadata.setTaskId(taskIdentifier);
    metadata.setCreateDateTime(LocalDateTime.now());
    metadata.setUpdateDateTime(LocalDateTime.now());
    metadata.setUserId(user);

    TagValue value = getTagValue(tagValue, dataType, new TagValue());

    TagData tagData = new TagData();
    tagData.setMeta(metadata);
    tagData.setValue(value);

    MetadataObj metadataObj = new MetadataObj();
    metadataObj.setDataType(dataType);
    metadataObj.setTag(tag);
    metadataObj.setType(type);
    metadataObj.setCurrent(tagData);
    return metadataObj;
  }

  private TagValue getTagValue(Object tagValue, String dataType, TagValue value) {
    switch (dataType) {
      case "string":
        value.setValueString((String) tagValue);
        break;
      case "integer":
        value.setValueInteger((Integer) tagValue);
        break;
      case "date":
        value.setValueDate((LocalDateTime) tagValue);
        break;
      case "double":
        value.setValueDouble((Double) tagValue);
        break;
      case "boolean":
        value.setValueBoolean((Boolean) tagValue);
        break;
      default:
        value.setValueString((String) tagValue);
        break;
    }
    return value;
  }

}
