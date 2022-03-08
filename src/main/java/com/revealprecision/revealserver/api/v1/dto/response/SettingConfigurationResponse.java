package com.revealprecision.revealserver.api.v1.dto.response;

import java.util.List;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class SettingConfigurationResponse {
  private String identifier; //not uuid, but the identifier of configuration, like global_configs
  private List<SettingResponse> settings;
}
