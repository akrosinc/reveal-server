package com.revealprecision.revealserver.persistence.specification;

import static com.revealprecision.revealserver.enums.SearchTypeEnum.EQUALS;
import static com.revealprecision.revealserver.enums.SearchTypeEnum.LIKE;
import static com.revealprecision.revealserver.enums.WhereClauseEnum.AND;
import static com.revealprecision.revealserver.enums.WhereClauseEnum.OR;

import com.revealprecision.revealserver.enums.GenderEnum;
import com.revealprecision.revealserver.enums.SearchTypeEnum;
import com.revealprecision.revealserver.enums.WhereClauseEnum;
import com.revealprecision.revealserver.persistence.domain.Group;
import com.revealprecision.revealserver.persistence.domain.Location;
import com.revealprecision.revealserver.persistence.domain.Person;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.UUID;
import javax.persistence.criteria.JoinType;
import javax.persistence.criteria.SetJoin;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.jpa.domain.Specification;

@Slf4j
public class PersonSpec {

  public static Specification<Person> getPersonSpecification(String firstName, String lastName,
      GenderEnum gender, String locationName, UUID locationIdentifier, String groupName,
      UUID groupIdentifier, LocalDate birthDate, LocalDate fromDate, LocalDate toDate) {

    Specification<Person> personSpecification = Specification.where(null);


    if (groupName != null) {
      personSpecification = PersonSpec.getSpecification(personSpecification,
          PersonSpec.whereGroupNameIgnoreCase(EQUALS, groupName), AND);
    }

    if (locationName != null) {
      personSpecification = PersonSpec.getSpecification(personSpecification,
          PersonSpec.whereLocationNameIgnoreCase(EQUALS, locationName), AND);
    }

    if (locationIdentifier != null) {
      personSpecification = PersonSpec.getSpecification(personSpecification,
          PersonSpec.whereLocationByIdentifier(locationIdentifier), AND);
    }

    if (groupIdentifier != null) {
      personSpecification = PersonSpec.getSpecification(personSpecification,
          PersonSpec.whereGroupIdentifierEquals(locationIdentifier), AND);
    }

    if (firstName != null) {
      personSpecification = PersonSpec.getSpecification(personSpecification,
          PersonSpec.whereGivenNameOrTextNameIgnoreCase(EQUALS, firstName), AND);
    }

    if (lastName != null) {
      personSpecification = PersonSpec.getSpecification(personSpecification,
          PersonSpec.whereFamilyNameOrNameSuffixIgnoreCase(EQUALS, lastName), AND);
    }

    if (gender != null) {
      personSpecification = PersonSpec.getSpecification(personSpecification,
          PersonSpec.whereGender(EQUALS, gender), AND);
    }

    if (birthDate != null) {
      personSpecification = PersonSpec.getSpecification(personSpecification,
          PersonSpec.whereBirthDateEquals(
              Date.from(birthDate.atStartOfDay(ZoneId.systemDefault()).toInstant())), AND);

    } else {

      if (fromDate != null && toDate != null) {
        Date from = Date.from(fromDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
        Date to = Date.from(toDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
        personSpecification = personSpecification.and(PersonSpec.whereBirthDateBetween(from, to));
      }
    }
    return personSpecification;
  }

  public static Specification<Person> getOrSearchByNamePersonSpecification(String firstName,
      String lastName) {

    Specification<Person> personSpecification = Specification.where(null);


    if (firstName != null) {
      personSpecification = PersonSpec.getSpecification(personSpecification,
          PersonSpec.whereGivenNameOrTextNameIgnoreCase(LIKE, firstName), OR);
    }

    if (lastName != null) {
      personSpecification = PersonSpec.getSpecification(personSpecification,
          PersonSpec.whereFamilyNameOrNameSuffixIgnoreCase(LIKE, lastName), OR);
    }

    return personSpecification;
  }


  private static Specification<Person> whereGroupNameIgnoreCase(SearchTypeEnum type, String groupName) {
    switch (type) {
      case EQUALS:
        return (root, query, criteriaBuilder) -> criteriaBuilder.equal(
            criteriaBuilder.lower(root.join("groups").<Group>get("name").as(String.class)), groupName.toLowerCase());
      case LIKE:
        return (root, query, criteriaBuilder) -> criteriaBuilder.like(
            criteriaBuilder.lower(root.join("groups").<Group>get("name").as(String.class)),
            groupName.toLowerCase());
    }
    return null;
  }

  private static Specification<Person> whereGroupIdentifierEquals(UUID groupIdentifier) {

    return (root, query, criteriaBuilder) -> criteriaBuilder.equal(
        root.join("groups").<Group>get("identifier"), groupIdentifier);

  }

  private static Specification<Person> whereLocationNameIgnoreCase(SearchTypeEnum type,
      String locationName) {

    switch (type) {
      case EQUALS:
        return (root, query, criteriaBuilder) -> criteriaBuilder.equal(
            criteriaBuilder.lower(root.join("groups").join("location").<Location>get("name").as(String.class)), locationName.toLowerCase());
      case LIKE:
        return (root, query, criteriaBuilder) -> {
          SetJoin<Object, Object> groups = root.joinSet("groups", JoinType.LEFT);
          return criteriaBuilder.like(criteriaBuilder.lower(
                  groups.join("location", JoinType.LEFT).<Location>get("name").as(String.class)),
              "%".concat(locationName).concat("%").toLowerCase());
        };
    }
    return null;
  }

  private static Specification<Person> whereLocationByIdentifier(UUID locationIdentifier) {
    // person join group g on ... join location l on g.location_identifier = l.identifier where l.identifier = "?"
    return (root, query, criteriaBuilder) -> criteriaBuilder.equal(
        root.join("groups").join("location").<Location>get("identifier"), locationIdentifier);
  }


  private static Specification<Person> whereGivenNameOrTextNameIgnoreCase(SearchTypeEnum type,
      String firstName) {

    switch (type) {
      // where lower(person.nameText) = lower("?") or lower(group.nameGiven) = lower("?")
      case EQUALS:
        return (root, query, criteriaBuilder) -> criteriaBuilder.or(
            criteriaBuilder.equal(criteriaBuilder.lower(root.get("nameText").as(String.class)), firstName.toLowerCase()),
            criteriaBuilder.equal(criteriaBuilder.lower(root.get("nameGiven").as(String.class)), firstName.toLowerCase()));

      // where person.nameText like "%?%" or group.nameGiven = "%?%"
      case LIKE:
        return (root, query, criteriaBuilder) -> criteriaBuilder.or(
            criteriaBuilder.like(criteriaBuilder.lower(root.get("nameText")),
                "%".concat(firstName).concat("%").toLowerCase()),
            criteriaBuilder.like(criteriaBuilder.lower(root.get("nameGiven")),
                "%".concat(firstName).concat("%").toLowerCase()));
    }
    return null;
  }

  private static Specification<Person> whereFamilyNameOrNameSuffixIgnoreCase(SearchTypeEnum type,
      String lastname) {

    switch (type) {
      case EQUALS:
        return (root, query, criteriaBuilder) -> criteriaBuilder.or(
            criteriaBuilder.equal(criteriaBuilder.lower(root.get("nameFamily").as(String.class)), lastname.toLowerCase()),
            criteriaBuilder.equal(criteriaBuilder.lower(root.get("nameSuffix").as(String.class)), lastname.toLowerCase()));

      case LIKE:
        return (root, query, criteriaBuilder) -> criteriaBuilder.or(
            criteriaBuilder.like(criteriaBuilder.lower(root.get("nameFamily")),
                "%".concat(lastname).concat("%").toLowerCase()),
            criteriaBuilder.like(criteriaBuilder.lower(root.get("nameSuffix")),
                "%".concat(lastname).concat("%").toLowerCase()));
    }
    return null;

  }

  private static Specification<Person> whereBirthDateEquals(Date birthdate) {
    return (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("birthDate"),
        birthdate);
  }

  private static Specification<Person> whereGender(SearchTypeEnum type, GenderEnum gender) {

    switch (type) {
      case EQUALS:
        return (root, query, criteriaBuilder) -> criteriaBuilder.equal(criteriaBuilder.lower(root.get("gender").as(String.class)), gender.name().toLowerCase());

      case LIKE:
        return (root, query, criteriaBuilder) -> criteriaBuilder.like(
            criteriaBuilder.lower(root.get("gender")),
            "%".concat(gender.name()).concat("%").toLowerCase());
    }
    return null;
  }

  private static Specification<Person> whereBirthDateBetween(Date fromDate, Date toDate) {
    return (root, query, criteriaBuilder) -> criteriaBuilder.and(
        criteriaBuilder.between(root.get("birthDate"), fromDate, toDate));
  }

  private static Specification<Person> getSpecification(
      Specification<Person> originalPersonSpecification,
      Specification<Person> additionPersonSpecification, WhereClauseEnum type) {
    if (type.equals(AND)) {
      return originalPersonSpecification.and(additionPersonSpecification);
    } else {
      return originalPersonSpecification.or(additionPersonSpecification);
    }
  }

  public static Specification<Person> getBlankPredicate() {
    return (root, query, criteriaBuilder) -> null;
  }

}
