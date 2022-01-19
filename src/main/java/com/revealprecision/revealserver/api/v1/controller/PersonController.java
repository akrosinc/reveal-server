package com.revealprecision.revealserver.api.v1.controller;

import com.revealprecision.revealserver.api.v1.dto.factory.PersonResponseFactory;
import com.revealprecision.revealserver.api.v1.dto.request.PersonRequest;
import com.revealprecision.revealserver.api.v1.dto.response.PersonResponse;
import com.revealprecision.revealserver.api.v1.dto.response.PersonResponse.PersonResponseBuilder;
import com.revealprecision.revealserver.enums.SummaryEnum;
import com.revealprecision.revealserver.service.PersonService;
import com.revealprecision.revealserver.service.models.PersonSearchCriteria;
import io.micrometer.core.lang.Nullable;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.Explode;
import java.time.LocalDate;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
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
@RequestMapping("/api/v1/person")
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
  @GetMapping( produces = MediaType.APPLICATION_JSON_VALUE)

  public ResponseEntity<?> getPersonsSearch(
      @Parameter(description = "Single value search across values") @RequestParam(name = "search", required = false) String searchParam,
      @Parameter(description = "Multi-value search across specified values") @Nullable PersonSearchCriteria criteria,
      @RequestParam(name = "_summary", required = false, defaultValue = "TRUE") SummaryEnum summary,
      Pageable pageable) {

    switch (summary) {
      case TRUE:
        if (searchParam != null) {
          return new ResponseEntity<>(new PageImpl<>(
              personService.searchPersonByOneValueAcrossAllFields(searchParam, pageable).stream()
                  .map(PersonResponseFactory::getPersonResponseBuilder)
                  .map(PersonResponseBuilder::build).collect(Collectors.toList())), HttpStatus.OK);
        } else {
          return new ResponseEntity<>(new PageImpl<>(
              personService.searchPersonByMultipleValuesAcrossFields(criteria, pageable).stream()
                  .map(person -> PersonResponseFactory.getPersonResponseBuilder(person).build())
                  .collect(Collectors.toList())), HttpStatus.OK);
        }

      case FALSE:
        if (searchParam != null) {
          return new ResponseEntity<>(new PageImpl<>(
              personService.searchPersonByOneValueAcrossAllFields(searchParam, pageable).stream()
                  .map(PersonResponseFactory::fromEntity).collect(Collectors.toList())),
              HttpStatus.OK);
        } else {
          return new ResponseEntity<>(new PageImpl<>(
              personService.searchPersonByMultipleValuesAcrossFields(criteria, pageable).stream()
                  .map(PersonResponseFactory::fromEntity).collect(Collectors.toList())),
              HttpStatus.OK);
        }

      case COUNT:
        if (searchParam != null) {
          return new ResponseEntity<>(
              personService.countPersonByOneValueAcrossAllFields(searchParam), HttpStatus.OK);
        } else {
          return new ResponseEntity<>(PersonResponseFactory.fromCount(
              personService.countPersonByMultipleValuesAcrossFields(criteria)), HttpStatus.OK);
        }
    }
    return new ResponseEntity<>(personService.getAllPersons(pageable), HttpStatus.OK);
  }

  @Operation(summary = "Fetch a person by identfier", description = "Fetch a person by identfier", tags = {
      "Person"})
  @ResponseStatus(HttpStatus.OK)
  @GetMapping(value = "/{identifier}", produces = MediaType.APPLICATION_JSON_VALUE)
  public PersonResponse getGroupByIdentifier(
      @Parameter(description = "Person identifier") @PathVariable("identifier") UUID personIdentifier) {
    return PersonResponseFactory.fromEntity(personService.getPersonByIdentifier(personIdentifier));
  }

  @Operation(summary = "Create a person", description = "Create a Person", tags = {"Person"})
  @ResponseStatus(HttpStatus.CREATED)
  @PostMapping( consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
  public PersonResponse createPerson(@RequestBody PersonRequest personRequest) {
    return PersonResponseFactory.fromEntity(personService.createPerson(personRequest));
  }

  @Operation(summary = "Delete a person by identfier", description = "Delete a person by identfier", tags = {
      "Person"})
  @DeleteMapping(value = "/{identifier}", produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<?> removeGroupByIdentifier(
      @Parameter(description = "Person identifier") @PathVariable("identifier") UUID personIdentifier) {
    personService.removePerson(personIdentifier);
    return ResponseEntity.status(HttpStatus.NO_CONTENT)
        .body("Person with identifier " + personIdentifier + " removed");
  }

  @Operation(summary = "Update a person by identfier", description = "Update a person by identfier", tags = {
      "Person"})
  @PutMapping(value = "/{identifier}", produces = MediaType.APPLICATION_JSON_VALUE)
  public PersonResponse updateGroupByIdentifier(
      @Parameter(description = "Group identifier") @PathVariable("identifier") UUID personIdentifier,
      @Validated @RequestBody PersonRequest personRequest) {
    return PersonResponseFactory.fromEntity(
        personService.updatePerson(personIdentifier, personRequest));
  }

}