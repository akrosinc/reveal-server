package com.revealprecision.revealserver.service;

import static com.revealprecision.revealserver.props.BusinessStatusProperties.GENERAL;

import com.revealprecision.revealserver.enums.LookupEntityTypeTableEnum;
import com.revealprecision.revealserver.exceptions.NotFoundException;
import com.revealprecision.revealserver.persistence.domain.EntityTag;
import com.revealprecision.revealserver.persistence.domain.EntityTag.Fields;
import com.revealprecision.revealserver.persistence.domain.Task;
import com.revealprecision.revealserver.props.BusinessStatusProperties;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class BusinessStatusService {

  private final BusinessStatusProperties businessStatusProperties;
  private final MetaDataJdbcService metaDataJdbcService;
  private final EntityTagService entityTagService;

  public void setBusinessStatus(Task task, String businessStatus) {

    UUID planIdentifier = task.getAction().getGoal().getPlan().getIdentifier();

    String businessStatusField = businessStatusProperties.getBusinessStatusMapping().get(GENERAL);

    if (planIdentifier != null) {
      String businessStatusFieldProperty = businessStatusProperties.getBusinessStatusMapping()
          .get(planIdentifier.toString());
      if (businessStatusFieldProperty != null) {
        businessStatusField = businessStatusFieldProperty;
      }
    }

    Optional<EntityTag> entityTag = entityTagService.getEntityTagByTagName(businessStatusField);

    if (entityTag.isPresent()) {
      metaDataJdbcService.createOrUpdateMetadata(task.getBaseEntityIdentifier(),
          task.getAction().getLookupEntityType().getTableName(), businessStatusField,
          businessStatus, String.class);
    } else {
      throw new NotFoundException(Pair.of(Fields.tag, businessStatusField), EntityTag.class);
    }
  }

  public void setBusinessStatusForAllKeys(UUID baseEntityIdentifier, String businessStatus,
      LookupEntityTypeTableEnum lookupEntityTypeTableEnum) {

    Map<String, String> businessStatusMapping = businessStatusProperties.getBusinessStatusMapping();

    businessStatusMapping.forEach((key, businessStatusTagName) -> {

      Optional<EntityTag> entityTag = entityTagService.getEntityTagByTagName(businessStatusTagName);
      if (entityTag.isPresent()) {
        metaDataJdbcService.createOrUpdateMetadata(baseEntityIdentifier,
            lookupEntityTypeTableEnum.getLookupEntityType(), businessStatusTagName, businessStatus,
            String.class);
      } else {
        throw new NotFoundException(Pair.of(Fields.tag, businessStatusTagName), EntityTag.class);
      }
    });
  }


  public Object getBusinessStatus(Task task) {
    String businessStatusField = businessStatusProperties.getBusinessStatusMapping()
        .get(task.getAction().getGoal().getPlan().getIdentifier().toString());
    if (businessStatusField == null) {
      businessStatusField = businessStatusProperties.getBusinessStatusMapping().get(GENERAL);
    }

    Object businessStatus = businessStatusProperties.getDefaultLocationBusinessStatus();
    if (task.getLocation() != null) {
      org.apache.commons.lang3.tuple.Pair<Class<?>, Object> locationMetadata = metaDataJdbcService.getMetadataFor(
          LookupEntityTypeTableEnum.LOCATION_TABLE.getLookupEntityType(),
          task.getLocation().getIdentifier()).get(businessStatusField);
      if (locationMetadata != null) {
        if (locationMetadata.getKey() != null) {
          Class<?> aClass = locationMetadata.getKey();
          businessStatus = aClass.cast(locationMetadata.getValue());
        }
      }
    }
    if (task.getPerson() != null) {
      org.apache.commons.lang3.tuple.Pair<Class<?>, Object> personMetadata = metaDataJdbcService.getMetadataFor(
          LookupEntityTypeTableEnum.PERSON_TABLE.getLookupEntityType(),
          task.getPerson().getIdentifier()).get(businessStatusField);
      if (personMetadata != null) {
        if (personMetadata.getKey() != null) {
          Class<?> aClass = personMetadata.getKey();
          businessStatus = aClass.cast(personMetadata.getValue());
        }
      }
    }
    return businessStatus;
  }

}
