package com.revealprecision.revealserver.persistence.domain;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.revealprecision.revealserver.enums.LocationStatus;
import java.util.UUID;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
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
@JsonInclude(value = Include.NON_NULL)
public class LocationProperty {


  @NotBlank
  private String name;

  @NotNull
  private LocationStatus status;

  private UUID externalId;
  @NotNull
  private String geographicLevel;

  private Long structures;

  private Boolean isOtherArea;
  
  private String surveyLocationType;

  private String structureNumber;
}
