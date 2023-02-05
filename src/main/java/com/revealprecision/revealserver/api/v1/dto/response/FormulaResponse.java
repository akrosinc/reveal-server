package com.revealprecision.revealserver.api.v1.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.revealprecision.revealserver.enums.InputTypeEnum;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FormulaResponse {

  private String question;
  private String fieldName;
  private FieldType fieldType;
  private SkipPattern skipPattern;

  public static FormulaResponse[] list = {
      new FormulaResponse("What year is the next campaign?", "mda_year", new FieldType(
          InputTypeEnum.DROPDOWN, List.of(new String[]{"2022", "2023", "2024", "2025", "2026", "2027"}), null, null), null),
      new FormulaResponse("What year was the uploaded population counted?", "pop_year", new FieldType(
          InputTypeEnum.DROPDOWN, List.of(new String[]{"2022", "2021", "2020", "2019", "2018", "2017", "2016", "2015", "2014", "2013", "2012", "2011", "2010", "2009", "2008", "2007"}), null, null), null),
      new FormulaResponse("What is the estimated annual percent growth rate of the population?", "pop_growth", new FieldType(
          InputTypeEnum.DECIMAL, null, 0, 100), null),
      new FormulaResponse("Is the number of days of the campaign fixed?", "choice_days", new FieldType(
          InputTypeEnum.DROPDOWN, List.of(new String[]{"Yes", "No"}), null, null), null),
      new FormulaResponse("How many days will this campaign run?", "mda_days", new FieldType(
          InputTypeEnum.INTEGER, null, 1, 90), new SkipPattern("No", "choice_days")),
      new FormulaResponse("Is the number of CDDs per location fixed?", "choice_cdd", new FieldType(
          InputTypeEnum.DROPDOWN, List.of(new String[]{"Yes", "No"}), null, null), null),
      new FormulaResponse("What is the average number of CDDs per location selected on the map?", "cdd_number", new FieldType(
          InputTypeEnum.INTEGER, null, null, null), new SkipPattern("No", "choice_cdd")),
      new FormulaResponse("Are you planning your CDDs based on the campaign target population for the campaign? (The campaign target population is a sub-set of the total population) If no, then the total population with growth rate will be used to calculate CDD requirements on the dashboard.", "cdd_denom", new FieldType(
          InputTypeEnum.DROPDOWN, List.of(new String[]{"Yes", "No"}), null, null), null),
      new FormulaResponse("On average, how many people can you estimate that 1 CDD can treat in 1 campaign day?", "cdd_target", new FieldType(
          InputTypeEnum.INTEGER, null, null, null), null),
      new FormulaResponse("On average, how many structures (or households) can be visited on 1 campaign day by 1 CDD?", "structure_day", new FieldType(
          InputTypeEnum.INTEGER, null, null, null), null),
      new FormulaResponse("What is the daily allowance for CDDs? (in local currency)", "cdd_allow", new FieldType(
          InputTypeEnum.INTEGER, null, null, null), null),
      new FormulaResponse("What is the estimated cost of supplies, job aids, registers, and other materials for 1 CDD? (in local currency)", "cdd_job", new FieldType(
          InputTypeEnum.DECIMAL, null, null, null), null),
      new FormulaResponse("How many CDDs report to 1 supervisor (on average)?", "cdd_super", new FieldType(
          InputTypeEnum.INTEGER, null, null, null), null),
      new FormulaResponse("What is the daily allowance for CDDs supervisors? (in local currency)", "super_allow", new FieldType(
          InputTypeEnum.INTEGER, null, null, null), null)
  };
}
