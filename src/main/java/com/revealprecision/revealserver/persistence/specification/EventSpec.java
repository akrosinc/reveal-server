package com.revealprecision.revealserver.persistence.specification;

import static com.revealprecision.revealserver.enums.WhereClauseEnum.AND;

import com.revealprecision.revealserver.enums.WhereClauseEnum;
import com.revealprecision.revealserver.persistence.domain.Event;
import com.revealprecision.revealserver.persistence.domain.Location;
import com.revealprecision.revealserver.persistence.domain.Organization;
import com.revealprecision.revealserver.persistence.domain.Task;
import com.revealprecision.revealserver.service.models.EventSearchCriteria;
import java.util.List;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.jpa.domain.Specification;

@Slf4j
public class EventSpec {

  public static Specification<Event> getEventSpecification(
      EventSearchCriteria eventSearchCriteria) {

    Specification<Event> eventSpecification = Specification.where(null);

    if (eventSearchCriteria.getBaseIdentifier() != null) {
      eventSpecification = getSpecification(eventSpecification,
          whereBaseIdentifierEquals(eventSearchCriteria.getBaseIdentifier()), AND);
    }
    if (eventSearchCriteria.getOrganizationIdentifier() != null) {
      eventSpecification = getSpecification(eventSpecification,
          whereOrganizationIdEquals(eventSearchCriteria.getOrganizationIdentifier()), AND);
    }
    if (eventSearchCriteria.getOrganizationName() != null) {
      eventSpecification = getSpecification(eventSpecification,
          whereOrganizationNameEquals(eventSearchCriteria.getOrganizationName()), AND);
    }
    if (eventSearchCriteria.getProviderIdentifier() != null) {
      eventSpecification = getSpecification(eventSpecification,
          whereProvideIdEquals(eventSearchCriteria.getProviderIdentifier()), AND);
    }
    if (eventSearchCriteria.getLocationIdentifier() != null) {
      eventSpecification = getSpecification(eventSpecification,
          whereLocationIdentifierEquals(eventSearchCriteria.getLocationIdentifier()), AND);
    }
    return eventSpecification;
  }

  private static Specification<Event> whereBaseIdentifierEquals(List<UUID> baseIdentifiers) {
    return (root, query, criteriaBuilder) -> root.get("task").<Task>get("baseIdentifier")
        .in(baseIdentifiers);
  }

  private static Specification<Event> whereProvideIdEquals(List<UUID> providerIdentifiers) {
    return (root, query, criteriaBuilder) -> root.get("userIdentifier").in(providerIdentifiers);
  }

  private static Specification<Event> whereLocationIdentifierEquals(
      List<UUID> locationIdentifiers) {
    return (root, query, criteriaBuilder) -> root.get("location").<Location>get("identifier")
        .in(locationIdentifiers);
  }

  private static Specification<Event> whereOrganizationNameEquals(List<String> organizationNames) {
    return (root, query, criteriaBuilder) -> root.get("task").<Task>get("organization")
        .<Organization>get("name").in(organizationNames);
  }

  private static Specification<Event> whereOrganizationIdEquals(List<UUID> identifiers) {
    return (root, query, criteriaBuilder) -> root.get("task").<Task>get("organization")
        .<Organization>get("identifier").in(identifiers);
  }


  private static Specification<Event> getSpecification(Specification<Event> original,
      Specification<Event> additional, WhereClauseEnum type) {
    if (type.equals(AND)) {
      return original.and(additional);
    } else {
      return original.or(additional);
    }
  }

}
