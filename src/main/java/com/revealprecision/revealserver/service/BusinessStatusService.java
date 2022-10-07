package com.revealprecision.revealserver.service;

import com.revealprecision.revealserver.api.v1.dto.factory.EntityTagEventFactory;
import com.revealprecision.revealserver.enums.LookupEntityTypeCodeEnum;
import com.revealprecision.revealserver.persistence.domain.EntityTag;
import com.revealprecision.revealserver.persistence.domain.Plan;
import com.revealprecision.revealserver.persistence.domain.Task;
import com.revealprecision.revealserver.props.BusinessStatusProperties;
import com.revealprecision.revealserver.util.ActionUtils;
import com.revealprecision.revealserver.util.UserUtils;
import java.util.Optional;
import javax.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class BusinessStatusService {

  private final BusinessStatusProperties businessStatusProperties;
  private final MetadataService metadataService;
  private final EntityTagService entityTagService;

  private EntityTag personEntityTag;
  private EntityTag locationEntityTag;

  public void setBusinessStatus(Task task, String businessStatus)  {

    Plan plan = task.getAction().getGoal().getPlan();

    String baseBusinessStatusTagName = businessStatusProperties.getBusinessStatusTagName();

    String businessStatusTagKey = baseBusinessStatusTagName;
    if (plan != null && task.getIdentifier() != null) {
      businessStatusTagKey = businessStatusTagKey.concat("_")
          .concat(plan.getIdentifier().toString()).concat("_")
          .concat(task.getIdentifier().toString());
    }

    if (ActionUtils.isActionForLocation(task.getAction())) {
      metadataService.updateLocationMetadata(task.getBaseEntityIdentifier(), businessStatus,
          plan, task.getIdentifier(), UserUtils.getCurrentPrincipleName(), "string",
          EntityTagEventFactory.getEntityTagEvent(locationEntityTag), baseBusinessStatusTagName, task.getLocation(),
          task.getAction().getTitle(), businessStatusTagKey, null);
    }

    if (ActionUtils.isActionForPerson(task.getAction())) {
      metadataService.updatePersonMetadata(task.getBaseEntityIdentifier(), businessStatus,
          plan, task.getIdentifier(), UserUtils.getCurrentPrincipleName(), "string",
          EntityTagEventFactory.getEntityTagEvent(personEntityTag), baseBusinessStatusTagName, task.getPerson(), task.getAction().getTitle(),
          businessStatusTagKey, null);
    }
  }

  public void deactivateBusinessStatus(Task task) {

    Plan plan = task.getAction().getGoal().getPlan();

    if (ActionUtils.isActionForLocation(task.getAction())) {
      metadataService.activateOrDeactivateLocationMetadata(task.getBaseEntityIdentifier(),
          locationEntityTag, plan, false);
    }

    if (ActionUtils.isActionForPerson(task.getAction())) {
      metadataService.activateOrDeactivatePersonMetadata(task.getBaseEntityIdentifier(),
          personEntityTag, plan,false);
    }
  }

  public void activateBusinessStatus(Task task) {

    Plan plan = task.getAction().getGoal().getPlan();

    if (ActionUtils.isActionForLocation(task.getAction())) {
      metadataService.activateOrDeactivateLocationMetadata(task.getBaseEntityIdentifier(),
          locationEntityTag, plan, true);
    }

    if (ActionUtils.isActionForPerson(task.getAction())) {
      metadataService.activateOrDeactivatePersonMetadata(task.getBaseEntityIdentifier(),
          personEntityTag, plan, true);
    }
  }

  @PostConstruct
  private void loadBusinessStatusEntitiesTag() {
    Optional<EntityTag> locationBusinessStatusEntityTagOptional = entityTagService.getEntityTagByTagNameAndLookupEntityType(
        businessStatusProperties.getBusinessStatusTagName(),
        LookupEntityTypeCodeEnum.LOCATION_CODE);
    locationBusinessStatusEntityTagOptional.ifPresent(entityTag -> locationEntityTag = entityTag);

    Optional<EntityTag> personBusinessStatusEntityTagOptional = entityTagService.getEntityTagByTagNameAndLookupEntityType(
        businessStatusProperties.getBusinessStatusTagName(),
        LookupEntityTypeCodeEnum.PERSON_CODE);
    personBusinessStatusEntityTagOptional.ifPresent(entityTag -> personEntityTag = entityTag);
  }
}
