package com.revealprecision.revealserver.api.v1.dto.response;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/*
Entity tag counts of a location. count is the value set for the location itself (null when none
was set); total adds the values set for its descendants.
*/
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class InstanceLocationEntityTagCountResponse {

  private UUID locationId;

  private LocalDate dataCaptureDate;

  private List<TagCount> tags;

  @Getter
  @Setter
  @Builder
  @AllArgsConstructor
  @NoArgsConstructor
  public static class TagCount {

    private String tag;

    private Double count;

    private Double total;
  }
}
