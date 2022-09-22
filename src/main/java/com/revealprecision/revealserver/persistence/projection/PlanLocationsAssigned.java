package com.revealprecision.revealserver.persistence.projection;

import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PlanLocationsAssigned {

  private UUID value;
  private String label;
}
