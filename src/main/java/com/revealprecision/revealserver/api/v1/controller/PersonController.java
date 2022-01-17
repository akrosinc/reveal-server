package com.revealprecision.revealserver.api.v1.controller;

import com.revealprecision.revealserver.api.v1.dto.factory.PersonResponseFactory;
import com.revealprecision.revealserver.api.v1.dto.request.PersonRequest;
import com.revealprecision.revealserver.api.v1.dto.response.PersonResponse;
import com.revealprecision.revealserver.api.v1.dto.response.PersonResponse.PersonResponseBuilder;
import com.revealprecision.revealserver.persistence.domain.Person;
import com.revealprecision.revealserver.service.PersonService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/")
@Slf4j
public class PersonController {

  private final PersonService personService;

  @Autowired
  public PersonController(PersonService personService) {
    this.personService = personService;
  }


  @Operation(summary = "Search person across fields", description = "Search person across fields", tags = {
      "Person"})
  @ResponseStatus(HttpStatus.OK)
  @GetMapping(value = "/person", produces = "application/json")
  public ResponseEntity<?> getPersonsSearch(
      @RequestParam(name = "search", required = false) String searchParam,
      @RequestParam(name = "first_name", required = false) String searchFirstName,
      @RequestParam(name = "last_name", required = false) String searchLastName,
      @RequestParam(name = "gender", required = false) String searchGender,
      @RequestParam(name = "location", required = false) String searchLocation,
      @RequestParam(name = "group", required = false) String searchGroup,
      @RequestParam(name = "birthDate", required = false) String searchBirthDate,
      @RequestParam(name = "birthDate_lessThan", required = false) String searchBirthDateLessThan,
      @RequestParam(name = "birthDate_greaterThan", required = false) String searchBirthDateMoreThan,
      @RequestParam(name = "_summary", required = false) String searchSummary,
      @RequestParam(defaultValue = "0", required = false) Integer pageNumber,
      @RequestParam(defaultValue = "50", required = false) Integer pageSize) {

    String summary = "true";
    if (searchSummary != null) {
      summary = searchSummary;
    }

    if (searchParam != null) {
      switch (summary) {
        case "true":
          return new ResponseEntity<>(new PageImpl<>(
              personService.searchPersonByOneValueAcrossAllFields(searchParam, pageNumber, pageSize)
                  .stream()
                  .map(PersonResponseFactory::getPersonResponseBuilder)
                  .map(PersonResponseBuilder::build)
                  .collect(Collectors.toList())), HttpStatus.OK);

        case "false":
          Page<Person> people = personService.searchPersonByOneValueAcrossAllFields(
              searchParam, pageNumber, pageSize);

          Page<PersonResponse> personResponses = new PageImpl<>(people.stream()
              .map(PersonResponseFactory::fromEntity)
              .collect(Collectors.toList()));

          return new ResponseEntity<>(
              personResponses, HttpStatus.OK);

        case "count":
          return new ResponseEntity<>(
              personService.countPersonByOneValueAcrossAllFields(searchParam), HttpStatus.OK);

      }
    } else {
      switch (summary) {
        case "true":
          return new ResponseEntity<>(
              new PageImpl<>(personService.searchPersonByMultipleValuesAcrossFields(searchFirstName,
                      searchLastName, searchGender,
                      searchLocation, searchGroup, searchBirthDate, searchBirthDateLessThan,
                      searchBirthDateMoreThan).stream()
                  .map(person -> PersonResponseFactory.getPersonResponseBuilder(person).build())
                  .collect(Collectors.toList())), HttpStatus.OK);

        case "false":
          return new ResponseEntity<>(
              new PageImpl<>(personService.searchPersonByMultipleValuesAcrossFields(searchFirstName,
                      searchLastName, searchGender,
                      searchLocation, searchGroup, searchBirthDate, searchBirthDateLessThan,
                      searchBirthDateMoreThan).stream()
                  .map(PersonResponseFactory::fromEntity)
                  .collect(Collectors.toList())), HttpStatus.OK);

        case "count":
          return new ResponseEntity<>(PersonResponseFactory.fromCount(
              personService.countPersonByMultipleValuesAcrossFields(searchFirstName,
                  searchLastName, searchGender,
                  searchLocation, searchGroup, searchBirthDate, searchBirthDateLessThan,
                  searchBirthDateMoreThan)), HttpStatus.OK);

      }
    }
    return null;
  }


  @Operation(summary = "Fetch a person by identfier", description = "Fetch a person by identfier", tags = {
      "Person"})
  @ResponseStatus(HttpStatus.OK)
  @GetMapping(value = "/person/{identifier}", produces = MediaType.APPLICATION_JSON_VALUE)
  public PersonResponse getGroupByIdentifier(
      @Parameter(description = "Person identifier") @PathVariable("identifier") UUID personIdentifier) {
    return PersonResponseFactory.fromEntity(personService.getPersonByIdentifier(personIdentifier));
  }

  @Operation(summary = "Create a person", description = "Create a Person", tags = {"Person"})
  @ResponseStatus(HttpStatus.CREATED)
  @PostMapping(value = "/person", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
  public PersonResponse createPerson(@Validated @RequestBody PersonRequest personRequest) {
    return PersonResponseFactory.fromEntity(personService.createPerson(personRequest));
  }

  @Operation(summary = "Delete a person by identfier", description = "Delete a person by identfier", tags = {
      "Person"})
  @DeleteMapping(value = "/person/{identifier}", produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<?> removeGroupByIdentifier(
      @Parameter(description = "Person identifier") @PathVariable("identifier") UUID personIdentifier) {
    personService.removePerson(personIdentifier);
    return ResponseEntity.status(HttpStatus.NO_CONTENT)
        .body("Person with identifier " + personIdentifier + " deleted");
  }

  @Operation(summary = "Update a person by identfier", description = "Update a person by identfier", tags = {
      "Person"})
  @PutMapping(value = "/person/{identifier}", produces = MediaType.APPLICATION_JSON_VALUE)
  public PersonResponse updateGroupByIdentifier(
      @Parameter(description = "Group identifier") @PathVariable("identifier") UUID personIdentifier,
      @Validated @RequestBody PersonRequest personRequest) {
    return PersonResponseFactory.fromEntity(
        personService.updatePerson(personIdentifier, personRequest));
  }

}