package com.revealprecision.revealserver.persistence.specification;

import static com.revealprecision.revealserver.enums.WhereClauseEnum.AND;

import com.revealprecision.revealserver.enums.WhereClauseEnum;
import com.revealprecision.revealserver.persistence.domain.Event;
import com.revealprecision.revealserver.persistence.domain.Organization;
import com.revealprecision.revealserver.persistence.domain.User;
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

    if (eventSearchCriteria.getBaseIdentifiers() != null) {
      eventSpecification = getSpecification(eventSpecification,
          whereBaseIdentifierIn(eventSearchCriteria.getBaseIdentifiers()), AND);
    }
    if (eventSearchCriteria.getOrganizationIdentifiers() != null) {
      eventSpecification = getSpecification(eventSpecification,
          whereOrganizationIdIn(eventSearchCriteria.getOrganizationIdentifiers()), AND);
    }
    if (eventSearchCriteria.getOrganizationNames() != null) {
      eventSpecification = getSpecification(eventSpecification,
          whereOrganizationNameIn(eventSearchCriteria.getOrganizationNames()), AND);
    }
    if (eventSearchCriteria.getUserIdentifiers() != null) {
      eventSpecification = getSpecification(eventSpecification,
          whereUserIdentifierIn(eventSearchCriteria.getUserIdentifiers()), AND);
    }
    if (eventSearchCriteria.getLocationIdentifiers() != null) {
      eventSpecification = getSpecification(eventSpecification,
          whereLocationIn(eventSearchCriteria.getLocationIdentifiers()), AND);
    }
    if (eventSearchCriteria.getUserNames() != null) {
      eventSpecification = getSpecification(eventSpecification,
          whereUserNamesIn(eventSearchCriteria.getUserNames()), AND);
    }

    if(eventSearchCriteria.getServerVersion() != null) {
      eventSpecification = getSpecification(eventSpecification,
          whereServerVersionIsGreaterThan(eventSearchCriteria.getServerVersion()), AND);
    }
    return eventSpecification;
  }

  private static Specification<Event> whereBaseIdentifierIn(List<UUID> baseIdentifiers) {
    return (root, query, criteriaBuilder) -> root.get("baseEntityIdentifier")
        .in(baseIdentifiers);
  }

  private static Specification<Event> whereUserIdentifierIn(List<UUID> providerIdentifiers) {
    return (root, query, criteriaBuilder) -> root.get("user").<User>get("identifier").in(providerIdentifiers);
  }

  private static Specification<Event> whereLocationIn(
      List<UUID> locationIdentifiers) {
    return (root, query, criteriaBuilder) -> root.get("locationIdentifier")
        .in(locationIdentifiers);
  }

  private static Specification<Event> whereOrganizationNameIn(List<String> organizationNames) {
    return (root, query, criteriaBuilder) -> root.get("organization")
        .<Organization>get("name").in(organizationNames);
  }

  private static Specification<Event> whereUserNamesIn(List<String> usernames) {
    return (root, query, criteriaBuilder) -> root.get("user").<User>get("username").in(usernames);
  }

  private static Specification<Event> whereOrganizationIdIn(List<UUID> identifiers) {
    return (root, query, criteriaBuilder) -> root.get("organization")
        .<Organization>get("identifier").in(identifiers);
  }

  private static Specification<Event> whereServerVersionIsGreaterThan(Long serverVersion){
    return (root, query, criteriaBuilder) -> criteriaBuilder.greaterThan(root.get("serverVersion"),serverVersion);
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
