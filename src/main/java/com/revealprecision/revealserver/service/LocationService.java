package com.revealprecision.revealserver.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.revealprecision.revealserver.api.v1.dto.request.LocationRequest;
import com.revealprecision.revealserver.constants.LocationConstants;
import com.revealprecision.revealserver.enums.EntityStatus;
import com.revealprecision.revealserver.enums.PlanInterventionTypeEnum;
import com.revealprecision.revealserver.exceptions.NotFoundException;
import com.revealprecision.revealserver.persistence.domain.GeographicLevel;
import com.revealprecision.revealserver.persistence.domain.Location;
import com.revealprecision.revealserver.persistence.domain.LocationHierarchy;
import com.revealprecision.revealserver.persistence.domain.LocationRelationship;
import com.revealprecision.revealserver.persistence.domain.Plan;
import com.revealprecision.revealserver.persistence.domain.PlanAssignment;
import com.revealprecision.revealserver.persistence.domain.PlanLocations;
import com.revealprecision.revealserver.persistence.es.LocationElastic;
import com.revealprecision.revealserver.persistence.projection.LocationChildrenCountProjection;
import com.revealprecision.revealserver.persistence.projection.LocationCoordinatesProjection;
import com.revealprecision.revealserver.persistence.projection.PlanLocationDetails;
import com.revealprecision.revealserver.persistence.repository.LocationRepository;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LocationService {

  private final LocationRepository locationRepository;
  private final GeographicLevelService geographicLevelService;
  private final LocationRelationshipService locationRelationshipService;
  private final PlanService planService;
  private final RestHighLevelClient client;
  private final StorageService storageService;

  public Location createLocation(LocationRequest locationRequest) throws IOException {
    GeographicLevel geographicLevel = geographicLevelService.findByName(
        locationRequest.getProperties().getGeographicLevel());
    var locationToSave = Location.builder().geographicLevel(geographicLevel)
        .type(locationRequest.getType()).geometry(locationRequest.getGeometry())
        .name(locationRequest.getProperties().getName())
        .status(locationRequest.getProperties().getStatus())
        .externalId(locationRequest.getProperties().getExternalId()).build();
    if (locationRequest.getProperties().getExternalId() != null) {
      locationToSave.setIdentifier(locationRequest.getProperties().getExternalId());
    }
    locationToSave.setEntityStatus(EntityStatus.ACTIVE);
    var savedLocation = locationRepository.save(locationToSave);
    locationRelationshipService.updateLocationRelationshipsForNewLocation(savedLocation);
    return savedLocation;
  }

  public Location findByIdentifier(UUID identifier) {
    return locationRepository.findById(identifier).orElseThrow(
        () -> new NotFoundException(Pair.of(Location.Fields.identifier, identifier),
            Location.class));
  }

  public Page<Location> getLocations(String search, Pageable pageable) {
    return locationRepository.findAlLByCriteria(search, pageable);
  }

  public long getAllCount(String search) {
    return locationRepository.findAllCountByCriteria(search);
  }

  public void deleteLocation(UUID identifier) {
    Location location = locationRepository.findById(identifier).orElseThrow(
        () -> new NotFoundException(Pair.of(Location.Fields.identifier, identifier),
            Location.class));
    locationRepository.delete(location);
  }

  public Location updateLocation(UUID identifier, LocationRequest locationRequest) {
    Location location = findByIdentifier(identifier);
    GeographicLevel geographicLevel = geographicLevelService.findByName(
        locationRequest.getProperties().getGeographicLevel());
    return locationRepository.save(location.update(locationRequest, geographicLevel));
  }

  public List<Location> getAllByIdentifiers(List<UUID> identifiers) {
    return locationRepository.getAllByIdentifiers(identifiers);
  }

  public List<Location> getAllByNames(List<String> names) {
    return locationRepository.getAllByNames(names);
  }

  public List<Location> getStructuresByParentIdentifiers(List<UUID> parentIdentifiers,
      LocationHierarchy locationHierarchy, Long serverVersion) {
    List<LocationRelationship> locationRelationships = locationHierarchy.getLocationRelationships();
    List<Location> locations = locationRelationships.stream().filter(
            locationRelationship -> locationRelationship.getParentLocation() != null
                && parentIdentifiers.contains(locationRelationship.getParentLocation().getIdentifier()))
        .map(LocationRelationship::getLocation).collect(Collectors.toList());

    return locations.stream()
        .filter(location -> location.getGeographicLevel().getName().equalsIgnoreCase(
            LocationConstants.STRUCTURE)
            && location.getServerVersion() >= serverVersion)
        .collect(Collectors.toList());//TODO: to update once we figure the target level part.
  }

  public List<PlanLocationDetails> getLocationsByParentIdentifierAndPlanIdentifier(
      UUID parentIdentifier,
      UUID planIdentifier) {
    Plan plan = planService.findPlanByIdentifier(planIdentifier);
    Location location = findByIdentifier(parentIdentifier);

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

  public List<PlanLocationDetails> getAssignedLocationsByParentIdentifierAndPlanIdentifier(
      UUID parentIdentifier,
      UUID planIdentifier,
      boolean isBeforeStructure) {
    Plan plan = planService.findPlanByIdentifier(planIdentifier);
    Location location = findByIdentifier(parentIdentifier);
    Map<UUID, Long> childrenCount;
    if (!isBeforeStructure) {
      childrenCount = locationRelationshipService.getLocationAssignedChildrenCount(
              plan.getLocationHierarchy().getIdentifier(), planIdentifier)
          .stream().filter(loc -> loc.getParentIdentifier() != null)
          .collect(Collectors.toMap(loc -> UUID.fromString(loc.getParentIdentifier()),
              LocationChildrenCountProjection::getChildrenCount));
    } else {
      childrenCount = locationRelationshipService.getLocationChildrenCount(
              plan.getLocationHierarchy().getIdentifier())
          .stream().filter(loc -> loc.getParentIdentifier() != null)
          .collect(Collectors.toMap(loc -> UUID.fromString(loc.getParentIdentifier()),
              LocationChildrenCountProjection::getChildrenCount));
    }
    List<PlanLocationDetails> response = locationRelationshipService
        .getAssignedLocationChildrenByLocationParentIdentifierAndPlanIdentifier(
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

  public PlanLocationDetails getRootLocationByPlanIdentifier(UUID planIdentifier) {
    Plan plan = planService.findPlanByIdentifier(planIdentifier);
    Map<UUID, Long> childrenCount = locationRelationshipService.getLocationChildrenCount(
            plan.getLocationHierarchy().getIdentifier())
        .stream().filter(loc -> loc.getParentIdentifier() != null)
        .collect(Collectors.toMap(loc -> UUID.fromString(loc.getParentIdentifier()),
            LocationChildrenCountProjection::getChildrenCount));
    PlanLocationDetails response = locationRelationshipService.getRootLocationDetailsByPlanId(
        planIdentifier);
    response.setChildrenNumber(
        childrenCount.containsKey(response.getLocation().getIdentifier()) ? childrenCount.get(
            response.getLocation().getIdentifier()) : 0);
    return response;
  }

  public Set<Location> getAssignedLocationsFromPlanAssignments(
      Set<PlanAssignment> planAssignments) {
    return planAssignments.stream().map(PlanAssignment::getPlanLocations)
        .map(PlanLocations::getLocation).collect(Collectors.toSet());
  }

  public LocationCoordinatesProjection getLocationCentroidCoordinatesByIdentifier(
      UUID locationIdentifier) {
    return locationRepository.getLocationCentroidCoordinatesByIdentifier(locationIdentifier);
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

  public Location getLocationParent(Location location, LocationHierarchy locationHierarchy) {
    return locationRelationshipService.getLocationParent(location, locationHierarchy);
  }

  public Location getLocationParentByLocationIdentifierAndHierarchyIdentifier(
      UUID locationIdentifier, UUID locationHierarchyIdentifier) {
    return locationRelationshipService.getLocationParentByLocationIdentifierAndHierarchyIdentifier(
        locationIdentifier, locationHierarchyIdentifier);
  }

  public List<UUID> getAllLocationChildren(UUID locationIdentifier, UUID hierarchyIdentifier) {
    return locationRepository.getAllLocationChildren(locationIdentifier, hierarchyIdentifier);
  }

  public List<UUID> getAllLocationChildrenNotLike(UUID locationIdentifier, UUID hierarchyIdentifier,
      List<String> targetNode) {
    return locationRepository.getAllLocationChildrenNotLike(locationIdentifier, hierarchyIdentifier,
        targetNode);
  }

  public List<Location> getLocationsByPeople(UUID personIdentifier) {
    return locationRepository.getLocationsByPeople_Identifier(personIdentifier);
  }

  public ByteArrayResource downloadLocations(UUID hierarchyIdentifier, UUID locationIdentifier)
      throws IOException {
    ObjectMapper mapper = new ObjectMapper();
    Location location = findByIdentifier(locationIdentifier);
    SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
    sourceBuilder.query(QueryBuilders.termsQuery("ancestry." + hierarchyIdentifier.toString(), locationIdentifier.toString())).size(1000);
    SearchRequest searchRequest = new SearchRequest("location");
    searchRequest.source(sourceBuilder);
    SearchResponse searchResponse = client.search(searchRequest, RequestOptions.DEFAULT);

    Workbook workbook = new XSSFWorkbook();

    Sheet sheet = workbook.createSheet("Persons");
    sheet.setColumnWidth(0, 9600);
    sheet.setColumnWidth(1, 6000);
    sheet.setColumnWidth(2, 6400);

    Row header = sheet.createRow(0);

    CellStyle headerStyle = workbook.createCellStyle();
    headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
    headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

    XSSFFont font = ((XSSFWorkbook) workbook).createFont();
    font.setFontName("Arial");
    font.setFontHeightInPoints((short) 16);
    font.setBold(true);
    headerStyle.setFont(font);

    Cell headerCell = header.createCell(0);
    headerCell.setCellValue("Identifier");
    headerCell.setCellStyle(headerStyle);

    headerCell = header.createCell(1);
    headerCell.setCellValue("Name");
    headerCell.setCellStyle(headerStyle);

    headerCell = header.createCell(2);
    headerCell.setCellValue("Geographic level");
    headerCell.setCellStyle(headerStyle);

    CellStyle style = workbook.createCellStyle();
    style.setWrapText(true);

    Row row = sheet.createRow(1);
    Cell cell = row.createCell(0);
    cell.setCellValue(location.getIdentifier().toString());
    cell.setCellStyle(style);

    cell = row.createCell(1);
    cell.setCellValue(location.getName());
    cell.setCellStyle(style);

    cell = row.createCell(2);
    cell.setCellValue(location.getGeographicLevel().getName());
    cell.setCellStyle(style);

    int index = 2;
    for(SearchHit hit : searchResponse.getHits().getHits()) {
      LocationElastic locationElastic = mapper.readValue(hit.getSourceAsString(), LocationElastic.class);
      row = sheet.createRow(index);
      cell = row.createCell(0);
      cell.setCellValue(locationElastic.getId());
      cell.setCellStyle(style);

      cell = row.createCell(1);
      cell.setCellValue(locationElastic.getName());
      cell.setCellStyle(style);

      cell = row.createCell(2);
      cell.setCellValue(locationElastic.getLevel());
      cell.setCellStyle(style);
      index++;
    }

    File currDir = new File(".");
    String path = currDir.getAbsolutePath();
    //TODO: refactor this line because of multiuser problems.. filename could be temp.concat(userId).xlsx
    String fileLocation = path.substring(0, path.length() - 1) + "temp.xlsx";

    FileOutputStream outputStream = new FileOutputStream(fileLocation);
    workbook.write(outputStream);
    workbook.close();

    Path filePath = Paths.get(fileLocation);


    ByteArrayResource resource = new ByteArrayResource(Files.readAllBytes(filePath));
    storageService.deleteFile(fileLocation);
    return resource;
  }
}
