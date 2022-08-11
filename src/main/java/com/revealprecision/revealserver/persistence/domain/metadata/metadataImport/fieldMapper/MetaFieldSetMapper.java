package com.revealprecision.revealserver.persistence.domain.metadata.metadataImport.fieldMapper;


import static com.revealprecision.revealserver.constants.EntityTagDataTypes.BOOLEAN;
import static com.revealprecision.revealserver.constants.EntityTagDataTypes.DOUBLE;
import static com.revealprecision.revealserver.constants.EntityTagDataTypes.INTEGER;
import static com.revealprecision.revealserver.constants.EntityTagDataTypes.STRING;

import com.revealprecision.revealserver.api.v1.dto.factory.EntityTagEventFactory;
import com.revealprecision.revealserver.exceptions.FileFormatException;
import com.revealprecision.revealserver.exceptions.NotFoundException;
import com.revealprecision.revealserver.messaging.message.EntityTagEvent;
import com.revealprecision.revealserver.persistence.domain.EntityTag;
import com.revealprecision.revealserver.persistence.domain.Location;
import com.revealprecision.revealserver.persistence.domain.LocationHierarchy;
import com.revealprecision.revealserver.persistence.domain.metadata.metadataImport.MetaImportDTO;
import com.revealprecision.revealserver.service.EntityTagService;
import com.revealprecision.revealserver.service.LocationHierarchyService;
import com.revealprecision.revealserver.service.LocationService;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

  public List<MetaImportDTO> mapMetaFields(XSSFSheet sheet) throws FileFormatException {
    //starting from 1st
    int fileRowsCount = sheet.getPhysicalNumberOfRows();
    if (fileRowsCount > 1) {
      log.info("Import file has records");
    } else {
      throw new FileFormatException(
          "File is empty or not valid.");
    }
    XSSFRow headerRow = sheet.getRow(0);

    int physicalNumberOfCells = headerRow.getPhysicalNumberOfCells();
    if (physicalNumberOfCells > 4) {
      log.info("Import file has metadata columns");
    } else {
      throw new FileFormatException(
          "File is does not have any metadata columns.");
    }

    Set<String> metadataTagNames = IntStream.range(4, physicalNumberOfCells)
        .mapToObj(i -> headerRow.getCell(i).getStringCellValue())
        .takeWhile(Objects::nonNull)
        .collect(Collectors.toSet());

    Set<EntityTag> entityTagsByTagNames = entityTagService.getEntityTagsByTagNames(
        metadataTagNames);

    if (metadataTagNames.size() > entityTagsByTagNames.size()) {
      throw new FileFormatException(
          "File metadata columns are not entity tags on reveal server or column names are invalid.");
    } else {
      log.info("Import file has metadata columns that are valid");
    }

    Map<String, EntityTagEvent> entityTagMap = entityTagsByTagNames.stream()
        .map(EntityTagEventFactory::getEntityTagEvent)
        .collect(Collectors.toMap(EntityTagEvent::getTag, a -> a));

    Set<UUID> locationList = new HashSet<>();
    Set<UUID> hierarchyList = new HashSet<>();
    int rowCount = 0;
    for (int i = 1; i < fileRowsCount; i++) {

      XSSFRow dataRow = sheet.getRow(i);
      if (dataRow != null) {
        rowCount++;
        try {
          hierarchyList.add(UUID.fromString(dataRow.getCell(0).toString()));
        } catch (IllegalArgumentException e) {
          log.warn("Hierarchy Id is not a uuid in row: {}", i);
          throw new FileFormatException("Hierarchy Id is not a uuid in row: " + i);

        }
        try {
          locationList.add(UUID.fromString(dataRow.getCell(1).toString()));
        } catch (IllegalArgumentException e) {
          log.warn("Location Id is not a uuid in row: {}", i);
          throw new FileFormatException("Location Id is not a uuid in row: " + i);
        }
      } else{
        log.info("Exiting file processing loop...empty dataRow encountered!!");
        break;
      }
    }

    Set<Location> locations = locationService.findLocationsWithoutGeoJsonByIdentifierIn(locationList);
    Set<LocationHierarchy> locationHierarchies = locationHierarchyService.getLocationHierarchiesIn(
        hierarchyList);

    if (locationList.size() > locations.size()) {
      throw new FileFormatException("Not all locations passed found in Reveal");
    }

    if (hierarchyList.size() > locationHierarchies.size()) {
      throw new FileFormatException("Not all hierarchies passed found in Reveal");
    }

    Map<UUID, Location> locationMap = locations.stream()
        .collect(Collectors.toMap(Location::getIdentifier, location -> location));

    Map<UUID, LocationHierarchy> hierarchyMap = locationHierarchies.stream()
        .collect(Collectors.toMap(LocationHierarchy::getIdentifier, hierarchy -> hierarchy));

    List<MetaImportDTO> metaImportDTOS = new ArrayList<>();
    boolean searchedLocationHierarchy = false;
    LocationHierarchy locationHierarchy = null;
    for (int i = 1; i < rowCount+1; i++) {
      XSSFRow dataRow = sheet.getRow(i);
      MetaImportDTO metaImportDTO = new MetaImportDTO();
      if (dataRow != null) {
        for (int j = 0; j < 4 + metadataTagNames.size(); j++) {
          if (dataRow.getCell(j).getRawValue() == null) {
            log.warn("Empty cell encountered: coord: {},{}", i, j);
          } else {
            switch (j) {
              case 0:
                if (!searchedLocationHierarchy) {
                  try {
                    locationHierarchy = hierarchyMap.get(
                        UUID.fromString(dataRow.getCell(j).toString()));
                    searchedLocationHierarchy = true;
                  } catch (NotFoundException | IllegalArgumentException exception) {
                    log.warn("locationHierarchy not found or not uuid coord: {},{}", i, j);
                  }
                }
                metaImportDTO.setLocationHierarchy(locationHierarchy);
                break;
              case 1:
                try {
                  Location loc = locationMap.get(
                      UUID.fromString(dataRow.getCell(j).toString()));
                  metaImportDTO.setLocation(loc);
                } catch (NotFoundException notFoundException) {
                  log.warn("location not  not found or not uuid coord: {},{}", i, j);
                }
                break;
              default:

                EntityTagEvent entityTag = entityTagMap.get(
                    headerRow.getCell(j).getStringCellValue());

                Object sheetValue;
                switch (dataRow.getCell(j).getCellType()) {
                  case STRING:
                    sheetValue = dataRow.getCell(j).getStringCellValue();
                    break;
                  case NUMERIC:
                    sheetValue = dataRow.getCell(j).getNumericCellValue();
                    break;
                  case BOOLEAN:
                    sheetValue = dataRow.getCell(j).getBooleanCellValue();
                    break;
                  default:
                    sheetValue = dataRow.getCell(j).getRawValue();

                }

                metaImportDTO.getRawEntityData()
                    .put(entityTag, sheetValue);

                Object value;

                try {
                  switch (entityTag.getValueType()) {
                    case INTEGER:
                      value = Double.valueOf((double)sheetValue).intValue();
                      break;
                    case DOUBLE:
                      value = (double) sheetValue;
                      break;
                    case BOOLEAN:
                      value = Boolean.valueOf((boolean) sheetValue);
                      break;
                    case STRING:
                    default:
                      value = String.valueOf(sheetValue);
                      break;
                  }
                  metaImportDTO.getConvertedEntityData()
                      .put(entityTag, value);

                } catch (IllegalArgumentException | ClassCastException e) {
                  metaImportDTO.getErrors().put(entityTag, e.getMessage());
                }
                break;
            }
          }
        }
      } else {
        log.info("Exiting file processing loop...empty dataRow encountered!!");
        break;
      }
      metaImportDTOS.add(metaImportDTO);
    }

    return metaImportDTOS;
  }


}
