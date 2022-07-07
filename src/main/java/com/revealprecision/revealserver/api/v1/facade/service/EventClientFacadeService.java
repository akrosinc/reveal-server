package com.revealprecision.revealserver.api.v1.facade.service;

import static com.revealprecision.revealserver.constants.EventClientConstants.CLIENTS;
import static com.revealprecision.revealserver.constants.EventClientConstants.EVENTS;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.revealprecision.revealserver.api.v1.facade.factory.EventSearchCriteriaFactory;
import com.revealprecision.revealserver.api.v1.facade.models.ClientFacade;
import com.revealprecision.revealserver.api.v1.facade.models.ClientFacadeMetadata;
import com.revealprecision.revealserver.api.v1.facade.models.EventFacade;
import com.revealprecision.revealserver.api.v1.facade.models.SyncParamFacade;
import com.revealprecision.revealserver.api.v1.facade.util.DateTimeFormatter;
import com.revealprecision.revealserver.enums.EntityStatus;
import com.revealprecision.revealserver.enums.NameUseEnum;
import com.revealprecision.revealserver.exceptions.NotFoundException;
import com.revealprecision.revealserver.persistence.domain.Event;
import com.revealprecision.revealserver.persistence.domain.Group;
import com.revealprecision.revealserver.persistence.domain.Location;
import com.revealprecision.revealserver.persistence.domain.Person;
import com.revealprecision.revealserver.persistence.es.PersonElastic;
import com.revealprecision.revealserver.service.EventService;
import com.revealprecision.revealserver.service.GroupService;
import com.revealprecision.revealserver.service.LocationService;
import com.revealprecision.revealserver.service.OrganizationService;
import com.revealprecision.revealserver.service.PersonService;
import com.revealprecision.revealserver.service.UserService;
import com.revealprecision.revealserver.service.models.EventSearchCriteria;
import com.revealprecision.revealserver.util.ElasticModelUtil;
import java.io.IOException;
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
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.reindex.UpdateByQueryRequest;
import org.elasticsearch.script.Script;
import org.elasticsearch.script.ScriptType;
import org.json.JSONObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.util.Pair;
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

  private final RestHighLevelClient client;


  public Pair<List<EventFacade>, List<ClientFacade>> processEventsClientsRequest(
      JSONObject eventClientRequestJSON)
      throws JsonProcessingException {
    ObjectMapper mapper = new ObjectMapper();
    List<EventFacade> failedEvents = new ArrayList<>();
    if (eventClientRequestJSON.has(EVENTS)) {
      String rawEventsRequest = eventClientRequestJSON.getString(EVENTS);
      List<EventFacade> eventFacades = List
          .of(mapper.readValue(rawEventsRequest, EventFacade[].class));
      failedEvents = addOrUpdateEvents(eventFacades);
    }
    List<ClientFacade> failedClients = new ArrayList<>();
    if (eventClientRequestJSON.has(CLIENTS)) {
      String rawClientsRequest = eventClientRequestJSON.getString(CLIENTS);
      List<ClientFacade> clientFacades = List
          .of(mapper.readValue(rawClientsRequest, ClientFacade[].class));
      failedClients = addOrUpdateClients(clientFacades);
    }

    return Pair.of(failedEvents, failedClients);
  }

  private List<EventFacade> addOrUpdateEvents(List<EventFacade> eventFacadeList) {
    List<EventFacade> failedEvents = new ArrayList<>();
    eventFacadeList.forEach(eventFacade -> {
      try {
        saveEvent(eventFacade);
      } catch (Exception exception) {
        exception.printStackTrace();
        failedEvents.add(eventFacade);
      }
    });
    return failedEvents;
  }

  private List<ClientFacade> addOrUpdateClients(List<ClientFacade> clientFacadeList) {
    List<ClientFacade> failedClients = new ArrayList<>();
    clientFacadeList.forEach(clientFacade -> {
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
    return failedClients;
  }

  private void savePerson(ClientFacade clientFacade, Location location) throws IOException {
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
    person = personService.savePerson(person);

    PersonElastic personElastic = new PersonElastic(person);
    Map<String, Object> parameters = new HashMap<>();

    parameters.put("person", ElasticModelUtil.toMapFromPersonElastic(personElastic));
    parameters.put("personId", personElastic.getIdentifier());
    UpdateByQueryRequest request = new UpdateByQueryRequest("location");
    List<String> locationIds = person.getLocations().stream().map(loc -> loc.getIdentifier().toString()).collect(
        Collectors.toList());

    request.setQuery(QueryBuilders.termsQuery("_id", locationIds));
    request.setScript(new Script(
        ScriptType.INLINE, "painless",
        "def foundPerson = ctx._source.person.find(attr-> attr.identifier == params.personId);"
            + " if(foundPerson == null) {ctx._source.person.add(params.person);}",
        parameters
    ));
    client.updateByQuery(request, RequestOptions.DEFAULT);
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
    Map<String, String> details = eventFacade.getDetails();
    Event event = Event.builder()
        .additionalInformation(objectMapper.valueToTree(eventFacade)).captureDatetime(
            DateTimeFormatter
                .getLocalDateTimeFromZonedAndroidFacadeString(eventFacade.getEventDate()))
        .eventType(eventFacade.getEventType())
        .details(objectMapper.valueToTree(details))
        .organization(organizationService.findById(UUID.fromString(eventFacade.getTeamId()), false))
        .user(userService.getByUserName(eventFacade.getProviderId()))
        .planIdentifier(UUID.fromString(details.get("planIdentifier")))
        .build();
    String taskIdentifier = details.get("taskIdentifier");
    if (StringUtils.isNotBlank(taskIdentifier)) {
      event.setTaskIdentifier(UUID.fromString(taskIdentifier));
    }
    String baseEntityId = eventFacade.getBaseEntityId();
    if (StringUtils.isNotBlank(baseEntityId)) {
      event.setBaseEntityIdentifier(UUID.fromString(baseEntityId));
    }
    String locationIdentifier = eventFacade.getLocationId() == null ? details.get("location_id")
        : eventFacade.getLocationId();
    if (StringUtils.isNotBlank(locationIdentifier)) {
      event.setLocationIdentifier(UUID.fromString(locationIdentifier));
    }
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
    return searchEvents.stream().filter(event -> event.getBaseEntityIdentifier() != null)
        .map(Event::getBaseEntityIdentifier).map(personIdentifier -> {
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
    ObjectMapper mapper = new ObjectMapper();
    EventFacade eventFacade = mapper
        .convertValue(event.getAdditionalInformation(), EventFacade.class);
    eventFacade.setEventId(event.getIdentifier().toString());
    eventFacade.setServerVersion(event.getServerVersion());
    return eventFacade;
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
        if (eventMap.get(event.getIdentifier()).getCaptureDatetime()
            .isBefore(event.getCaptureDatetime())) {
          eventMap.put(event.getIdentifier(), event);
        }
      } else {
        eventMap.put(event.getIdentifier(), event);
      }
    }
    return new ArrayList<>(eventMap.values());
  }

  public Pair<List<EventFacade>, List<ClientFacade>> getSyncedEventsAndClients(
      SyncParamFacade syncParam) {
    EventSearchCriteria eventSearchCriteria = EventSearchCriteriaFactory.getEventSearchCriteria(
        syncParam);
    PageRequest pageRequest = PageRequest.of(0, syncParam.getLimit());

    Page<Event> searchEvents = searchEvents(eventSearchCriteria,
        pageRequest);

    List<Event> events = findLatestCaptureDatePerIdentifier(
        searchEvents.get().collect(Collectors.toList()));

    List<EventFacade> eventFacades = getEventFacades(events);

    List<ClientFacade> combinedClientFacade = getClientFacades(events);

    return Pair.of(eventFacades, combinedClientFacade);
  }

}
