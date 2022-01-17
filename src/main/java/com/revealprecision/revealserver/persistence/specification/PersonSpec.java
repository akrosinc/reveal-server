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
import javax.persistence.criteria.JoinType;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.SetJoin;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.jpa.domain.Specification;

@Slf4j
public class PersonSpec {

  public static Specification<Person> getPersonSpecification(String searchFirstName,
      String searchLastName, String searchGender, String searchLocation, String searchGroup,
      LocalDate searchBirthDate, LocalDate searchBirthDateLessThan,
      LocalDate searchBirthDateMoreThan, boolean andSearch) {

    Specification<Person> personSpecification = Specification.where(null);

    StringSearchType searchType = andSearch ? EQUALS : LIKE;

    if (searchGroup != null) {
      if (searchLocation != null) {
        personSpecification = PersonSpec.getSpecification(
            personSpecification,
            PersonSpec.whereGroupAndLocationName(searchType, searchGroup)
            , searchType);
      } else {
        personSpecification = PersonSpec.getSpecification(personSpecification,
            PersonSpec.whereGroupName(searchType, searchGroup), searchType);
      }
    } else {
      if (searchLocation != null) {
        personSpecification = PersonSpec.getSpecification(personSpecification,
            PersonSpec.whereLocationName(searchType, searchLocation), searchType);
      }
    }

    if (searchFirstName != null) {
      personSpecification = PersonSpec.getSpecification(personSpecification,
          PersonSpec.whereGivenNameOrTextName(searchType, searchFirstName), searchType);
    }

    if (searchLastName != null) {
      personSpecification = PersonSpec.getSpecification(personSpecification,
          PersonSpec.whereFamilyNameOrNameSuffix(searchType, searchLastName), searchType);
    }

    if (searchGender != null) {
      personSpecification = PersonSpec.getSpecification(personSpecification,
          PersonSpec.whereGender(searchType, searchGender), searchType);
    }

    if (searchBirthDate != null) {
      personSpecification = PersonSpec.getSpecification(personSpecification,
          PersonSpec.whereBirthDateEquals(
              Date.from(searchBirthDate.atStartOfDay(ZoneId.systemDefault()).toInstant())),
          searchType);

    } else if (searchBirthDateLessThan != null && searchBirthDateMoreThan != null) {
      var fromDate = Date.from(
          searchBirthDateLessThan.atStartOfDay(ZoneId.systemDefault()).toInstant());
      var toDate = Date.from(
          searchBirthDateMoreThan.atStartOfDay(ZoneId.systemDefault()).toInstant());
      personSpecification = personSpecification.and(
          PersonSpec.whereBirthDateBetween(fromDate, toDate));
    } else {
      //TODO add Exception
      log.info("Both parameters");
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
//      case LIKE:
//        return (root, query, criteriaBuilder) -> criteriaBuilder.like(
//            criteriaBuilder.lower(
//                root.joinList("personGroups", JoinType.LEFT)
//                        .join("group",JoinType.LEFT)
//                    .join("location",JoinType.LEFT).<Location>get("name").as(String.class)),
//            "%".concat(locationName).concat("%").toLowerCase());
    }
    return null;
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
                  groups.join("location", JoinType.LEFT).<Location>get("name").as(String.class)),
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

  private static Specification<Person> whereBirthDateBetween(Date birthdateBefore,
      Date birthdateAfter) {
    return (root, query, criteriaBuilder) -> criteriaBuilder.and(
        criteriaBuilder.between(root.get("birthDate"), birthdateBefore, birthdateAfter));
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
