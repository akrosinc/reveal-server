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
      new FormulaResponse("Which campaign(s) are you planning in the selected locations?", "NTD_name", new FieldType(
          InputTypeEnum.DROPDOWN, List.of(new String[]{"STH", "SCH", "LF", "Onchocerciasis", "Tarchoma", "Vitamin A"}), null, null), null),
      new FormulaResponse("What year is the next campaign?", "mda_year", new FieldType(
          InputTypeEnum.DROPDOWN, List.of(new String[]{"2022", "2023", "2024", "2025", "2026", "2027"}), null, null), null),
      new FormulaResponse("What year was the uploaded population counted?", "Pop_year", new FieldType(
          InputTypeEnum.DROPDOWN, List.of(new String[]{"2022", "2021", "2020", "2019", "2018", "2017", "2016", "2015", "2014", "2013", "2012", "2011", "2010", "2009", "2008", "2007"}), null, null), null),
      new FormulaResponse("What is the estimated annual percent growth rate of the population?", "Pop_growth", new FieldType(
          InputTypeEnum.DECIMAL, null, 0, 100), null),
      new FormulaResponse("Is the number of days of the campaign fixed?", "choice_days", new FieldType(
          InputTypeEnum.DROPDOWN, List.of(new String[]{"Yes", "No"}), null, null), new SkipPattern("Yes", "cdd_denom")),
      new FormulaResponse("How many days will this campaign run?", "mda_days", new FieldType(
          InputTypeEnum.INTEGER, null, 1, 90), null),
  };
}
