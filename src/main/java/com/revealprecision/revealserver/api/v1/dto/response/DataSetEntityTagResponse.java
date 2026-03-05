package com.revealprecision.revealserver.api.v1.dto.response;

import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DataSetEntityTagResponse {
  private UUID identifier;
  private String tag;
  private List<String> instances;
  private Boolean isPublic;
}
