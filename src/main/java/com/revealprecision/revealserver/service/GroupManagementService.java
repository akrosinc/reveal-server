package com.revealprecision.revealserver.service;

import com.revealprecision.revealserver.api.v1.dto.request.GroupManagementRequest;
import com.revealprecision.revealserver.api.v1.dto.response.GeoTreeResponse;
import com.revealprecision.revealserver.config.InstanceContext;
import com.revealprecision.revealserver.enums.EntityStatus;
import com.revealprecision.revealserver.enums.OrganizationTypeEnum;
import com.revealprecision.revealserver.persistence.domain.EntityTag;
import com.revealprecision.revealserver.persistence.domain.EntityTagAccGrantsOrganization;
import com.revealprecision.revealserver.persistence.domain.Instance;
import com.revealprecision.revealserver.persistence.domain.Location;
import com.revealprecision.revealserver.persistence.domain.Organization;
import com.revealprecision.revealserver.persistence.domain.OrganizationLocation;
import com.revealprecision.revealserver.persistence.domain.OrganizationRole;
import com.revealprecision.revealserver.persistence.domain.OrganizationRoleMapping;
import com.revealprecision.revealserver.persistence.domain.User;
import com.revealprecision.revealserver.persistence.projection.GroupManagementProjection;
import com.revealprecision.revealserver.persistence.repository.EntityTagAccGrantsOrganizationRepository;
import com.revealprecision.revealserver.persistence.repository.InstanceRepository;
import com.revealprecision.revealserver.persistence.repository.OrganizationLocationRepository;
import com.revealprecision.revealserver.persistence.repository.OrganizationRepository;
import com.revealprecision.revealserver.persistence.repository.OrganizationRoleMappingRepository;
import com.revealprecision.revealserver.persistence.repository.OrganizationRoleRepository;
import com.revealprecision.revealserver.persistence.repository.UserRepository;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GroupManagementService {

  private final EntityTagService entityTagService;
  private final UserService userService;
  private final LocationService locationService;
  private final InstanceRepository instanceRepository;
  private final OrganizationRepository organizationRepository;
  private final EntityTagAccGrantsOrganizationRepository entityTagAccGrantsOrganizationRepository;
  private final OrganizationLocationRepository organizationLocationRepository;
  private final OrganizationRoleRepository organizationRoleRepository;
  private final OrganizationRoleMappingRepository organizationRoleMappingRepository;
  private final LocationRelationshipService locationRelationshipService;
  private final UserRepository userRepository;

  public void createGroup(GroupManagementRequest request) {

    UUID instanceIdentifier = InstanceContext.get();

    Instance instance =
        instanceRepository.findById(instanceIdentifier)
            .orElseThrow(() -> new IllegalArgumentException("Instance not found"));

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
}
