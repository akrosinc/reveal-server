package com.revealprecision.revealserver.service;

import com.revealprecision.revealserver.api.v1.dto.request.GroupRequest;
import com.revealprecision.revealserver.enums.EntityStatus;
import com.revealprecision.revealserver.exceptions.NotFoundException;
import com.revealprecision.revealserver.persistence.domain.Group;
import com.revealprecision.revealserver.persistence.domain.Location;
import com.revealprecision.revealserver.persistence.repository.GroupRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class GroupService {

  GroupRepository groupRepository;
  LocationService locationService;

  @Autowired
  public GroupService(GroupRepository groupRepository, LocationService locationService) {
    this.groupRepository = groupRepository;
    this.locationService = locationService;
  }


  public Page<Group> getGroups(String searchParam,
      String  groupName,
      String locationName, Integer pageNumber, Integer pageSize) {

    if (searchParam != null) {
      return  groupRepository.findGroupByNameContainingIgnoreCaseOrLocation_NameContainingIgnoreCase(
          searchParam, searchParam, PageRequest.of(pageNumber, pageSize));
    } else {
      if (groupName != null) {
        if (locationName != null) {
          return groupRepository.findGroupByNameAndLocation_NameIgnoreCase(
              groupName, locationName, PageRequest.of(pageNumber, pageSize));
        } else {
          return groupRepository.findGroupByNameIgnoreCase(groupName,
              PageRequest.of(pageNumber, pageSize));
        }
      } else {
        if (locationName != null) {
          return groupRepository.findGroupByLocation_NameIgnoreCase(locationName,PageRequest.of(pageNumber, pageSize));
        } else {
          return groupRepository.findAll(PageRequest.of(pageNumber, pageSize));
        }
      }
    }
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

      if (location.isEmpty()) {
        throw new NotFoundException(
            "Cannot create group with locationidentifier " + groupRequest.getLocationIdentifier()
                + " as location is not found");
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