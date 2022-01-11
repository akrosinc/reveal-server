package com.revealprecision.revealserver.api.v1.controller;

import com.revealprecision.revealserver.api.v1.dto.factory.GroupResponseFactory;
import com.revealprecision.revealserver.api.v1.dto.factory.PersonResponseFactory;
import com.revealprecision.revealserver.api.v1.dto.request.GroupRequest;
import com.revealprecision.revealserver.api.v1.dto.request.PersonRequest;
import com.revealprecision.revealserver.api.v1.dto.response.GroupResponse;
import com.revealprecision.revealserver.api.v1.dto.response.PersonResponse;
import com.revealprecision.revealserver.persistence.domain.Group;
import com.revealprecision.revealserver.persistence.domain.Person;
import com.revealprecision.revealserver.service.GroupService;
import com.revealprecision.revealserver.service.PersonService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/")
public class PersonController {

    private final PersonService personService;

    @Autowired
    public PersonController(PersonService personService) {
        this.personService = personService;
    }

  @Operation(summary = "Fetch all persons",
      description = "Fetch all persons",
      tags = {"Person"}
  )
  @ResponseStatus(HttpStatus.OK)
  @GetMapping(value = "/person",
      produces = "application/json"
  )
  public Page<Person> getPersons(
      @Parameter(description = "Page number to return") @RequestParam(defaultValue = "0", required = false) Integer pageNumber,
      @Parameter(description = "Number of records per page") @RequestParam(defaultValue = "50", required = false) Integer pageSize) {
    return personService.getPersons(pageNumber, pageSize);
  }

    @Operation(summary = "Fetch a person by identfier",
            description = "Fetch a person by identfier",
            tags = {"Person"}
    )
    @ResponseStatus(HttpStatus.OK)
    @GetMapping(value = "/person/{identifier}",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public Person getGroupByIdentifier(
            @Parameter(description = "Person identifier") @PathVariable("identifier") UUID personIdentifier) {
        return personService.getPersonByIdentifier(personIdentifier);
    }

    @Operation(summary = "Create a person",
            description = "Create a Person",
            tags = {"Person"}
    )
    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping(value = "/person", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public PersonResponse createPerson(@Validated @RequestBody PersonRequest personRequest) {

        return PersonResponseFactory.fromEntity(personService.createPerson(personRequest));
    }
    @Operation(summary = "Delete a person by identfier",
            description = "Delete a person by identfier",
            tags = {"Person"}
    )

    @DeleteMapping(value = "/person/{identifier}",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<?> removeGroupByIdentifier(
            @Parameter(description = "Person identifier") @PathVariable("identifier") UUID groupIdentifier) {
        personService.removePerson(groupIdentifier);
        return ResponseEntity.noContent().build();
    }

    @PutMapping(value = "/person/{identifier}",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public PersonResponse updateGroupByIdentifier(
            @Parameter(description = "Group identifier") @PathVariable("identifier") UUID personIdentifier,
            @Validated @RequestBody PersonRequest personRequest) {
        return PersonResponseFactory.fromEntity(personService.updatePerson(personIdentifier, personRequest));
    }

}