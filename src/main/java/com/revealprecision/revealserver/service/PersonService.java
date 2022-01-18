package com.revealprecision.revealserver.service;

import com.revealprecision.revealserver.api.v1.dto.request.PersonRequest;
import com.revealprecision.revealserver.enums.EntityStatus;
import com.revealprecision.revealserver.exceptions.InvalidDateFormatException;
import com.revealprecision.revealserver.exceptions.NotFoundException;
import com.revealprecision.revealserver.persistence.domain.Group;
import com.revealprecision.revealserver.persistence.domain.Person;
import com.revealprecision.revealserver.persistence.repository.PersonRepository;
import com.revealprecision.revealserver.persistence.specification.PersonSpec;
import com.revealprecision.revealserver.service.models.PersonSearchCriteria;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class PersonService {

  PersonRepository personRepository;
  GroupService groupService;

  @Autowired
  public PersonService(GroupService groupService, PersonRepository personRepository) {
    this.personRepository = personRepository;
    this.groupService = groupService;
  }

  public Person createPerson(PersonRequest personRequest) {
//TODO Use factory
    var person = Person.builder().nameFamily(personRequest.getName().getFamily())
        .nameGiven(personRequest.getName().getGiven())
        .namePrefix(personRequest.getName().getPrefix())
        .nameSuffix(personRequest.getName().getSuffix()).nameText(personRequest.getName().getText())
        .nameUse(personRequest.getName().getUse().name()).birthDate(Date.from(
            personRequest.getBirthDate().atStartOfDay(ZoneId.systemDefault()).toInstant()))
        .gender(personRequest.getGender().name()).active(personRequest.isActive()).build();

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
    var person = personRepository.findByIdentifier(personIdentifier);

    if (person.isEmpty()) {
      throw new NotFoundException("Person with identifier " + personIdentifier + " not found");
    }

    return person.get();
  }

  public void removePerson(UUID personIdentifier) {
    var person = personRepository.findByIdentifier(personIdentifier);

    if (person.isEmpty()) {
      throw new NotFoundException("Group with identifier " + personIdentifier + " not found");
    }

    personRepository.delete(person.get());
  }
//TODO fix person update
  public Person updatePerson(UUID personIdentifier, PersonRequest personRequest) {
    var person = personRepository.findByIdentifier(personIdentifier);

    if (person.isEmpty()) {
      throw new NotFoundException("Person with identifier " + personIdentifier + " not found");
    }

    var personRetrieved = person.get();
    return personRepository.save(personRetrieved);
  }

  public Page<Person> searchPersonByOneValueAcrossAllFields(String searchParam, Integer pageNumber,
      Integer pageSize) {

    Page<Person> persons;
    try {
      LocalDate localDate = LocalDate.parse(searchParam);
      persons = searchPersonByBirthDate(localDate, pageNumber, pageSize);
    } catch (DateTimeParseException e) {
      log.info("Search param {} does not match date format and will not be used in search",
          searchParam);
      persons = searchPersonByNameLike(searchParam, true, pageNumber, pageSize);
    }
    return persons;
  }

  public Long countPersonByOneValueAcrossAllFields(String searchParam) {

    Long count = 0L;
    try {
      LocalDate localDate = LocalDate.parse(searchParam);
      count = countPersonByBirthDate(localDate);
    } catch (DateTimeParseException e) {
      log.info("Search param {} does not match date format and will not be used in search",
          searchParam);
      count = countPersonByNameLike(searchParam, true);
    }
    return count;
  }


  public Page<Person> searchPersonByNameLike(String searchParam, boolean active, Integer pageNumber,
      Integer pageSize) {

    Specification<Person> personSpecification = PersonSpec.getPersonSpecification(searchParam,
        searchParam, searchParam, searchParam, searchParam, null, null, null, false);

    return personRepository.findAll(personSpecification, PageRequest.of(pageNumber, pageSize));

  }

  public Long countPersonByNameLike(String searchParam, boolean active) {

    Specification<Person> personSpecification = PersonSpec.getPersonSpecification(searchParam,
        searchParam, searchParam, searchParam, searchParam, null, null, null, false);

    return personRepository.count(personSpecification);

  }

  public Page<Person> searchPersonByBirthDate(LocalDate date, Integer pageNumber,
      Integer pageSize) {
    return personRepository.findPersonByBirthDate(
        Date.from(date.atStartOfDay(ZoneId.systemDefault()).toInstant()),
        PageRequest.of(pageNumber, pageSize));
  }

  public Long countPersonByBirthDate(LocalDate date) {
    return personRepository.countPersonByBirthDate(
        Date.from(date.atStartOfDay(ZoneId.systemDefault()).toInstant()));
  }


  public Page<Person> searchPersonByMultipleValuesAcrossFields(PersonSearchCriteria criteria, Integer pageNumber,
      Integer pageSize) {

    Specification<Person> personSpecification = getPersonSearchQuery(
        criteria);

    return personRepository.findAll(personSpecification,PageRequest.of(pageNumber, pageSize));

  }

  public Page<Person> getAllPersons(Integer pageNumber,
      Integer pageSize) {
    return personRepository.findAll(PageRequest.of(pageNumber, pageSize));
  }

  public Long countPersonByMultipleValuesAcrossFields(PersonSearchCriteria criteria) {

    Specification<Person> personSpecification = getPersonSearchQuery(
        criteria);

    return personRepository.count(personSpecification);

  }

  private Specification<Person> getPersonSearchQuery(PersonSearchCriteria criteria) {
    LocalDate searchBirthLocalDate;
    LocalDate searchFromDateLocalDate;
    LocalDate searchToDateLocalDate;

    try {
      searchBirthLocalDate = criteria.getBirthdate() != null ? LocalDate.parse(criteria.getBirthdate()) : null;
      searchFromDateLocalDate =
          criteria.getFromDate() != null ? LocalDate.parse(criteria.getFromDate()) : null;
      searchToDateLocalDate =
          criteria.getToDate() != null ? LocalDate.parse(criteria.getToDate()) : null;
    } catch (DateTimeParseException e) {
      throw new InvalidDateFormatException(e.getMessage());
    }

    Specification<Person> personSpecification = PersonSpec.getPersonSpecification(criteria.getFirstName(),
        criteria.getLastName(), criteria.getGender(), criteria.getLocation(), criteria.getGroup(), searchBirthLocalDate,
        searchFromDateLocalDate, searchToDateLocalDate, true);
    return personSpecification;
  }

//  public Page<Person> searchPersonByMultipleValuesAcrossFields(String searchFirstName,
//      String searchLastName, String searchGender, String searchLocation, String searchGroup,
//      String searchBirthDate, String searchBirthDateLessThan, String searchBirthDateMoreThan, Integer pageNumber,
//      Integer pageSize) {
//
//    LocalDate searchBirthLocalDate;
//    LocalDate searchBirthDateLessThanLocalDate;
//    LocalDate searchBirthDateMoreThanLocalDate;
//
//    try {
//      searchBirthLocalDate = searchBirthDate != null ? LocalDate.parse(searchBirthDate) : null;
//      searchBirthDateLessThanLocalDate =
//          searchBirthDateLessThan != null ? LocalDate.parse(searchBirthDateLessThan) : null;
//      searchBirthDateMoreThanLocalDate =
//          searchBirthDateMoreThan != null ? LocalDate.parse(searchBirthDateMoreThan) : null;
//    } catch (DateTimeParseException e) {
//      throw new InvalidDateFormatException(e.getMessage());
//    }
//
//    Specification<Person> personSpecification = PersonSpec.getPersonSpecification(searchFirstName,
//        searchLastName, searchGender, searchLocation, searchGroup, searchBirthLocalDate,
//        searchBirthDateLessThanLocalDate, searchBirthDateMoreThanLocalDate, true);
//
//    return personRepository.findAll(personSpecification,PageRequest.of(pageNumber, pageSize));
//
//  }



//  public Long countPersonByMultipleValuesAcrossFields(String searchFirstName, String searchLastName,
//      String searchGender, String searchLocation, String searchGroup, String searchBirthDate,
//      String searchBirthDateLessThan, String searchBirthDateMoreThan) {
//
//    LocalDate searchBirthLocalDate;
//    LocalDate searchBirthDateLessThanLocalDate;
//    LocalDate searchBirthDateMoreThanLocalDate;
//    try {
//      searchBirthLocalDate = searchBirthDate != null ? LocalDate.parse(searchBirthDate) : null;
//      searchBirthDateLessThanLocalDate =
//          searchBirthDateLessThan != null ? LocalDate.parse(searchBirthDateLessThan) : null;
//      searchBirthDateMoreThanLocalDate =
//          searchBirthDateMoreThan != null ? LocalDate.parse(searchBirthDateMoreThan) : null;
//    } catch (DateTimeParseException e) {
//      throw new InvalidDateFormatException(e.getMessage());
//    }
//
//    Specification<Person> personSpecification = PersonSpec.getPersonSpecification(searchFirstName,
//        searchLastName, searchGender, searchLocation, searchGroup, searchBirthLocalDate,
//        searchBirthDateLessThanLocalDate, searchBirthDateMoreThanLocalDate, true);
//
//    return personRepository.count(personSpecification);
//
//  }


}
