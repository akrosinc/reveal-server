package com.revealprecision.revealserver.api.v1.dto.models;

import java.io.Serializable;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hl7.fhir.utilities.xls.XLSXmlParser.Row;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TableRow implements Serializable {
  private UUID parentLocationIdentifier;
  private UUID reportIdentifier;
  private UUID planIdentifier;
  private List<RowData> rowData;
}
