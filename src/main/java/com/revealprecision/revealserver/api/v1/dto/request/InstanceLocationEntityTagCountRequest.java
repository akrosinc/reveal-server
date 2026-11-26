package com.revealprecision.revealserver.api.v1.dto.request;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/*
Counts of numeric entity tags (e.g. sites_outreach_to_add) for a location of the current
instance. Each count replaces the previous value for the location, tag and dataCaptureDate.
*/
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class InstanceLocationEntityTagCountRequest {
  private UUID locationId;
  // optional, as in a dataset upload; values are kept per capture date
  private LocalDate dataCaptureDate;
  private String tag;
  private Double count;
}
