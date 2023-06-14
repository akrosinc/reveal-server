package com.revealprecision.revealserver.service;

import com.revealprecision.revealserver.api.v1.dto.factory.OrganizationResponseFactory;
import com.revealprecision.revealserver.api.v1.dto.request.AssignLocationsToTeamRequest;
import com.revealprecision.revealserver.api.v1.dto.response.GeoTreeResponse;
import com.revealprecision.revealserver.api.v1.dto.response.OrganizationResponse;
import com.revealprecision.revealserver.constants.KafkaConstants;
import com.revealprecision.revealserver.constants.LocationConstants;
import com.revealprecision.revealserver.enums.PlanInterventionTypeEnum;
import com.revealprecision.revealserver.exceptions.NotFoundException;
import com.revealprecision.revealserver.messaging.message.PlanLocationAssignMessage;
import com.revealprecision.revealserver.persistence.domain.Location;
import com.revealprecision.revealserver.persistence.domain.LocationHierarchy;
import com.revealprecision.revealserver.persistence.domain.Organization;
import com.revealprecision.revealserver.persistence.domain.Plan;
import com.revealprecision.revealserver.persistence.domain.PlanAssignment;
import com.revealprecision.revealserver.persistence.domain.PlanLocations;
import com.revealprecision.revealserver.persistence.projection.LocationChildrenCountProjection;
import com.revealprecision.revealserver.persistence.projection.PlanLocationDetails;
import com.revealprecision.revealserver.persistence.projection.PlanLocationsAssigned;
import com.revealprecision.revealserver.persistence.repository.LocationRepository;
import com.revealprecision.revealserver.persistence.repository.PlanAssignmentRepository;
import com.revealprecision.revealserver.persistence.repository.PlanLocationsRepository;
import com.revealprecision.revealserver.props.KafkaProperties;
import com.revealprecision.revealserver.util.UserUtils;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Service
@Slf4j
public class PlanLocationsService {

  private final PlanLocationsRepository planLocationsRepository;
  private final PlanService planService;
  private final LocationService locationService;
  private final LocationHierarchyService locationHierarchyService;
  private final PlanAssignmentService planAssignmentService;
  private final PlanAssignmentRepository planAssignmentRepository;
  private final PublisherService publisherService;
  private final KafkaProperties kafkaProperties;
  private final OrganizationService organizationService;
  private final LocationRelationshipService locationRelationshipService;
  private final LocationRepository locationRepository;

  @Autowired
  public PlanLocationsService(PlanLocationsRepository planLocationsRepository,
      @Lazy PlanService planService, LocationService locationService,
      LocationHierarchyService locationHierarchyService,
      @Lazy PlanAssignmentService planAssignmentService,
      PublisherService publisherService,
      KafkaProperties kafkaProperties,
      OrganizationService organizationService,
      PlanAssignmentRepository planAssignmentRepository,
      LocationRelationshipService locationRelationshipService,
      LocationRepository locationRepository){
    this.planLocationsRepository = planLocationsRepository;
    this.planService = planService;
    this.locationService = locationService;
    this.locationHierarchyService = locationHierarchyService;
    this.planAssignmentService = planAssignmentService;
    this.publisherService = publisherService;
    this.kafkaProperties = kafkaProperties;
    this.organizationService = organizationService;
    this.planAssignmentRepository = planAssignmentRepository;
    this.locationRelationshipService = locationRelationshipService;
    this.locationRepository = locationRepository;
  }


  public Long getAssignedChildrenOfLocationBelow(UUID planIdentifier, UUID locationIdentifier,
      UUID locationHierarchyIdentifier) {
    return planLocationsRepository.getAssignedChildrenOfLocationBelow(locationIdentifier,
        locationHierarchyIdentifier, planIdentifier);
  }


  public PlanLocations getPlanLocationByPlanIdentifierAndLocationIdentifier(UUID planId,
      UUID locationId) {
    return planLocationsRepository.findByPlan_IdentifierAndLocation_Identifier(planId, locationId);
  }

  public List<PlanLocations> getByPlanIdAndLocationIdentifiers(UUID planIdentifier,
      List<UUID> locationIdentifiers) {
    return planLocationsRepository.findByPlan_IdentifierAndLocation_IdentifierIn(planIdentifier,
        locationIdentifiers);
  }

