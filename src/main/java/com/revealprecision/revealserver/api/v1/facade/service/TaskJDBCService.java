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
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
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

  public List<Task> getTasksByPlanAndJurisdictionList(UUID planIdentifier,
      List<String> jurisdictionList) {

    List<UUID> locationUIIDs = new ArrayList<>();
    List<UUID> parentLocationUUIDs = new ArrayList<>();
    jurisdictionList.forEach(
        location -> {
          if (locationRelationshipService.findLocationRelationshipUiidsByParentLocationIdentifier(
              UUID.fromString(location)).size() > 0) {
            parentLocationUUIDs.add(UUID.fromString(location));
          } else {
            locationUIIDs.add(UUID.fromString(location));
          }
        });

    return jdbcTemplate.query(
        getTaskQueryForPlanAndGroupIds(planIdentifier, locationUIIDs, parentLocationUUIDs),
        (rs, rowNum) -> buildTask(rs)

    );

  }

  private String getTaskQueryForPlanAndGroupIds(UUID planIdentifier, List<UUID> locationUIIDs,
      List<UUID> parentLocationUUIDs) {

    String locationUUIDString = locationUIIDs.stream()
        .map(location -> "'".concat(location.toString()).concat("'"))
        .collect(Collectors.joining(","));
    String parentLocationUUIDString = parentLocationUUIDs.stream()
        .map(location -> "'".concat(location.toString()).concat("'"))
        .collect(Collectors.joining(","));

    return "SELECT locationsOfTasksInPlan.* ,\n"
        + "   l.name                                as location_name,\n"
        + "   l.geometry                as location_geometry,\n"
        + "   l.type                                as location_type,\n"
        + "   l.status                   as location_status,\n"
        + "   l.external_id                as location_external_id,\n"
        + "   l.geographic_level_identifier                as location_geographic_level_identifier,\n"
        + "   l.entity_status                as location_entity_status,\n"
        + "   l.location_bulk_identifier                as location_location_bulk_identifier,\n"
        + "   l.created_by                as location_created_by,\n"
        + "   l.created_datetime                as location_created_datetime,\n"
        + "   l.modified_by                as location_modified_by,\n"
        + "   l.modified_datetime                as location_modified_datetimefrom \n" + "   FROM\n"
        + "(select \n"
        + "   (case \n"
        + "      WHEN tp.identifier IS NOT NULL THEN pl.location_identifier \n"
        + "      WHEN tl.identifier IS NOT NULL THEN tl.location_identifier\n"
        + "   END) as location_identifier,\n"
        + "   (case \n"
        + "      WHEN tp.identifier IS NOT NULL THEN pl.person_identifier \n"
        + "      WHEN tl.identifier IS NOT NULL THEN tl.location_identifier\n"
        + "   END) as entity_identifier,\n"
        + "   t.identifier                as task_identifier,\n"
        + "   t.entity_status                as task_entity_status,\n"
        + "   t.created_by                as task_created_by,\n"
        + "   t.created_datetime                as task_created_datetime,\n"
        + "   t.modified_by                as task_modified_by,\n"
        + "   t.modified_datetime                as task_modified_datetime,\n"
        + "   t.base_entity_identifier           as task_base_entity_identifier, \n"
        + "   t.priority                as task_priority,\n"
        + "   t.authored_on                as task_authored_on,\n"
        + "   t.description                as task_description,\n"
        + "   t.last_modified                as task_last_modified,\n"
        + "   t.execution_period_start                as task_execution_period_start,\n"
        + "   t.execution_period_end                as task_execution_period_end,\n"
        + "   t.lookup_task_status_identifier                as task_lookup_task_status_identifier,\n"
        + "   t.action_identifier                as task_action_identifier,\n"
        + "   lts.identifier                as lookup_task_status_identifier,\n"
        + "   lts.name                   as lookup_task_status_name,\n"
        + "   lts.code                   as lookup_task_status_code,\n"
        + "   lts.entity_status                as lookup_task_status_entity_status,\n"
        + "   lts.created_by                as lookup_task_status_created_by,\n"
        + "   lts.created_datetime                as lookup_task_status_created_datetime,\n"
        + "   lts.modified_by                as lookup_task_status_modified_by,\n"
        + "   lts.modified_datetime                as lookup_task_status_modified_datetime,\n"
        + "   a.identifier                as action_identifier,\n"
        + "   a.title                                as action_title,\n"
        + "   a.description                as action_description,\n"
        + "   a.timing_period_start                as action_timing_period_start,\n"
        + "   a.timing_period_end                as action_timing_period_end,\n"
        + "   a.form_identifier                as action_form_identifier,\n"
        + "   a.goal_identifier                as action_goal_identifier,\n"
        + "   a.type                                as action_type,\n"
        + "   a.entity_status                as action_entity_status,\n"
        + "   a.created_by                as action_created_by,\n"
        + "   a.created_datetime                as action_created_datetime,\n"
        + "   a.modified_by                as action_modified_by,\n"
        + "   a.modified_datetime                as action_modified_datetime,\n"
        + "   a.lookup_entity_type_identifier                as action_lookup_entity_type_identifier,\n"
        + "   g.identifier                as goal_identifier,\n"
        + "   g.description                as goal_description,\n"
        + "   g.priority                as goal_priority,\n"
        + "   g.plan_identifier                as goal_plan_identifier,\n"
        + "   g.entity_status                as goal_entity_status,\n"
        + "   g.created_by                as goal_created_by,\n"
        + "   g.created_datetime                as goal_created_datetime,\n"
        + "   g.modified_by                as goal_modified_by,\n"
        + "   g.modified_datetime                as goal_modified_datetime,\n"
        + "   p.identifier                                as plan_identifier,\n"
        + "   p.name                                      as plan_name,\n"
        + "   p.title                                     as plan_title,\n"
        + "   p.status                                    as plan_status,\n"
        + "   p.date                                      as plan_date,\n"
        + "   p.effective_period_start                    as plan_effective_period_start,\n"
        + "   p.effective_period_end                      as plan_effective_period_end,\n"
        + "   p.lookup_intervention_type_identifier       as plan_lookup_intervention_type_identifier,\n"
        + "   p.hierarchy_identifier                      as plan_hierarchy_identifier,\n"
        + "   p.entity_status                             as plan_entity_status,\n"
        + "   p.created_by                                as plan_created_by,\n"
        + "   p.created_datetime                          as plan_created_datetime,\n"
        + "   p.modified_by                               as plan_modified_by,\n"
        + "   p.modified_datetime                         as plan_modified_datetime,\n"
        + "   lit.identifier                as lookup_intervention_type_identifier,\n"
        + "   lit.name                as lookup_intervention_type_name,\n"
        + "   lit.code                as lookup_intervention_type_code,\n"
        + "   lit.entity_status                as lookup_intervention_type_entity_status,\n"
        + "   lit.created_by                as lookup_intervention_type_created_by,\n"
        + "   lit.created_datetime                as lookup_intervention_type_created_datetime,\n"
        + "   lit.modified_by                as lookup_intervention_type_modified_by,\n"
        + "   lit.modified_datetime                as lookup_intervention_type_modified_datetime,\n"
        + "   let.identifier                as lookup_entity_type_identifier,\n"
        + "   let.code                as lookup_entity_type_code\n"
        + "   from task t \n"
        + "inner join action a on t.action_identifier = a.identifier\n"
        + "left join lookup_entity_type let on let.identifier = a.lookup_entity_type_identifier\n"
        + "inner join goal g on g.identifier = a.goal_identifier\n"
        + "inner join plan p on p.identifier = g.plan_identifier\n"
        + "left join task_location tl on tl.task_identifier = t.identifier\n"
        + "left join task_person tp on tp.task_identifier = t.identifier\n"
        + "left join person pn on pn.identifier = tp.person_identifier\n"
        + "left join person_location pl on pl.person_identifier = pn.identifier\n"
        + "left join lookup_task_status lts on lts.identifier = t.lookup_task_status_identifier\n"
        + "left join lookup_intervention_type lit on lit.identifier = p.lookup_intervention_type_identifier\n"
        + "where p.identifier='" + planIdentifier + "'\n" + "   )  as locationsOfTasksInPlan \n"
        + "   inner join  location_relationship lr on lr.location_identifier = locationsOfTasksInPlan.location_identifier \n"
        + "   LEFT join location l on l.identifier =locationsOfTasksInPlan.location_identifier\n"
        + ((parentLocationUUIDs.size() > 0 || locationUIIDs.size() > 0) ? " WHERE " : " ") + " \n"
        + (parentLocationUUIDs.size() > 0 ? " (ARRAY[" + parentLocationUUIDString
        + "]::uuid[] && lr.ancestry) \n" : " ")
        + ((parentLocationUUIDs.size() > 0 && locationUIIDs.size() > 0) ? " OR " : " ") + " \n"
        + (locationUIIDs.size() > 0 ? " ( lr.location_identifier in (" + locationUUIDString + ") )"
        : "");
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
