package com.revealprecision.revealserver.persistence.specification;

import static com.revealprecision.revealserver.enums.WhereClauseEnum.AND;

import com.revealprecision.revealserver.enums.WhereClauseEnum;
import com.revealprecision.revealserver.persistence.domain.Action;
import com.revealprecision.revealserver.persistence.domain.Goal;
import com.revealprecision.revealserver.persistence.domain.Location;
import com.revealprecision.revealserver.persistence.domain.LookupTaskStatus;
import com.revealprecision.revealserver.persistence.domain.Plan;
import com.revealprecision.revealserver.persistence.domain.Task;
import com.revealprecision.revealserver.service.models.TaskSearchCriteria;
import java.util.UUID;
import javax.persistence.criteria.JoinType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.jpa.domain.Specification;

@Slf4j
public class TaskSpec {

  public static Specification<Task> getTaskSpecification(TaskSearchCriteria taskSearchCriteria) {

    Specification<Task> taskSpecification = Specification.where(null);

    if (taskSearchCriteria.getPlanIdentifier() != null) {
      taskSpecification = getSpecification(taskSpecification
          , wherePlanIdentifierEquals(taskSearchCriteria.getPlanIdentifier()), AND);
    }
    if (taskSearchCriteria.getTaskStatusIdentifier() != null) {
      taskSpecification = getSpecification(taskSpecification
          , whereTaskStatusIdentifierEquals(taskSearchCriteria.getTaskStatusIdentifier()), AND);
    }
    if (taskSearchCriteria.getActionIdentifier() != null) {
      taskSpecification = getSpecification(taskSpecification
          , whereActionIdentifierEquals(taskSearchCriteria.getActionIdentifier()), AND);
    }
    if (taskSearchCriteria.getLocationIdentifier() != null) {
      taskSpecification = getSpecification(taskSpecification
          , whereLocationIdentifierEquals(taskSearchCriteria.getLocationIdentifier()), AND);
    }
    return taskSpecification;
  }

  private static Specification<Task> wherePlanIdentifierEquals(UUID planIdentifier) {
    return (root, query, criteriaBuilder) -> criteriaBuilder.equal(
        root.get("action").<Action>get("goal").<Goal>get("plan").<Plan>get("identifier"), planIdentifier);
  }

  private static Specification<Task> whereLocationIdentifierEquals(UUID planIdentifier) {
    return (root, query, criteriaBuilder) -> criteriaBuilder.equal(
        root.join("locations", JoinType.INNER).<Location>get("identifier"), planIdentifier);
  }

  private static Specification<Task> whereActionIdentifierEquals(UUID actionIdentifier) {
    return (root, query, criteriaBuilder) -> criteriaBuilder.equal(
        root.get("actionIdentifier"), actionIdentifier);
  }

  private static Specification<Task> whereTaskStatusIdentifierEquals(UUID identifier) {
    return (root, query, criteriaBuilder) -> criteriaBuilder.equal(
        root.get("lookupTaskStatus").<LookupTaskStatus>get("identifier"), identifier);
  }


  private static Specification<Task> getSpecification(
      Specification<Task> original,
      Specification<Task> additional, WhereClauseEnum type) {
    if (type.equals(AND)) {
      return original.and(additional);
    } else {
      return original.or(additional);
    }
  }

}
