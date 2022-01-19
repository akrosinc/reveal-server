package com.revealprecision.revealserver.service;

import com.revealprecision.revealserver.api.v1.dto.factory.PersonEntityFactory;
import com.revealprecision.revealserver.api.v1.dto.request.PersonRequest;
import com.revealprecision.revealserver.enums.EntityStatus;
import com.revealprecision.revealserver.exceptions.InvalidDateFormatException;
import com.revealprecision.revealserver.exceptions.NotFoundException;
import com.revealprecision.revealserver.persistence.domain.Group;
import com.revealprecision.revealserver.persistence.domain.Organization.Fields;
import com.revealprecision.revealserver.persistence.domain.Person;
import com.revealprecision.revealserver.persistence.repository.PersonRepository;
import com.revealprecision.revealserver.persistence.specification.PersonSpec;
import com.revealprecision.revealserver.service.models.PersonSearchCriteria;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class PersonService {

  final PersonRepository personRepository;
  final GroupService groupService;

  @Autowired
  public PersonService(GroupService groupService, PersonRepository personRepository) {
    this.personRepository = personRepository;
    this.groupService = groupService;
  }

  public Person createPerson(PersonRequest personRequest) {

    Person person = PersonEntityFactory.fromRequestObj(personRequest);

    String[] groups = personRequest.getGroups();
    Set<Group> groupList = new HashSet<>();
    if (groups != null && groups.length > 0) {
      for (String groupIdentifier : groups) {
        if (!groupIdentifier.isEmpty()) {
          Group group = groupService.getGroupByIdentifier(UUID.fromString(groupIdentifier));
          groupList.add(group);
        }
      }
    }

    person.setGroups(groupList);

    person.setEntityStatus(EntityStatus.ACTIVE);
    Person save = personRepository.save(person);
    log.info("Group saved to database as {}", person);

    return save;
  }

  public Person getPersonByIdentifier(UUID personIdentifier) {
    return personRepository.findByIdentifier(personIdentifier).orElseThrow(
        () -> new NotFoundException(Pair.of(Fields.identifier, personIdentifier), Person.class));
  }


  public void removePerson(UUID personIdentifier) {
    Person person = getPersonByIdentifier(personIdentifier);
    personRepository.delete(person);
  }


  public Person updatePerson(UUID personIdentifier, PersonRequest personRequest) {
    Person personRetrieved = getPersonByIdentifier(personIdentifier);

    if (personRequest.getGroups() != null) {
      List<Group> groups = Arrays.stream(personRequest.getGroups())
          .map(group -> groupService.getGroupByIdentifier(UUID.fromString(group)))
          .collect(Collectors.toList());
      personRetrieved.setGroups(new HashSet<>(groups));
    }
    if (personRequest.getBirthDate() != null) {
      personRetrieved.setBirthDate(
          Date.from(personRequest.getBirthDate().atStartOfDay(ZoneId.systemDefault()).toInstant()));
    }
    if (personRequest.getName() != null) {
      if (personRequest.getName().getFamily() != null) {
        personRetrieved.setNameFamily(personRequest.getName().getFamily());
      }
      if (personRequest.getName().getPrefix() != null) {
        personRetrieved.setNamePrefix(personRequest.getName().getPrefix());
      }
      if (personRequest.getName().getSuffix() != null) {
        personRetrieved.setNameSuffix(personRequest.getName().getSuffix());
      }
      if (personRequest.getName().getText() != null) {
        personRetrieved.setNameText(personRequest.getName().getText());
      }
      if (personRequest.getName().getGiven() != null) {
        personRetrieved.setNameGiven(personRequest.getName().getGiven());
      }
    }

    if (personRequest.getGender() != null) {
      personRetrieved.setGender(personRequest.getGender().getValue());
    }

    return personRepository.save(personRetrieved);
  }

  public Page<Person> searchPersonByOneValueAcrossAllFields(String searchParam, Pageable pageable) {

    try {

      LocalDate localDate = LocalDate.parse(searchParam);
      return personRepository.findPersonByBirthDate(
          Date.from(localDate.atStartOfDay(ZoneId.systemDefault()).toInstant()), pageable);

    } catch (DateTimeParseException e) {

      log.info("Search param {} does not match date format and will not be used in search",
          searchParam);
      Specification<Person> personSpecification = PersonSpec.getOrSearchByNamePersonSpecification(
          searchParam, searchParam);
      return personRepository.findAll(personSpecification, pageable);

    }
  }

  public Long countPersonByOneValueAcrossAllFields(String searchParam) {

    try {
      LocalDate localDate = LocalDate.parse(searchParam);
      return personRepository.countPersonByBirthDate(
          Date.from(localDate.atStartOfDay(ZoneId.systemDefault()).toInstant()));
    } catch (DateTimeParseException e) {
      log.info("Search param {} does not match date format and will not be used in search",
          searchParam);
      Specification<Person> personSpecification = PersonSpec.getOrSearchByNamePersonSpecification(
          searchParam, searchParam);

      return personRepository.count(personSpecification);
    }

  }


  public Page<Person> searchPersonByMultipleValuesAcrossFields(PersonSearchCriteria criteria,
      Pageable pageable) {

    Specification<Person> personSpecification = getPersonSearchQuery(criteria);
    return personRepository.findAll(personSpecification, pageable);

  }

  public Page<Person> getAllPersons(Pageable pageable) {
    return personRepository.findAll(pageable);
  }

  public Long countPersonByMultipleValuesAcrossFields(PersonSearchCriteria criteria) {

    Specification<Person> personSpecification = getPersonSearchQuery(criteria);
    return personRepository.count(personSpecification);

  }

  private Specification<Person> getPersonSearchQuery(PersonSearchCriteria criteria) {
    LocalDate searchBirthLocalDate;
    LocalDate searchFromDateLocalDate;
    LocalDate searchToDateLocalDate;

    if (criteria != null) {
      try {
        searchBirthLocalDate =
            criteria.getBirthdate() != null ? LocalDate.parse(criteria.getBirthdate()) : null;
        searchFromDateLocalDate =
            criteria.getFromDate() != null ? LocalDate.parse(criteria.getFromDate()) : null;
        searchToDateLocalDate =
            criteria.getToDate() != null ? LocalDate.parse(criteria.getToDate()) : null;
      } catch (DateTimeParseException e) {
        throw new InvalidDateFormatException(e.getMessage());
      }

      return PersonSpec.getPersonSpecification(criteria.getFirstName(), criteria.getLastName(),
          criteria.getGender(), criteria.getLocationName(), criteria.getLocationIdentifier(),
          criteria.getGroupName(), criteria.getGroupIdentifier(), searchBirthLocalDate,
          searchFromDateLocalDate, searchToDateLocalDate);
    } else {
      return PersonSpec.getBlankPredicate();
    }
  }

}
