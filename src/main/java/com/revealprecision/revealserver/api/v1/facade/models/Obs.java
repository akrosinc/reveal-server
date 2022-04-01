package com.revealprecision.revealserver.api.v1.facade.models;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;

@Setter
@Getter
@Builder
@FieldNameConstants
@JsonInclude(value = Include.NON_NULL)
@AllArgsConstructor
@NoArgsConstructor
public class Obs {

  private static final long serialVersionUID = 1L;

  private String fieldType;

  private String fieldDataType;

  private String fieldCode;

  private String parentCode;

  private List<Object> values;
  Set<String> set;

  private String comments;

  private String formSubmissionField;

  private String effectiveDatetime;

  private List<Object> humanReadableValues;

  private Map<String, Object> keyValPairs;
  private boolean saveObsAsArray;
}
