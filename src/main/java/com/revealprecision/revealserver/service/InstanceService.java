package com.revealprecision.revealserver.service;

import com.revealprecision.revealserver.api.v1.dto.factory.IdentifierNameResponseFactory;
import com.revealprecision.revealserver.api.v1.dto.factory.InstanceContextResponseFactory;
import com.revealprecision.revealserver.api.v1.dto.factory.InstanceResponseFactory;
import com.revealprecision.revealserver.api.v1.dto.factory.LocationHierarchyResponseFactory;
import com.revealprecision.revealserver.api.v1.dto.request.GlobalUserRequest;
import com.revealprecision.revealserver.api.v1.dto.request.InstanceRequest;
import com.revealprecision.revealserver.api.v1.dto.response.GeoTreeResponse;
import com.revealprecision.revealserver.api.v1.dto.response.IdentifierNameResponse;
import com.revealprecision.revealserver.api.v1.dto.response.InstanceContextResponse;
import com.revealprecision.revealserver.api.v1.dto.response.InstanceResponse;
import com.revealprecision.revealserver.api.v1.dto.response.InstanceUserListResponse;
import com.revealprecision.revealserver.api.v1.dto.response.LocationHierarchyResponse;
import com.revealprecision.revealserver.api.v1.dto.response.UserRolesResponse;
import com.revealprecision.revealserver.config.InstanceContext;
import com.revealprecision.revealserver.constants.KafkaConstants;
import com.revealprecision.revealserver.enums.ApplicableReportsEnum;
import com.revealprecision.revealserver.enums.EntityStatus;
import com.revealprecision.revealserver.enums.HierarchyStatus;
import com.revealprecision.revealserver.enums.InstanceRoleEnum;
import com.revealprecision.revealserver.enums.PlanStatusEnum;
import com.revealprecision.revealserver.exceptions.ConflictException;
import com.revealprecision.revealserver.exceptions.NotFoundException;
import com.revealprecision.revealserver.exceptions.constant.Error;
import com.revealprecision.revealserver.exceptions.handler.BadRequestException;
import com.revealprecision.revealserver.messaging.message.PlanUpdateMessage;
import com.revealprecision.revealserver.messaging.message.PlanUpdateType;
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
import com.revealprecision.revealserver.persistence.domain.id.InstanceEntityTagId;
import com.revealprecision.revealserver.persistence.domain.id.InstanceLocationId;
import com.revealprecision.revealserver.persistence.domain.id.InstanceUserId;
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
import com.revealprecision.revealserver.persistence.repository.PlanAssignmentRepository;
import com.revealprecision.revealserver.persistence.repository.PlanLocationsRepository;
import com.revealprecision.revealserver.persistence.repository.UserRepository;
import com.revealprecision.revealserver.util.UserUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
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
  private final LocationBulkService locationBulkService;
  private final UserRepository userRepository;
  private final PlanLocationsRepository planLocationsRepository;
  private final PlanAssignmentRepository planAssignmentRepository;

  @Transactional
  public void create(InstanceRequest instanceRequest) {

    validateInstanceRequest(instanceRequest, null, null);

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

  private void validateInstanceRequest(InstanceRequest instanceRequest, String currentName,
      UUID identifier) {
    if (instanceRequest.getInstanceName() != null && !instanceRequest.getInstanceName()
        .equals(currentName)) {
      boolean exists;
      if (identifier != null) {
        exists = instanceRepository.existsByNameAndIdentifierNot(instanceRequest.getInstanceName(),
            identifier);
      } else {
        exists = instanceRepository.existsByName(instanceRequest.getInstanceName());
      }
      if (exists) {
        throw new IllegalArgumentException(
            String.format(Error.NON_UNIQUE, "Instance name", instanceRequest.getInstanceName()));
      }
    }
    if (instanceRequest.getMembers() == null || instanceRequest.getMembers().isEmpty()) {
      throw new IllegalArgumentException("At least one member is required");
    }
    if (instanceRequest.getAreas() == null || instanceRequest.getAreas().isEmpty()) {
      throw new IllegalArgumentException("At least one area is required");
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

  private Plan getInstancePlan(Instance instance){
    return instance.getPlans().stream().findFirst().orElseThrow(
        () -> new IllegalArgumentException("Instance plan not found"));
  }

  public InstanceResponse getInstanceResponse(UUID identifier) {
    Instance instance = findById(identifier);

    List<IdentifierNameProjection> assignedLocations = instanceLocationRepository.getAreasIdNamesByInstance(
        identifier);
    List<GeoTreeResponse> areas = getAssignedInstanceAreasTree(instance.getIdentifier());

    Plan instancePlan = getInstancePlan(instance);

    List<UUID> assignedLocationsIds = assignedLocations.stream()
        .map(IdentifierNameProjection::getIdentifier)
        .collect(Collectors.toList());

    return InstanceResponseFactory.fromEntity(instance, areas, instancePlan, assignedLocationsIds);
  }

  @Transactional
  public void update(UUID identifier, InstanceRequest instanceRequest) {

    Instance instance = findById(identifier);

    validateInstanceRequest(instanceRequest, instance.getName(), identifier);

    instance.setName(instanceRequest.getInstanceName());
    LocationHierarchy locationHierarchy = locationHierarchyService.findByIdentifier(
        instanceRequest.getLocationHierarchy());
    instance.setLocationHierarchy(locationHierarchy);

    instanceRepository.save(instance);

    Plan plan = getInstancePlan(instance);


    planService.updatePlan(instanceRequest.getPlanRequest(), plan.getIdentifier());

    // Update tags
    List<UUID> incomingTagIds =
        instanceRequest.getDatasets_tags() != null ? instanceRequest.getDatasets_tags()
            : new ArrayList<>();
    List<IdentifierNameProjection> currentTags = instanceEntityTagRepository.getDatasetsIdNamesByInstance(
        identifier);
    Set<UUID> currentTagIds = currentTags.stream().map(IdentifierNameProjection::getIdentifier)
        .collect(Collectors.toSet());

    List<InstanceEntityTagId> tagsToDelete = currentTagIds.stream()
        .filter(tagId -> !incomingTagIds.contains(tagId))
        .map(tagId -> new InstanceEntityTagId(identifier, tagId))
        .collect(Collectors.toList());

    List<UUID> tagsToAdd = incomingTagIds.stream()
        .filter(tagId -> !currentTagIds.contains(tagId))
        .collect(Collectors.toList());

    if (!tagsToDelete.isEmpty()) {
      instanceEntityTagRepository.deleteAllById(tagsToDelete);
    }

    if (!tagsToAdd.isEmpty()) {
      List<EntityTag> tags = entityTagService.findEntityTagsByIdentifierIn(tagsToAdd);
      List<InstanceEntityTag> instanceEntityTags = tags.stream().distinct().map(tag -> {
        InstanceEntityTag mapping = new InstanceEntityTag();
        mapping.populate(instance, tag);
        return mapping;
      }).collect(Collectors.toList());
      instanceEntityTagRepository.saveAll(instanceEntityTags);
    }

    // Update users
    List<UUID> incomingMemberIds = instanceRequest.getMembers();
    List<IdentifierNameProjection> currentMembers = instanceUserRepository.getInstancesUsers(
        identifier);
    Set<UUID> currentMemberIds = currentMembers.stream().map(IdentifierNameProjection::getIdentifier)
        .collect(Collectors.toSet());

    List<InstanceUserId> membersToDelete = currentMemberIds.stream()
        .filter(memberId -> !incomingMemberIds.contains(memberId))
        .map(memberId -> new InstanceUserId(identifier, memberId))
        .collect(Collectors.toList());

    List<UUID> membersToAdd = incomingMemberIds.stream()
        .filter(memberId -> !currentMemberIds.contains(memberId))
        .collect(Collectors.toList());

    if (!membersToDelete.isEmpty()) {
      instanceUserRepository.deleteAllById(membersToDelete);
    }

    if (!membersToAdd.isEmpty()) {
      List<User> users = userService.findAllById(membersToAdd);
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
    List<UUID> incomingAreaIds = instanceRequest.getAreas();
    List<UUID> currentAreaIdsList = instanceLocationRepository.findLocationIdentifiersByInstanceId(
        identifier);
    Set<UUID> currentAreaIds = new java.util.HashSet<>(currentAreaIdsList);

    List<InstanceLocationId> areasToDelete = currentAreaIds.stream()
        .filter(areaId -> !incomingAreaIds.contains(areaId))
        .map(areaId -> new InstanceLocationId(identifier, areaId))
        .collect(Collectors.toList());

    List<UUID> areasToAdd = incomingAreaIds.stream()
        .filter(areaId -> !currentAreaIds.contains(areaId))
        .collect(Collectors.toList());

    if (!areasToDelete.isEmpty()) {
      instanceLocationRepository.deleteAllById(areasToDelete);
    }

    if (!areasToAdd.isEmpty()) {
      List<Location> locations = locationService.findAllIdentifiersWithoutStructureAndGeoJSON(
          areasToAdd);
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

    Plan instancePlan = planService.findPlanByInstanceIdentifier(effectiveInstanceId).stream().findFirst().get();

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

    return InstanceContextResponseFactory.buildInstanceContextResponse(instanceUser, groups, instancePlan);
  }

  public boolean isMember(UUID userId, UUID instanceId) {
    Optional<Instance> instanceOptional = instanceUserRepository.findFirstInstanceByUserIdentifierAndInstanceIdentifier
        (userId, instanceId).stream().findFirst();
    return instanceOptional.isPresent();
  }

  public List<GeoTreeResponse> getAssignedInstanceAreasTree(List<String> nodeList) {
    return getAssignedInstanceAreasTree(null , nodeList);
  }

  public List<GeoTreeResponse> getAssignedInstanceAreasTree() {
    return getAssignedInstanceAreasTree(null , null);
  }

  public List<GeoTreeResponse> getAssignedInstanceAreasTree(UUID instanceIdentifier) {
    return getAssignedInstanceAreasTree(instanceIdentifier , null);
  }


  public List<GeoTreeResponse> getAssignedInstanceAreasTree(UUID instanceIdentifier , List<String> nodeList ) {

    List<IdentifierNameResponse> instancesAreas = null;

    Instance instance = findById(instanceIdentifier);

    if (instanceIdentifier == null) {
      instancesAreas = getAssignedInstanceAreas();
    } else {
      instancesAreas = getAssignedInstanceAreas(instanceIdentifier);
    }

    List<UUID> instancesAreasIds = instancesAreas.stream()
        .map(IdentifierNameResponse::getIdentifier)
        .collect(Collectors.toList());

    List<GeoTreeResponse> geoTreeResponses = locationRelationshipService.getFilteredGeoTreeByLocationIds(instance.getLocationHierarchy(),
        instancesAreasIds , nodeList);

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

  public LocationHierarchy getInstanceHierarchy(UUID instanceIdentifier) {
    Instance instance = findById(instanceIdentifier);
    LocationHierarchy instanceLocationHierarchy =  instance.getLocationHierarchy();
    return instanceLocationHierarchy;
  }


  public LocationHierarchyResponse getInstanceHierarchyTreeResponse(UUID instanceIdentifier) {
    if (instanceIdentifier == null) {
      // Get base hierarchy and build full geo tree
      LocationHierarchy baseHierarchy = locationHierarchyService.getBaseLocationHierarchy();
      List <GeoTreeResponse> hierarchyTree  = locationHierarchyService.getGeoTreeFromLocationHierarchy(baseHierarchy, true);

      return LocationHierarchyResponse.builder().identifier(baseHierarchy.getIdentifier().toString())
          .name(baseHierarchy.getName())
          .geoTree(hierarchyTree)
          .nodeOrder(baseHierarchy.getNodeOrder()).build();
    }
    Instance instance = findById(instanceIdentifier);

    LocationHierarchy instanceLocationHierarchy =  instance.getLocationHierarchy();

    List <GeoTreeResponse> hierarchyTree  =   getAssignedInstanceAreasTree(instanceIdentifier );
    return LocationHierarchyResponse.builder().identifier(instanceLocationHierarchy.getIdentifier().toString())
        .name(instanceLocationHierarchy.getName())
        .geoTree(hierarchyTree)
        .nodeOrder(instanceLocationHierarchy.getNodeOrder()).build();
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


  public LocationHierarchyResponse getInstanceHierarchyWithGroups(UUID instanceIdentifier) {
    if (instanceIdentifier == null) {
      // Get base hierarchy and build full geo tree
      LocationHierarchy baseHierarchy = locationHierarchyService.getBaseLocationHierarchy();
      List <GeoTreeResponse> hierarchyTree  = locationHierarchyService.getGeoTreeFromLocationHierarchy(baseHierarchy, true);

      return LocationHierarchyResponse.builder().identifier(baseHierarchy.getIdentifier().toString())
          .name(baseHierarchy.getName())
          .geoTree(hierarchyTree)
          .nodeOrder(baseHierarchy.getNodeOrder()).build();
    }
    Instance instance = findById(instanceIdentifier);

    LocationHierarchy instanceLocationHierarchy =  instance.getLocationHierarchy();

    List <GeoTreeResponse> hierarchyTree  =   getAssignedInstanceAreasTree(instanceIdentifier);
    return LocationHierarchyResponse.builder().identifier(instanceLocationHierarchy.getIdentifier().toString())
        .name(instanceLocationHierarchy.getName())
        .geoTree(hierarchyTree)
        .nodeOrder(instanceLocationHierarchy.getNodeOrder()).build();

  }

  public void activateInstancePlan(UUID instanceIdentifier) {
    Instance instance = findById(instanceIdentifier);

    Plan instancePlan = planService.findPlanByInstanceIdentifier(instanceIdentifier).stream().findFirst()
        .orElseThrow(() -> new IllegalArgumentException("Instance Plan not found"));

    if(instancePlan.getStatus() != null && instancePlan.getStatus().equals(PlanStatusEnum.ACTIVE)){
      throw new IllegalArgumentException("Instance plan is already activated");
    }


    boolean hasLocations = planLocationsRepository.countByPlan_Identifier(instancePlan.getIdentifier()) > 0;
    if (!hasLocations) {
      throw new IllegalArgumentException(
          "Cannot activate plan - no locations assigned to plan. " +
              "Please assign at least one location before activating."
      );
    }

    // Check 3 - at least one team assigned
    boolean hasTeams = planAssignmentRepository
        .existsByPlanIdentifier(instancePlan.getIdentifier());
    if (!hasTeams) {
      throw new IllegalArgumentException(
          "Cannot activate plan - no teams assigned to plan. " +
              "Please assign at least one team before activating."
      );
    }

    LocationHierarchy  locationHierarchy = instance.getLocationHierarchy();

    if(locationHierarchy.getHierarchyStatus().equals(HierarchyStatus.INACTIVE)){
      throw new IllegalArgumentException("Location hierarchy is inactive, cannot activate instance plan");
    }
    planService.activatePlan(instancePlan);
  }

  public Page<InstanceListProjection> getInstanceForReports(String reportType, Pageable pageable) {
    if (reportType.isBlank()) {
      return findInstanceByInterventionType(reportType, pageable);
    } else {
      ApplicableReportsEnum applicableReportsEnum = null;
      for (ApplicableReportsEnum applicableReport : ApplicableReportsEnum.values()) {
        if (applicableReport.getReportName().contains(reportType)) {
          applicableReportsEnum = applicableReport;
          break;
        }
      }
      return findInstanceByInterventionType(applicableReportsEnum.name(), pageable);
    }
  }

  private Page<InstanceListProjection> findInstanceByInterventionType(String interventionType, Pageable pageable) {
    return instanceRepository.findByInterventionType(interventionType, pageable);
  }
}
