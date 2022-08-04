package com.revealprecision.revealserver.persistence.domain.metadata.metadataImport.fieldMapper;


import com.revealprecision.revealserver.exceptions.FileFormatException;
import com.revealprecision.revealserver.persistence.domain.metadata.metadataImport.MetaImportDTO;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;

@Slf4j
public class MetaFieldSetMapper {

  public static List<MetaImportDTO> mapMetaFields(XSSFSheet sheet) throws FileFormatException {
    //starting from 1st
    List<MetaImportDTO> metaImportDTOS = new ArrayList<>();
    int fileRowsCount = sheet.getPhysicalNumberOfRows();
    if (fileRowsCount > 1) {
      for (int i = 1; i < fileRowsCount; i++) {
        XSSFRow row = sheet.getRow(i);
        // mapping xlsx file to metaImportDTO
        if (row != null) {
          MetaImportDTO metaImportDTO = new MetaImportDTO();
          for (int j = 0; j < row.getPhysicalNumberOfCells(); j++) {
            if (row.getCell(j).getRawValue() == null) {
              throw new FileFormatException(
                  "Field " + sheet.getRow(0).getCell(j).toString() + " can't be empty.");
            }
            switch (j) {
              case 0:
                metaImportDTO.setLocationIdentifier(UUID.fromString(row.getCell(j).toString()));
                break;
              case 1:
                metaImportDTO.setLocationName(row.getCell(j).toString());
                break;
              case 2:
                metaImportDTO.setGeographicLevel(row.getCell(j).toString());
                break;
              default:
                //set all dynamic cells to string type to avoid conversion issues
                row.getCell(j).setCellType(CellType.STRING);
                metaImportDTO.getEntityTags()
                    .put(sheet.getRow(0).getCell(j).toString(),
                        row.getCell(j).getStringCellValue());
                break;
            }
          }
          metaImportDTOS.add(metaImportDTO);
        } else {
          log.info("Existing file processing loop...empty row encountered!!");
          break;
        }
      }
    } else {
      throw new FileFormatException(
          "File is empty or not valid.");
    }

    return metaImportDTOS;
  }

}
