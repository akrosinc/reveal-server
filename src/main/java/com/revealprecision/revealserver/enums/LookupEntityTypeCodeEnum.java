package com.revealprecision.revealserver.enums;

import com.revealprecision.revealserver.exceptions.WrongEnumException;
import com.revealprecision.revealserver.exceptions.constant.Error;
import java.io.Serializable;
import java.util.stream.Stream;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum LookupEntityTypeCodeEnum implements Serializable {
  PERSON_CODE("Person"), LOCATION_CODE("Location"), GROUP("Group");

  private final String lookupEntityType;

  public static LookupEntityTypeCodeEnum lookup(String lookupEntityType) {
    return Stream.of(LookupEntityTypeCodeEnum.values()).filter(
        lookupEntityTypeCodeEnum -> lookupEntityTypeCodeEnum.getLookupEntityType()
            .equals(lookupEntityType)).findFirst().orElseThrow(()->new WrongEnumException(String.format(
        Error.WRONG_ENUM, LookupEntityTypeCodeEnum.class.getSimpleName(), lookupEntityType)));
  }
}
