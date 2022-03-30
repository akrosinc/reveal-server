package com.revealprecision.revealserver.api.v1.facade.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.revealprecision.revealserver.api.v1.facade.factory.EventFacadeFactory;
import com.revealprecision.revealserver.api.v1.facade.models.ClientFacade;
import com.revealprecision.revealserver.api.v1.facade.models.ClientFacadeMetadata;
import com.revealprecision.revealserver.api.v1.facade.models.EventClientFacade;
import com.revealprecision.revealserver.api.v1.facade.models.EventFacade;
import com.revealprecision.revealserver.api.v1.facade.models.Obs;
import com.revealprecision.revealserver.api.v1.facade.util.DateTimeFormatter;
import com.revealprecision.revealserver.enums.EntityStatus;
import com.revealprecision.revealserver.enums.NameUseEnum;
import com.revealprecision.revealserver.exceptions.NotFoundException;
import com.revealprecision.revealserver.persistence.domain.Event;
import com.revealprecision.revealserver.persistence.domain.FormData;
import com.revealprecision.revealserver.persistence.domain.Group;
import com.revealprecision.revealserver.persistence.domain.Location;
import com.revealprecision.revealserver.persistence.domain.Person;
import com.revealprecision.revealserver.persistence.projection.EventMaxVersionProjection;
import com.revealprecision.revealserver.service.EventService;
import com.revealprecision.revealserver.service.FormService;
import com.revealprecision.revealserver.service.GroupService;
import com.revealprecision.revealserver.service.LocationService;
import com.revealprecision.revealserver.service.OrganizationService;
import com.revealprecision.revealserver.service.PersonService;
import com.revealprecision.revealserver.service.UserService;
import com.revealprecision.revealserver.service.models.EventSearchCriteria;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EventClientFacadeService {

  private final PersonService personService;

  private final EventService eventService;

  private final LocationService locationService;

  private final OrganizationService organizationService;

  private final UserService userService;

  private final GroupService groupService;

  private final ObjectMapper objectMapper;

  private final FormService formService;


  public EventClientFacade saveEventClient(EventClientFacade eventClientFacade) {

    List<EventFacade> failedEvents = new ArrayList<>();
    if (eventClientFacade.getEvents() != null) {
      eventClientFacade.getEvents().forEach(eventFacade -> {
        try {
          saveEvent(eventFacade);
        } catch (Exception exception) {
          exception.printStackTrace();
          failedEvents.add(eventFacade);
        }
      });
    }

    List<ClientFacade> failedClients = new ArrayList<>();
    if (eventClientFacade.getClients() != null) {
      eventClientFacade.getClients().forEach(clientFacade -> {
        try {
          Location location = getLocationFromClientFacade(clientFacade);

          if (clientFacade.getClientType() != null && clientFacade.getClientType()
              .equals("Family")) {
            saveGroup(clientFacade, location);
          } else {
            savePerson(clientFacade, location);
          }
        } catch (Exception e) {
          failedClients.add(clientFacade);
        }
      });
    }

    EventClientFacade eventClientFacadeFailed = new EventClientFacade();
    eventClientFacadeFailed.setEvents(failedEvents);
    eventClientFacadeFailed.setClients(failedClients);

    return eventClientFacadeFailed;

  }

  private void savePerson(ClientFacade clientFacade, Location location) {
    Person person = getExistingOrNewPersonFromClientFacade(clientFacade);
    Set<Group> groups = getPersonGroups(clientFacade);

    if (groups != null) {
      person.setGroups(groups);
    }

    if (location != null) {
      if (person.getLocations() == null || person.getLocations().size() == 0) {
        person.setLocations(Set.of(location));
      } else {
        Set<Location> locations = person.getLocations();
        locations.add(location);
        person.setLocations(locations);
      }
    }

    ClientFacadeMetadata personAdditionalInfo = getAdditionalInfo(clientFacade);
    person.setAdditionalInfo(objectMapper.valueToTree(personAdditionalInfo));
    personService.savePerson(person);
  }

  private void saveGroup(ClientFacade clientFacade, Location location) {
    Group group = getExistingOrNewGroupFromClientFacade(clientFacade);
    ClientFacadeMetadata groupFacadeMetadata = getAdditionalInfo(clientFacade);
    groupFacadeMetadata.setFamilyHeadGender(clientFacade.getGender());
    group.setLocation(location);
    group.setAdditionalInfo(objectMapper.valueToTree(groupFacadeMetadata));
    groupService.saveGroup(group);
  }

  private Location getLocationFromClientFacade(ClientFacade clientFacade) {
    Location location = null;
    if (clientFacade.getAttributes() != null) {
      if (clientFacade.getAttributes().containsKey("residence")) {
        String residence = (String) clientFacade.getAttributes().get("residence");
        try {
          UUID locationId = UUID.fromString(residence);

          location = locationService.findByIdentifier(locationId);

        } catch (IllegalArgumentException e) {
          log.error("Client residence id: {} is not a valid UUID", residence);
        }
      }
    }
    return location;
  }


  private Set<Group> getPersonGroups(ClientFacade clientFacade) {
    if (clientFacade.getRelationships() != null) {
      if (clientFacade.getRelationships().containsKey("family")) {

        return clientFacade.getRelationships().get("family").stream().map(item -> {
          try {
            return UUID.fromString(item);
          } catch (IllegalArgumentException e) {
            return null;
          }
        }).filter(Objects::nonNull).map(groupId -> {
          try {
            return groupService.getGroupByIdentifier(groupId);
          } catch (NotFoundException e) {
            return null;
          }
        }).filter(Objects::nonNull).collect(Collectors.toSet());
      }
    }
    return null;
  }


  private ClientFacadeMetadata getAdditionalInfo(ClientFacade clientFacade) {
    return ClientFacadeMetadata.builder().attributes(clientFacade.getAttributes())
        .relationShips(clientFacade.getRelationships()).identifiers(clientFacade.getIdentifiers())
        .build();
  }

  private Group getExistingOrNewGroupFromClientFacade(ClientFacade clientFacade) {

    Group group;
    try {
      group = groupService.getGroupByIdentifier(UUID.fromString(clientFacade.getBaseEntityId()));
    } catch (NotFoundException notFoundException) {
      group = new Group();
      group.setIdentifier(UUID.fromString(clientFacade.getBaseEntityId()));
      group.setEntityStatus(EntityStatus.ACTIVE);
    }

    if (clientFacade.getClientType() != null) {
      group.setType(clientFacade.getClientType());
    }
    if (clientFacade.getFirstName() != null) {
      group.setName(clientFacade.getFirstName());
    }

    return group;
  }

  private Person getExistingOrNewPersonFromClientFacade(ClientFacade clientFacade) {

    Person person;
    try {
      person = personService.getPersonByIdentifier(UUID.fromString(clientFacade.getBaseEntityId()));
    } catch (Exception notFoundException) {
      person = new Person();
      person.setEntityStatus(EntityStatus.ACTIVE);
      person.setIdentifier(UUID.fromString(clientFacade.getBaseEntityId()));
      person.setActive(true);
    }

    if (clientFacade.getGender() != null) {
      person.setGender(clientFacade.getGender());
    }

    if (clientFacade.getLastName() != null) {
      person.setNameFamily(clientFacade.getLastName());
    }

    if (clientFacade.getFirstName() != null) {
      person.setNameGiven(clientFacade.getFirstName());
      person.setNameText(clientFacade.getFirstName());
    }

    if (clientFacade.getBirthdate() != null) {
      person.setBirthDate(Date.from(DateTimeFormatter.getLocalDateTimeFromZonedAndroidFacadeString(
          clientFacade.getBirthdate()).atZone(ZoneId.systemDefault()).toInstant()));
    }

    if (clientFacade.getBirthdate() != null) {
      person.setBirthDate(Date.from(DateTimeFormatter.getLocalDateTimeFromZonedAndroidFacadeString(
          clientFacade.getBirthdate()).atZone(ZoneId.systemDefault()).toInstant()));
    }

    if (clientFacade.getDeathdate() != null) {
      person.setDeathDate(Date.from(DateTimeFormatter.getLocalDateTimeFromZonedAndroidFacadeString(
          clientFacade.getDeathdate()).atZone(ZoneId.systemDefault()).toInstant()));
    }

    if (clientFacade.getBirthdateApprox()) {
      person.setBirthDateApprox(true);
    }

    if (clientFacade.getDeathdateApprox()) {
      person.setDeathDateApprox(true);
    }

    if (person.getNameUse() == null) {
      person.setNameUse(NameUseEnum.USUAL.name());
    }

    if (person.getNamePrefix() == null) {
      person.setNamePrefix("");
    }

    if (person.getNameSuffix() == null) {
      person.setNameSuffix("");
    }

    return person;
  }


  private Event saveEvent(EventFacade eventFacade) {

    Event event = Event.builder()
        .taskIdentifier(UUID.fromString(eventFacade.getDetails().get("taskIdentifier")))
        .additionalInformation(objectMapper.valueToTree(eventFacade)).captureDate(
            DateTimeFormatter.getLocalDateTimeFromZonedAndroidFacadeString(
                eventFacade.getEventDate())).eventType(eventFacade.getEventType())
        .details(objectMapper.valueToTree(eventFacade.getDetails()))
        .locationIdentifier(UUID.fromString(eventFacade.getLocationId()))
        .organization(organizationService.findById(UUID.fromString(eventFacade.getTeamId()), false))
        .user(userService.getByUserName(eventFacade.getProviderId()))
        .planIdentifier(UUID.fromString(eventFacade.getDetails().get("planIdentifier")))
        .baseEntityIdentifier(UUID.fromString(eventFacade.getBaseEntityId()))
        .build();

    event.setEntityStatus(EntityStatus.ACTIVE);

    Optional<EventMaxVersionProjection> eventMaxVersionProjections = eventService.findMaxVersionForEventByTaskIdentifier(
        UUID.fromString(eventFacade.getDetails().get("taskIdentifier")), event.getEventType());
    if (eventMaxVersionProjections.isPresent()) {
      if (event.getEventType().equals(eventMaxVersionProjections.get().getEventType())) {
        event.setIdentifier(UUID.fromString(eventMaxVersionProjections.get().getIdentifier()));
      } else {
        event.setIdentifier(UUID.randomUUID());
      }
      event.setVersion(eventMaxVersionProjections.get().getVersion() + 1);
    } else {
      event.setIdentifier(UUID.randomUUID());
      event.setVersion(1);
    }

    FormData formData = new FormData();
    formData.setForm(formService.findById(UUID.fromString(eventFacade.getFormSubmissionId())));
    formData.setEntityStatus(EntityStatus.ACTIVE);

    formData.setPayload(objectMapper.valueToTree(eventFacade.getObs()));
    event.setFormData(formData);

    event.setEntityStatus(EntityStatus.ACTIVE);

    return eventService.saveEvent(event);
  }

  public Page<Event> searchEvents(EventSearchCriteria eventSearchCriteria, Pageable pageable) {
    return eventService.searchEvents(eventSearchCriteria, pageable);
  }

  public ClientFacade getClientFacade(Person person) {

    ClientFacade clientFacade = ClientFacade.builder().birthdate(
            person.getBirthDate() == null ? null
                : DateTimeFormatter.getDateTimeFacadeStringFromDate(person.getBirthDate())).deathdate(
            person.getDeathDate() == null ? null
                : DateTimeFormatter.getDateTimeFacadeStringFromDate(person.getDeathDate()))
        .birthdateApprox(person.isBirthDateApprox()).deathdateApprox(person.isDeathDateApprox())
        .firstName(person.getNameText()).gender(person.getGender()).lastName(person.getNameFamily())
        .clientType("Client").build();

    try {
      ClientFacadeMetadata clientFacadeMetadata = getClientMetaData(person);
      if (clientFacadeMetadata != null) {
        clientFacade.setRelationships(clientFacadeMetadata.getRelationShips());
        clientFacade.setAttributes(clientFacadeMetadata.getAttributes());
        clientFacade.setIdentifiers(clientFacadeMetadata.getIdentifiers());
      }
    } catch (JsonProcessingException e) {
      log.error("Additional Info on Person cannot be mapped to metadata Object");
      e.printStackTrace();
    }
    clientFacade.setBaseEntityId(person.getIdentifier().toString());
    clientFacade.setDateCreated(
        DateTimeFormatter.getDateTimeFacadeStringFromLocalDateTime(person.getCreatedDatetime()));
    return clientFacade;
  }

  public ClientFacade getGroupClientFacade(Group group) {

    ClientFacade clientFacade = ClientFacade.builder().birthdate(
            DateTimeFormatter.getDateTimeFacadeStringFromLocalDateTime(
                LocalDateTime.ofEpochSecond(0, 0, ZoneOffset.UTC))).clientType(group.getType())
        .firstName(group.getName()).lastName(group.getType()).build();

    try {
      ClientFacadeMetadata clientFacadeMetadata = getClientMetaData(group);
      clientFacade.setRelationships(clientFacadeMetadata.getRelationShips());
      clientFacade.setAttributes(clientFacadeMetadata.getAttributes());
      clientFacade.setIdentifiers(clientFacadeMetadata.getIdentifiers());
      clientFacade.setGender(clientFacadeMetadata.getFamilyHeadGender());
    } catch (JsonProcessingException e) {
      log.error("Additional Info on Person cannot be mapped to metadata Object");
      e.printStackTrace();
    }
    clientFacade.setBaseEntityId(group.getIdentifier().toString());
    clientFacade.setDateCreated(
        DateTimeFormatter.getDateTimeFacadeStringFromLocalDateTime(group.getCreatedDatetime()));
    return clientFacade;
  }


  private ClientFacadeMetadata getClientMetaData(Person person) throws JsonProcessingException {

    ClientFacadeMetadata clientFacadeMetadata = null;

    if (person.getAdditionalInfo() != null) {
      clientFacadeMetadata = objectMapper.treeToValue(person.getAdditionalInfo(),
          ClientFacadeMetadata.class);
    }
    return clientFacadeMetadata;
  }

  private ClientFacadeMetadata getClientMetaData(Group group) throws JsonProcessingException {

    ClientFacadeMetadata clientFacadeMetadata = null;

    if (group.getAdditionalInfo() != null) {

      clientFacadeMetadata = objectMapper.treeToValue(group.getAdditionalInfo(),
          ClientFacadeMetadata.class);

    }
    return clientFacadeMetadata;
  }

  public List<Person> searchPeople(List<Event> searchEvents) {
    return searchEvents.stream()
        .map(event -> event.getAdditionalInformation().get("baseEntityId").textValue())
        .map(UUID::fromString).map(personIdentifier -> {
          try {
            return personService.getPersonByIdentifier(personIdentifier);
          } catch (NotFoundException notFoundException) {
            return null;
          }
        }).filter(Objects::nonNull).collect(Collectors.toList());
  }

  public List<EventFacade> getEventFacades(List<Event> searchEvents) {

    return searchEvents.stream().map(this::getEventFacade).filter(Objects::nonNull)
        .collect(Collectors.toList());
  }

  private EventFacade getEventFacade(Event event) {

    try {

      List<Obs> obs = objectMapper.readValue(
          objectMapper.writeValueAsString(event.getFormData().getPayload()),
          new TypeReference<List<Obs>>() {
          });

      Map<String, String> details = objectMapper.readValue(
          objectMapper.writeValueAsString(event.getDetails()),
          new TypeReference<Map<String, String>>() {
          });

      return EventFacadeFactory.getEventFacade(event, obs, details);

    } catch (JsonProcessingException e) {
      e.printStackTrace();
      return null;
    }
  }

  public List<Group> searchGroups(List<Person> people) {
    return people.stream().map(Person::getGroups).flatMap(Collection::stream)
        .filter(group -> group.getType().equals("Family")).map(Group::getIdentifier)
        .map(groupIdentifier -> {
          try {
            return groupService.getGroupByIdentifier(groupIdentifier);
          } catch (NotFoundException notFoundException) {
            return null;
          }
        }).filter(Objects::nonNull).collect(Collectors.toList());
  }

  public List<ClientFacade> getClientFacades(List<Event> events) {
    List<Person> people = searchPeople(events);

    List<ClientFacade> clientFacades = people.stream().map(this::getClientFacade)
        .collect(Collectors.toList());

    List<Group> groups = searchGroups(people);

    List<ClientFacade> groupClientFacades = groups.stream().map(this::getGroupClientFacade)
        .collect(Collectors.toList());

    List<ClientFacade> combinedClientFacade = new ArrayList<>();
    combinedClientFacade.addAll(clientFacades);
    combinedClientFacade.addAll(groupClientFacades);
    return combinedClientFacade;
  }

  public List<Event> findLatestCaptureDatePerIdentifier(List<Event> events) {

    Map<UUID, Event> eventMap = new HashMap<>();
    for (Event event : events) {
      if (eventMap.containsKey(event.getIdentifier())) {
        if (eventMap.get(event.getIdentifier()).getCaptureDate().isBefore(event.getCaptureDate())) {
          eventMap.put(event.getIdentifier(), event);
        }
      } else {
        eventMap.put(event.getIdentifier(), event);
      }
    }
    return new ArrayList<>(eventMap.values());
  }


}
