package com.revealprecision.revealserver.service;

import com.revealprecision.revealserver.api.v1.dto.factory.InstanceResponseFactory;
import com.revealprecision.revealserver.api.v1.dto.request.InstanceRequest;
import com.revealprecision.revealserver.api.v1.dto.response.InstanceResponse;
import com.revealprecision.revealserver.api.v1.dto.response.InstanceUserListResponse;
import com.revealprecision.revealserver.exceptions.NotFoundException;
import com.revealprecision.revealserver.persistence.domain.EntityTag;
import com.revealprecision.revealserver.persistence.domain.Instance;
import com.revealprecision.revealserver.persistence.domain.InstanceEntityTag;
import com.revealprecision.revealserver.persistence.domain.InstanceLocation;
import com.revealprecision.revealserver.persistence.domain.InstanceRole;
import com.revealprecision.revealserver.persistence.domain.InstanceUser;
import com.revealprecision.revealserver.persistence.domain.Location;
import com.revealprecision.revealserver.persistence.domain.LocationHierarchy;
import com.revealprecision.revealserver.persistence.domain.Plan;
import com.revealprecision.revealserver.persistence.domain.User;
import com.revealprecision.revealserver.persistence.projection.InstanceProjection;
import com.revealprecision.revealserver.persistence.repository.InstanceEntityTagRepository;
import com.revealprecision.revealserver.persistence.repository.InstanceLocationRepository;
import com.revealprecision.revealserver.persistence.repository.InstanceRepository;
import com.revealprecision.revealserver.persistence.repository.InstanceUserRepository;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class InstanceService {

  private final PlanService planService;
  private final EntityTagService entityTagService;
  private final UserService userService;
  private final LocationService locationService;
  private final InstanceRepository instanceRepository;
  private final InstanceEntityTagRepository instanceEntityTagRepository;
  private final InstanceRoleService instanceRoleService;
  private final InstanceUserRepository instanceUserRepository;
  private final InstanceLocationRepository instanceLocationRepository;
  private final LocationHierarchyService locationHierarchyService;

  @Transactional
  public void create(InstanceRequest instanceRequest) {

    Plan plan = planService.createPlan(instanceRequest.getPlanRequest());

    Instance instance = new Instance();
    instance.setName(instanceRequest.getInstanceName());
    instance.setPlans(Set.of(plan));
    LocationHierarchy locationHierarchy = locationHierarchyService.findByIdentifier(
        instanceRequest.getLocationHierarchy());
    instance.setLocationHierarchy(locationHierarchy);


    plan.setInstance(instance);


    Instance savedInstance = instanceRepository.save(instance);

    //    Adding inatnce datasets tags

    List<EntityTag> tags = entityTagService.findEntityTagsByIdentifierIn(
        instanceRequest.getDatasets_tags());

    List<InstanceEntityTag> instanceEntityTags = tags.stream().map(tag -> {

      InstanceEntityTag mapping = new InstanceEntityTag();

      mapping.setInstance(savedInstance);
      mapping.setEntityTag(tag);

      return mapping;
    }).collect(Collectors.toList());

    instanceEntityTagRepository.saveAll(instanceEntityTags);

    //Adding instance users
    List<User> users = userService.findAllById(instanceRequest.getMembers());

    InstanceRole adminRole = instanceRoleService.getInstanceAdminRole();

    List<InstanceUser> instanceUsers = users.stream().map(user -> {
      InstanceUser mapping = new InstanceUser();
      mapping.setInstance(savedInstance);
      mapping.setUser(user);
      mapping.setRole(adminRole);
      return mapping;
    }).collect(Collectors.toList());

    instanceUserRepository.saveAll(instanceUsers);

    //Adding Areas
    List<Location> locations = locationService.findAllIdentifiersWithoutStructureAndGeoJSON(
        instanceRequest.getAreas());

    List<InstanceLocation> areas = locations.stream().map(location -> {

      InstanceLocation mapping = new InstanceLocation();
      mapping.setInstance(savedInstance);
      mapping.setLocation(location);

      return mapping;
    }).collect(Collectors.toList());

    instanceLocationRepository.saveAll(areas);
  }

  public Page<InstanceResponse> searchInstance(String searchParam, Pageable pageable) {
    Page<Instance> instancePage = instanceRepository.searchInstance(searchParam, pageable);
    return InstanceResponseFactory.fromInstancePage(instancePage, pageable);
  }

  public Instance findById(UUID identifier) {
    return instanceRepository.findById(identifier)
        .orElseThrow(() -> new NotFoundException(Pair.of("identifier", identifier), Instance.class));
  }

  public InstanceResponse getInstanceResponse(UUID identifier) {
    return InstanceResponseFactory.fromEntity(findById(identifier));
  }

  @Transactional
  public void update(UUID identifier, InstanceRequest instanceRequest) {
    Instance instance = findById(identifier);

    instance.setName(instanceRequest.getInstanceName());
    LocationHierarchy locationHierarchy = locationHierarchyService.findByIdentifier(
        instanceRequest.getLocationHierarchy());
    instance.setLocationHierarchy(locationHierarchy);

    instanceRepository.save(instance);

    // Update tags
    instanceEntityTagRepository.deleteByInstance(instance);
    List<EntityTag> tags = entityTagService.findEntityTagsByIdentifierIn(
        instanceRequest.getDatasets_tags());
    List<InstanceEntityTag> instanceEntityTags = tags.stream().map(tag -> {
      InstanceEntityTag mapping = new InstanceEntityTag();
      mapping.setInstance(instance);
      mapping.setEntityTag(tag);
      return mapping;
    }).collect(Collectors.toList());
    instanceEntityTagRepository.saveAll(instanceEntityTags);

    // Update users
    instanceUserRepository.deleteByInstance(instance);
    List<User> users = userService.findAllById(instanceRequest.getMembers());
    InstanceRole adminRole = instanceRoleService.getInstanceAdminRole();
    List<InstanceUser> instanceUsers = users.stream().map(user -> {
      InstanceUser mapping = new InstanceUser();
      mapping.setInstance(instance);
      mapping.setUser(user);
      mapping.setRole(adminRole);
      return mapping;
    }).collect(Collectors.toList());
    instanceUserRepository.saveAll(instanceUsers);

    // Update locations
    instanceLocationRepository.deleteByInstance(instance);
    List<Location> locations = locationService.findAllIdentifiersWithoutStructureAndGeoJSON(
        instanceRequest.getAreas());
    List<InstanceLocation> areas = locations.stream().map(location -> {
      InstanceLocation mapping = new InstanceLocation();
      mapping.setInstance(instance);
      mapping.setLocation(location);
      return mapping;
    }).collect(Collectors.toList());
    instanceLocationRepository.saveAll(areas);
  }

  public List<InstanceProjection> findInstancesNamesByEntityIds(List<UUID> entityTagtIdList) {
    return instanceRepository.findInstancesNamesByEntityIds(entityTagtIdList);
  }

  public List<InstanceUserListResponse> getUserInstances() {

    User currentUser = userService.getCurrentUser();

    return instanceUserRepository.getUserInstances(currentUser.getIdentifier()).stream()
        .map(instanceUser -> {
          InstanceUserListResponse response = new InstanceUserListResponse();
          response.setIdentifier(instanceUser.getIdentifier());
          response.setName(instanceUser.getName());
          return response;
        }).collect(Collectors.toList());
  }
}
