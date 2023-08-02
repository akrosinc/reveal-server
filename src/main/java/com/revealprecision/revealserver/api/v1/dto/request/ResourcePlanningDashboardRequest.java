package com.revealprecision.revealserver.api.v1.dto.request;

import com.revealprecision.revealserver.api.v1.dto.response.FormulaResponse;
import com.revealprecision.revealserver.api.v1.dto.response.SecondStepQuestionsResponse;
import com.revealprecision.revealserver.model.GenericHierarchy;
import com.revealprecision.revealserver.model.resourceplanning.CountryCampaignDto;
import java.util.List;
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
  private CountryCampaignDto country;
  @NotNull
  private GenericHierarchy locationHierarchy;
  private String lowestGeography;
  @NotNull
  private IdentifierPair populationTag;
  private String name;
  private String baseName;
  private IdentifierPair structureCountTag;
  @NotNull
  private String minimalAgeGroup;
  private boolean countBasedOnImportedLocations;
  @NotEmpty
  private Map<String, Object> stepOneAnswers;
  @NotEmpty
  private Map<String, Object> stepTwoAnswers;

  private List<FormulaResponse> questionsOne;

  private List<SecondStepQuestionsResponse> questionsTwo;

  private boolean dataSubmittedToSimulation;

  @Setter
  @Getter
  public static class IdentifierPair {

    private UUID identifier;
    private String name;
  }


}
