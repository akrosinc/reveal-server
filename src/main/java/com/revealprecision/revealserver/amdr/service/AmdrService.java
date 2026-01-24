package com.revealprecision.revealserver.amdr.service;

import com.revealprecision.revealserver.amdr.api.v1.dto.response.AdmrImportResultsResponse;
import com.revealprecision.revealserver.amdr.api.v1.dto.response.AmdrImportResponse;
import com.revealprecision.revealserver.amdr.model.KeyValue;
import com.revealprecision.revealserver.amdr.persistence.domain.AmdrData;
import com.revealprecision.revealserver.amdr.persistence.domain.AmdrImport;
import com.revealprecision.revealserver.amdr.persistence.domain.AmdrMappings;
import com.revealprecision.revealserver.amdr.persistence.domain.AmdrProcessingStatus;
import com.revealprecision.revealserver.amdr.persistence.domain.AmdrSampleData;
import com.revealprecision.revealserver.amdr.persistence.projection.AmdrEventSampleProjection;
import com.revealprecision.revealserver.amdr.persistence.projection.AmdrPassiveEventProjection;
import com.revealprecision.revealserver.amdr.persistence.repository.AmdrImportRepository;
import com.revealprecision.revealserver.amdr.persistence.repository.AmdrMappingsRepository;
import com.revealprecision.revealserver.amdr.persistence.repository.AmdrRepository;
import com.revealprecision.revealserver.amdr.persistence.repository.AmdrSampleDataRepository;
import com.revealprecision.revealserver.api.v1.dto.factory.amdr.AmdrImportResponseFactory;
import com.revealprecision.revealserver.enums.EntityStatus;
import com.revealprecision.revealserver.exceptions.FileFormatException;
import com.revealprecision.revealserver.exceptions.NotFoundException;
import com.revealprecision.revealserver.persistence.domain.LocationHierarchy;
import com.revealprecision.revealserver.persistence.domain.User;
import com.revealprecision.revealserver.persistence.projection.LocationMainDataWithGeo;
import com.revealprecision.revealserver.persistence.repository.LocationRepository;
import com.revealprecision.revealserver.service.LocationHierarchyService;
import com.revealprecision.revealserver.service.StorageService;
import com.revealprecision.revealserver.service.UserService;
import com.revealprecision.revealserver.util.UserUtils;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.Principal;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import javax.annotation.Nullable;
import javax.persistence.PersistenceException;
import javax.ws.rs.BadRequestException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.ClientAnchor;
import org.apache.poi.ss.usermodel.Comment;
import org.apache.poi.ss.usermodel.CreationHelper;
import org.apache.poi.ss.usermodel.DataFormat;
import org.apache.poi.ss.usermodel.DataValidation;
import org.apache.poi.ss.usermodel.DataValidationConstraint;
import org.apache.poi.ss.usermodel.DataValidationHelper;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Drawing;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.util.CellRangeAddressList;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.keycloak.KeycloakPrincipal;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.dao.DataAccessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class AmdrService {

  private final LocationRepository locationRepository;
  private final StorageService storageService;
  private final LocationHierarchyService locationHierarchyService;
  private final AmdrRepository amdrRepository;
  private final AmdrMappingsRepository amdrMappingsRepository;
  private final UserService userService;
  private final AmdrImportRepository amdrImportRepository;
  private final AmdrSampleDataRepository amdrSampleDataRepository;

  public ByteArrayResource downloadAmdrImportTemplate(UUID hierarchyIdentifier,
      String geographicLevelName, String amdrKey)
      throws IOException {
    LocationHierarchy locationHierarchy = locationHierarchyService.findByIdentifier(
        hierarchyIdentifier);

    int index = locationHierarchy.getNodeOrder().indexOf(geographicLevelName);

    if (index == -1) {
      throw new NotFoundException("GeoLevel not found");
    }

    ArrayList<String> nodes = IntStream.range(0, index + 1)
        .mapToObj(locationHierarchy.getNodeOrder()::get)
        .collect(Collectors.toCollection(ArrayList::new));

    List<LocationMainDataWithGeo> collect = nodes.stream().flatMap(node ->
        locationRepository.findLocationMainDataByGeographicLevelIdentifier(node).stream()
    ).collect(Collectors.toList());

    File currDir = new File(".");
    String path = currDir.getAbsolutePath();
    String fileLocation =
        path.substring(0, path.length() - 1) + "temp" + UUID.randomUUID().toString() + ".xlsx";

    try (XSSFWorkbook workbook = new XSSFWorkbook();

        FileOutputStream outputStream = new FileOutputStream(fileLocation)) {
      Sheet sheet = workbook.createSheet("Locations");
      sheet.setColumnWidth(0, 11000);
      sheet.setColumnWidth(1, 10000);
      sheet.setColumnWidth(2, 6500);
      sheet.setColumnWidth(3, 8000);

      CellStyle textStyle = workbook.createCellStyle();
      DataFormat dataFormat = workbook.createDataFormat();
      textStyle.setDataFormat(dataFormat.getFormat("@"));
      sheet.setDefaultColumnStyle(0, textStyle);

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

      CellStyle style = workbook.createCellStyle();
      style.setWrapText(true);

      Row header = sheet.createRow(rowIndex);
      header.setRowStyle(headerStyle);

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

      headerCell = header.createCell(4);
      headerCell.setCellValue("Date");
      headerCell.setCellStyle(headerStyle);

      DataValidationHelper dvHelper = sheet.getDataValidationHelper();
      DataValidationConstraint dvConstraint = dvHelper.createDateConstraint(
          DataValidationConstraint.OperatorType.LESS_OR_EQUAL,
          "TODAY()", // Upper bound: today
          null,      // No lower bound
          "yyyy-MM-dd"
      );

      headerCell = header.createCell(5);
      headerCell.setCellValue(amdrKey);
      headerCell.setCellStyle(headerStyle);

      int headerIndex = 6;
      AmdrMappings byAmdrKey = amdrMappingsRepository.findFirstByAmdrKey(amdrKey);
      for (String el : byAmdrKey.getAmdrSubKeys()) {

        sheet.setColumnWidth(headerIndex, 9600);
        Cell tagNameRowCell = header.createCell(headerIndex);
        tagNameRowCell.setCellValue(el);
        tagNameRowCell.setCellStyle(headerStyle);
        headerIndex++;
      }

      rowIndex++;

      for (LocationMainDataWithGeo location : collect) {

        CellRangeAddressList addressList = new CellRangeAddressList(rowIndex, rowIndex, 4,
            4); // A2:A101
        DataValidation validation = dvHelper.createValidation(dvConstraint, addressList);

        Row row = sheet.createRow(rowIndex);
        validation.setErrorStyle(DataValidation.ErrorStyle.STOP);
        validation.setShowErrorBox(true);
        validation.createErrorBox(
            "Invalid Date",
            "Please enter a date that is today or earlier."
        );

        CreationHelper createHelper = workbook.getCreationHelper();
        ClientAnchor anchor = createHelper.createClientAnchor();
        anchor.setCol1(4); // Start column of the comment
        anchor.setRow1(rowIndex); // Start row of the comment
        anchor.setCol2(4); // End column of the comment
        anchor.setRow2(rowIndex); // End row of the comment
        Drawing<?> drawing = sheet.createDrawingPatriarch();
        // Create the cell comment
        Comment comment = drawing.createCellComment(anchor);
        comment.setString(createHelper.createRichTextString("Double click for date picker"));

        // Add to sheet
        sheet.addValidationData(validation);

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
        cell.setCellValue(location.getGeographicLevelName());
        cell.setCellStyle(style);
        rowIndex++;
      }

      workbook.write(outputStream);
    }

    Path filePath = Paths.get(fileLocation);

    ByteArrayResource resource = new ByteArrayResource(Files.readAllBytes(filePath));
    storageService.deleteFile(fileLocation);
    return resource;
  }

  public List<String> getAmdrKeys() {
    return amdrMappingsRepository.getAmdrKeys();
  }


  public void saveImportFile(String file, String fileName) throws DataAccessException {

    User currentUser = userService.getCurrentUser();

    AmdrImport amdrImport = new AmdrImport();
    amdrImport.setFilename(fileName);
    amdrImport.setEntityStatus(EntityStatus.ACTIVE);
    amdrImport.setUploadedDatetime(LocalDateTime.now());
    amdrImport.setUploadedBy(currentUser.getSid().toString());

    Principal principal = UserUtils.getCurrentPrinciple();
    User user;
    UUID keycloakId = null;
    if (principal instanceof KeycloakPrincipal) {
      keycloakId = UUID.fromString(principal.getName());
    }
    user = userService.getByKeycloakId(keycloakId);
    amdrImport.setUploadedBy(user.getUsername());
    amdrImport.setStatus(AmdrProcessingStatus.BUSY);

    AmdrImport currentMetaImport = amdrImportRepository.save(amdrImport);

    int rowCounter = 0;
    try (XSSFWorkbook workbook = new XSSFWorkbook(file)) {
      XSSFSheet sheet = workbook.getSheetAt(0);

      XSSFRow headerRow = sheet.getRow(rowCounter);

      XSSFCell locationHierarchyCell = headerRow.getCell(0);
      XSSFCell locationIdentifierCell = headerRow.getCell(1);
      XSSFCell locationNameCell = headerRow.getCell(2);
      XSSFCell locationGeographicLevelCell = headerRow.getCell(3);
      XSSFCell locationDateCell = headerRow.getCell(4);
      XSSFCell locationAmdrKeyCell = headerRow.getCell(5);

      String key = getStringValue(locationAmdrKeyCell);

      if (key != null) {
        List<List<String>> amdrSubKeysByAmdrKey = amdrMappingsRepository.getAmdrSubKeysByAmdrKey(
            key);

        rowCounter += 1;
        XSSFRow row = sheet.getRow(rowCounter);

        if (!amdrSubKeysByAmdrKey.isEmpty()) {
          List<String> subKeys = amdrSubKeysByAmdrKey.get(0);

          List<AmdrData> amdrDataList = new ArrayList<>();

          while (row != null && row.getCell(0) != null && checkCell(row.getCell(0))) {

            XSSFRow finalRow = row;

            int cellCounter = 6;
            XSSFCell dataCell = finalRow.getCell(cellCounter);
            Map<String, String> subKeyMap = new HashMap<>();
            while (checkCell(dataCell)) {

              XSSFCell headerRowCell = headerRow.getCell(cellCounter);
              if (checkCell(headerRowCell)) {
                if (checkCell(dataCell) && getStringValue(dataCell) != null) {
                  subKeyMap.put(headerRowCell.getStringCellValue(), getStringValue(dataCell));
                } else {
                  subKeyMap.put(headerRowCell.getStringCellValue(), "0");
                }
              }
              cellCounter += 1;
              dataCell = finalRow.getCell(cellCounter);
            }
            ;

            if (!subKeyMap.isEmpty()) {
              XSSFCell keyRowCell = headerRow.getCell(5);

              if (checkCell(keyRowCell)) {

                List<KeyValue> subKeyValues = subKeyMap.entrySet().stream()
                    .map(subKeyEntry -> KeyValue
                        .builder()
                        .key(subKeyEntry.getKey())
                        .number(subKeyEntry.getValue())
                        .build()).collect(Collectors.toList());

                String locationId =
                    checkCell(finalRow.getCell(1)) ? getStringValue(finalRow.getCell(1)) : null;
                String overallValue =
                    checkCell(finalRow.getCell(5)) ? getStringValue(finalRow.getCell(5)) : null;

                String date =
                    checkCell(finalRow.getCell(4)) ? getStringValue(finalRow.getCell(4)) : null;

                if (overallValue != null && locationId != null && date != null) {
                  AmdrData amdrData = AmdrData
                      .builder()
                      .type(getStringValue(keyRowCell) == null ? "0" : getStringValue(keyRowCell))
                      .data(subKeyValues)
                      .overallValue(overallValue)
                      .locationId(UUID.fromString(locationId))
                      .datetime(LocalDate.parse(date, DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                          .atStartOfDay())
                      .build();
                  amdrDataList.add(amdrData);
                } else {
                  amdrImport.setStatus(AmdrProcessingStatus.FAILED);
                  amdrImportRepository.save(amdrImport);
                  throw new BadRequestException("Error with location Id on line: " + rowCounter);
                }
              }
            }

            rowCounter += 1;
            row = sheet.getRow(rowCounter);
          }
          amdrRepository.saveAll(amdrDataList);
        }

        try {
          amdrRepository.refreshAmdrImportData();
          amdrRepository.summarizeImportData();
        } catch (DataAccessException | PersistenceException e) {
          log.error(e.getMessage(), e);

          amdrImport.setStatus(AmdrProcessingStatus.FAILED);
          amdrImportRepository.save(amdrImport);
          throw e;
        }

        amdrImport.setStatus(AmdrProcessingStatus.SUCCESSFUL);
        amdrImportRepository.save(amdrImport);
      } else {

        amdrImport.setStatus(AmdrProcessingStatus.FAILED);
        amdrImportRepository.save(amdrImport);
        throw new FileFormatException("AMDR Key is invalid");
      }

    } catch (Exception e) {
      log.error(e.getMessage(), e);

      amdrImport.setStatus(AmdrProcessingStatus.FAILED);
      amdrImportRepository.save(amdrImport);
      throw new FileFormatException(e.getMessage());
    }
  }

  public void saveImportRawFile(String file, String fileName) throws IOException {

    User currentUser = userService.getCurrentUser();

    AmdrImport amdrImport = new AmdrImport();
    amdrImport.setFilename(fileName);
    amdrImport.setEntityStatus(EntityStatus.ACTIVE);
    amdrImport.setUploadedDatetime(LocalDateTime.now());
    amdrImport.setUploadedBy(currentUser.getSid().toString());

    Principal principal = UserUtils.getCurrentPrinciple();
    User user;
    UUID keycloakId = null;
    if (principal instanceof KeycloakPrincipal) {
      keycloakId = UUID.fromString(principal.getName());
    }
    user = userService.getByKeycloakId(keycloakId);
    amdrImport.setUploadedBy(user.getUsername());
    amdrImport.setStatus(AmdrProcessingStatus.SAVING_DATA);

    AmdrImport currentMetaImport = amdrImportRepository.save(amdrImport);

    int rowCounter = 0;
    try (XSSFWorkbook workbook = new XSSFWorkbook(file)) {
      XSSFSheet sheet = workbook.getSheetAt(0);

      XSSFRow headerRow = sheet.getRow(rowCounter);

      Map<String, Integer> columnPositions = new HashMap<>();

      for (int i = 0; i < 19; i++) {

        XSSFCell headerRowCell = headerRow.getCell(i);
        String stringValue = getStringValue(headerRowCell);

        if (stringValue == null) {
          continue; // skip empty cells
        }

        switch (stringValue) {
          case "Sample.Internal.ID":
            columnPositions.put("Sample.Internal.ID", i);
            break;
          case "Kelch":
            columnPositions.put("Kelch", i);
            break;
          case "PfCRT:72":
            columnPositions.put("PfCRT:72", i);
            break;
          case "PfCRT:74":
            columnPositions.put("PfCRT:74", i);
            break;
          case "PfCRT:75":
            columnPositions.put("PfCRT:75", i);
            break;
          case "PfCRT:76":
            columnPositions.put("PfCRT:76", i);
            break;
          case "PfDHFR:51":
            columnPositions.put("PfDHFR:51", i);
            break;
          case "PfDHFR:59":
            columnPositions.put("PfDHFR:59", i);
            break;
          case "PfDHFR:108":
            columnPositions.put("PfDHFR:108", i);
            break;
          case "PfDHFR:164":
            columnPositions.put("PfDHFR:164", i);
            break;
          case "PfDHPS:436":
            columnPositions.put("PfDHPS:436", i);
            break;
          case "PfDHPS:437":
            columnPositions.put("PfDHPS:437", i);
            break;
          case "PfDHPS:540":
            columnPositions.put("PfDHPS:540", i);
            break;
          case "PfDHPS:581":
            columnPositions.put("PfDHPS:581", i);
            break;
          case "PfDHPS:613":
            columnPositions.put("PfDHPS:613", i);
            break;
          case "PfMDR1:86":
            columnPositions.put("PfMDR1:86", i);
            break;
          case "PfMDR1:184":
            columnPositions.put("PfMDR1:184", i);
            break;
          case "PfMDR1:1246":
            columnPositions.put("PfMDR1:1246", i);
            break;
          case "Region":
            columnPositions.put("Region", i);
            break;
          case "Year_collection":
            columnPositions.put("Year_collection", i);
            break;
          default:
            // Unknown column — do nothing or log
            break;
        }
      }

      rowCounter += 1;
      XSSFRow row = sheet.getRow(rowCounter);

      List<AmdrSampleData> amdrDataList = new ArrayList<>();

      while (row != null && row.getCell(0) != null && checkCell(row.getCell(0))) {

        AmdrSampleData data = new AmdrSampleData();

        // Populate entity from Excel
        String sample = getCellValue(row, columnPositions, "Sample.Internal.ID");
        data.setSampleInternalId(sample);
        data.setKelch(getCellValue(row, columnPositions, "Kelch"));
        data.setPfcrt72(getCellValue(row, columnPositions, "PfCRT:72"));
        data.setPfcrt74(getCellValue(row, columnPositions, "PfCRT:74"));
        data.setPfcrt75(getCellValue(row, columnPositions, "PfCRT:75"));
        data.setPfcrt76(getCellValue(row, columnPositions, "PfCRT:76"));
        data.setPfdhfr51(getCellValue(row, columnPositions, "PfDHFR:51"));
        data.setPfdhfr59(getCellValue(row, columnPositions, "PfDHFR:59"));
        data.setPfdhfr108(getCellValue(row, columnPositions, "PfDHFR:108"));
        data.setPfdhfr164(getCellValue(row, columnPositions, "PfDHFR:164"));
        data.setPfdhps436(getCellValue(row, columnPositions, "PfDHPS:436"));
        data.setPfdhps437(getCellValue(row, columnPositions, "PfDHPS:437"));
        data.setPfdhps540(getCellValue(row, columnPositions, "PfDHPS:540"));
        data.setPfdhps581(getCellValue(row, columnPositions, "PfDHPS:581"));
        data.setPfdhps613(getCellValue(row, columnPositions, "PfDHPS:613"));
        data.setPfmdr186(getCellValue(row, columnPositions, "PfMDR1:86"));
        data.setPfmdr1184(getCellValue(row, columnPositions, "PfMDR1:184"));
        data.setPfmdr11246(getCellValue(row, columnPositions, "PfMDR1:1246"));
        data.setStatus(AmdrProcessingStatus.UNPROCESSED);
        data.setImportId(currentMetaImport.getIdentifier());
        amdrDataList.add(data);
        rowCounter++;
        row = sheet.getRow(rowCounter);
      }
      amdrSampleDataRepository.saveAll(amdrDataList);

      amdrImport.setStatus(AmdrProcessingStatus.SAVED_BUSY_PROCESSING);
      amdrImportRepository.save(amdrImport);

    } catch (DataAccessException | PersistenceException e) {
      log.error(e.getMessage(), e);

      amdrImport.setStatus(AmdrProcessingStatus.FAILED);
      amdrImportRepository.save(amdrImport);
      throw e;

    } catch (Exception e) {
      log.error(e.getMessage(), e);

      amdrImport.setStatus(AmdrProcessingStatus.FAILED);
      amdrImportRepository.save(amdrImport);
      throw new FileFormatException(e.getMessage());
    }
  }

  public void processData() {
    getDateAndLocationForSample();
  }

  public AdmrImportResultsResponse getImportResults(UUID importId){

    int sampleIds = amdrSampleDataRepository.countByImportId(importId);

    return AdmrImportResultsResponse.builder()
        .sampleIds(sampleIds)
        .build();
  }

  private void getDateAndLocationForSample() {

    List<AmdrImport> allByStatus = amdrImportRepository.findAllByStatusNotOrderByCreatedDatetime(
        AmdrProcessingStatus.SUCCESSFUL);

    allByStatus.stream().forEach(importFile -> {
      int page = 0;
      int size = 500; // batch size
      Page<AmdrSampleData> batch;
      boolean awaitingParasitology = false;
      do {
        Pageable pageable = PageRequest.of(page, size);

        batch = amdrSampleDataRepository
            .findAllByStatus(AmdrProcessingStatus.UNPROCESSED, pageable);

        List<AmdrSampleData> content = batch.getContent();
        for (AmdrSampleData record : content) {
          List<AmdrEventSampleProjection> sampleData = amdrRepository.getSampleData(
              record.getSampleInternalId());

          AmdrEventSampleProjection latest =
              sampleData.stream()
                  .max(Comparator.comparing(AmdrEventSampleProjection::getCaptureDatetime))
                  .orElse(null);
          if (latest != null) {
            record.setDateCollection(latest.getCaptureDatetime());
          } else {
            awaitingParasitology = true;
            continue;
          }
          // process each record

          List<AmdrPassiveEventProjection> passiveSampleData = amdrRepository.getPassiveCaseSampleDataCluster(
              record.getSampleInternalId());
          if (passiveSampleData != null && !passiveSampleData.isEmpty() && passiveSampleData.get(0).getLocationName()!=null) {
            record.setRegion(passiveSampleData.get(0).getLocationName());
            record.setLocationIdentifier(passiveSampleData.get(0).getLocationIdentifier());
            record.setStatus(AmdrProcessingStatus.PROCESSED);
          } else {
            log.info("Unable to find event for sample: {}", record.getSampleInternalId());
            awaitingParasitology = true;
            continue;
          }
        }

        amdrSampleDataRepository.saveAll(content);
        page++;

      } while (!batch.isLast());

      amdrRepository.refreshAmdrImportData();
      amdrRepository.summarizeImportData();


      importFile.setStatus(awaitingParasitology? AmdrProcessingStatus.AWAITING_PARASITOLOGY : AmdrProcessingStatus.SUCCESSFUL);

      amdrImportRepository.save(importFile);
    });

  }

  private String getCellValue(XSSFRow row, Map<String, Integer> map, String key) {
    Integer idx = map.get(key);
    if (idx == null) {
      return null;
    }
    XSSFCell cell = row.getCell(idx);
    return getStringValue(cell);
  }

  private boolean checkCell(XSSFCell cell) {

    String stringValue = getStringValue(cell);

    return stringValue != null && !stringValue.isEmpty();

  }

  private @Nullable
  String getStringValue(XSSFCell cell) {

    if (cell != null) {
      if (cell.getCellType().equals(CellType.BLANK)) {
        return null;
      }
      if (cell.getCellType().equals(CellType.STRING)) {
        if (!cell.getStringCellValue().isEmpty()) {
          return cell.getStringCellValue();
        }
        return null;
      }
      if (cell.getCellType().equals(CellType.FORMULA) || cell.getCellType()
          .equals(CellType.NUMERIC)) {

        if (DateUtil.isCellDateFormatted(cell)) {
          Date date = cell.getDateCellValue();
          SimpleDateFormat dateFormat = new SimpleDateFormat(
              "yyyy-MM-dd"); // You can customize format
          return dateFormat.format(date);
        }
        return String.valueOf(cell.getNumericCellValue());
      }

    }
    return null;
  }

  public Page<AmdrImportResponse> getAmdrImportList(Pageable pageable) {
    Page<AmdrImport> all = amdrImportRepository.findAll(pageable);
    return AmdrImportResponseFactory.fromEntityPage(all, pageable);
  }


}
