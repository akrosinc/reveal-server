package com.revealprecision.revealserver.service;

import com.revealprecision.revealserver.api.v1.dto.factory.InstanceResponseFactory;
import com.revealprecision.revealserver.api.v1.dto.request.InstanceRequest;
import com.revealprecision.revealserver.api.v1.dto.response.IdentifierNameResponse;
import com.revealprecision.revealserver.api.v1.dto.response.InstanceResponse;
import com.revealprecision.revealserver.api.v1.dto.response.InstanceUserListResponse;
import com.revealprecision.revealserver.enums.EntityStatus;
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
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
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

      Instance instance = new Instance();
      instance.setName(instanceRequest.getInstanceName());
      instance.setEntityStatus(EntityStatus.ACTIVE);
      LocationHierarchy locationHierarchy = locationHierarchyService.findByIdentifier(
          instanceRequest.getLocationHierarchy());
      instance.setLocationHierarchy(locationHierarchy);

      Instance savedInstance = instanceRepository.save(instance);


      Plan plan = planService.createPlan(instanceRequest.getPlanRequest(), savedInstance);

      //    Adding instance datasets tags

      if (instanceRequest.getDatasets_tags() != null) {
        List<EntityTag> tags = entityTagService.findEntityTagsByIdentifierIn(
            instanceRequest.getDatasets_tags());

        List<InstanceEntityTag> instanceEntityTags = tags.stream().distinct().map(tag -> {

          InstanceEntityTag mapping = new InstanceEntityTag();

          mapping.populate(savedInstance, tag);

          return mapping;
        }).collect(Collectors.toList());

        instanceEntityTagRepository.saveAll(instanceEntityTags);
      }

      //Adding instance users
      if (instanceRequest.getMembers() != null) {
        List<User> users = userService.findAllById(instanceRequest.getMembers());

        InstanceRole adminRole = instanceRoleService.getInstanceAdminRole();

        List<InstanceUser> instanceUsers = users.stream().distinct().map(user -> {
          InstanceUser mapping = new InstanceUser();
          mapping.populate(savedInstance, user);
          mapping.setRole(adminRole);
          return mapping;
        }).collect(Collectors.toList());

        instanceUserRepository.saveAll(instanceUsers);
      }

      //Adding Areas
      if (instanceRequest.getAreas() != null) {
        List<Location> locations = locationService.findAllIdentifiersWithoutStructureAndGeoJSON(
            instanceRequest.getAreas());

        List<InstanceLocation> areas = locations.stream().distinct().map(location -> {

          InstanceLocation mapping = new InstanceLocation();
          mapping.populate(savedInstance, location);

          return mapping;
        }).collect(Collectors.toList());

        instanceLocationRepository.saveAll(areas);
      }

  }

  public Page<InstanceResponse> searchInstance(String searchParam, Pageable pageable) {
    Page<Instance> instancePage ;
    if(StringUtils.isBlank(searchParam)) {
      instancePage = instanceRepository.findAll(pageable);
    }
    else {
      instancePage  = instanceRepository.searchInstance(searchParam, pageable);
    }

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
    if (instanceRequest.getDatasets_tags() != null) {
      List<EntityTag> tags = entityTagService.findEntityTagsByIdentifierIn(
          instanceRequest.getDatasets_tags());
      List<InstanceEntityTag> instanceEntityTags = tags.stream().distinct().map(tag -> {
        InstanceEntityTag mapping = new InstanceEntityTag();
        mapping.populate(instance, tag);
        return mapping;
      }).collect(Collectors.toList());
      instanceEntityTagRepository.saveAll(instanceEntityTags);
    }

    // Update users
    instanceUserRepository.deleteByInstance(instance);
    if (instanceRequest.getMembers() != null) {
      List<User> users = userService.findAllById(instanceRequest.getMembers());
      InstanceRole adminRole = instanceRoleService.getInstanceAdminRole();
      List<InstanceUser> instanceUsers = users.stream().distinct().map(user -> {
        InstanceUser mapping = new InstanceUser();
        mapping.populate(instance, user);
        mapping.setRole(adminRole);
        return mapping;
      }).collect(Collectors.toList());
      instanceUserRepository.saveAll(instanceUsers);
    }

    // Update locations
    instanceLocationRepository.deleteByInstance(instance);
    if (instanceRequest.getAreas() != null) {
      List<Location> locations = locationService.findAllIdentifiersWithoutStructureAndGeoJSON(
          instanceRequest.getAreas());
      List<InstanceLocation> areas = locations.stream().distinct().map(location -> {
        InstanceLocation mapping = new InstanceLocation();
        mapping.populate(instance, location);
        return mapping;
      }).collect(Collectors.toList());
      instanceLocationRepository.saveAll(areas);
    }
  }

  public List<InstanceProjection> findInstancesNamesByEntityIds(List<UUID> entityTagtIdList) {
    return instanceRepository.findInstancesNamesByEntityIds(entityTagtIdList);
  }

  public List<InstanceUserListResponse> getUsersInstances() {

    User currentUser = userService.getCurrentUser();

    return instanceUserRepository.getUserInstances(currentUser.getIdentifier()).stream()
        .map(instanceUser -> {
          InstanceUserListResponse response = new InstanceUserListResponse();
          response.setIdentifier(instanceUser.getIdentifier());
          response.setName(instanceUser.getName());
          return response;
        }).collect(Collectors.toList());
  }

  public List<InstanceUserListResponse> getAssignedInstanceUsers() {

    User currentUser = userService.getCurrentUser();

    return instanceUserRepository.getUserInstances(currentUser.getIdentifier()).stream()
        .map(instanceUser -> {
          InstanceUserListResponse response = new InstanceUserListResponse();
          response.setIdentifier(instanceUser.getIdentifier());
          response.setName(instanceUser.getName());
          return response;
        }).collect(Collectors.toList());
  }

  public List<IdentifierNameResponse> getAssignedInstanceAreas() {
//    User currentUser = userService.getCurrentUser();
    UUID instanceId = userService.getCurrentUser().getIdentifier();

    return instanceLocationRepository.getAreasByInstance(instanceId).stream()
        .map(area -> {
          IdentifierNameResponse response = new IdentifierNameResponse();
          response.setIdentifier(area.getIdentifier());
          response.setName(area.getName());
          return response;
        }).collect(Collectors.toList());
  }

  public IdentifierNameResponse instanceContext(UUID identifier) {
    User currentUser = userService.getCurrentUser();

    if(identifier == null){
      Optional<Instance> instanceOptional = instanceUserRepository.findFirstInstanceByUserIdentifier(currentUser.getIdentifier());

      if(instanceOptional.isPresent()) {
        IdentifierNameResponse response = new IdentifierNameResponse();
        response.setIdentifier(instanceOptional.get().getIdentifier());
        response.setName(instanceOptional.get().getName());
        return response;
      }
      else {
        throw new NotFoundException("User has no instances");
      }
    }
    else {
      Optional<Instance> instanceOptional = instanceUserRepository.findFirstInstanceByUserIdentifierAndInstanceIddentifier
                  (currentUser.getIdentifier(), identifier);
      if(instanceOptional.isPresent()) {
        IdentifierNameResponse response = new IdentifierNameResponse();
        response.setIdentifier(instanceOptional.get().getIdentifier());
        response.setName(instanceOptional.get().getName());
        return response;
      }
      else {
        throw new NotFoundException("User has no instances");
      }
    }
  }

  public boolean isMember(UUID userId, UUID instanceId) {
    Optional<Instance> instanceOptional = instanceUserRepository.findFirstInstanceByUserIdentifierAndInstanceIddentifier
        (userId, instanceId);
    return instanceOptional.isPresent();
  }
}
