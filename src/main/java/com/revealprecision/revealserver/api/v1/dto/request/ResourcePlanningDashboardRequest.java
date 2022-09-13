package com.revealprecision.revealserver.api.v1.dto.request;

import java.util.Map;
import java.util.UUID;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ResourcePlanningDashboardRequest {

  @NotNull
  private UUID campaign;
  @NotNull
  private UUID country;
  @NotNull
  private UUID locationHierarchy;
  @NotNull
  private String lowestGeography;
  @NotNull
  private UUID populationTag;
  private UUID structureCountTag;
  @NotNull
  private String minimalAgeGroup;
  private boolean countBasedOnImportedLocations;
  @NotEmpty
  private Map<String, Object> stepOneAnswers;
  @NotEmpty
  private Map<String, Object> stepTwoAnswers;
}
