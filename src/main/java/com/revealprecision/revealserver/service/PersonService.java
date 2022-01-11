package com.revealprecision.revealserver.service;

import com.revealprecision.revealserver.api.v1.dto.request.GroupRequest;
import com.revealprecision.revealserver.api.v1.dto.request.PersonRequest;
import com.revealprecision.revealserver.enums.EntityStatus;
import com.revealprecision.revealserver.exceptions.NotFoundException;
import com.revealprecision.revealserver.persistence.domain.Group;
import com.revealprecision.revealserver.persistence.domain.Person;
import com.revealprecision.revealserver.persistence.repository.PersonRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@Slf4j
public class PersonService {

    @Autowired
    private PersonRepository personRepository;

//TODO - TB: Wire in the location if need be
//    @Autowired
//    private LocationService locationService;

    public Page<Person> getPersons(Integer pageNumber, Integer pageSize) {
        return personRepository.findAll(PageRequest.of(pageNumber, pageSize));
    }

    public Person createPerson(PersonRequest personRequest) {
        var person = Person.builder()
//                .type(personRequest.getType().toString())
//                .name(personRequest.getName())
                .build();

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

    public Person updatePerson(UUID personIdentifier, PersonRequest personRequest) {
        var person = personRepository.findByIdentifier(personIdentifier);

        if (person.isEmpty()) {
            throw new NotFoundException("Person with identifier " + personIdentifier + " not found");
        }

        var personRetrieved = person.get();
//
//        personRetrieved.setName(personRequest.getName());
//        personRetrieved.setType(personRequest.getType().toString());

        return personRepository.save(personRetrieved);
    }
}