package com.revealprecision.revealserver.persistence.specification;

import static com.revealprecision.revealserver.enums.StringSearchType.EQUALS;
import static com.revealprecision.revealserver.enums.StringSearchType.LIKE;

import com.revealprecision.revealserver.enums.StringSearchType;
import com.revealprecision.revealserver.persistence.domain.Group;
import com.revealprecision.revealserver.persistence.domain.Location;
import com.revealprecision.revealserver.persistence.domain.Person;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.UUID;
import javax.persistence.criteria.JoinType;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.SetJoin;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.jpa.domain.Specification;

@Slf4j
public class PersonSpec {

  public static Specification<Person> getPersonSpecification(String firstName,
      String lastName, String gender, String location, String group,
      LocalDate birthDate, LocalDate fromDate,
      LocalDate toDate, boolean andSearch) {

    Specification<Person> personSpecification = Specification.where(null);

    StringSearchType searchType = andSearch ? EQUALS : LIKE;

    boolean searchLocationByIdentifier = false;
    UUID locationIdentifier = null;
    try{
      locationIdentifier = UUID.fromString(location);
      searchLocationByIdentifier = true;
    } catch (IllegalArgumentException illegalArgumentException){
      log.info("Location: {} passed is not a uuid and therefore a name search will be done",location);
    }

    if (group != null) {
      if (location != null) {
        personSpecification = PersonSpec.getSpecification(
            personSpecification,
            PersonSpec.whereGroupAndLocationName(searchType, group)
            , searchType);
      } else {
        personSpecification = PersonSpec.getSpecification(personSpecification,
            PersonSpec.whereGroupName(searchType, group), searchType);
      }
    } else {
      if (location != null) {
        if (searchLocationByIdentifier){
          personSpecification = PersonSpec.getSpecification(personSpecification,
              PersonSpec.whereLocationByIdentifier(locationIdentifier), searchType);
        } else{
          personSpecification = PersonSpec.getSpecification(personSpecification,
              PersonSpec.whereLocationName(searchType, location), searchType);
        }
      }
    }

    if (firstName != null) {
      personSpecification = PersonSpec.getSpecification(personSpecification,
          PersonSpec.whereGivenNameOrTextName(searchType, firstName), searchType);
    }

    if (lastName != null) {
      personSpecification = PersonSpec.getSpecification(personSpecification,
          PersonSpec.whereFamilyNameOrNameSuffix(searchType, lastName), searchType);
    }

    if (gender != null) {
      personSpecification = PersonSpec.getSpecification(personSpecification,
          PersonSpec.whereGender(searchType, gender), searchType);
    }

    if (birthDate != null) {
      personSpecification = PersonSpec.getSpecification(personSpecification,
          PersonSpec.whereBirthDateEquals(
              Date.from(birthDate.atStartOfDay(ZoneId.systemDefault()).toInstant())),
          searchType);

    } else {

      if (fromDate != null && toDate != null) {
        var from = Date.from(
            fromDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
        var to = Date.from(
            toDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
        personSpecification = personSpecification.and(
            PersonSpec.whereBirthDateBetween(from, to));
      }
    }
    return personSpecification;
  }

  private static Specification<Person> whereGroupName(StringSearchType type, String groupName) {
    switch (type) {
      case EQUALS:
        return (root, query, criteriaBuilder) -> criteriaBuilder.equal(
            root.join("groups").<Group>get("name"), groupName);
      case LIKE:
        return (root, query, criteriaBuilder) -> criteriaBuilder.like(
            criteriaBuilder.lower(root.join("groups").<Group>get("name").as(String.class)),
            groupName.toLowerCase());
    }
    return null;
  }


  private static Specification<Person> whereLocationName(StringSearchType type,
      String locationName) {

    switch (type) {
      case EQUALS:
        return (root, query, criteriaBuilder) -> criteriaBuilder.equal(
            root.join("groups").join("location").<Location>get("name"), locationName);
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

    return (root, query, criteriaBuilder) -> criteriaBuilder.equal(
        root.join("groups").join("location").<Location>get("identifer"), locationIdentifier);
  }

  private static Specification<Person> whereGroupAndLocationNameV1(StringSearchType type,
      String locationName) {

    switch (type) {
      case EQUALS:
        return (root, query, criteriaBuilder) -> {
          SetJoin<Object, Object> groups = root.joinSet("groups", JoinType.LEFT);
          Predicate locationPredicate = criteriaBuilder.equal(
              groups.join("location", JoinType.LEFT).<Location>get("name"),
              locationName);
          Predicate groupPredicate = criteriaBuilder.equal(
              groups.<Group>get("name").as(String.class), locationName);
          return criteriaBuilder.and(locationPredicate, groupPredicate);
        };

      case LIKE:
        return (root, query, criteriaBuilder) -> {
          SetJoin<Object, Object> groups = root.joinSet("groups", JoinType.LEFT);
          String pattern = "%".concat(locationName).concat("%").toLowerCase();
          Predicate locationPredicate = criteriaBuilder.like(criteriaBuilder.lower(
                  groups.join("location", JoinType.LEFT).<Location>get("name").as(String.class)),
              pattern);
          Predicate groupPredicate = criteriaBuilder.like(criteriaBuilder.lower(
              criteriaBuilder.lower(groups.<Group>get("name").as(String.class))), pattern);
          return criteriaBuilder.or(locationPredicate, groupPredicate);
        };
    }
    return null;
  }


  private static Specification<Person> whereGroupAndLocationName(StringSearchType type,
      String name) {

    return (root, query, criteriaBuilder) -> {

      SetJoin<Object, Object> groups = root.joinSet("groups", JoinType.LEFT);

      // where location.name = "?" and group.name = "?"
      switch (type) {
        case EQUALS:
          Predicate locationPredicateEquals = criteriaBuilder.equal(
              groups.join("location", JoinType.LEFT).<Location>get("name"),
              name);
          Predicate groupPredicateEquals = criteriaBuilder.equal(
              groups.<Group>get("name").as(String.class), name);
          return criteriaBuilder.and(locationPredicateEquals, groupPredicateEquals);

        // where location.name like "%?%" or group.name like "%?%"
        case LIKE:
          String pattern = "%".concat(name).concat("%").toLowerCase();
          Predicate locationPredicateLike =
              criteriaBuilder.like(
                  criteriaBuilder.lower(
                      groups.join("location", JoinType.LEFT).<Location>get("name")
                          .as(String.class)),
                  pattern);
          Predicate groupPredicateLike = criteriaBuilder.like(criteriaBuilder.lower(
              criteriaBuilder.lower(groups.<Group>get("name").as(String.class))), pattern);
          return criteriaBuilder.or(locationPredicateLike, groupPredicateLike);

      }
      return null;
    };
  }


  private static Specification<Person> whereGivenNameOrTextName(StringSearchType type,
      String firstName) {

    switch (type) {
      // where person.nameText = "?" or group.nameGiven = "?"
      case EQUALS:
        return (root, query, criteriaBuilder) -> criteriaBuilder.or(
            criteriaBuilder.equal(root.get("nameText"), firstName),
            criteriaBuilder.equal(root.get("nameGiven"), firstName));

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

  private static Specification<Person> whereFamilyNameOrNameSuffix(StringSearchType type,
      String lastname) {

    switch (type) {
      case EQUALS:
        return (root, query, criteriaBuilder) -> criteriaBuilder.or(
            criteriaBuilder.equal(root.get("nameFamily"), lastname),
            criteriaBuilder.equal(root.get("nameSuffix"), lastname));

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

  private static Specification<Person> whereGender(StringSearchType type, String gender) {

    switch (type) {
      case EQUALS:
        return (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("gender"), gender);

      case LIKE:
        return (root, query, criteriaBuilder) -> criteriaBuilder.like(
            criteriaBuilder.lower(root.get("gender")),
            "%".concat(gender).concat("%").toLowerCase());
    }
    return null;
  }

  private static Specification<Person> whereBirthDateBetween(Date fromDate,
      Date toDate) {
    return (root, query, criteriaBuilder) -> criteriaBuilder.and(
        criteriaBuilder.between(root.get("birthDate"), fromDate, toDate));
  }

  private static Specification<Person> getSpecification(
      Specification<Person> originalPersonSpecification,
      Specification<Person> additionPersonSpecification, StringSearchType type) {
    if (type.equals(StringSearchType.EQUALS)) {
      return originalPersonSpecification.and(additionPersonSpecification);
    } else {
      return originalPersonSpecification.or(additionPersonSpecification);
    }
  }

}
