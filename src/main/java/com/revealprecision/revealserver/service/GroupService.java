package com.revealprecision.revealserver.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.revealprecision.revealserver.api.v1.dto.request.GroupRequest;
import com.revealprecision.revealserver.enums.EntityStatus;
import com.revealprecision.revealserver.exceptions.NotFoundException;
import com.revealprecision.revealserver.persistence.domain.Group;
import com.revealprecision.revealserver.persistence.domain.Location;
import com.revealprecision.revealserver.persistence.domain.Plan;
import com.revealprecision.revealserver.persistence.repository.GroupRepository;
import com.revealprecision.revealserver.persistence.repository.LocationRepository;
import com.sun.xml.bind.v2.TODO;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.apache.james.mime4j.field.Fields;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service
@Slf4j
public class GroupService {

  @Autowired
  private GroupRepository groupRepository;
  @Autowired
  private LocationService locationService;

  @Autowired
  private PersonService personService;

//TODO - TB: Wire in the location if need be
//    @Autowired
//    private LocationService locationService;

  public Page<Group> getGroups(Integer pageNumber, Integer pageSize) {
    return groupRepository.findAll(PageRequest.of(pageNumber, pageSize));
  }

  public Group createGroup(GroupRequest groupRequest) {
    var groupBuilder = Group.builder()
        .type(groupRequest.getType().toString())
        .name(groupRequest.getName());

    if (groupRequest.getLocationIdentifier() != null) {
      Optional<Location> locationOptional = locationService.findByIdentifier(
          groupRequest.getLocationIdentifier());
      locationOptional.ifPresent(groupBuilder::location);
    }

    Group group = groupBuilder.build();
    group.setEntityStatus(EntityStatus.ACTIVE);
    Group save = groupRepository.save(group);
    log.info("Group saved to database as {}", group);

    return save;
  }

  public Group getGroupByIdentifier(UUID groupIdentifier) {
    var group = groupRepository.findByIdentifier(groupIdentifier);

    if (group.isEmpty()) {
//            Group.Fields.identifier
      throw new NotFoundException("Group with identifier " + groupIdentifier + " not found");
    }


    return group.get();
  }

  public void removeGroup(UUID groupIdentifier) {
    var group = groupRepository.findByIdentifier(groupIdentifier);

    if (group.isEmpty()) {
      throw new NotFoundException("Group with identifier " + groupIdentifier + " not found");
    }

    groupRepository.delete(group.get());
  }

  public Group updateGroup(UUID groupIdentifier, GroupRequest groupRequest) {
    var group = groupRepository.findByIdentifier(groupIdentifier);

    if (group.isEmpty()) {
      throw new NotFoundException("Group with identifier " + groupIdentifier + " not found");
    }

    var groupRetrieved = group.get();

    groupRetrieved.setName(groupRequest.getName());
    groupRetrieved.setType(groupRequest.getType().toString());

    if (groupRequest.getLocationIdentifier() != null) {
      var location = locationService.findByIdentifier(groupRequest.getLocationIdentifier());

      if (location.isEmpty()){
        throw new NotFoundException("Cannot create group with locationidentifier " + groupRequest.getLocationIdentifier() + " as location is not found");
      }

      groupRetrieved.setLocation(location.get());
    } else {
      groupRetrieved.setLocation(null);
    }


    return groupRepository.save(groupRetrieved);
  }

  public List<Group> getAllGroups() {
    return groupRepository.findAll();
  }
}