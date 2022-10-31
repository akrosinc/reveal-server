package com.revealprecision.revealserver.enums;

import com.revealprecision.revealserver.exceptions.WrongEnumException;
import com.revealprecision.revealserver.exceptions.constant.Error;
import java.util.stream.Stream;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ActionTitleEnum {
  RACD_REGISTER_FAMILY("RACD Register Family", LookupEntityTypeCodeEnum.LOCATION_CODE),
  MDA_DISPENSE("MDA Dispense", LookupEntityTypeCodeEnum.PERSON_CODE),
  MDA_ADHERENCE("MDA Adherence", LookupEntityTypeCodeEnum.PERSON_CODE),
  IRS("IRS", LookupEntityTypeCodeEnum.LOCATION_CODE),
  IRS_VERIFICATION("IRS Verification", LookupEntityTypeCodeEnum.LOCATION_CODE),
  CDD_SUPERVISION ("CDD Supervision", LookupEntityTypeCodeEnum.LOCATION_CODE),
  CELL_COORDINATION("Cell Coordination", LookupEntityTypeCodeEnum.LOCATION_CODE),
  MDA_SURVEY("MDA Survey",LookupEntityTypeCodeEnum.LOCATION_CODE),
  HABITAT_SURVEY("Habitat Survey",LookupEntityTypeCodeEnum.LOCATION_CODE),
  LSM_HOUSEHOLD_SURVEY("LSM Household Survey",LookupEntityTypeCodeEnum.LOCATION_CODE);

  private final String actionTitle;
  private final LookupEntityTypeCodeEnum entityType;

  public static ActionTitleEnum lookup(String actionTitle) {
    return Stream.of(ActionTitleEnum.values())
        .filter(value -> value.getActionTitle().equals(actionTitle)).findFirst()
        .orElseThrow(() -> new WrongEnumException(
            String.format(Error.WRONG_ENUM, ActionTitleEnum.class.getSimpleName(), actionTitle)));
  }

}
