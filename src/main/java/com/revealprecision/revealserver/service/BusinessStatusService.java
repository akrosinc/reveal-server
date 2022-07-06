package com.revealprecision.revealserver.service;

import com.revealprecision.revealserver.persistence.domain.Plan;
import com.revealprecision.revealserver.persistence.domain.Task;
import com.revealprecision.revealserver.persistence.domain.metadata.LocationMetadata;
import com.revealprecision.revealserver.persistence.domain.metadata.PersonMetadata;
import com.revealprecision.revealserver.props.BusinessStatusProperties;
import com.revealprecision.revealserver.util.ActionUtils;
import com.revealprecision.revealserver.util.UserUtils;
import java.io.IOException;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class BusinessStatusService {

  private final BusinessStatusProperties businessStatusProperties;
  private final MetadataService metadataService;

  public void setBusinessStatus(Task task, String businessStatus) throws IOException {

      Plan plan = task.getAction().getGoal().getPlan();

      String baseBusinessStatusTagName = businessStatusProperties.getBusinessStatusTagName();

      String businessStatusTagName = baseBusinessStatusTagName;
      if (plan != null && task.getIdentifier() != null) {
        businessStatusTagName = businessStatusTagName.concat("_")
            .concat(plan.getIdentifier().toString()).concat("_").concat(task.getIdentifier().toString());
      }

      if (ActionUtils.isActionForLocation(task.getAction())) {
        metadataService.updateLocationMetadata(task.getBaseEntityIdentifier(), businessStatus,
            plan, task.getIdentifier(), UserUtils.getCurrentPrincipleName(), "string",
            businessStatusTagName, baseBusinessStatusTagName, task.getLocation(), task.getAction().getTitle());
      }

      if (ActionUtils.isActionForPerson(task.getAction())) {
        metadataService.updatePersonMetadata(task.getBaseEntityIdentifier(), businessStatus,
            plan, task.getIdentifier(), UserUtils.getCurrentPrincipleName(), "string",
            businessStatusTagName, baseBusinessStatusTagName, task.getPerson(), task.getAction().getTitle());
      }
  }

  public void deactivateBusinessStatus(Task task) {

    Plan plan = task.getAction().getGoal().getPlan();
    UUID planIdentifier = plan.getIdentifier();

    String businessStatusTagName = businessStatusProperties.getBusinessStatusTagName();
    if (planIdentifier != null && task.getIdentifier() != null) {
      businessStatusTagName = businessStatusTagName.concat("_")
          .concat(planIdentifier.toString()).concat("_").concat(task.getIdentifier().toString());
    }

    if (ActionUtils.isActionForLocation(task.getAction())) {
      metadataService.deactivateLocationMetadata(task.getBaseEntityIdentifier(),
          businessStatusTagName, plan);
    }

    if (ActionUtils.isActionForPerson(task.getAction())) {
      metadataService.deactivatePersonMetadata(task.getBaseEntityIdentifier(),
          businessStatusTagName, plan);
    }
  }

  public String getBusinessStatus(Task task) {

    if (task.getLocation() != null) {
      LocationMetadata locationMetadata = metadataService.getLocationMetadataByLocation(
          task.getLocation().getIdentifier());
      if (locationMetadata != null) {
        Optional<String> businessStatusValueOptional = locationMetadata.getEntityValue()
            .getMetadataObjs().stream()
            .filter(
                metadataObj -> metadataObj.getTag()
                    .equals(businessStatusProperties.getBusinessStatusTagName().concat("_")
                        .concat(task.getPlan().getIdentifier().toString()).concat("_")
                        .concat(task.getIdentifier().toString())))
            .map(metadataObj -> metadataObj.getCurrent().getValue().getValueString())
            .findFirst();
        if (businessStatusValueOptional.isPresent()) {
          return businessStatusValueOptional.get();
        }
      }
    }
    if (task.getPerson() != null) {
      PersonMetadata personMetadata = metadataService.getPersonMetadataByPerson(
          task.getPerson().getIdentifier());
      if (personMetadata != null) {
        Optional<String> businessStatusValueOptional = personMetadata.getEntityValue()
            .getMetadataObjs().stream()
            .filter(
                metadataObj -> metadataObj.getTag()
                    .equals(businessStatusProperties.getBusinessStatusTagName().concat("_")
                        .concat(task.getPlan().getIdentifier().toString()).concat("_")
                        .concat(task.getIdentifier().toString())))
            .map(metadataObj -> metadataObj.getCurrent().getValue().getValueString())
            .findFirst();
        if (businessStatusValueOptional.isPresent()) {
          return businessStatusValueOptional.get();
        }
      }
    }
    return null;
  }

}
