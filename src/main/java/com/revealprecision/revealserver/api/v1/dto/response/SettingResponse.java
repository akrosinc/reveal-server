package com.revealprecision.revealserver.api.v1.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import java.util.List;
import java.util.UUID;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
@JsonInclude(Include.NON_NULL)
public class SettingResponse {

  private UUID id;
  private String key;
  private String value;
  private List<Object> values;
  private String label;
  private String description;
  private String identifier; //not uuid but the type of setting, like global_config
  private String type;
}
