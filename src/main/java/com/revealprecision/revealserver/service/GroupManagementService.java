package com.revealprecision.revealserver.service;

import com.revealprecision.revealserver.api.v1.dto.request.AssignLocationsToTeamRequest;
import com.revealprecision.revealserver.api.v1.dto.request.GlobalUserRequest;
import com.revealprecision.revealserver.api.v1.dto.request.GroupManagementRequest;
import com.revealprecision.revealserver.api.v1.dto.request.OrganizationRoleRequest;
import com.revealprecision.revealserver.api.v1.dto.response.CountResponse;
import com.revealprecision.revealserver.api.v1.dto.response.GeoTreeResponse;
import com.revealprecision.revealserver.api.v1.dto.response.GroupManagementResponse;
import com.revealprecision.revealserver.api.v1.dto.response.GroupStatsResponse;
import com.revealprecision.revealserver.api.v1.dto.response.IdentifierNameResponse;
import com.revealprecision.revealserver.api.v1.dto.response.LocationHierarchyResponse;
import com.revealprecision.revealserver.config.InstanceContext;
import com.revealprecision.revealserver.enums.EntityStatus;
import com.revealprecision.revealserver.enums.OrganizationTypeEnum;
import com.revealprecision.revealserver.enums.PlanInterventionTypeEnum;
import com.revealprecision.revealserver.exceptions.NotFoundException;
import com.revealprecision.revealserver.exceptions.handler.BadRequestException;
import com.revealprecision.revealserver.persistence.domain.EntityTag;
import com.revealprecision.revealserver.persistence.domain.EntityTagAccGrantsOrganization;
import com.revealprecision.revealserver.persistence.domain.Instance;
import com.revealprecision.revealserver.persistence.domain.InstanceRole;
import com.revealprecision.revealserver.persistence.domain.InstanceUser;
import com.revealprecision.revealserver.persistence.domain.Location;
import com.revealprecision.revealserver.persistence.domain.LocationHierarchy;
import com.revealprecision.revealserver.persistence.domain.Organization;
import com.revealprecision.revealserver.persistence.domain.OrganizationLocation;
import com.revealprecision.revealserver.persistence.domain.OrganizationRole;
import com.revealprecision.revealserver.persistence.domain.OrganizationRoleMapping;
import com.revealprecision.revealserver.persistence.domain.OrganizationRolePermission;
import com.revealprecision.revealserver.persistence.domain.Permission;
import com.revealprecision.revealserver.persistence.domain.Plan;
import com.revealprecision.revealserver.persistence.domain.PlanAssignment;
import com.revealprecision.revealserver.persistence.domain.User;
import com.revealprecision.revealserver.persistence.domain.id.InstanceUserId;
import com.revealprecision.revealserver.persistence.domain.id.OrganizationLocationId;
import com.revealprecision.revealserver.persistence.domain.id.OrganizationRoleMappingId;
import com.revealprecision.revealserver.persistence.projection.GroupManagementProjection;
import com.revealprecision.revealserver.persistence.projection.PopulationSummaryProjection;
import com.revealprecision.revealserver.persistence.repository.EntityTagAccGrantsOrganizationRepository;
import com.revealprecision.revealserver.persistence.repository.InstanceUserRepository;
import com.revealprecision.revealserver.persistence.repository.OrganizationLocationRepository;
import com.revealprecision.revealserver.persistence.repository.OrganizationRepository;
import com.revealprecision.revealserver.persistence.repository.OrganizationRoleMappingRepository;
import com.revealprecision.revealserver.persistence.repository.OrganizationRolePermissionRepository;
import com.revealprecision.revealserver.persistence.repository.OrganizationRoleRepository;
import com.revealprecision.revealserver.persistence.repository.PermissionRepository;
import com.revealprecision.revealserver.persistence.repository.PlanLocationsRepository;
import com.revealprecision.revealserver.persistence.repository.TaskRepository;
import com.revealprecision.revealserver.persistence.repository.UserRepository;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.BooleanUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GroupManagementService {

  private final EntityTagService entityTagService;
  private final UserService userService;
  private final LocationService locationService;
  private final InstanceService instanceService;
  private final OrganizationRepository organizationRepository;
  private final EntityTagAccGrantsOrganizationRepository entityTagAccGrantsOrganizationRepository;
  private final OrganizationLocationRepository organizationLocationRepository;
  private final OrganizationRoleRepository organizationRoleRepository;
  private final OrganizationRoleMappingRepository organizationRoleMappingRepository;
  private final LocationRelationshipService locationRelationshipService;
  private final UserRepository userRepository;
  private final PlanLocationsService planLocationsService ;
  private final PlanService planService;
  private final PlanAssignmentService planAssignmentService;
  private final PlanLocationsRepository planLocationsRepository;
  private final InstanceRoleService instanceRoleService;
  private final InstanceUserRepository instanceUserRepository;
  private final PermissionRepository permissionRepository;
  private final OrganizationRolePermissionRepository organizationRolePermissionRepository;
  private final TaskRepository taskRepository;

  public void createGroup(GroupManagementRequest request) {

    validateGroupRequest(request, null, null);

    UUID instanceIdentifier = InstanceContext.get();

    Instance instance = instanceService.findById(instanceIdentifier);

    Organization organization = Organization.builder()
        .name(request.getName())
        .type(OrganizationTypeEnum.GROUP)
        .active(true)
        .instance(instance)
        .build();
    organization.setEntityStatus(EntityStatus.ACTIVE);

    if(request.getIsTeam()){
      organization.setType(OrganizationTypeEnum.TEAM);
    }


    Organization savedOrg = organizationRepository.save(organization);

    List<User> users = userService.findAllById(request.getMembersIdentifiers());

    users.forEach(user -> user.getOrganizations().add(organization));

    userService.saveAll(users);

    InstanceRole standardRole = instanceRoleService.getStandardRole();

    List<InstanceUser> instanceUsers =  users.stream()
        .map(user -> {
          InstanceUser instanceUser = new InstanceUser();
          instanceUser.setRole(standardRole);
          instanceUser.populate(instance, user);
          return instanceUser;
        })
        .collect(Collectors.toList());

    instanceUserRepository.saveAll(instanceUsers);

    if(!request.getIsTeam()){
      List<EntityTag> tags = entityTagService.findEntityTagsByIdentifierIn(
          request.getDatasetsIdentifiers());

      List<EntityTagAccGrantsOrganization> entityTagAccGrantsOrganizations = tags.stream()
          .map(entityTag -> {
            EntityTagAccGrantsOrganization entityTagAccGrantsOrganization = new EntityTagAccGrantsOrganization();
            entityTagAccGrantsOrganization.setEntityTag(entityTag);
            entityTagAccGrantsOrganization.setOrganizationId(savedOrg.getIdentifier());
            return entityTagAccGrantsOrganization;
          }).collect(Collectors.toList());

      entityTagAccGrantsOrganizationRepository.saveAll(entityTagAccGrantsOrganizations);

      //Adding Areas
      List<Location> locations = locationService.findAllIdentifiersWithoutStructureAndGeoJSON(
          request.getAreasIdentifiers());

      List<OrganizationLocation> areas = locations.stream().map(location -> {

        OrganizationLocation mapping = new OrganizationLocation();
        mapping.populate(savedOrg, location);

        return mapping;
      }).collect(Collectors.toList());

      organizationLocationRepository.saveAll(areas);

      List<OrganizationRole> roles = organizationRoleRepository.findAllById(request.getRolesIdentifiers());

      List<OrganizationRoleMapping> orgRoleMapping = roles.stream().map(role -> {

        OrganizationRoleMapping mapping = new OrganizationRoleMapping();
        mapping.populate(savedOrg, role);

        return mapping;
      }).collect(Collectors.toList());

      organizationRoleMappingRepository.saveAll(orgRoleMapping);
    }
  }

  public Page<GroupManagementProjection> getGroups(UUID instanceIdentifier, Pageable pageable) {

    final UUID computedInstanceIdentifier;

    if(instanceIdentifier == null) {
      computedInstanceIdentifier = InstanceContext.get();
    }
    else {
      computedInstanceIdentifier = instanceIdentifier;
    }

    return organizationRepository.findByInstanceId(computedInstanceIdentifier, pageable);
  }

  public List<GeoTreeResponse> getUserLocations(UUID userId) {
    UUID instanceIdentifier = InstanceContext.get();

    List<UUID> userLocationsIds = organizationLocationRepository.findLocationIdentifiersByInstanceAndUser(
        instanceIdentifier, userId);

    Instance instance = instanceService.findById(instanceIdentifier);

    List<GeoTreeResponse>  geoTreeResponses = locationRelationshipService.getFilteredGeoTreeByLocationIds(instance.getLocationHierarchy(), userLocationsIds , null);
    return  geoTreeResponses;
  }

  public List<String> getUserGroups(UUID userId) {
    UUID instanceIdentifier = InstanceContext.get();
    return userRepository.findOrganizationsNamesByUserId(userId, instanceIdentifier);
  }

  public List<String> getUserDatasetTags(UUID userId) {
    UUID instanceIdentifier = InstanceContext.get();
    return entityTagAccGrantsOrganizationRepository.findDatasetsByUserIdAndInstanceId(userId, instanceIdentifier);
  }

  @Transactional
  public void assignLocations(AssignLocationsToTeamRequest assignLocationsToTeamRequest) {

    UUID instanceIdentifier = InstanceContext.get();

    List<Plan> plans =  planService.findPlanByInstanceIdentifier(instanceIdentifier);

    ///  as there is one plan only so assign location to that plan
    Plan selectedPlan = plans.get(0);

    planLocationsService.assignLocationsToTeam(selectedPlan.getIdentifier(), assignLocationsToTeamRequest);
  }

  public LocationHierarchyResponse getInstanceGroupsLocationsTree() {

    UUID instanceIdentifier = InstanceContext.get();

    List<Plan> plans =  planService.findPlanByInstanceIdentifier(instanceIdentifier);

    ///  as there is one plan only so assign a location to that plan
    Plan plan = plans.get(0);

    LocationHierarchy locationHierarchy = instanceService.getInstanceHierarchy(instanceIdentifier);

    List<GeoTreeResponse> geoTreeResponses;

    if ((plan.getInterventionType().getCode().equals(PlanInterventionTypeEnum.IRS_LITE.name())
        || plan.getInterventionType()
        .getCode()
        .equals(PlanInterventionTypeEnum.MDA_LITE.name()))) {
      int i = locationHierarchy.getNodeOrder()
          .indexOf(plan.getPlanTargetType().getGeographicLevel().getName());
      List<String> elList = locationHierarchy.getNodeOrder()
          .subList(i + 1, locationHierarchy.getNodeOrder().size());
      if (elList.isEmpty()) {
        geoTreeResponses = instanceService.getAssignedInstanceAreasTree(instanceIdentifier);
      } else {
        geoTreeResponses = instanceService.getAssignedInstanceAreasTree(instanceIdentifier, elList);
      }
    } else {
      geoTreeResponses = instanceService.getAssignedInstanceAreasTree(instanceIdentifier);
    }

    Set<Location> locations = planLocationsRepository.findLocationsByPlan_Identifier(
        plan.getIdentifier());

    Map<UUID, Location> locationMap = locations.stream()
        .collect(Collectors.toMap(Location::getIdentifier, location -> location));

    List<PlanAssignment> planAssignments = planAssignmentService.getPlanAssignmentsByPlanIdentifier(plan.getIdentifier());

    Map<UUID, List<PlanAssignment>> planAssignmentMap = planAssignments.stream()
        .collect(Collectors.groupingBy(
            planAssignment -> planAssignment.getPlanLocations().getLocation().getIdentifier()));
    geoTreeResponses.forEach(el -> planLocationsService.assignLocations(locationMap, el, planAssignmentMap));

    return LocationHierarchyResponse.builder().identifier(locationHierarchy.getIdentifier().toString())
        .name(locationHierarchy.getName())
        .geoTree(geoTreeResponses)
        .nodeOrder(locationHierarchy.getNodeOrder()).build();
  }

  public GroupManagementResponse getGroupById(UUID identifier) {
    Organization org = findById(identifier);

    // members
    List<IdentifierNameResponse> members = org.getUsers().stream()
        .map(user -> IdentifierNameResponse.builder()
            .identifier(user.getIdentifier())
            .name(user.getUsername())
            .build())
        .collect(Collectors.toList());

    if(org.getType().equals(OrganizationTypeEnum.TEAM)){
      return GroupManagementResponse.builder()
          .identifier(org.getIdentifier())
          .name(org.getName())
          .type(org.getType().name())
          .active(org.isActive())
          .members(members)
          .build();
    }

    // datasets
    List<EntityTagAccGrantsOrganization> entityTags = entityTagAccGrantsOrganizationRepository
        .findByOrganizationId(identifier);

    List<IdentifierNameResponse> datasets = entityTags.stream()
        .map(et -> IdentifierNameResponse.builder()
            .identifier(et.getEntityTag().getIdentifier())
            .name(et.getEntityTag().getTag())
            .build())
        .collect(Collectors.toList());

    // roles
    List<IdentifierNameResponse> roles = organizationRoleRepository
        .findByOrganizationId(identifier).stream()
        .map(role -> IdentifierNameResponse.builder()
            .identifier(role.getIdentifier())
            .name(role.getName())
            .build())
        .collect(Collectors.toList());


    List<OrganizationLocation> orgLocations = organizationLocationRepository
        .findByOrganizationIdentifier(identifier);

    Set<UUID> groupLocationIds = orgLocations.stream()
        .map(ol -> ol.getLocation().getIdentifier())
        .collect(Collectors.toSet());

    List<GeoTreeResponse> areas = instanceService.getAssignedInstanceAreasTree();
    markSelectedAreas(areas, groupLocationIds);

    return GroupManagementResponse.builder()
        .identifier(org.getIdentifier())
        .name(org.getName())
        .type(org.getType().name())
        .active(org.isActive())
        .members(members)
        .datasets(datasets)
        .roles(roles)
        .areas(areas)
        .build();
  }


  private void validateGroupRequest(GroupManagementRequest request, String currentName,
      UUID identifier) {

    UUID instanceIdentifier = InstanceContext.get();

    boolean exists = false;
    if (identifier == null) {
      exists = organizationRepository.existsByNameAndInstance_Identifier(request.getName(),
          instanceIdentifier);
    } else if (currentName == null || !currentName.equals(request.getName())) {
      exists = organizationRepository.existsByNameAndInstance_IdentifierAndIdentifierNot(
          request.getName(), instanceIdentifier, identifier);
    }

    if (exists) {
      throw new IllegalArgumentException("Group/Team with the same name already exists");
    }

    if (request.getMembersIdentifiers() == null || request.getMembersIdentifiers().isEmpty()) {
      throw new IllegalArgumentException("At least one member is required");
    }

    if (!BooleanUtils.isTrue(request.getIsTeam())) {
      if (request.getAreasIdentifiers() == null || request.getAreasIdentifiers().isEmpty()) {
        throw new IllegalArgumentException("At least one area is required");
      }
      if (request.getRolesIdentifiers() == null || request.getRolesIdentifiers().isEmpty()) {
      }
    }
  }

  private void markSelectedAreas(List<GeoTreeResponse> areas, Set<UUID> selectedIds) {
    if (areas == null) return;
    areas.forEach(area -> {
      area.setSelected(selectedIds.contains(area.getIdentifier()));
      markSelectedAreas(area.getChildren(), selectedIds);
    });
  }


  @Transactional
  public void updateGroup(UUID identifier, GroupManagementRequest request) {

    Organization org = findById(identifier);
    validateGroupRequest(request, org.getName(), identifier);

    UUID instanceIdentifier = InstanceContext.get();
    Instance instance = instanceService.findById(instanceIdentifier);

    // Update basic fields
    org.setName(request.getName());
    if (request.getIsTeam() != null) {
      org.setType(request.getIsTeam() ? OrganizationTypeEnum.TEAM : OrganizationTypeEnum.GROUP);
    }
    organizationRepository.save(org);

    // Update members
    List<UUID> incomingMemberIds = request.getMembersIdentifiers();
    List<User> currentUsers = userRepository.findByOrganizationId(identifier);
    Set<UUID> currentMemberIds = currentUsers.stream().map(User::getIdentifier)
        .collect(Collectors.toSet());

    List<User> membersToRemove = currentUsers.stream()
        .filter(user -> !incomingMemberIds.contains(user.getIdentifier()))
        .collect(Collectors.toList());

    List<UUID> membersToAddIds = incomingMemberIds.stream()
        .filter(memberId -> !currentMemberIds.contains(memberId))
        .collect(Collectors.toList());

    if (!membersToRemove.isEmpty()) {
      membersToRemove.forEach(user -> user.getOrganizations().remove(org));
      userService.saveAll(membersToRemove);

      List<InstanceUserId> instanceUserIdsToRemove = membersToRemove.stream()
          .map(user -> new InstanceUserId(instanceIdentifier, user.getIdentifier()))
          .collect(Collectors.toList());
      instanceUserRepository.deleteAllById(instanceUserIdsToRemove);
    }

    if (!membersToAddIds.isEmpty()) {
      List<User> membersToAdd = userService.findAllById(membersToAddIds);
      membersToAdd.forEach(user -> user.getOrganizations().add(org));
      userService.saveAll(membersToAdd);

      InstanceRole standardRole = instanceRoleService.getStandardRole();
      List<InstanceUser> instanceUsersToAdd = membersToAdd.stream()
          .map(user -> {
            InstanceUser instanceUser = new InstanceUser();
            instanceUser.setRole(standardRole);
            instanceUser.populate(instance, user);
            return instanceUser;
          }).collect(Collectors.toList());
      instanceUserRepository.saveAll(instanceUsersToAdd);
    }

    if (!BooleanUtils.isTrue(request.getIsTeam())) {

      // Update datasets
      List<UUID> incomingDatasetIds =
          request.getDatasetsIdentifiers() != null ? request.getDatasetsIdentifiers()
              : new java.util.ArrayList<>();
      List<EntityTagAccGrantsOrganization> currentDatasets = entityTagAccGrantsOrganizationRepository.findByOrganizationId(
          identifier);
      Set<UUID> currentDatasetTagIds = currentDatasets.stream()
          .map(mapping -> mapping.getEntityTag().getIdentifier()).collect(Collectors.toSet());

      List<EntityTagAccGrantsOrganization> datasetsToRemove = currentDatasets.stream()
          .filter(mapping -> !incomingDatasetIds.contains(mapping.getEntityTag().getIdentifier()))
          .collect(Collectors.toList());

      List<UUID> datasetsToAddIds = incomingDatasetIds.stream()
          .filter(tagId -> !currentDatasetTagIds.contains(tagId))
          .collect(Collectors.toList());

      if (!datasetsToRemove.isEmpty()) {
        entityTagAccGrantsOrganizationRepository.deleteAll(datasetsToRemove);
      }

      if (!datasetsToAddIds.isEmpty()) {
        List<EntityTag> tags = entityTagService.findEntityTagsByIdentifierIn(datasetsToAddIds);
        List<EntityTagAccGrantsOrganization> datasetsToAdd = tags.stream()
            .map(entityTag -> {
              EntityTagAccGrantsOrganization mapping = new EntityTagAccGrantsOrganization();
              mapping.setEntityTag(entityTag);
              mapping.setOrganizationId(identifier);
              return mapping;
            }).collect(Collectors.toList());
        entityTagAccGrantsOrganizationRepository.saveAll(datasetsToAdd);
      }

      // Update locations
      List<UUID> incomingAreaIds = request.getAreasIdentifiers();
      List<OrganizationLocation> currentLocations = organizationLocationRepository.findByOrganizationIdentifier(
          identifier);
      Set<UUID> currentAreaIds = currentLocations.stream()
          .map(mapping -> mapping.getLocation().getIdentifier()).collect(Collectors.toSet());

      List<OrganizationLocationId> areasToRemoveIds = currentAreaIds.stream()
          .filter(areaId -> !incomingAreaIds.contains(areaId))
          .map(areaId -> new OrganizationLocationId(identifier, areaId))
          .collect(Collectors.toList());

      List<UUID> areasToAddIds = incomingAreaIds.stream()
          .filter(areaId -> !currentAreaIds.contains(areaId))
          .collect(Collectors.toList());

      if (!areasToRemoveIds.isEmpty()) {
        organizationLocationRepository.deleteAllById(areasToRemoveIds);
      }

      if (!areasToAddIds.isEmpty()) {
        List<Location> locations = locationService.findAllIdentifiersWithoutStructureAndGeoJSON(
            areasToAddIds);
        List<OrganizationLocation> areasToAdd = locations.stream()
            .map(location -> {
              OrganizationLocation mapping = new OrganizationLocation();
              mapping.populate(org, location);
              return mapping;
            }).collect(Collectors.toList());
        organizationLocationRepository.saveAll(areasToAdd);
      }

      // Update roles
      List<UUID> incomingRoleIds = request.getRolesIdentifiers();
      List<OrganizationRoleMapping> currentRoleMappings = organizationRoleMappingRepository.findAll()
          .stream()
          .filter(mapping -> mapping.getOrganization().getIdentifier().equals(identifier))
          .collect(Collectors.toList());
      Set<UUID> currentRoleIds = currentRoleMappings.stream()
          .map(mapping -> mapping.getOrganizationRole().getIdentifier()).collect(Collectors.toSet());

      List<OrganizationRoleMappingId> rolesToRemoveIds = currentRoleIds.stream()
          .filter(roleId -> !incomingRoleIds.contains(roleId))
          .map(roleId -> new OrganizationRoleMappingId(roleId, identifier))
          .collect(Collectors.toList());

      List<UUID> rolesToAddIds = incomingRoleIds.stream()
          .filter(roleId -> !currentRoleIds.contains(roleId))
          .collect(Collectors.toList());

      if (!rolesToRemoveIds.isEmpty()) {
        organizationRoleMappingRepository.deleteAllById(rolesToRemoveIds);
      }

      if (!rolesToAddIds.isEmpty()) {
        List<OrganizationRole> rolesToAdd = organizationRoleRepository.findAllById(rolesToAddIds);
        List<OrganizationRoleMapping> mappingsToAdd = rolesToAdd.stream()
            .map(role -> {
              OrganizationRoleMapping mapping = new OrganizationRoleMapping();
              mapping.populate(org, role);
              return mapping;
            }).collect(Collectors.toList());
        organizationRoleMappingRepository.saveAll(mappingsToAdd);
      }
    }
  }

  public List<IdentifierNameResponse> getGroupRoles() {
    return organizationRoleRepository.findAll().stream()
        .map(role -> IdentifierNameResponse.builder()
            .identifier(role.getIdentifier())
            .name(role.getName())
            .build())
        .collect(Collectors.toList());
  }

  public IdentifierNameResponse createGroupRole(OrganizationRoleRequest request) {
    OrganizationRole role = OrganizationRole.builder()
        .name(request.getName())
        .build();
    OrganizationRole savedRole = organizationRoleRepository.save(role);

    if (request.getPermissionIdentifiers() != null && !request.getPermissionIdentifiers().isEmpty()) {
      List<Permission> permissions = permissionRepository.findAllById(request.getPermissionIdentifiers());
      List<OrganizationRolePermission> rolePermissions = permissions.stream()
          .map(permission -> {
            OrganizationRolePermission mapping = new OrganizationRolePermission();
            mapping.populate(savedRole, permission);
            return mapping;
          }).collect(Collectors.toList());
      organizationRolePermissionRepository.saveAll(rolePermissions);
    }

    return IdentifierNameResponse.builder()
        .identifier(savedRole.getIdentifier())
        .name(savedRole.getName())
        .build();
  }

  public IdentifierNameResponse updateGroupRole(UUID identifier,
            OrganizationRoleRequest request) {
    OrganizationRole role = organizationRoleRepository.findById(identifier)
        .orElseThrow(() -> new NotFoundException("Role not found: " + identifier));

    role.setName(request.getName());
    organizationRoleRepository.save(role);

    // Update permissions
    organizationRolePermissionRepository.deleteByOrganizationRoleIdentifier(identifier);
    if (request.getPermissionIdentifiers() != null && !request.getPermissionIdentifiers().isEmpty()) {
      List<Permission> permissions = permissionRepository.findAllById(request.getPermissionIdentifiers());
      List<OrganizationRolePermission> rolePermissions = permissions.stream()
          .map(permission -> {
            OrganizationRolePermission mapping = new OrganizationRolePermission();
            mapping.populate(role, permission);
            return mapping;
          }).collect(Collectors.toList());
      organizationRolePermissionRepository.saveAll(rolePermissions);
    }

    return IdentifierNameResponse.builder()
        .identifier(role.getIdentifier())
        .name(role.getName())
        .build();
  }

  public void deleteGroupRole(UUID identifier) {
    OrganizationRole role = organizationRoleRepository.findById(identifier)
        .orElseThrow(() -> new NotFoundException("Role not found: " + identifier));
    organizationRoleRepository.delete(role);
  }

  @Transactional
  public void addUser(GlobalUserRequest request) {
    User user = userService.createGlobalUser(request);

    if (request.getInstanceIdentifier() == null) {
      throw new IllegalArgumentException("Instance identifier not provided");
    }

    if (request.getGroupIdentifier() == null ) {
      throw new IllegalArgumentException("Group identifier not provided");
    }

    Instance instance = instanceService.findById(request.getInstanceIdentifier());

    final InstanceRole instanceRole;
    if (BooleanUtils.isTrue(request.getIsInstanceAdmin())){
      instanceRole = instanceRoleService.getInstanceAdminRole();
    } else {
      instanceRole = instanceRoleService.getStandardRole();
    }

    InstanceUser instanceUser = new InstanceUser();
    instanceUser.populate(instance, user);
    instanceUser.setRole(instanceRole);

    instanceUserRepository.save(instanceUser);

    Organization org = findById(
        request.getGroupIdentifier());


    if(user.getOrganizations() == null){
      user.setOrganizations(new HashSet<>());
    }

    user.getOrganizations().add(org);
    userService.saveAll(List.of(user));
  }

  private Organization findById(UUID request) {
    return organizationRepository.findById(request)
        .orElseThrow(() -> new NotFoundException("Group not found: " + request));
  }


  public List<GroupManagementProjection> getGroupsTeams() {
    UUID instanceIdentifier = InstanceContext.get();
    return organizationRepository.getGroupsByTypeEquals(instanceIdentifier, OrganizationTypeEnum.TEAM);
  }

  public CountResponse getGroupsCount() {
    UUID instanceIdentifier = InstanceContext.get();
    long count =  organizationRepository.getCountByTypeEquals(instanceIdentifier, OrganizationTypeEnum.TEAM);
    return  new CountResponse(count);
  }

  private Plan getInstancePlan(Instance instance){
    return instance.getPlans().stream().findFirst().orElseThrow(
                    () -> new IllegalArgumentException("Instance plan not found"));
  }

  public GroupStatsResponse getGroupsStats() {

    UUID instanceIdentifier = InstanceContext.get();
    Instance instance = instanceService.findById(instanceIdentifier);

    Plan plan = getInstancePlan(instance);

    Long targetAreas = planLocationsRepository.countByPlan_Identifier(plan.getIdentifier());

    PopulationSummaryProjection populationStats = planLocationsRepository
        .getLeafLocationPopulationByPlanId(plan.getIdentifier());

    // 2. Total structures from materialized view
    Long totalStructures = planLocationsRepository.sumStructuresByPlanId(plan.getIdentifier());

    Long totalTaskLocations = taskRepository
        .countTotalTaskLocationsByPlanId(plan.getIdentifier());
    Long completedTaskLocations = taskRepository
        .countCompletedTaskLocationsByPlanId(plan.getIdentifier());

    Double completionPercentage = 0.0;
    if (totalTaskLocations != null && totalTaskLocations > 0) {
      completionPercentage = Math.round(
          (completedTaskLocations.doubleValue() / totalTaskLocations.doubleValue())
              * 100 * 100.0) / 100.0;
    }

    return GroupStatsResponse.builder()
        .targetAreas(targetAreas != null ? targetAreas : 0L)
        .totalStructures(totalStructures != null ? totalStructures : 0L)
        .totalPopulation(populationStats.getTotalPopulation())
        .completionPercentage(completionPercentage)
        .build();
  }
}
