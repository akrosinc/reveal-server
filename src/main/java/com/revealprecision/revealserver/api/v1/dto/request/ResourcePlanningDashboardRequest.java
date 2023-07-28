package com.revealprecision.revealserver.api.v1.dto.request;

import com.revealprecision.revealserver.api.v1.dto.response.AgeGroupResponse;
import com.revealprecision.revealserver.api.v1.dto.response.FormulaResponse;
import com.revealprecision.revealserver.api.v1.dto.response.SecondStepQuestionsResponse;
import com.revealprecision.revealserver.model.GenericHierarchy;
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
  private UUID country;
  @NotNull
  private GenericHierarchy locationHierarchy;
  private String lowestGeography;
  @NotNull
  private UUID populationTag;
  private String name;
  private String baseName;
  private UUID structureCountTag;
  @NotNull
  private String minimalAgeGroup;
  private boolean countBasedOnImportedLocations;
  @NotEmpty
  private Map<String, Object> stepOneAnswers;
  @NotEmpty
  private Map<String, Object> stepTwoAnswers;

  private ResourcePlanningConfig resourcePlanningConfig;

  private List<FormulaResponse> questionsOne;

  private List<SecondStepQuestionsResponse> questionsTwo;

  @Setter
  @Getter
  public static class ResourcePlanningConfig {

    private String resourcePlanName;
    private CountryCampaignConfig country;
    private GenericHierarchyConfig hierarchy;
    private LabelPair lowestLocation;
    private boolean structureCount;
    private LabelPair populationTag;
    private LabelPair structureCountTag;
  }

  @Setter
  @Getter
  public static class LabelPair {

    private String label;
    private String value;
  }
  @Setter
  @Getter
  public static class CountryCampaignConfig {
    private String label;
    private String value;
    private List<AgeGroupResponse> ageGroups;
  }
  @Setter
  @Getter
  public static class GenericHierarchyConfig{
    private String label;
    private String value;
    private String type;
    List<String> nodeOrder;
  }
}
