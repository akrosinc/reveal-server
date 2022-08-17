package com.revealprecision.revealserver.persistence.domain;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import java.util.HashSet;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(Include.NON_NULL)
public class ReportIndicators {
  private Integer sprayedRooms;
  private Integer males;
  private Integer females;
  private Integer pregnantWomen;
  private String notSprayedReason;
  private String phoneNumber;
  private String mobilized;
  private String dateSprayed;
  private String mobilizationDate;
  private Set<String> uniqueSupervisionDates = new HashSet<>();
  private Integer supervisorFormSubmissionCount;
  private boolean irsDecisionFormFilled = false;
  private String businessStatus;
  private Integer discoveredStructures;
}
