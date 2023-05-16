package com.revealprecision.revealserver.persistence.domain.metadata.metadataImport;

import com.revealprecision.revealserver.persistence.domain.Location;
import com.revealprecision.revealserver.persistence.domain.LocationHierarchy;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MetaImportDTO {

  LocationHierarchy locationHierarchy;
  String hierarchyError;

  Location location;
  String locationError;

  SheetData sheetData ;

}
