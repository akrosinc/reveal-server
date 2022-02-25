package com.revealprecision.revealserver.api.v1.facade.service;

import static com.revealprecision.revealserver.api.v1.facade.constants.JDBCHelperConstants.BUSINESS_STATUS;
import static com.revealprecision.revealserver.api.v1.facade.constants.JDBCHelperConstants.LOCATION;
import static com.revealprecision.revealserver.api.v1.facade.constants.JDBCHelperConstants.PERSON;

import com.revealprecision.revealserver.api.v1.facade.factory.TaskFacadeFactory;
import com.revealprecision.revealserver.api.v1.facade.models.TaskFacade;
import com.revealprecision.revealserver.persistence.domain.Task;
import com.revealprecision.revealserver.persistence.domain.User;
import com.revealprecision.revealserver.service.UserService;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class TaskFacadeService {

  private final MetaDataJdbcService metaDataJdbcService;

  private final UserService userService;

  private final TaskJDBCService taskJDBCService;

  @Autowired
  public TaskFacadeService(MetaDataJdbcService metaDataJdbcService, UserService userService,
      TaskJDBCService taskJDBCService) {
    this.metaDataJdbcService = metaDataJdbcService;
    this.userService = userService;
    this.taskJDBCService = taskJDBCService;
  }

  public List<TaskFacade> syncTasks(String plan, String group) {
    return taskJDBCService.getTasksByPlanAndJurisdictionList(UUID.fromString(plan),
            Arrays.asList(group.split(",")))
        .stream().map(task -> {
          Object businessStatus = getBusinessStatus(task);
          String createdBy = task.getAction().getGoal().getPlan().getCreatedBy();
          User user = userService.getByIdentifier(UUID.fromString(createdBy));

          return TaskFacadeFactory.getEntity(task, (String) businessStatus, user.getUsername(),
              group);

        }).collect(Collectors.toList());
  }

  private Object getBusinessStatus(Task task) {
    Object businessStatus = null;
    if (task.getLocation() != null) {
      Pair<Class<?>, Object> locationMetadata = metaDataJdbcService.getMetadataFor(LOCATION,
          task.getLocation().getIdentifier()).get(BUSINESS_STATUS);
      if (locationMetadata != null) {
        if (locationMetadata.getKey() != null) {
          Class<?> aClass = locationMetadata.getKey();
          businessStatus = aClass.cast(locationMetadata.getValue());
        }
      }
    }
    if (task.getPerson() != null) {
      Pair<Class<?>, Object> personMetadata = metaDataJdbcService.getMetadataFor(PERSON,
          task.getPerson().getIdentifier()).get(BUSINESS_STATUS);
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
