package com.revealprecision.revealserver.persistence.domain.metadata.metadataImport.fieldMapper;


import static com.revealprecision.revealserver.constants.EntityTagDataTypes.BOOLEAN;
import static com.revealprecision.revealserver.constants.EntityTagDataTypes.DOUBLE;
import static com.revealprecision.revealserver.constants.EntityTagDataTypes.INTEGER;
import static com.revealprecision.revealserver.constants.EntityTagDataTypes.STRING;

import com.revealprecision.revealserver.api.v1.dto.factory.EntityTagEventFactory;
import com.revealprecision.revealserver.api.v1.dto.request.EntityTagItem;
import com.revealprecision.revealserver.api.v1.dto.request.EntityTagRequest;
import com.revealprecision.revealserver.exceptions.FileFormatException;
import com.revealprecision.revealserver.exceptions.NotFoundException;
import com.revealprecision.revealserver.messaging.message.EntityTagEvent;
import com.revealprecision.revealserver.persistence.domain.EntityTag;
import com.revealprecision.revealserver.persistence.domain.Location;
import com.revealprecision.revealserver.persistence.domain.LocationHierarchy;
import com.revealprecision.revealserver.persistence.domain.MetadataImport;
import com.revealprecision.revealserver.persistence.domain.User;
import com.revealprecision.revealserver.persistence.domain.metadata.metadataImport.MetaImportDTO;
import com.revealprecision.revealserver.persistence.domain.metadata.metadataImport.SheetData;
import com.revealprecision.revealserver.service.EntityTagService;
import com.revealprecision.revealserver.service.LocationHierarchyService;
import com.revealprecision.revealserver.service.LocationService;
import com.revealprecision.revealserver.util.UserUtils;
import java.security.Principal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class MetaFieldSetMapper {

  private final EntityTagService entityTagService;
  private final LocationService locationService;
  private final LocationHierarchyService locationHierarchyService;

  public List<MetaImportDTO> mapMetaFieldsDB(Map<String, EntityTagEvent> entityTagMap,
      XSSFSheet sheet, MetadataImport currentMetaImport) throws FileFormatException {
    //starting from 1st
    int fileRowsCount = getFileRowsCount(sheet);

    XSSFRow tagNameRow = sheet.getRow(0);

    Set<UUID> locationList = extractIdsFor(sheet, fileRowsCount, 1, "Location");
    Set<UUID> hierarchyList = extractIdsFor(sheet, fileRowsCount, 0, "Hierarchy");

    int rowCount = getRowCount(sheet, fileRowsCount);

    Map<UUID, Location> locationMap = validateLocationsAndReturnLocationMap(
        locationList);

    Map<UUID, LocationHierarchy> hierarchyMap = validateHierarchiesAndReturnHierarchyMap(
        hierarchyList);

    return extractMetadata(
        sheet, tagNameRow, entityTagMap, rowCount, locationMap, hierarchyMap);
  }

  public Map<String, EntityTagEvent> getTagsMap(XSSFSheet sheet,
      MetadataImport currentMetaImport, XSSFRow tagNameRow, int physicalNumberOfCells,
      User currentUser) {
    XSSFRow tagDataTypeRow = sheet.getRow(1);

    XSSFRow headerRow = sheet.getRow(2);

    Map<String, TagHelper> metadataTagNames = IntStream.range(4, physicalNumberOfCells)
        .mapToObj(i -> new TagHelper(tagNameRow.getCell(i).getStringCellValue(),
            tagDataTypeRow.getCell(i).getStringCellValue(),
            UserUtils.getCurrentPrinciple())
        )
        .takeWhile(Objects::nonNull)
        .collect(Collectors.toMap(TagHelper::getTagName, i -> i, (v, v1) -> v1));

    return validateTagNamesAndReturnMap(
        metadataTagNames, currentMetaImport, currentUser);
  }

  private List<MetaImportDTO> extractMetadata(XSSFSheet sheet, XSSFRow tagNameRow,
      Map<String, EntityTagEvent> entityTagMap, int rowCount,
      Map<UUID, Location> locationMap, Map<UUID, LocationHierarchy> hierarchyMap) {
    List<MetaImportDTO> metaImportDTOS = new ArrayList<>();
    boolean searchedLocationHierarchy = false;

    LocationHierarchy locationHierarchy = null;

    for (int i = 3; i < rowCount + 1; i++) {
      XSSFRow dataRow = sheet.getRow(i);
      MetaImportDTO metaImportDTO = new MetaImportDTO();

      if (dataRow != null) {
        if (!searchedLocationHierarchy) {
          locationHierarchy = getLocationHierarchy(
              hierarchyMap, i, dataRow, 0);
          searchedLocationHierarchy = true;
        }

        metaImportDTO.setLocationHierarchy(locationHierarchy);

        metaImportDTO.setLocation(getLocation(locationMap, i,
            dataRow, 1));

        SheetData sheetData = setMetadata(tagNameRow, entityTagMap, dataRow);
        metaImportDTO.setSheetData(sheetData);

      } else {
        log.info("Exiting file processing loop...empty dataRow encountered!!");
        break;
      }
      metaImportDTOS.add(metaImportDTO);
    }
    return metaImportDTOS;
  }

  private SheetData setMetadata(XSSFRow tagNameRow, Map<String, EntityTagEvent> entityTagMap,
      XSSFRow dataRow) {
    EntityTagEvent entityTag;
    SheetData sheetData = new SheetData();

    Map<String, EntityTagEvent> onlyNonAggregateTags = entityTagMap.entrySet().stream()
        .filter(entry -> !entry.getValue().isAggregate()).collect(
            Collectors.toMap(Entry::getKey, Entry::getValue));
    for (int j = 4; j < 4 + onlyNonAggregateTags.size(); j++) {
      Object value = null;
      Object sheetValue;
      if (dataRow.getCell(j).getRawValue() == null) {
        log.warn("Empty cell encountered: coord: {},{}", dataRow.getRowNum(), j);
      } else {
        entityTag = onlyNonAggregateTags.get(
            tagNameRow.getCell(j).getStringCellValue());
        sheetValue = getSheetValue(dataRow, j);
        log.trace("sheet data type = {}", dataRow.getCell(j).getCellType());
        log.trace("sheet value = {}", sheetValue);
        log.trace("entity type= {}", entityTag.getValueType());
        try {
          value = getEntityValueFromSheetValues(entityTag, sheetValue);
        } catch (IllegalArgumentException | ClassCastException e) {
          sheetData.getErrors().put(entityTag, e.getMessage());
        }

        sheetData.getRawEntityData()
            .put(entityTag, sheetValue);
        sheetData.getConvertedEntityData()
            .put(entityTag, value);
      }

    }
    return sheetData;
  }

  private Location getLocation(Map<UUID, Location> locationMap, int i, XSSFRow dataRow, int j) {
    Location loc = null;
    try {
      loc = locationMap.get(
          UUID.fromString(dataRow.getCell(j).toString()));
    } catch (NotFoundException notFoundException) {
      log.warn("location not  not found or not uuid coord: {},{}", i, j);
    }
    return loc;
  }

  private LocationHierarchy getLocationHierarchy(Map<UUID, LocationHierarchy> hierarchyMap, int i,
      XSSFRow dataRow, int j) {
    LocationHierarchy locationHierarchy = null;
    try {
      locationHierarchy = hierarchyMap.get(
          UUID.fromString(dataRow.getCell(j).toString()));

    } catch (NotFoundException | IllegalArgumentException exception) {
      log.warn("locationHierarchy not found or not uuid coord: {},{}", i, j);
    }
    return locationHierarchy;
  }

  private Object getEntityValueFromSheetValues(EntityTagEvent entityTag, Object sheetValue) {
    Object value;
    switch (entityTag.getValueType()) {
      case INTEGER:
        value = Double.valueOf((double) sheetValue).intValue();
        break;
      case DOUBLE:
        value = (double) sheetValue;
        break;
      case BOOLEAN:
        value = String.valueOf(sheetValue);
        break;
      case STRING:
      default:
        value = String.valueOf(sheetValue);
        break;
    }
    return value;
  }

  private Object getSheetValue(XSSFRow dataRow, int j) {
    Object sheetValue;
    switch (dataRow.getCell(j).getCellType()) {
      case STRING:
        sheetValue = dataRow.getCell(j).getStringCellValue();
        break;
      case NUMERIC:
        sheetValue = dataRow.getCell(j).getNumericCellValue();
        break;
      case BOOLEAN:
        sheetValue = String.valueOf(dataRow.getCell(j).getBooleanCellValue());
        break;
      case FORMULA:
        try {
          sheetValue = dataRow.getCell(j).getNumericCellValue();
        } catch (IllegalArgumentException e) {
          try {
            sheetValue = dataRow.getCell(j).getStringCellValue();
          } catch (IllegalArgumentException e2) {
            try {
              sheetValue = String.valueOf(dataRow.getCell(j).getBooleanCellValue());
            } catch (IllegalArgumentException e3) {
              throw new FileFormatException(
                  "File contains formular at pos [" + dataRow.getCell(j).getReference()
                      + "] which cannot be resolved to a string, numeric or boolean");
            }
          }
        }
        break;
      default:
        sheetValue = dataRow.getCell(j).getRawValue();

    }
    return sheetValue;
  }

  private Map<String, EntityTagEvent> validateTagNamesAndReturnMap(
      Map<String, TagHelper> metadataTagNames, MetadataImport currentMetaImport, User currentUser) {

    Set<EntityTag> entityTagsByTags = entityTagService.getEntityTagsByTagNames(
        metadataTagNames.keySet());

    List<EntityTag> tagsNotOwnerOf = entityTagsByTags.stream().filter(
        entityTag -> entityTag.getOwners().stream()
            .anyMatch(owner -> owner.getUserSid().equals(currentUser.getSid()))).collect(
        Collectors.toList());

    if (tagsNotOwnerOf.size() > 0) {

      throw new RuntimeException(
          "You cannot import data to tags " + tagsNotOwnerOf.stream().map(EntityTag::getTag)
              .collect(Collectors.joining(",")) + " as you are not the owner of it"
          );
    }

    Set<EntityTag> entityTagsByTagsReferringTo = entityTagService.findEntityTagsByReferencedTagIn(
        entityTagsByTags.stream().map(EntityTag::getIdentifier).collect(Collectors.toList()));

    entityTagsByTags.addAll(entityTagsByTagsReferringTo);

    Set<String> entityTagsByTagNames = entityTagsByTags.stream().map(EntityTag::getTag)
        .collect(Collectors.toSet());

    Map<String, TagHelper> toCreate = metadataTagNames.entrySet().stream()
        .filter(entityTagEntry -> !entityTagsByTagNames.contains(entityTagEntry.getKey()))
        .collect(Collectors.toMap(Entry::getKey, Entry::getValue));

    List<EntityTag> createdTags = toCreate.entrySet().stream().flatMap(tag -> {

      String valueType = "";
      switch (tag.getValue().getDataType()) {
        case "string": {
          valueType = STRING;
          break;
        }
        case "number": {
          valueType = DOUBLE;
          break;
        }
        case "boolean": {
          valueType = BOOLEAN;
          break;
        }
        default:
      }

      EntityTagRequest entityTagRequest = EntityTagRequest.builder()
          .valueType(valueType)
          .isAggregate(false)
          .tags(List.of(new EntityTagItem(tag.getKey())))
          .definition(tag.getKey())
          .metadataImport(currentMetaImport)
          .build();

      return entityTagService.createEntityTagsSkipExisting(entityTagRequest, true).stream();
    }).collect(Collectors.toList());

    Map<String, EntityTagEvent> createdEntityTagEvents = createdTags.stream().map(entityTag -> {
      EntityTagEvent entityTagEvent = EntityTagEventFactory.getEntityTagEvent(entityTag);
      entityTagEvent.setCreated(true);
      return entityTagEvent;
    }).collect(Collectors.toMap(EntityTagEvent::getTag, v -> v));

    Map<String, EntityTagEvent> existingEntityTagEvents = entityTagsByTags.stream()
        .map(entityTag -> {
          EntityTagEvent entityTagEvent = EntityTagEventFactory.getEntityTagEvent(entityTag);
          entityTagEvent.setCreated(false);
          return entityTagEvent;
        }).collect(Collectors.toMap(EntityTagEvent::getTag, v -> v));

    Map<String, EntityTagEvent> allEntityTagEvents = new HashMap<>();
    allEntityTagEvents.putAll(existingEntityTagEvents);
    allEntityTagEvents.putAll(createdEntityTagEvents);

    return allEntityTagEvents;
  }

  private Set<UUID> extractIdsFor(XSSFSheet sheet, int fileRowsCount, int cellPosition,
      String type) {
    Set<UUID> list = new HashSet<>();
    int rowCount = 0;
    for (int i = 3; i < fileRowsCount; i++) {

      XSSFRow dataRow = sheet.getRow(i);
      if (dataRow != null && dataRow.getCell(cellPosition) != null) {
        rowCount++;
        try {
          log.trace("dataRow.getCell({}) {}", cellPosition, dataRow.getCell(cellPosition));
          list.add(UUID.fromString(dataRow.getCell(cellPosition).toString()));
        } catch (IllegalArgumentException e) {
          log.warn("{} Id is not a uuid in row: {}", type, i);
          throw new FileFormatException(type + " Id is not a uuid in row: " + i);
        }
      } else {
        log.info("Exiting file processing loop...empty dataRow encountered!!");
        break;
      }
    }
    return list;
  }

  private int getRowCount(XSSFSheet sheet, int fileRowsCount) {
    int rowCount = 0;
    for (int i = 1; i < fileRowsCount; i++) {

      XSSFRow dataRow = sheet.getRow(i);
      if (dataRow != null && dataRow.getCell(0) != null) {
        rowCount++;
      } else {
        log.info("Exiting file processing loop...empty dataRow encountered!!");
        break;
      }
    }
    return rowCount;
  }

  private Map<UUID, LocationHierarchy> validateHierarchiesAndReturnHierarchyMap(
      Set<UUID> hierarchyList) {
    Set<LocationHierarchy> locationHierarchies = locationHierarchyService.getLocationHierarchiesIn(
        hierarchyList);
    if (hierarchyList.size() > locationHierarchies.size()) {
      throw new FileFormatException("Not all hierarchies passed found in Reveal");
    }
    return locationHierarchies.stream()
        .collect(Collectors.toMap(LocationHierarchy::getIdentifier, hierarchy -> hierarchy));
  }

  private Map<UUID, Location> validateLocationsAndReturnLocationMap(Set<UUID> locationList) {

    Set<Location> locations = locationService.findLocationsWithoutGeoJsonByIdentifierIn(
        locationList);
    if (locationList.size() > locations.size()) {
      throw new FileFormatException("Not all locations passed found in Reveal");
    }

    return locations.stream()
        .collect(Collectors.toMap(Location::getIdentifier, location -> location));
  }

  public int getPhysicalNumberOfCells(XSSFRow headerRow) {
    int physicalNumberOfCells = headerRow.getPhysicalNumberOfCells();
    if (physicalNumberOfCells > 1) {
      log.info("Import file has metadata columns");
    } else {
      throw new FileFormatException(
          "File is does not have any metadata columns.");
    }
    int cellCount = 0;
    XSSFCell cell = headerRow.getCell(cellCount);
    while (cell != null || cellCount < 4) {
      cellCount++;
      cell = headerRow.getCell(cellCount);
    }

    return cellCount;
  }

  public int getFileRowsCount(XSSFSheet sheet) {
    int fileRowsCount = sheet.getPhysicalNumberOfRows();
    if (fileRowsCount > 1) {
      log.info("Import file has records {}", fileRowsCount);
    } else {
      throw new FileFormatException(
          "File is empty or not valid.");
    }
    return fileRowsCount;
  }

}

@Setter
@Getter
@AllArgsConstructor
class TagHelper {

  private String tagName;
  private String dataType;
  private Principal owner;
}
