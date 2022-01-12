package com.revealprecision.revealserver.service;

import com.revealprecision.revealserver.api.v1.dto.request.GroupRequest;
import com.revealprecision.revealserver.api.v1.dto.request.PersonRequest;
import com.revealprecision.revealserver.enums.EntityStatus;
import com.revealprecision.revealserver.exceptions.NotFoundException;
import com.revealprecision.revealserver.persistence.domain.Group;
import com.revealprecision.revealserver.persistence.domain.Person;
import com.revealprecision.revealserver.persistence.domain.PersonGroup;
import com.revealprecision.revealserver.persistence.domain.PersonGroupKey;
import com.revealprecision.revealserver.persistence.repository.PersonRepository;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.ExampleMatcher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;

@Service
@Slf4j
public class PersonService {

    @Autowired
    private PersonRepository personRepository;

    @Autowired
    GroupService groupService;

//TODO - TB: Wire in the location if need be
//    @Autowired
//    private LocationService locationService;

    public Page<Person> getPersons(Integer pageNumber, Integer pageSize) {
        return personRepository.findAll(PageRequest.of(pageNumber, pageSize));
    }

    public Person createPerson(PersonRequest personRequest) {

        var person = Person.builder()
            .nameFamily(personRequest.getName().getFamily())
            .nameGiven(personRequest.getName().getGiven())
            .namePrefix(personRequest.getName().getPrefix())
            .nameSuffix(personRequest.getName().getSuffix())
            .nameText(personRequest.getName().getText())
            .nameUse(personRequest.getName().getUse().name())
            .birthDate(Date.from(personRequest.getBirthDate().atStartOfDay(ZoneId.systemDefault()).toInstant()))
            .gender(personRequest.getGender().name())
            .active(personRequest.isActive())
                .build();

        String[] groups = personRequest.getGroups();
        List<PersonGroup> groupList = new ArrayList<>();
        if (groups != null && groups.length > 0){
            for (String groupIdentifier:groups) {
                if (!groupIdentifier.isEmpty()){
                    Group group = groupService.getGroupByIdentifier(
                        UUID.fromString(groupIdentifier));
                    PersonGroup personGroup = new PersonGroup();
                    PersonGroupKey personGroupKey = new PersonGroupKey();
                    personGroupKey.setPersonIdentifier(person.getIdentifier());
                    personGroupKey.setGroupIdentifier(group.getIdentifier());
                    personGroup.setPersonGroupKey(personGroupKey);
                    personGroup.setPerson(person);
                    personGroup.setGroup(group);
                    personGroup.setEntityStatus(EntityStatus.ACTIVE);
                    groupList.add(personGroup);
                }
            }
        }


        person.setPersonGroups(groupList);

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