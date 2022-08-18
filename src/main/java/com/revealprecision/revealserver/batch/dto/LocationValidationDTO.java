package com.revealprecision.revealserver.batch.dto;

import com.revealprecision.revealserver.enums.BulkEntryStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class LocationValidationDTO {

  private String name;
  private BulkEntryStatus status;
  private String hashValue;
  private String error;

  public LocationValidationDTO(String name, String hashValue) {
    this.name = name;
    this.hashValue = hashValue;
  }
}
