package com.revealprecision.revealserver.service;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.revealprecision.revealserver.api.v1.dto.request.LocationRequest;
import com.revealprecision.revealserver.api.v1.dto.response.PopulationResponse;
import com.revealprecision.revealserver.api.v1.dto.response.PopulationResponseData;
import com.revealprecision.revealserver.client.PopulationClient;
import com.revealprecision.revealserver.enums.EntityStatus;
import com.revealprecision.revealserver.exceptions.NotFoundException;
import com.revealprecision.revealserver.model.ParentMap;
import com.revealprecision.revealserver.persistence.domain.EntityTag;
import com.revealprecision.revealserver.persistence.domain.GeographicLevel;
import com.revealprecision.revealserver.persistence.domain.Location;
import com.revealprecision.revealserver.persistence.domain.LocationHierarchy;
import com.revealprecision.revealserver.persistence.domain.Plan;
import com.revealprecision.revealserver.persistence.domain.PlanAssignment;
import com.revealprecision.revealserver.persistence.domain.PlanLocations;
import com.revealprecision.revealserver.persistence.projection.LocationCoordinatesProjection;
import com.revealprecision.revealserver.persistence.projection.LocationDetailsProjection;
import com.revealprecision.revealserver.persistence.projection.LocationWithAncestryProjection;
import com.revealprecision.revealserver.persistence.projection.LocationWithChildrenCountProjection;
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
import java.time.LocalDate;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.ClientAnchor;
import org.apache.poi.ss.usermodel.Comment;
import org.apache.poi.ss.usermodel.CreationHelper;
import org.apache.poi.ss.usermodel.DataValidation;
import org.apache.poi.ss.usermodel.DataValidationConstraint;
import org.apache.poi.ss.usermodel.DataValidationHelper;
import org.apache.poi.ss.usermodel.Drawing;
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
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class LocationService {

  private final LocationRepository locationRepository;
  private final GeographicLevelService geographicLevelService;
  private final LocationRelationshipService locationRelationshipService;
  private final StorageService storageService;
  private final EntityTagService entityTagService;
  private final LocationHierarchyService locationHierarchyService;
  private final PopulationClient populationClient;
  private final ObjectMapper objectMapper;
  private final PlanService planService;

  public Location createLocation(LocationRequest locationRequest, UUID parentLocationId,
          boolean buildHierarchy)
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


    if(buildHierarchy){
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
    }

    return savedLocation;
  }

  public Location findByIdentifier(UUID identifier) {
    return locationRepository.findById(identifier).orElseThrow(
        () -> new NotFoundException(Pair.of(Location.Fields.identifier, identifier),
            Location.class));
  }

  public List<Location> findAllById(List<UUID> ids) {
    return locationRepository.findByIdentifierIn(ids);
  }

  public Page<LocationWithChildrenCountProjection> findAllPageableById(List<UUID> ids,
      Pageable pageable) {
    return locationRepository.findPageableByIdentifierIn(ids, pageable);
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

  public List<UUID> getAllLocationDirectChildren(UUID locationIdentifier, UUID locationHierarchyIdentifier) {
//    UUID defaultHierarchyId = locationHierarchyService.getDefaultHierarchy().getIdentifier();
    return locationRepository.getAllDirectDescendantsOfLocation(locationIdentifier,
        locationHierarchyIdentifier);
  }

  public List<LocationDetailsProjection> getAllLocationDirectChildrenWithDetails(
      UUID locationIdentifier, UUID hierarchyIdentifier, UUID planId) {
    return locationRepository.getAllDirectDescendantsOfLocationWithProperties(locationIdentifier,
        hierarchyIdentifier, planId);
  }

  public List<LocationDetailsProjection> getLocationsWithPropertiesForAdminLevel(String geoLevel,
      UUID hierarchyIdentifier, UUID planId) {
    return locationRepository.getLocationsWithPropertiesForAdminLevel(geoLevel, hierarchyIdentifier,
        planId);
  }

  public List<UUID> getAllLocationChildrenNotLike(UUID locationIdentifier, UUID hierarchyIdentifier,
      List<String> targetNode) {
    return locationRepository.getAllLocationChildrenNotLike(locationIdentifier, hierarchyIdentifier,
        targetNode);
  }

  public List<Location> getLocationsByPeople(UUID personIdentifier) {
    return locationRepository.getLocationsByPeople_Identifier(personIdentifier);
  }


  private void getParentList(String id, List<ParentMap> list, Map<String, ParentMap> parentMap) {

    ParentMap parentMapProjection = parentMap.get(id);
    if (parentMapProjection != null) {
      list.add(parentMapProjection);
    }
    if (parentMapProjection != null && parentMapProjection.getParentId() != null) {
      getParentList(parentMapProjection.getParentId(), list, parentMap);
    }

  }

  public ByteArrayResource downloadLocations(UUID hierarchyIdentifier, String geographicLevelName,
      UUID userId, ArrayList<UUID> entityTags, LocalDate captureDate)
      throws IOException {
    List<String> nodeOrder = locationHierarchyService.findNodeOrderByIdentifier(
        hierarchyIdentifier);
    GeographicLevel geographicLevel = geographicLevelService.findByName(geographicLevelName);
    List<Location>   locationList= locationRepository.findByGeographicLevelIdentifierSorted(
        geographicLevel.getIdentifier());

    List<List<Location>> locationBatches = batchList(locationList, 1000);

    if(captureDate == null)
      captureDate = LocalDate.now();

    Map<String, ParentMap> collect = locationBatches.stream().flatMap(
            locationBatch -> locationRelationshipService.getParentMap(hierarchyIdentifier,
                locationBatch.stream().map(Location::getIdentifier).collect(
                    Collectors.toList())).entrySet().stream())
        .map(entry -> new SimpleEntry<>(entry.getKey(), ParentMap
            .builder()
            .locationId(entry.getValue().getLocationId())
            .locationName(entry.getValue().getLocationName())
            .parentId(entry.getValue().getParentId())
            .parentName(entry.getValue().getParentName())
            .nodeLevel(entry.getValue().getNodeLevel())
            .build()))
        .collect(toMap(Entry::getKey, Entry::getValue, (a, b) -> b));

    File currDir = new File(".");
    String path = currDir.getAbsolutePath();
    String fileLocation =
        path.substring(0, path.length() - 1) + "temp" + userId.toString() + ".xlsx";

    int additionalCols = 0;
    if (locationList != null && !locationList.isEmpty()) {
      List<ParentMap> oneOffParentList = new ArrayList<>();
      String id = locationList.get(0).getIdentifier().toString();
      getParentList(id, oneOffParentList, collect);
      additionalCols = oneOffParentList.size() - 1;
    }

    try (XSSFWorkbook workbook = new XSSFWorkbook();

        FileOutputStream outputStream = new FileOutputStream(fileLocation)) {
      Sheet sheet = workbook.createSheet("Locations");
      sheet.setColumnWidth(0, 11000);
      sheet.setColumnWidth(1, 10000);
      sheet.setColumnWidth(2, 6500);
      sheet.setColumnWidth(3, 8000);

      CellStyle headerStyle = workbook.createCellStyle();
      headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
      headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

      XSSFFont font = workbook.createFont();
      font.setFontName("Arial");
      font.setFontHeightInPoints((short) 16);
      font.setBold(true);
      headerStyle.setFont(font);

      XSSFFont headerRowFont = workbook.createFont();
      headerRowFont.setFontName("Arial");
      headerRowFont.setFontHeightInPoints((short) 11);
      headerRowFont.setBold(true);

      CellStyle rowHeaderStyle = workbook.createCellStyle();
      rowHeaderStyle.setFont(headerRowFont);

      int rowIndex = 0;

      int rowOneCellCount = 0;

      Row tagNameRow = sheet.createRow(rowIndex++);
      tagNameRow.setRowStyle(rowHeaderStyle);
      Cell tagNameRowLabelCell = tagNameRow.createCell(rowOneCellCount++);
      tagNameRowLabelCell.setCellValue("Tag Name");
      tagNameRowLabelCell.setCellStyle(headerStyle);
      Cell tagNameRowLabelCell1 = tagNameRow.createCell(rowOneCellCount++);
      tagNameRowLabelCell1.setCellStyle(headerStyle);
      Cell tagNameRowLabelCell2 = tagNameRow.createCell(rowOneCellCount++);
      tagNameRowLabelCell2.setCellStyle(headerStyle);
      Cell tagNameRowLabelCell3 = tagNameRow.createCell(rowOneCellCount++);
      tagNameRowLabelCell3.setCellStyle(headerStyle);

      for (int i = 0; i < additionalCols; i++) {
        Cell tagDataTypeLabelCellDyn = tagNameRow.createCell(rowOneCellCount++);
        tagDataTypeLabelCellDyn.setCellStyle(headerStyle);
      }

      int rowTwoCellCount = 0;

      Row tagDataTypeRow = sheet.createRow(rowIndex++);
      tagDataTypeRow.setRowStyle(rowHeaderStyle);
      Cell tagDataTypeLabelCell = tagDataTypeRow.createCell(rowTwoCellCount++);
      tagDataTypeLabelCell.setCellValue("Data Type");
      tagDataTypeLabelCell.setCellStyle(headerStyle);
      Cell tagDataTypeLabelCell1 = tagDataTypeRow.createCell(rowTwoCellCount++);
      tagDataTypeLabelCell1.setCellStyle(headerStyle);
      Cell tagDataTypeLabelCell2 = tagDataTypeRow.createCell(rowTwoCellCount++);
      tagDataTypeLabelCell2.setCellStyle(headerStyle);
      Cell tagDataTypeLabelCell3 = tagDataTypeRow.createCell(rowTwoCellCount++);
      tagDataTypeLabelCell3.setCellStyle(headerStyle);

      for (int i = 0; i < additionalCols; i++) {
        Cell tagDataTypeLabelCellDyn = tagDataTypeRow.createCell(rowTwoCellCount++);
        tagDataTypeLabelCellDyn.setCellStyle(headerStyle);
      }

      CellStyle style = workbook.createCellStyle();
      style.setWrapText(true);



      Row header = sheet.createRow(rowIndex++);
      header.setRowStyle(headerStyle);

      int headerCnt = 0;

      Cell headerCell = header.createCell(headerCnt++);
      headerCell.setCellValue("Location Hierarchy Identifier");
      headerCell.setCellStyle(headerStyle);

      headerCell = header.createCell(headerCnt++);
      headerCell.setCellValue("Identifier");
      headerCell.setCellStyle(headerStyle);

      headerCell = header.createCell(headerCnt++);
      headerCell.setCellValue("Date");
      headerCell.setCellStyle(headerStyle);


      headerCell = header.createCell(headerCnt++);
      headerCell.setCellValue("Name");
      headerCell.setCellStyle(headerStyle);

      headerCell = header.createCell(headerCnt++);
      headerCell.setCellValue("Geographic level");
      headerCell.setCellStyle(headerStyle);

      for (int i = 0; i < additionalCols; i++) {
        headerCell = header.createCell(headerCnt++);
        headerCell.setCellValue(nodeOrder.get(additionalCols - i -1));
        headerCell.setCellStyle(headerStyle);
      }

      int entityTagIndex = headerCnt;

      DataValidationHelper dvHelper = sheet.getDataValidationHelper();
      CellRangeAddressList addressList = new CellRangeAddressList(1, 1, entityTagIndex, 1000);
      DataValidationConstraint dvConstraint = dvHelper.createFormulaListConstraint("DATA_TYPES");
      dvConstraint.setExplicitListValues(
          List.of("string", "number", "boolean").toArray(String[]::new));
      DataValidation dataValidation = dvHelper.createValidation(dvConstraint, addressList);
      sheet.addValidationData(dataValidation);

      for (UUID el : entityTags) {
        EntityTag entityTag = entityTagService.getEntityTagByIdentifier(el);
        sheet.setColumnWidth(entityTagIndex, 9600);
        Cell tagNameRowCell = tagNameRow.createCell(entityTagIndex);
        tagNameRowCell.setCellValue(entityTag.getTag());
        tagNameRowCell.setCellStyle(rowHeaderStyle);
        Cell tagDataTypeDropDownCell = tagDataTypeRow.createCell(entityTagIndex);
        tagDataTypeDropDownCell.setCellStyle(rowHeaderStyle);
        tagDataTypeDropDownCell.setCellValue(
            entityTag.getValueType().equals("double") ? "number" : entityTag.getValueType());
        entityTagIndex++;
      }

      for (Location location : locationList) {
        Row row = sheet.createRow(rowIndex++);
        int colCount = 0;
        Cell cell = row.createCell(colCount++);
        cell.setCellValue(hierarchyIdentifier.toString());
        cell.setCellStyle(style);

        cell = row.createCell(colCount++);
        cell.setCellValue(location.getIdentifier().toString());
        cell.setCellStyle(style);

        cell = row.createCell(colCount++);
        cell.setCellValue(captureDate.toString());
        cell.setCellStyle(style);

        cell = row.createCell(colCount++);
        cell.setCellValue(location.getName());
        cell.setCellStyle(style);

        cell = row.createCell(colCount++);
        cell.setCellValue(location.getGeographicLevel().getName());
        cell.setCellStyle(style);

        List<ParentMap> list = new ArrayList<>();
        getParentList(location.getIdentifier().toString(), list,
            collect);

        List<ParentMap> orderedList = list.stream()
            .sorted(Comparator.comparingInt(ParentMap::getNodeLevel).reversed())
            .collect(Collectors.toCollection(LinkedList::new));

        for (ParentMap parentMap : orderedList) {
          if (!parentMap.getLocationId().equals(location.getIdentifier().toString())) {

            CreationHelper createHelper = workbook.getCreationHelper();
            ClientAnchor anchor = createHelper.createClientAnchor();
            anchor.setCol1(colCount); // Start column of the comment
            anchor.setRow1(rowIndex - 1); // Start row of the comment
            anchor.setCol2(colCount); // End column of the comment
            anchor.setRow2(rowIndex - 1); // End row of the comment
            Drawing<?> drawing = sheet.createDrawingPatriarch();
            // Create the cell comment
            Comment comment = drawing.createCellComment(anchor);
            comment.setString(createHelper.createRichTextString(parentMap.getLocationId()));

            // Assign the comment to the cell

            cell = row.createCell(colCount++);
            cell.setCellComment(comment);
            cell.setCellValue(parentMap.getLocationName());
            cell.setCellStyle(style);
          }
        }

      }

      workbook.write(outputStream);
    }

    Path filePath = Paths.get(fileLocation);

    ByteArrayResource resource = new ByteArrayResource(Files.readAllBytes(filePath));
    storageService.deleteFile(fileLocation);
    return resource;
  }

  public static <T> List<List<T>> batchList(List<T> inputList, int batchSize) {
    List<List<T>> batches = new ArrayList<>();
    for (int i = 0; i < inputList.size(); i += batchSize) {
      batches.add(inputList.subList(i, Math.min(i + batchSize, inputList.size())));
    }
    return batches;
  }

  public List<LocationWithAncestryProjection> getAllTargetAreasOfPlan(UUID planId,
      String planTargetLevelName) {

    Plan plan  = planService.getPlanByIdentifier(planId);
    LocationHierarchy locationHierarchy = plan.getLocationHierarchy();

//    LocationHierarchy defaultHierarchy = locationHierarchyService.getDefaultHierarchy();
    int idx = locationHierarchy.getNodeOrder().indexOf(planTargetLevelName);
    String targetAreaLevel = idx > 0 ? locationHierarchy.getNodeOrder().get(idx - 1) : null;
    return locationRepository.getAllTargetAreasOfPlan(planId, targetAreaLevel);
  }

  public Mono<PopulationResponseData> getPopulationDataForLocation(UUID locationId) {
    Location location = findByIdentifier(locationId);
    List<Object> coordinates = location.getGeometry().getCoordinates();
    var coordinatesWithElevation = addElevation(coordinates);
    Mono<PopulationResponse> response = populationClient.getPopulationForLocation(
        location.getGeometry().getType(), coordinatesWithElevation);
    return response.flatMap(res -> {
      if (res.getResults() != null && !res.getResults().isEmpty()) {
        var population = res.getResults().get(0).getPopulationData();
        JsonNode jsonNode = objectMapper.convertValue(population, JsonNode.class);
        updatePopulationData(locationId, jsonNode);
        return Mono.justOrEmpty(population);
      }
      return Mono.empty();
    });
  }

  @Transactional
  public void updatePopulationData(UUID locationId, JsonNode populationData) {
    locationRepository.updatePopulationData(locationId, populationData);
  }

  @SuppressWarnings("unchecked")
  private List<Object> addElevation(List<Object> coordinates) {
    List<Object> result = new ArrayList<>();

    for (Object item : coordinates) {
      if (item instanceof List) {
        List<Object> nestedList = (List<Object>) item;
        if (!nestedList.isEmpty() && nestedList.get(0) instanceof Number) {
          List<Double> updatedPoint = new ArrayList<>();
          for (Object value : nestedList) {
            if (value instanceof Number) {
              updatedPoint.add(((Number) value).doubleValue());
            } else {
              throw new IllegalArgumentException("Unexpected value in coordinate list: " + value);
            }
          }
          updatedPoint.add(0.0);
          result.add(updatedPoint);
        } else {
          result.add(addElevation(nestedList));
        }
      } else {
        throw new IllegalArgumentException("Unexpected non-list item in coordinates: " + item);
      }
    }

    return result;
  }

  List<Location> findAllIdentifiersWithoutStructureAndGeoJSON(List<UUID> identifiers){
    return locationRepository.findAllIdentifiersWithoutStructureAndGeoJSON(identifiers);
  }

  public List<LocationWithAncestryProjection> getLocationWithAncestryProjection(List<UUID> identifiers, UUID locationHierarchyId) {
    return locationRepository.getLocationWithAncestryProjection(identifiers, locationHierarchyId);
  }

}
