package com.revealprecision.revealserver.api.v1.facade.models;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.Serializable;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;

@Setter
@Getter

@FieldNameConstants
@JsonInclude(value = Include.NON_NULL)
@AllArgsConstructor
@NoArgsConstructor
public class BaseDataEntity implements Serializable {

  private static final long serialVersionUID = 1L;
  @JsonProperty
  private String type;
  public static final String ATTACHMENTS_NAME = "_attachments";
  @JsonProperty("_id")
  private String id;
  @JsonProperty("_rev")
  private String revision;
  @JsonProperty("_conflicts")
  private List<String> conflicts;
}
