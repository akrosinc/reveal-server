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
Entity tag counts to read for a location of the current instance. Without dataCaptureDate the
values set without a date are returned.
*/
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class InstanceLocationEntityTagCountSearchRequest {

  private UUID locationId;

  private List<String> tags;

  private LocalDate dataCaptureDate;
}
