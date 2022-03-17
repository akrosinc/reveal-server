package com.revealprecision.revealserver.api.v1.facade.service;

import com.revealprecision.revealserver.enums.ActionTypeEnum;
import com.revealprecision.revealserver.enums.EntityStatus;
import com.revealprecision.revealserver.enums.LookupUtil;
import com.revealprecision.revealserver.enums.PlanStatusEnum;
import com.revealprecision.revealserver.enums.PriorityEnum;
import com.revealprecision.revealserver.enums.TaskPriorityEnum;
import com.revealprecision.revealserver.persistence.domain.Action;
import com.revealprecision.revealserver.persistence.domain.Goal;
import com.revealprecision.revealserver.persistence.domain.Goal.GoalBuilder;
import com.revealprecision.revealserver.persistence.domain.Location;
import com.revealprecision.revealserver.persistence.domain.LocationHierarchy;
import com.revealprecision.revealserver.persistence.domain.LookupEntityType;
import com.revealprecision.revealserver.persistence.domain.LookupInterventionType;
import com.revealprecision.revealserver.persistence.domain.LookupTaskStatus;
import com.revealprecision.revealserver.persistence.domain.Person;
import com.revealprecision.revealserver.persistence.domain.Plan;
import com.revealprecision.revealserver.persistence.domain.Task;
import com.revealprecision.revealserver.service.LocationRelationshipService;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class TaskJDBCService {

  private final JdbcTemplate jdbcTemplate;
  private final LocationRelationshipService locationRelationshipService;

  @Autowired
  public TaskJDBCService(JdbcTemplate jdbcTemplate,
      LocationRelationshipService locationRelationshipService) {
    this.locationRelationshipService = locationRelationshipService;
    this.jdbcTemplate = jdbcTemplate;
  }

  private Task buildTask(ResultSet rs) throws SQLException {

    LookupInterventionType lookupInterventionType = getLookupInterventionType(rs);

    LocationHierarchy locationHierarchy = getLocationHierarchy(
        rs);

    Plan plan = getPlan(rs);
    plan.setInterventionType(lookupInterventionType);
    plan.setLocationHierarchy(locationHierarchy);

    Goal goal = getGoal(rs);
    goal.setPlan(plan);

    LookupEntityType lookupEntityType = getLookupEntityType(rs);

    Action action = getAction(rs, lookupEntityType);
    action.setGoal(goal);

    LookupTaskStatus lookupTaskStatus = getLookupTaskStatus(rs);

    Task task = getTask(rs);
    task.setAction(action);
    task.setLookupTaskStatus(lookupTaskStatus);

    if (action.getLookupEntityType().getCode().equals("Person")) {
      task.setPerson(getPerson(rs));
    }
    if (action.getLookupEntityType().getCode().equals("Location")) {
      task.setLocation(getLocation(rs));
    }

    return task;
  }

  private Person getPerson(ResultSet rs) throws SQLException {
    return Person.builder()
        .identifier((UUID) rs.getObject("entity_identifier"))
        .build();
  }

  private Location getLocation(ResultSet rs) throws SQLException {
    return Location.builder()
        .identifier((UUID) rs.getObject("entity_identifier"))
        .build();
  }

  private Task getTask(ResultSet rs)
      throws SQLException {
    return Task.builder()
        .authoredOn(rs.getDate("task_authored_on").toLocalDate().atStartOfDay())
        .description(rs.getString("task_description"))
        .lastModified(rs.getDate("task_last_modified").toLocalDate().atStartOfDay())
        .priority(TaskPriorityEnum.valueOf(rs.getString("task_priority")))
        .executionPeriodEnd(rs.getDate("task_execution_period_end").toLocalDate())
        .executionPeriodStart(rs.getDate("task_execution_period_start").toLocalDate())
        .identifier((UUID) rs.getObject("task_identifier"))
        .build();
  }

  private LookupTaskStatus getLookupTaskStatus(ResultSet rs) throws SQLException {
    return LookupTaskStatus.builder()
        .code(rs.getString("lookup_task_status_code"))
        .name(rs.getString("lookup_task_status_name"))
        .identifier((UUID) rs.getObject("lookup_task_status_identifier"))
        .build();
  }

  private Action getAction(ResultSet rs, LookupEntityType lookupEntityType) throws SQLException {
    return Action.builder().description(rs.getString("action_description"))
        .title(rs.getString("action_title")).identifier((UUID) rs.getObject("action_identifier"))
        .timingPeriodEnd(rs.getDate("action_timing_period_end").toLocalDate())
        .timingPeriodStart(rs.getDate("action_timing_period_start").toLocalDate())
        .type(LookupUtil.lookup(ActionTypeEnum.class, rs.getString("action_type")))
        .lookupEntityType(lookupEntityType)
        .build();
  }

  private LookupEntityType getLookupEntityType(ResultSet rs) throws SQLException {
    return LookupEntityType.builder()
        .code(rs.getString("lookup_entity_type_code"))
        .build();
  }

  private Goal getGoal(ResultSet rs) throws SQLException {
    GoalBuilder goalBuilder = Goal.builder().description(rs.getString("goal_description"))
        .identifier((UUID) rs.getObject("goal_identifier"))
        .priority(PriorityEnum.valueOf(rs.getString("goal_priority")));

    Goal goal = goalBuilder.build();
    goal.setCreatedBy(rs.getString("goal_created_by"));
    goal.setModifiedBy(rs.getString("goal_modified_by"));
    goal.setCreatedDatetime(rs.getDate("goal_created_datetime").toLocalDate().atStartOfDay());
    goal.setModifiedDatetime(rs.getDate("goal_modified_datetime").toLocalDate().atStartOfDay());
    return goal;
  }

  private Plan getPlan(ResultSet rs) throws SQLException {
    Plan.PlanBuilder planBuilder = Plan.builder();
    planBuilder.date(rs.getDate("plan_date").toLocalDate())
        .identifier((UUID) rs.getObject("plan_identifier")).title(rs.getString("plan_title"))

        .status(PlanStatusEnum.valueOf(rs.getString("plan_status")))
        .effectivePeriodEnd(rs.getDate("plan_effective_period_start").toLocalDate())
        .effectivePeriodEnd(rs.getDate("plan_effective_period_end").toLocalDate());

    Plan plan = planBuilder.build();
    plan.setCreatedBy(rs.getString("plan_created_by"));
    plan.setModifiedBy(rs.getString("plan_modified_by"));
    plan.setCreatedDatetime(rs.getDate("plan_created_datetime").toLocalDate().atStartOfDay());
    plan.setModifiedDatetime(rs.getDate("plan_modified_datetime").toLocalDate().atStartOfDay());
    plan.setEntityStatus(EntityStatus.valueOf(rs.getString("plan_entity_status")));
    return plan;
  }

  private LocationHierarchy getLocationHierarchy(ResultSet rs) throws SQLException {
    return LocationHierarchy.builder()
        .identifier((UUID) rs.getObject("plan_hierarchy_identifier")).build();
  }

  private LookupInterventionType getLookupInterventionType(ResultSet rs) throws SQLException {
    return LookupInterventionType.builder()
        .code(rs.getString("lookup_intervention_type_code"))
        .name(rs.getString("lookup_intervention_type_name"))
        .identifier((UUID) rs.getObject("lookup_intervention_type_identifier")).build();
  }
}
