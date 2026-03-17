package com.revealprecision.revealserver.service;

import com.revealprecision.revealserver.api.v1.dto.factory.LocationHierarchyResponseFactory;
import com.revealprecision.revealserver.api.v1.dto.request.AssignLocationsToTeamRequest;
import com.revealprecision.revealserver.api.v1.dto.request.GroupManagementRequest;
import com.revealprecision.revealserver.api.v1.dto.response.GeoTreeResponse;
import com.revealprecision.revealserver.api.v1.dto.response.GroupManagementResponse;
import com.revealprecision.revealserver.api.v1.dto.response.IdentifierNameResponse;
import com.revealprecision.revealserver.config.InstanceContext;
import com.revealprecision.revealserver.enums.EntityStatus;
import com.revealprecision.revealserver.enums.OrganizationTypeEnum;
import com.revealprecision.revealserver.exceptions.NotFoundException;
import com.revealprecision.revealserver.persistence.domain.EntityTag;
import com.revealprecision.revealserver.persistence.domain.EntityTagAccGrantsOrganization;
import com.revealprecision.revealserver.persistence.domain.Instance;
import com.revealprecision.revealserver.persistence.domain.Location;
import com.revealprecision.revealserver.persistence.domain.Organization;
import com.revealprecision.revealserver.persistence.domain.OrganizationLocation;
import com.revealprecision.revealserver.persistence.domain.OrganizationRole;
import com.revealprecision.revealserver.persistence.domain.OrganizationRoleMapping;
import com.revealprecision.revealserver.persistence.domain.Plan;
import com.revealprecision.revealserver.persistence.domain.PlanAssignment;
import com.revealprecision.revealserver.persistence.domain.User;
import com.revealprecision.revealserver.persistence.projection.GroupManagementProjection;
import com.revealprecision.revealserver.persistence.repository.EntityTagAccGrantsOrganizationRepository;
import com.revealprecision.revealserver.persistence.repository.InstanceRepository;
import com.revealprecision.revealserver.persistence.repository.OrganizationLocationRepository;
import com.revealprecision.revealserver.persistence.repository.OrganizationRepository;
import com.revealprecision.revealserver.persistence.repository.OrganizationRoleMappingRepository;
import com.revealprecision.revealserver.persistence.repository.OrganizationRoleRepository;
import com.revealprecision.revealserver.persistence.repository.PlanLocationsRepository;
import com.revealprecision.revealserver.persistence.repository.UserRepository;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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

    List<GeoTreeResponse>  geoTreeResponses = locationRelationshipService.getFilteredGeoTreeByLocationIds(userLocationsIds);
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

  public List<GeoTreeResponse> getInstanceGroupsLocations() {

    UUID instanceIdentifier = InstanceContext.get();

    List<Plan> plans =  planService.findPlanByInstanceIdentifier(instanceIdentifier);

    ///  as there is one plan only so assign a location to that plan
    Plan selectedPlan = plans.get(0);

    List<GeoTreeResponse> geoTreeResponses =  instanceService.getAssignedInstanceAreasTree();

    Set<Location> locations = planLocationsRepository.findLocationsByPlan_Identifier(
        selectedPlan.getIdentifier());

    Map<UUID, Location> locationMap = locations.stream()
        .collect(Collectors.toMap(Location::getIdentifier, location -> location));

    List<PlanAssignment> planAssignments = planAssignmentService.getPlanAssignmentsByPlanIdentifier(selectedPlan.getIdentifier());

    Map<UUID, List<PlanAssignment>> planAssignmentMap = planAssignments.stream()
        .collect(Collectors.groupingBy(
            planAssignment -> planAssignment.getPlanLocations().getLocation().getIdentifier()));
    geoTreeResponses.forEach(el -> planLocationsService.assignLocations(locationMap, el, planAssignmentMap));
    return geoTreeResponses;
  }

  public GroupManagementResponse getGroupById(UUID identifier) {
    Organization org = organizationRepository.findById(identifier)
        .orElseThrow(() -> new NotFoundException("Group not found: " + identifier));

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
    Organization org = organizationRepository.findById(identifier)
        .orElseThrow(() -> new NotFoundException("Group not found: " + identifier));

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

    List<User> newUsers = userService.findAllById(request.getMembersIdentifiers());
    newUsers.forEach(user -> user.getOrganizations().add(org));
    userService.saveAll(newUsers);

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
}
