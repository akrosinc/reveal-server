package com.revealprecision.revealserver.service;

import com.revealprecision.revealserver.api.v1.dto.factory.IdentifierNameResponseFactory;
import com.revealprecision.revealserver.api.v1.dto.factory.InstanceContextResponseFactory;
import com.revealprecision.revealserver.api.v1.dto.factory.InstanceResponseFactory;
import com.revealprecision.revealserver.api.v1.dto.request.GlobalUserRequest;
import com.revealprecision.revealserver.api.v1.dto.request.InstanceRequest;
import com.revealprecision.revealserver.api.v1.dto.response.GeoTreeResponse;
import com.revealprecision.revealserver.api.v1.dto.response.IdentifierNameResponse;
import com.revealprecision.revealserver.api.v1.dto.response.InstanceContextResponse;
import com.revealprecision.revealserver.api.v1.dto.response.InstanceResponse;
import com.revealprecision.revealserver.api.v1.dto.response.InstanceUserListResponse;
import com.revealprecision.revealserver.api.v1.dto.response.UserRolesResponse;
import com.revealprecision.revealserver.config.InstanceContext;
import com.revealprecision.revealserver.enums.EntityStatus;
import com.revealprecision.revealserver.enums.InstanceRoleEnum;
import com.revealprecision.revealserver.exceptions.ConflictException;
import com.revealprecision.revealserver.exceptions.NotFoundException;
import com.revealprecision.revealserver.exceptions.constant.Error;
import com.revealprecision.revealserver.persistence.domain.EntityTag;
import com.revealprecision.revealserver.persistence.domain.Instance;
import com.revealprecision.revealserver.persistence.domain.InstanceEntityTag;
import com.revealprecision.revealserver.persistence.domain.InstanceLocation;
import com.revealprecision.revealserver.persistence.domain.InstanceRole;
import com.revealprecision.revealserver.persistence.domain.InstanceUser;
import com.revealprecision.revealserver.persistence.domain.Location;
import com.revealprecision.revealserver.persistence.domain.LocationHierarchy;
import com.revealprecision.revealserver.persistence.domain.Organization;
import com.revealprecision.revealserver.persistence.domain.OrganizationRole;
import com.revealprecision.revealserver.persistence.domain.OrganizationRoleMapping;
import com.revealprecision.revealserver.persistence.domain.Plan;
import com.revealprecision.revealserver.persistence.domain.User;
import com.revealprecision.revealserver.persistence.projection.IdentifierNameProjection;
import com.revealprecision.revealserver.persistence.projection.InstanceEntityTagIdProjection;
import com.revealprecision.revealserver.persistence.projection.InstanceListProjection;
import com.revealprecision.revealserver.persistence.projection.UserIdInstanceNameProjection;
import com.revealprecision.revealserver.persistence.repository.EntityTagAccGrantsOrganizationRepository;
import com.revealprecision.revealserver.persistence.repository.InstanceEntityTagRepository;
import com.revealprecision.revealserver.persistence.repository.InstanceLocationRepository;
import com.revealprecision.revealserver.persistence.repository.InstanceRepository;
import com.revealprecision.revealserver.persistence.repository.InstanceUserRepository;
import com.revealprecision.revealserver.persistence.repository.OrganizationLocationRepository;
import com.revealprecision.revealserver.persistence.repository.OrganizationRoleMappingRepository;
import com.revealprecision.revealserver.persistence.repository.OrganizationRoleRepository;
import com.revealprecision.revealserver.persistence.repository.UserRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import org.apache.commons.lang3.BooleanUtils;
import org.springframework.transaction.annotation.Transactional;
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
  private final LocationRelationshipService locationRelationshipService;
  private final OrganizationRoleMappingRepository organizationRoleMappingRepository;
  private final OrganizationLocationRepository organizationLocationRepository;
  private final EntityTagAccGrantsOrganizationRepository entityTagAccGrantsOrganizationRepository;
  private final UserRepository userRepository;

  @Transactional
  public void create(InstanceRequest instanceRequest) {

    instanceRepository.findByName(instanceRequest.getInstanceName()).ifPresent(instance -> {
      throw new ConflictException(
          String.format(Error.NON_UNIQUE, "Instance name", instanceRequest.getInstanceName()));
    });

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

  public Page<InstanceListProjection> searchInstance(String searchParam, Pageable pageable) {
    Page<InstanceListProjection> projectionPage = StringUtils.isBlank(searchParam)
        ? instanceRepository.findAllInstances(pageable)
        : instanceRepository.findInstanceListBySearch(searchParam, pageable);

    return projectionPage;
  }

  public Instance findById(UUID identifier) {
    return instanceRepository.findById(identifier)
        .orElseThrow(
            () -> new NotFoundException(Pair.of("identifier", identifier), Instance.class));
  }

  public InstanceResponse getInstanceResponse(UUID identifier) {
    Instance instance = findById(identifier);

    List<IdentifierNameProjection> assignedLocations = instanceLocationRepository.getAreasIdNamesByInstance(
        identifier);
    List<GeoTreeResponse> areas = getAssignedInstanceAreasTree(instance.getIdentifier());

    Plan instancePlan = instance.getPlans().stream().findFirst().orElse(null);

    List<UUID> assignedLocationsIds = assignedLocations.stream()
        .map(IdentifierNameProjection::getIdentifier)
        .collect(Collectors.toList());

    return InstanceResponseFactory.fromEntity(instance, areas, instancePlan, assignedLocationsIds);
  }

  @Transactional
  public void update(UUID identifier, InstanceRequest instanceRequest) {
    Instance instance = findById(identifier);

    if (instanceRequest.getInstanceName() != null && !instance.getName()
        .equals(instanceRequest.getInstanceName())) {
      instanceRepository.findByName(instanceRequest.getInstanceName()).ifPresent(instance1 -> {
        if (!instance1.getIdentifier().equals(instance.getIdentifier())) {
          throw new ConflictException(
              String.format(Error.NON_UNIQUE, "Instance name", instanceRequest.getInstanceName()));
        }
      });
    }

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

  public List<InstanceEntityTagIdProjection> findInstancesNamesByEntityIds(
      List<UUID> entityTagtIdList) {
    return instanceRepository.findInstancesNamesByEntityIds(entityTagtIdList);
  }

  public List<InstanceUserListResponse> getUsersInstances() {
    User currentUser = userService.getCurrentUser();
    return getInstancesByUserId(currentUser.getIdentifier());
  }

  public List<IdentifierNameResponse> getAssignedInstanceUsers() {
    UUID instanceIdentifier = InstanceContext.get();
    return instanceUserRepository.getInstancesUsers(instanceIdentifier).stream()
        .map(IdentifierNameResponseFactory::toIdentifierNameResponse).collect(Collectors.toList());
  }

  public List<IdentifierNameResponse> getAssignedInstanceAreas() {
    UUID instanceIdentifier = InstanceContext.get();
    return getAssignedInstanceAreas(instanceIdentifier);
  }

  public List<IdentifierNameResponse> getAssignedInstanceAreas(UUID instanceIdentifier) {
    return instanceLocationRepository.getAreasIdNamesByInstance(instanceIdentifier).stream()
        .map(IdentifierNameResponseFactory::toIdentifierNameResponse).collect(Collectors.toList());
  }

  public List<IdentifierNameResponse> getAssignedInstanceDatasets() {

    UUID instanceIdentifier = InstanceContext.get();

    return instanceEntityTagRepository.getDatasetsIdNamesByInstance(instanceIdentifier).stream()
        .map(IdentifierNameResponseFactory::toIdentifierNameResponse).collect(Collectors.toList());
  }

  public InstanceContextResponse instanceContext(final UUID instanceIdentifier) {

    User currentUser = userService.getCurrentUser();
    UUID userId = currentUser.getIdentifier();

    // Resolve instance identifier - use provided, fall back to context, then first available
    UUID resolvedInstanceId = instanceIdentifier != null
        ? instanceIdentifier
        : InstanceContext.getSafe().orElse(null);

    InstanceUser instanceUser = (resolvedInstanceId == null
        ? instanceUserRepository.findByUser(userId)
        : instanceUserRepository.findByUserAndInstance(userId, resolvedInstanceId))
        .stream()
        .findFirst()
        .orElseThrow(() -> new NotFoundException(resolvedInstanceId == null
            ? "User is not a member of any instance"
            : "User is not a member of this instance"));

    // Use the resolved instance id from the found instanceUser
    UUID effectiveInstanceId = instanceUser.getInstance().getIdentifier();

    User userWithOrganizations = userService.findByIdWithOrganizations(userId)
        .stream()
        .findFirst()
        .orElseThrow(() -> new NotFoundException("User not found"));

    List<InstanceContextResponse.GroupContextInfo> groups = userWithOrganizations.getOrganizations()
        .stream()
        .filter(org -> org.getInstance().getIdentifier().equals(effectiveInstanceId))
        .map(org -> {
          List<OrganizationRole> orgRoles = organizationRoleMappingRepository
              .findRolesByUserAndOrganization(userId, org.getIdentifier())
              .stream()
              .map(OrganizationRoleMapping::getOrganizationRole)
              .collect(Collectors.toList());

          return InstanceContextResponseFactory.toGroupContextInfo(org, orgRoles);
        })
        .collect(Collectors.toList());

    return InstanceContextResponseFactory.buildInstanceContextResponse(instanceUser, groups);
  }

  public boolean isMember(UUID userId, UUID instanceId) {
    Optional<Instance> instanceOptional = instanceUserRepository.findFirstInstanceByUserIdentifierAndInstanceIdentifier
        (userId, instanceId).stream().findFirst();
    return instanceOptional.isPresent();
  }

  public List<GeoTreeResponse> getAssignedInstanceAreasTree() {
    return getAssignedInstanceAreasTree(null);
  }

  public List<GeoTreeResponse> getAssignedInstanceAreasTree(UUID instanceIdentifier) {

    List<IdentifierNameResponse> instancesAreas = null;

    if (instanceIdentifier == null) {
      instancesAreas = getAssignedInstanceAreas();
    } else {
      instancesAreas = getAssignedInstanceAreas(instanceIdentifier);
    }

    List<UUID> instancesAreasIds = instancesAreas.stream()
        .map(IdentifierNameResponse::getIdentifier)
        .collect(Collectors.toList());

    List<GeoTreeResponse> geoTreeResponses = locationRelationshipService.getFilteredGeoTreeByLocationIds(
        instancesAreasIds);

    return geoTreeResponses;
  }

  public List<InstanceUserListResponse> getInstancesByUserId(UUID userId) {
    User currentUser = userService.getCurrentUser();

    return instanceUserRepository.getUserInstances(currentUser.getIdentifier()).stream()
        .map(instanceUser -> {
          InstanceUserListResponse response = new InstanceUserListResponse();
          response.setIdentifier(instanceUser.getIdentifier());
          response.setName(instanceUser.getName());
          return response;
        }).collect(Collectors.toList());
  }

  public List<UserIdInstanceNameProjection> findInstanceNamesUserIdByUserIds(List<UUID> userIds) {
    return instanceUserRepository.getUserInstancesByUserIds(userIds);
  }

  public List<GeoTreeResponse> getLocationsByUserId(UUID userId) {
    return null;
  }

  public List<IdentifierNameResponse> getDatasetsByUserId(UUID userId) {
    List<InstanceUser> instanceUsers = instanceUserRepository.findByUser(userId);

    if (instanceUsers.isEmpty()) {
      return List.of();
    }

    Map<InstanceRoleEnum, List<UUID>> partitionedInstanceIds = instanceUsers.stream()
        .collect(Collectors.groupingBy(
            iu -> InstanceRoleEnum.valueOf(iu.getRole().getName()),
            Collectors.mapping(iu -> iu.getInstance().getIdentifier(), Collectors.toList())
        ));

    List<UUID> adminInstanceIds = partitionedInstanceIds
        .getOrDefault(InstanceRoleEnum.ADMIN, List.of());
    List<UUID> standardInstanceIds = partitionedInstanceIds
        .getOrDefault(InstanceRoleEnum.STANDARD, List.of());

    List<IdentifierNameProjection> datasets = new ArrayList<>();

    if (!adminInstanceIds.isEmpty()) {
      datasets.addAll(
          instanceEntityTagRepository.findDatasetTagsByInstanceIds(adminInstanceIds)
      );
    }

    if (!standardInstanceIds.isEmpty()) {
      datasets.addAll(
          entityTagAccGrantsOrganizationRepository
              .findDatasetsByUserIdAndInstanceIds(userId, standardInstanceIds)
      );
    }

    return datasets.stream()
        .collect(Collectors.toMap(
            IdentifierNameProjection::getIdentifier,
            p -> IdentifierNameResponse.builder()
                .identifier(p.getIdentifier())
                .name(p.getName())
                .build(),
            (a, b) -> a))
        .values()
        .stream()
        .collect(Collectors.toList());
  }

  public UserRolesResponse getRolesByUserId(UUID userId) {

    List<InstanceUser> instanceUsers = instanceUserRepository.findByUser(userId);

    if (instanceUsers.isEmpty()) {
      return UserRolesResponse.builder()
          .instanceInfos(List.of())
          .build();
    }

    User userWithOrgs = userRepository.findByIdWithOrganizations(userId)
        .stream()
        .findFirst()
        .orElseThrow(() -> new NotFoundException("User not found"));

    // Group organizations by instance for quick lookup
    Map<UUID, List<Organization>> orgsByInstance = userWithOrgs.getOrganizations()
        .stream()
        .collect(Collectors.groupingBy(
            org -> org.getInstance().getIdentifier()
        ));

    List<UserRolesResponse.InstanceInfo> instanceInfos = instanceUsers.stream()
        .map(instanceUser -> {
          UUID instanceId = instanceUser.getInstance().getIdentifier();

          // Instance role
          IdentifierNameResponse instanceRole = IdentifierNameResponse.builder()
              .identifier(instanceUser.getRole().getIdentifier())
              .name(instanceUser.getRole().getName())
              .build();

          // Group roles within this instance
          List<UserRolesResponse.GroupRoleInfo> groupRoles = orgsByInstance
              .getOrDefault(instanceId, List.of())
              .stream()
              .map(org -> {
                List<IdentifierNameResponse> roles = organizationRoleMappingRepository
                    .findRolesByUserAndOrganization(userId, org.getIdentifier())
                    .stream()
                    .map(rm -> IdentifierNameResponse.builder()
                        .identifier(rm.getOrganizationRole().getIdentifier())
                        .name(rm.getOrganizationRole().getName())
                        .build())
                    .collect(Collectors.toList());

                return UserRolesResponse.GroupRoleInfo.builder()
                    .group(IdentifierNameResponse.builder()
                        .identifier(org.getIdentifier())
                        .name(org.getName())
                        .build())
                    .roles(roles)
                    .build();
              })
              .collect(Collectors.toList());

          return UserRolesResponse.InstanceInfo.builder()
              .instanceRole(instanceRole)
              .groupRoles(groupRoles)
              .build();
        })
        .collect(Collectors.toList());

    return UserRolesResponse.builder()
        .instanceInfos(instanceInfos)
        .build();
  }

  public List<GeoTreeResponse> getInstanceHierarchy(UUID instanceIdentifier) {
    if (instanceIdentifier == null) {
      // Get base hierarchy and build full geo tree
      LocationHierarchy baseHierarchy = locationHierarchyService.getBaseLocationHierarchy();
      return locationHierarchyService.getGeoTreeFromLocationHierarchy(baseHierarchy, true);
    }
    Instance instance = findById(instanceIdentifier);
    return getAssignedInstanceAreasTree(instanceIdentifier);
  }

  @Transactional
  public void addUser(GlobalUserRequest globalUserRequest) {
    User user = userService.createGlobalUser(globalUserRequest);

    if (globalUserRequest.getInstanceIdentifier() == null) {
      throw new IllegalArgumentException("Instance identifier not provided");
    }

    Instance instance = findById(globalUserRequest.getInstanceIdentifier());

    final InstanceRole instanceRole;
    if (BooleanUtils.isTrue(globalUserRequest.getIsInstanceAdmin())){
      instanceRole = instanceRoleService.getInstanceAdminRole();
    } else {
      instanceRole = instanceRoleService.getStandardRole();
    }

    InstanceUser instanceUser = new InstanceUser();
    instanceUser.populate(instance, user);
    instanceUser.setRole(instanceRole);

    instanceUserRepository.save(instanceUser);
  }

  public long getCountFindAll(String searchParam) {
    return StringUtils.isBlank(searchParam)
        ? instanceRepository.countAllInstances()
        : instanceRepository.countInstanceListBySearch(searchParam);
  }
}