  public void assignLocation(UUID planIdentifier, UUID locationIdentifier) {
    Plan plan = planService.findPlanByIdentifier(planIdentifier);

    List<UUID> locationsToAdd;

    if (!plan.getInterventionType().getCode().equals(PlanInterventionTypeEnum.IRS_LITE.name())
        && !plan.getInterventionType()
        .getCode()
        .equals(PlanInterventionTypeEnum.MDA_LITE.name())) {
      locationsToAdd = locationService.getAllLocationChildren(locationIdentifier,
          plan.getLocationHierarchy().getIdentifier());
    } else {
      if (plan.getPlanTargetType() == null) {
        locationsToAdd = locationService.getAllLocationChildrenNotLike(locationIdentifier,
            plan.getLocationHierarchy().getIdentifier(),
            new ArrayList<>(Collections.singletonList(LocationConstants.OPERATIONAL)));
      } else {
        LocationHierarchy locationHierarchy = plan.getLocationHierarchy();
        int i = locationHierarchy.getNodeOrder()
            .indexOf(plan.getPlanTargetType().getGeographicLevel().getName());
        List<String> elList = locationHierarchy.getNodeOrder()
            .subList(i + 1, locationHierarchy.getNodeOrder().size());
        locationsToAdd = locationService.getAllLocationChildrenNotLike(locationIdentifier,
            plan.getLocationHierarchy().getIdentifier(), elList);
      }
    }

    locationsToAdd.add(locationIdentifier);
    List<PlanLocations> addPlanLocations = locationsToAdd.stream()
        .map(loc -> new PlanLocations(plan, loc))
        .collect(Collectors.toList());
    planLocationsRepository.saveAll(addPlanLocations);

    // Proceed with caution here as new updates / removals to the object will prevent rewind of the streams application.
    // In the event of new data being introduced, ensure that null pointers are catered in the streams
    // application if the event comes through, and it does not have the new fields populated
    PlanLocationAssignMessage planLocationAssignMessage = new PlanLocationAssignMessage();
    planLocationAssignMessage.setPlanIdentifier(planIdentifier);
    planLocationAssignMessage.setLocationsAdded(
        locationsToAdd.stream()
            .map(UUID::toString)
            .collect(Collectors.toList())
    );
    planLocationAssignMessage.setOwnerId(UserUtils.getCurrentPrincipleName());
    planLocationAssignMessage.setLocationsRemoved(new ArrayList<>());

    publisherService.send(kafkaProperties.getTopicMap().get(KafkaConstants.PLAN_LOCATION_ASSIGNED),
        planLocationAssignMessage);
    log.info("sent plan location");

  }

  public void selectPlanLocations(UUID planIdentifier, Set<UUID> locations) {
    Plan plan = planService.findPlanByIdentifier(planIdentifier);
    PlanLocationAssignMessage planLocationAssignMessage = new PlanLocationAssignMessage();
    planLocationAssignMessage.setPlanIdentifier(planIdentifier);
    planLocationAssignMessage.setLocationsAdded(
        locations.stream()
            .map(UUID::toString)
            .collect(Collectors.toList())
    );
    planLocationAssignMessage.setOwnerId(UserUtils.getCurrentPrincipleName());

    List<UUID> removedPlanLocations = plan.getPlanLocations().stream()
        .map(PlanLocations::getLocation)
        .map(Location::getIdentifier).collect(Collectors.toList());
    removedPlanLocations.removeAll(locations);
    List<String> removedPlanLocationsString = removedPlanLocations.stream().map(UUID::toString)
        .collect(Collectors.toList());
    planLocationAssignMessage.setLocationsRemoved(removedPlanLocationsString);

    int deleteByPlan = -1;
    int saveAll = -1;
    int deleteByPlanAndLocation = -1;

    if (locations.size() == 0) {
      deleteByPlan = planLocationsRepository.deleteByPlanIdentifier(planIdentifier);
      planLocationAssignMessage.setDeleteByPlan(deleteByPlan);
      planLocationAssignMessage.setDeleteByPlanAndLocation(deleteByPlanAndLocation);
      planLocationAssignMessage.setSaveAll(saveAll);
      publisherService.send(kafkaProperties.getTopicMap().get(KafkaConstants.PLAN_LOCATION_ASSIGNED),
          planLocationAssignMessage);
    } else {
      Set<UUID> currentLocation = planLocationsRepository.findByPlan_Identifier(planIdentifier)
          .stream()
          .map(planLocations1 -> planLocations1.getLocation().getIdentifier()).collect(
              Collectors.toSet());
      Set<UUID> locationsToAdd = new HashSet<>(locations);
      locationsToAdd.removeAll(currentLocation);
      currentLocation.removeAll(locations);

      List<PlanLocations> addPlanLocations = locationsToAdd.stream()
          .map(location -> new PlanLocations(plan, location))
          .collect(Collectors.toList());

      if (addPlanLocations.size() > 0) {
        List<PlanLocations> planLocations = planLocationsRepository.saveAll(addPlanLocations);
        saveAll = planLocations.size();
      }
      if (currentLocation.size() > 0) {
        deleteByPlanAndLocation = planLocationsRepository.deletePlanLocationsByPlanAndLocation(
            planIdentifier,
            new ArrayList<>(currentLocation));
      }
      planLocationAssignMessage.setDeleteByPlan(deleteByPlan);
      planLocationAssignMessage.setDeleteByPlanAndLocation(deleteByPlanAndLocation);
      planLocationAssignMessage.setSaveAll(saveAll);
      publisherService.send(kafkaProperties.getTopicMap().get(KafkaConstants.PLAN_LOCATION_ASSIGNED),
          planLocationAssignMessage);
      log.info("sent plan location");
    }

  }

