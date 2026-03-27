package com.revealprecision.revealserver.service;

import com.revealprecision.revealserver.api.v1.dto.request.AssignLocationsToTeamRequest;
import com.revealprecision.revealserver.api.v1.dto.request.GlobalUserRequest;
import com.revealprecision.revealserver.api.v1.dto.request.GroupManagementRequest;
import com.revealprecision.revealserver.api.v1.dto.request.OrganizationRoleRequest;
import com.revealprecision.revealserver.api.v1.dto.response.CountResponse;
import com.revealprecision.revealserver.api.v1.dto.response.GeoTreeResponse;
import com.revealprecision.revealserver.api.v1.dto.response.GroupManagementResponse;
import com.revealprecision.revealserver.api.v1.dto.response.IdentifierNameResponse;
import com.revealprecision.revealserver.api.v1.dto.response.LocationHierarchyResponse;
import com.revealprecision.revealserver.config.InstanceContext;
import com.revealprecision.revealserver.enums.EntityStatus;
import com.revealprecision.revealserver.enums.OrganizationTypeEnum;
import com.revealprecision.revealserver.enums.PlanInterventionTypeEnum;
import com.revealprecision.revealserver.exceptions.NotFoundException;
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
import com.revealprecision.revealserver.persistence.projection.GroupManagementProjection;
import com.revealprecision.revealserver.persistence.repository.EntityTagAccGrantsOrganizationRepository;
import com.revealprecision.revealserver.persistence.repository.InstanceUserRepository;
import com.revealprecision.revealserver.persistence.repository.OrganizationLocationRepository;
import com.revealprecision.revealserver.persistence.repository.OrganizationRepository;
import com.revealprecision.revealserver.persistence.repository.OrganizationRoleMappingRepository;
import com.revealprecision.revealserver.persistence.repository.OrganizationRolePermissionRepository;
import com.revealprecision.revealserver.persistence.repository.OrganizationRoleRepository;
import com.revealprecision.revealserver.persistence.repository.PermissionRepository;
import com.revealprecision.revealserver.persistence.repository.PlanLocationsRepository;
import com.revealprecision.revealserver.persistence.repository.UserRepository;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.BooleanUtils;
import org.checkerframework.checker.nullness.qual.NonNull;
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

  public void createGroup(GroupManagementRequest request) {

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

  public Page<GroupManagementProjection> getGroups(Pageable pageable) {
    UUID instanceIdentifier = InstanceContext.get();
    return organizationRepository.findByInstanceId(instanceIdentifier, pageable);
  }

  public List<GeoTreeResponse> getUserLocations(UUID userId) {
    UUID instanceIdentifier = InstanceContext.get();

    List<UUID> userLocationsIds = organizationLocationRepository.findLocationIdentifiersByInstanceAndUser(
        instanceIdentifier, userId);

    List<GeoTreeResponse>  geoTreeResponses = locationRelationshipService.getFilteredGeoTreeByLocationIds(userLocationsIds , null);
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


  private void markSelectedAreas(List<GeoTreeResponse> areas, Set<UUID> selectedIds) {
    if (areas == null) return;
    areas.forEach(area -> {
      area.setSelected(selectedIds.contains(area.getIdentifier()));
      markSelectedAreas(area.getChildren(), selectedIds);
    });
  }


  @Transactional
  public void updateGroup(UUID identifier, GroupManagementRequest request) {

    UUID instanceIdentifier = InstanceContext.get();
    Instance instance = instanceService.findById(instanceIdentifier);

    Organization org = findById(identifier);

    // Update basic fields
    org.setName(request.getName());
    if (request.getIsTeam() != null) {
      org.setType(request.getIsTeam() ? OrganizationTypeEnum.TEAM : OrganizationTypeEnum.GROUP);
    }
    organizationRepository.save(org);

    // Update members
    List<User> currentUsers = userRepository.findByOrganizationId(identifier);
    currentUsers.forEach(user -> user.getOrganizations().remove(org));
    userService.saveAll(currentUsers);

    // Remove instance users for current members
    List<UUID> currentUserIds = currentUsers.stream()
        .map(User::getIdentifier)
        .collect(Collectors.toList());
    instanceUserRepository.deleteByUserIdsAndInstanceId(currentUserIds, instanceIdentifier);

    // Add new members
    List<User> newUsers = userService.findAllById(request.getMembersIdentifiers());
    newUsers.forEach(user -> user.getOrganizations().add(org));
    userService.saveAll(newUsers);

    // Add instance users for new members
    InstanceRole standardRole = instanceRoleService.getStandardRole();
    List<InstanceUser> instanceUsers = newUsers.stream()
        .map(user -> {
          InstanceUser instanceUser = new InstanceUser();
          instanceUser.setRole(standardRole);
          instanceUser.populate(instance, user);
          return instanceUser;
        }).collect(Collectors.toList());
    instanceUserRepository.saveAll(instanceUsers);

    if (!request.getIsTeam()) {

      // Update datasets
      entityTagAccGrantsOrganizationRepository.deleteByOrganizationId(identifier);
      List<EntityTag> tags = entityTagService.findEntityTagsByIdentifierIn(
          request.getDatasetsIdentifiers());
      List<EntityTagAccGrantsOrganization> entityTagAccGrantsOrganizations = tags.stream()
          .map(entityTag -> {
            EntityTagAccGrantsOrganization mapping = new EntityTagAccGrantsOrganization();
            mapping.setEntityTag(entityTag);
            mapping.setOrganizationId(identifier);
            return mapping;
          }).collect(Collectors.toList());
      entityTagAccGrantsOrganizationRepository.saveAll(entityTagAccGrantsOrganizations);

      // Update locations
      organizationLocationRepository.deleteByOrganizationIdentifier(identifier);
      List<Location> locations = locationService.findAllIdentifiersWithoutStructureAndGeoJSON(
          request.getAreasIdentifiers());
      List<OrganizationLocation> areas = locations.stream()
          .map(location -> {
            OrganizationLocation mapping = new OrganizationLocation();
            mapping.populate(org, location);
            return mapping;
          }).collect(Collectors.toList());
      organizationLocationRepository.saveAll(areas);

      // Update roles
      organizationRoleMappingRepository.deleteByOrganizationIdentifier(identifier);
      List<OrganizationRole> roles = organizationRoleRepository.findAllById(
          request.getRolesIdentifiers());
      List<OrganizationRoleMapping> orgRoleMappings = roles.stream()
          .map(role -> {
            OrganizationRoleMapping mapping = new OrganizationRoleMapping();
            mapping.populate(org, role);
            return mapping;
          }).collect(Collectors.toList());
      organizationRoleMappingRepository.saveAll(orgRoleMappings);
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
}
