package com.revealprecision.revealserver.service;

import com.revealprecision.revealserver.api.v1.dto.request.GroupRequest;
import com.revealprecision.revealserver.enums.EntityStatus;
import com.revealprecision.revealserver.exceptions.NotFoundException;
import com.revealprecision.revealserver.persistence.domain.Group;
import com.revealprecision.revealserver.persistence.domain.Location;
import com.revealprecision.revealserver.persistence.domain.Organization.Fields;
import com.revealprecision.revealserver.persistence.repository.GroupRepository;
import com.revealprecision.revealserver.service.models.GroupSearchCriteria;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class GroupService {

  final GroupRepository groupRepository;
  final LocationService locationService;

  @Autowired
  public GroupService(GroupRepository groupRepository, LocationService locationService) {
    this.groupRepository = groupRepository;
    this.locationService = locationService;
  }

  public Page<Group> getGroups(String searchParam, GroupSearchCriteria criteria,
      Pageable pageable) {

    if (searchParam != null) {
      return groupRepository.findGroupByNameContainingIgnoreCase(searchParam, pageable);
    }

    if (criteria != null) {
      if (criteria.getGroupName() != null && criteria.getGroupType() != null) {
        return groupRepository.findGroupByNameIgnoreCaseAndTypeIgnoreCase(criteria.getGroupName(),
            criteria.getGroupType().getValue(), pageable);
      } else {
        if (criteria.getGroupName() != null) {
          return groupRepository.findGroupByNameIgnoreCase(criteria.getGroupName(), pageable);
        }
        if (criteria.getGroupType() != null) {
          return groupRepository.findGroupByTypeIgnoreCase(criteria.getGroupType().getValue(),
              pageable);

        }
      }
    }
    return groupRepository.findAll(pageable);
  }

  public Group createGroup(GroupRequest groupRequest) {
    Group.GroupBuilder groupBuilder = Group.builder().type(groupRequest.getType().getValue())
        .name(groupRequest.getName());

    if (groupRequest.getLocationIdentifier() != null) {
      Location location = locationService.findByIdentifier(groupRequest.getLocationIdentifier());
      groupBuilder.location(location);
    }

    Group group = groupBuilder.build();
    group.setEntityStatus(EntityStatus.ACTIVE);

    return groupRepository.save(group);
  }

  public Group getGroupByIdentifier(UUID groupIdentifier) {
    return groupRepository.findByIdentifier(groupIdentifier).orElseThrow(
        () -> new NotFoundException(Pair.of(Fields.identifier, groupIdentifier), Group.class));
  }

  public void removeGroup(UUID groupIdentifier) {
    Group group = getGroupByIdentifier(groupIdentifier);
    groupRepository.delete(group);
  }

  public Group updateGroup(UUID groupIdentifier, GroupRequest groupRequest) {

    Group groupRetrieved = getGroupByIdentifier(groupIdentifier);

    groupRetrieved.setName(groupRequest.getName());
    groupRetrieved.setType(groupRequest.getType().getValue());

    if (groupRequest.getLocationIdentifier() != null) {
      Location location = locationService.findByIdentifier(groupRequest.getLocationIdentifier());
      groupRetrieved.setLocation(location);
    }

    return groupRepository.save(groupRetrieved);
  }

}