  public List<GeoTreeResponse> getHierarchyByPlanIdentifier(UUID identifier) {
    Plan plan = planService.findPlanByIdentifier((identifier));
    LocationHierarchy locationHierarchy = locationHierarchyService.findByIdentifier(
        plan.getLocationHierarchy().getIdentifier());

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
        geoTreeResponses = locationHierarchyService.getGeoTreeFromLocationHierarchyWithoutStructure(
            locationHierarchy, null);
      } else {
        geoTreeResponses = locationHierarchyService.getGeoTreeFromLocationHierarchyWithoutStructure(
            locationHierarchy, elList);
      }
    } else {
      geoTreeResponses = locationHierarchyService.getGeoTreeFromLocationHierarchyWithoutStructure(
          locationHierarchy, null);
    }

    Set<Location> locations = planLocationsRepository.findLocationsByPlan_Identifier(
        plan.getIdentifier());
    Map<UUID, Location> locationMap = locations.stream()
        .collect(Collectors.toMap(Location::getIdentifier, location -> location));
    List<PlanAssignment> planAssignments = planAssignmentService.getPlanAssignmentsByPlanIdentifier(
        identifier);
    Map<UUID, List<PlanAssignment>> planAssignmentMap = planAssignments.stream()
        .collect(Collectors.groupingBy(
            planAssignment -> planAssignment.getPlanLocations().getLocation().getIdentifier()));
    geoTreeResponses.forEach(el -> assignLocations(locationMap, el, planAssignmentMap));
    return geoTreeResponses;
  }

  public void assignLocations(Map<UUID, Location> locationMap, GeoTreeResponse geoTreeResponse,
      Map<UUID, List<PlanAssignment>> planAssignmentMap) {
    List<PlanAssignment> planAssignments = planAssignmentMap.get(geoTreeResponse.getIdentifier());
    if (planAssignments == null) {
      geoTreeResponse.setTeams(new ArrayList<>());
    } else {
      List<OrganizationResponse> teams = planAssignments.stream()
          .map(el -> OrganizationResponseFactory.fromEntityIdAndName(el.getOrganization()))
          .collect(Collectors.toList());
      geoTreeResponse.setTeams(teams);
    }
    geoTreeResponse.setActive(locationMap.containsKey(geoTreeResponse.getIdentifier()));
    if (geoTreeResponse.getChildren() != null) {
      geoTreeResponse.getChildren()
          .forEach(el -> assignLocations(locationMap, el, planAssignmentMap));
    }
  }

  public Long getPlanLocationsCount(UUID planIdentifier) {
    Plan plan = planService.findPlanByIdentifier(planIdentifier);
    return planLocationsRepository.countByPlan_Identifier(plan.getIdentifier());
  }

  public Set<PlanLocations> getPlanLocationsByPlanIdAndLocationIds(UUID planId,
      List<UUID> locationIds) {
    return planLocationsRepository.getPlanLocationsByPlanIdAndLocationIdentifiers(planId,
        locationIds);
  }


  public List<PlanLocationsAssigned> getPlanLocationsWithSearch(UUID planIdentifier,
      String search) {
    planService.findPlanByIdentifier(planIdentifier);
    return planLocationsRepository.getPlanLocationByPlanIdentifierAndSearch(planIdentifier, search);
  }

  public PlanLocationDetails getLocationDetailsByIdentifierAndPlanIdentifier(
      UUID locationIdentifier, UUID planIdentifier) {
    Plan plan = planService.findPlanByIdentifier(planIdentifier);
    PlanLocationDetails details = locationRepository.getLocationDetailsByIdentifierAndPlanIdentifier(
        locationIdentifier, plan.getIdentifier());

    details.setParentLocation(
        locationRelationshipService.findParentLocationByLocationIdAndHierarchyId(locationIdentifier,
            plan.getLocationHierarchy().getIdentifier()));
    return details;
  }

  public List<PlanLocationsAssigned> getAssignedLocationsToTeam(UUID planIdentifier,
      UUID organizationIdentifier) {
    planService.findPlanByIdentifier(planIdentifier);
    organizationService.findById(organizationIdentifier, true);
    return planLocationsRepository.getAssignedLocationsToTeam(planIdentifier,
        organizationIdentifier);
  }
  public List<PlanLocationDetails> getLocationsByParentIdentifierAndPlanIdentifier(
      UUID parentIdentifier,
      UUID planIdentifier) {
    Plan plan = planService.findPlanByIdentifier(planIdentifier);
    Location location = locationService.findByIdentifier(parentIdentifier);

    // if location is at plan target level do not load child location
    if (plan.getInterventionType().getCode().equals(PlanInterventionTypeEnum.MDA_LITE.name())) {
      PlanLocationDetails planLocationDetails = new PlanLocationDetails();
      planLocationDetails.setLocation(location);

      return List.of(planLocationDetails);
    } else {
      if (Objects.equals(plan.getPlanTargetType().getGeographicLevel().getName(),
          location.getGeographicLevel().getName())) {
        throw new NotFoundException("Child location is not in plan target level");
      }
    }

    Map<UUID, Long> childrenCount = locationRelationshipService.getLocationChildrenCount(
            plan.getLocationHierarchy().getIdentifier())
        .stream().filter(loc -> loc.getParentIdentifier() != null)
        .collect(Collectors.toMap(loc -> UUID.fromString(loc.getParentIdentifier()),
            LocationChildrenCountProjection::getChildrenCount));
    List<PlanLocationDetails> response = locationRelationshipService
        .getLocationChildrenByLocationParentIdentifierAndPlanIdentifier(
            parentIdentifier, planIdentifier);
    response.forEach(resp -> resp.setChildrenNumber(
        childrenCount.containsKey(resp.getLocation().getIdentifier()) ? childrenCount.get(
            resp.getLocation().getIdentifier()) : 0));
    if (location != null) {
      response = response.stream()
          .peek(planLocationDetail -> planLocationDetail.setParentLocation(location))
          .collect(Collectors.toList());
    }
    return response;
  }

  @Transactional
  public void assignLocationsToTeam(UUID planIdentifier, AssignLocationsToTeamRequest request) {
    Plan plan = planService.findPlanByIdentifier(planIdentifier);
    Organization organization = organizationService.findById(request.getOrganizationIdentifier(),
        true);
    if (request.getLocationIdentifiers().isEmpty()) {
      planAssignmentRepository.deletePlanAssignmentsPerOrganizationPerPlan(
          planIdentifier, organization.getIdentifier());
      return;
    }

    List<UUID> locationsWithChildren = planLocationsRepository.getChildrenAssignedLocations(
        planIdentifier, request.getLocationIdentifiers(),
        plan.getLocationHierarchy().getIdentifier());
    List<List<UUID>> parentLocations = planLocationsRepository.getParentsAssignedLocations(
        request.getLocationIdentifiers(), planIdentifier,
        plan.getLocationHierarchy().getIdentifier());
    Set<UUID> currentLocations = new HashSet<>(locationsWithChildren);
    currentLocations.addAll(request.getLocationIdentifiers());
    if (parentLocations != null) {
      parentLocations.stream().filter(Objects::nonNull).forEach(currentLocations::addAll);
    }

    List<UUID> locations = planLocationsRepository.getPlanLocationsIdentifiers(planIdentifier,
        request.getOrganizationIdentifier());

    List<UUID> toBeAdded = new ArrayList<>(currentLocations);
    toBeAdded.removeAll(locations);

    List<UUID> toBeRemoved = new ArrayList<>(locations);
    toBeRemoved.removeAll(currentLocations);

    if (!toBeAdded.isEmpty()) {
      Set<PlanLocations> planLocations = planLocationsRepository.getPlanLocationsByPlanIdAndLocationIdentifiers(
          planIdentifier, toBeAdded);
      List<PlanAssignment> addAssignments = new ArrayList<>();
      planLocations.forEach(el -> addAssignments.add(new PlanAssignment(organization, el,
          request.getLocationIdentifiers().contains(el.getLocation().getIdentifier()))));

      planAssignmentRepository.saveAll(addAssignments);
    }

    if (!toBeRemoved.isEmpty()) {
      planAssignmentRepository.deleteAllByPlanIdentifierAndLocationIdentifiers(toBeRemoved,
          planIdentifier);
    }
  }

  List<UUID> getPlanLocationsForHabitatSurvey(UUID planIdentifier, String surveyLocationType) {
    return planLocationsRepository.getPlanLocationsForHabitatSurvey(planIdentifier,
        surveyLocationType).stream().map(UUID::fromString).collect(Collectors.toList());
  }

  List<UUID> getPlanLocationsForHouseholdSurvey(UUID planIdentifier) {
    return planLocationsRepository.getPlanLocationsForHouseholdSurvey(planIdentifier)
        .stream().map(UUID::fromString).collect(Collectors.toList());
  }
}