package com.revealprecision.revealserver.api.v1.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class EntityTagResponse {

  private String identifier;

  private String tag;

  private String valueType;

  private String definition;

  private String fieldType;

  private String subType;

  private boolean isAggregate;

  private boolean simulationDisplay;

  private List<String> levels;
}
