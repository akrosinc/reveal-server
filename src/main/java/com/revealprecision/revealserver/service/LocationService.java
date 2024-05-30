package com.revealprecision.revealserver.service;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.toSet;

import com.revealprecision.revealserver.api.v1.dto.request.LocationRequest;
import com.revealprecision.revealserver.enums.EntityStatus;
import com.revealprecision.revealserver.exceptions.NotFoundException;
import com.revealprecision.revealserver.persistence.domain.EntityTag;
import com.revealprecision.revealserver.persistence.domain.GeographicLevel;
import com.revealprecision.revealserver.persistence.domain.Location;
import com.revealprecision.revealserver.persistence.domain.LocationHierarchy;
import com.revealprecision.revealserver.persistence.domain.Plan;
import com.revealprecision.revealserver.persistence.domain.PlanAssignment;
import com.revealprecision.revealserver.persistence.domain.PlanLocations;
import com.revealprecision.revealserver.persistence.projection.LocationCoordinatesProjection;
import com.revealprecision.revealserver.persistence.projection.LocationWithParentProjection;
import com.revealprecision.revealserver.persistence.repository.LocationRepository;
import com.revealprecision.revealserver.util.ElasticModelUtil;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.DataValidation;
import org.apache.poi.ss.usermodel.DataValidationConstraint;
import org.apache.poi.ss.usermodel.DataValidationHelper;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.util.CellRangeAddressList;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
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
  private final StorageService storageService;
  private final EntityTagService entityTagService;
  private final LocationHierarchyService locationHierarchyService;

  public Location createLocation(LocationRequest locationRequest, UUID parentLocationId)
      throws Exception {
    GeographicLevel geographicLevel = geographicLevelService.findByName(
        locationRequest.getProperties().getGeographicLevel());
    MessageDigest digest = MessageDigest.getInstance("SHA-256");

    String hash = ElasticModelUtil.bytesToHex(digest.digest(locationRequest
        .getGeometry()
        .toString()
        .getBytes(StandardCharsets.UTF_8)));

    var locationToSave = Location.builder().geographicLevel(geographicLevel)
        .type(locationRequest.getType()).geometry(locationRequest.getGeometry())
        .name(locationRequest.getProperties().getName())
        .status(locationRequest.getProperties().getStatus())
        .locationProperty(locationRequest.getProperties())
        .hashValue(hash)
        .externalId(locationRequest.getProperties().getExternalId()).build();
    if (locationRequest.getProperties().getExternalId() != null) {
      locationToSave.setIdentifier(locationRequest.getProperties().getExternalId());
    }
    locationToSave.setEntityStatus(EntityStatus.ACTIVE);
    var savedLocation = locationRepository.save(locationToSave);
    Location parentLocation =
        parentLocationId != null ? locationRepository.findById(parentLocationId).orElse(null)
            : null;

    if (parentLocation != null) {
      LocationHierarchy locationHierarchy = locationHierarchyService.getActiveLocationHierarchy();
      locationRelationshipService.createLocationRelationship(parentLocation, savedLocation,
          locationHierarchy);
    } else {
      locationRelationshipService.updateLocationRelationshipsForNewLocation(savedLocation);
    }

    return savedLocation;
  }

  public Location findByIdentifier(UUID identifier) {
    return locationRepository.findById(identifier).orElseThrow(
        () -> new NotFoundException(Pair.of(Location.Fields.identifier, identifier),
            Location.class));
  }

  public Optional<Location> findNullableByIdentifier(UUID identifier) {
    return locationRepository.findById(identifier);
  }

  public Map<UUID, Location> getLocationsByIdentifierList(List<UUID> locationList) {
    return locationRepository.findLocationsByIdentifierIn(locationList).stream()
        .collect(Collectors.toMap(Location::getIdentifier, a -> a));
  }

  public Map<String, LocationCoordinatesProjection> getLocationCentroidCoordinatesMap(
      List<UUID> locationList) {
    List<LocationCoordinatesProjection> locationCentroidCoordinatesByIdentifierList = locationRepository.getLocationCentroidCoordinatesByIdentifierList(
        locationList);
    return locationCentroidCoordinatesByIdentifierList.stream()
        .collect(Collectors.toMap(
            LocationCoordinatesProjection::getIdentifier,
            locationCoordinatesProjection -> locationCoordinatesProjection, (a, b) -> a));
  }

  public Location findByIdentifierWithoutGeoJson(UUID identifier) {
    return locationRepository.findByIdentifierWithoutGeoJson(identifier).orElseThrow(
        () -> new NotFoundException(Pair.of(Location.Fields.identifier, identifier),
            Location.class));
  }

  public Set<Location> findLocationsWithoutGeoJsonByIdentifierIn(Set<UUID> identifiers) {
    return locationRepository.findLocationsWithoutGeoJsonByIdentifierIn(identifiers);
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

  public List<LocationWithParentProjection> getAllNotStructuresByIdentifiersAndServerVersion(
      List<UUID> identifiers, long serverVersion) {
    return locationRepository.getAllNotStructuresByIdentifiersAndServerVersion(identifiers,
        serverVersion);
  }

  public List<Location> getAllByNames(List<String> names) {
    return locationRepository.getAllByNames(names);
  }

  public List<LocationWithParentProjection> getAllNotStructureByNamesAndServerVersion(
      List<String> names, Long serverVersion) {
    return locationRepository.getAllNotStructureByNamesAndServerVersion(names, serverVersion);
  }

  public Map<Plan, Set<Location>> getAssignedLocationsFromPlanAssignments(
      Set<PlanAssignment> planAssignments) {
    return planAssignments.stream().map(PlanAssignment::getPlanLocations)
        .collect(groupingBy(PlanLocations::getPlan, mapping(PlanLocations::getLocation, toSet())));
  }

  public LocationCoordinatesProjection getLocationCentroidCoordinatesByIdentifier(
      UUID locationIdentifier) {
    return locationRepository.getLocationCentroidCoordinatesByIdentifier(locationIdentifier);
  }

  public Location getLocationParent(UUID locationIdentifier, UUID locationHierarchyIdentifier) {
    return locationRelationshipService.getLocationParent(locationIdentifier,
        locationHierarchyIdentifier);
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

  public ByteArrayResource downloadLocations(UUID hierarchyIdentifier, String geographicLevelName,
      UUID userId, ArrayList<UUID> entityTags)
      throws IOException {
    LocationHierarchy locationHierarchy = locationHierarchyService.findByIdentifier(
        hierarchyIdentifier);
    GeographicLevel geographicLevel = geographicLevelService.findByName(geographicLevelName);
    List<Location> locationList = locationRepository.findByGeographicLevelIdentifier(
        geographicLevel.getIdentifier());

    File currDir = new File(".");
    String path = currDir.getAbsolutePath();
    String fileLocation =
        path.substring(0, path.length() - 1) + "temp" + userId.toString() + ".xlsx";

    try (XSSFWorkbook workbook = new XSSFWorkbook();

        FileOutputStream outputStream = new FileOutputStream(fileLocation)) {
      Sheet sheet = workbook.createSheet("Locations");
      sheet.setColumnWidth(0, 9600);
      sheet.setColumnWidth(1, 6000);
      sheet.setColumnWidth(2, 6400);

      CellStyle headerStyle = workbook.createCellStyle();
      headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
      headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

      int rowIndex = 0;

      Row tagNameRow = sheet.createRow(rowIndex++);
      Cell tagNameRowLabelCell = tagNameRow.createCell(0);
      tagNameRowLabelCell.setCellValue("Tag Name");
      tagNameRowLabelCell.setCellStyle(headerStyle);

      Row tagDataTypeRow = sheet.createRow(rowIndex++);
      Cell tagDataTypeLabelCell = tagDataTypeRow.createCell(0);
      tagDataTypeLabelCell.setCellValue("Data Type");
      tagDataTypeLabelCell.setCellStyle(headerStyle);

      Cell tagDataTypeDropDownCell = tagDataTypeRow.createCell(4);

      DataValidationHelper dvHelper = sheet.getDataValidationHelper();
      CellRangeAddressList addressList = new CellRangeAddressList(1, 1, 4, 4);
      DataValidationConstraint dvConstraint = dvHelper.createFormulaListConstraint("DATA_TYPES");
      dvConstraint.setExplicitListValues(
          List.of("string", "number", "boolean").toArray(String[]::new));
      DataValidation dataValidation = dvHelper.createValidation(dvConstraint, addressList);
      sheet.addValidationData(dataValidation);

      Row header = sheet.createRow(rowIndex++);
      XSSFFont font = workbook.createFont();
      font.setFontName("Arial");
      font.setFontHeightInPoints((short) 16);
      font.setBold(true);
      headerStyle.setFont(font);

      Cell headerCell = header.createCell(0);
      headerCell.setCellValue("Location Hierarchy Identifier");
      headerCell.setCellStyle(headerStyle);

      headerCell = header.createCell(1);
      headerCell.setCellValue("Identifier");
      headerCell.setCellStyle(headerStyle);

      headerCell = header.createCell(2);
      headerCell.setCellValue("Name");
      headerCell.setCellStyle(headerStyle);

      headerCell = header.createCell(3);
      headerCell.setCellValue("Geographic level");
      headerCell.setCellStyle(headerStyle);

      CellStyle style = workbook.createCellStyle();
      style.setWrapText(true);

//      int index = 1;
      for (Location location : locationList) {
        Row row = sheet.createRow(rowIndex++);
        Cell cell = row.createCell(0);
        cell.setCellValue(locationHierarchy.getIdentifier().toString());
        cell.setCellStyle(style);

        cell = row.createCell(1);
        cell.setCellValue(location.getIdentifier().toString());
        cell.setCellStyle(style);

        cell = row.createCell(2);
        cell.setCellValue(location.getName());
        cell.setCellStyle(style);

        cell = row.createCell(3);
        cell.setCellValue(location.getGeographicLevel().getName());
        cell.setCellStyle(style);
//        index++;

        int entityTagIndex = 4;
        for (UUID el : entityTags) {
          EntityTag entityTag = entityTagService.getEntityTagByIdentifier(el);
          sheet.setColumnWidth(entityTagIndex, 9600);
          headerCell = header.createCell(entityTagIndex);
          headerCell.setCellValue(entityTag.getTag());
          headerCell.setCellStyle(headerStyle);
          cell = row.createCell(entityTagIndex);
          cell.setCellStyle(style);
          entityTagIndex++;
        }
      }

      workbook.write(outputStream);
    }

    Path filePath = Paths.get(fileLocation);

    ByteArrayResource resource = new ByteArrayResource(Files.readAllBytes(filePath));
    storageService.deleteFile(fileLocation);
    return resource;
  }
}
