package com.revealprecision.revealserver.persistence.specification;

import static com.revealprecision.revealserver.enums.WhereClauseEnum.AND;

import com.revealprecision.revealserver.enums.TaskStatusEnum;
import com.revealprecision.revealserver.enums.WhereClauseEnum;
import com.revealprecision.revealserver.persistence.domain.Plan;
import com.revealprecision.revealserver.persistence.domain.Task;
import com.revealprecision.revealserver.service.models.TaskSearchCriteria;
import java.util.UUID;
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
    if (taskSearchCriteria.getCode() != null) {
      taskSpecification = getSpecification(taskSpecification
          , whereCodeEquals(taskSearchCriteria.getCode()), AND);
    }
    if (taskSearchCriteria.getCode() != null) {
      taskSpecification = getSpecification(taskSpecification
          , whereCodeEquals(taskSearchCriteria.getCode()), AND);
    }
    if (taskSearchCriteria.getTaskStatus() != null) {
      taskSpecification = getSpecification(taskSpecification
          , whereStatusEquals(taskSearchCriteria.getTaskStatus()), AND);
    }
    return taskSpecification;
  }

  private static Specification<Task> wherePlanIdentifierEquals(UUID planIdentifier) {
    return (root, query, criteriaBuilder) -> criteriaBuilder.equal(
        root.get("plan").<Plan>get("identifier"), planIdentifier);
  }

  private static Specification<Task> whereCodeEquals(String code) {
    return (root, query, criteriaBuilder) -> criteriaBuilder.equal(
        root.get("code"), code);
  }

  private static Specification<Task> whereStatusEquals(TaskStatusEnum statusEnum) {
    return (root, query, criteriaBuilder) -> criteriaBuilder.equal(
        root.get("status"), statusEnum);
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
