package com.revealprecision.revealserver.persistence.domain.metadata.metadataImport.fieldMapper;


import com.revealprecision.revealserver.exceptions.FileFormatException;
import com.revealprecision.revealserver.persistence.domain.metadata.metadataImport.MetaImportDTO;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;

public class MetaFieldSetMapper {

  public static List<MetaImportDTO> mapMetaFields(XSSFSheet sheet) throws FileFormatException {
    //starting from 1st
    List<MetaImportDTO> metaImportDTOS = new ArrayList<>();
    int fileRowsCount = sheet.getPhysicalNumberOfRows();
    if (fileRowsCount > 1) {
      for (int i = 1; i < fileRowsCount; i++) {
        XSSFRow row = sheet.getRow(i);
        // mapping xlsx file to metaImportDTO
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
              metaImportDTO.getEntityTags()
                  .put(sheet.getRow(0).getCell(j).toString(), row.getCell(j).toString());
              break;
          }
        }
        metaImportDTOS.add(metaImportDTO);
      }
    } else {
      throw new FileFormatException(
          "File is empty or not valid.");
    }

    return metaImportDTOS;
  }

}
