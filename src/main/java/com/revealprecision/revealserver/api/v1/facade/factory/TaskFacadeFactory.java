package com.revealprecision.revealserver.api.v1.facade.factory;

import com.revealprecision.revealserver.api.v1.facade.models.Period;
import com.revealprecision.revealserver.api.v1.facade.models.TaskFacade;
import com.revealprecision.revealserver.api.v1.facade.models.TaskFacade.TaskPriority;
import com.revealprecision.revealserver.api.v1.facade.models.TaskFacade.TaskStatus;
import com.revealprecision.revealserver.api.v1.facade.util.DateTimeFormatter;
import com.revealprecision.revealserver.persistence.domain.Action;
import com.revealprecision.revealserver.persistence.domain.Location;
import com.revealprecision.revealserver.persistence.domain.Person;
import com.revealprecision.revealserver.persistence.domain.Task;
import com.revealprecision.revealserver.persistence.domain.User;
import com.revealprecision.revealserver.util.ActionUtils;
import java.util.Optional;

public class TaskFacadeFactory {

  public static TaskFacade getEntity(Task task, String businessStatus, User user,
      String group) {
    String userName = user != null ? user.getUsername() : null;
    Action action = task.getAction();
    String structureId = getStructureId(task, action);
    return TaskFacade.builder()
        .code(task.getAction().getTitle())
        .authoredOn(
            DateTimeFormatter.getDateTimeFacadeStringFromLocalDateTime(task.getAuthoredOn()))
        .description(task.getAction().getDescription())
        .executionPeriod(Period
            .between(DateTimeFormatter.getDateTimeFacadeStringFromLocalDateTime(
                task.getExecutionPeriodStart().atStartOfDay())
                , DateTimeFormatter.getDateTimeFacadeStringFromLocalDateTime(
                    task.getExecutionPeriodEnd().atStartOfDay())))
        .focus(task.getAction().getIdentifier().toString())
        .forEntity(task.getBaseEntityIdentifier().toString())
        .identifier(task.getIdentifier().toString())
        .planIdentifier(task.getAction().getGoal().getPlan().getIdentifier().toString())
        .priority(TaskPriority.get(task.getPriority().name().toLowerCase()))
        .lastModified(
            DateTimeFormatter.getDateTimeFacadeStringFromLocalDateTime(task.getLastModified()))
        .status(TaskStatus.get(task.getLookupTaskStatus().getCode().toLowerCase()))
        .businessStatus(businessStatus)
        .owner(userName)
        .requester(userName)
        .groupIdentifier(group)
        .structureId(structureId)
        .build();
  }

  private static String getStructureId(Task task, Action action ){
    String structureId = null;
    if(ActionUtils.isActionForLocation(action)){
      structureId = task.getLocation().getIdentifier().toString();
    } else {
      //other entity we have is Person for now
      Person person = task.getPerson();
      Optional<Location> personLocation = person.getLocations().stream().findFirst();//TODO; update needed here , we are getting first but there could be based on model design
      if(personLocation.isPresent()){
        structureId = personLocation.get().getIdentifier().toString();
      }
    }
    return structureId;
  }


}
