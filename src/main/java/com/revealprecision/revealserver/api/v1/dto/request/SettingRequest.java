package com.revealprecision.revealserver.api.v1.dto.request;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.json.JSONArray;

@Getter
@Setter
@JsonInclude(Include.NON_NULL)
public class SettingRequest {

  @NotBlank
  private String key;
  @NotNull
  private String value;
  @NotNull
  private JSONArray values;
  @NotBlank
  private String label;
  @NotBlank
  private String description;
  @NotBlank
  private String identifier;          //not uuid but the type of setting, like global_config
  @NotBlank
  private String type;
}
