package com.revealprecision.revealserver.persistence.projection;

import com.revealprecision.revealserver.persistence.domain.Location;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class PlanLocationDetails {
  private Location location;
  private Long assignedLocations;
  private Long assignedTeams;
  private Location parentLocation;
  private Long childrenNumber;
  private boolean hasChildren = true;

  public PlanLocationDetails(Location location, Long assignedLocations, Long assignedTeams) {
    this.location = location;
    this.assignedTeams = assignedTeams;
    this.assignedLocations = assignedLocations;
  }
}
