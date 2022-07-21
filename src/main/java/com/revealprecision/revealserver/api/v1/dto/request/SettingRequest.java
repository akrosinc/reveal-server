package com.revealprecision.revealserver.api.v1.dto.request;

import java.util.List;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SettingRequest {
  @NotBlank
  private String key;
  @NotNull
  private String value;
  @NotNull
  private List<Object> values;
  @NotBlank
  private String label;
  @NotBlank
  private String description;
  @NotBlank
  private String settingIdentifier;
  @NotBlank
  private String type;
}
