package com.revealprecision.revealserver.persistence.projection;


import com.revealprecision.revealserver.persistence.domain.Location;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
public class LocationWithAncestryProjection {
    Location location;
    Object ancestry;
    Long numberOfTeams;

    public LocationWithAncestryProjection(Location location, Object ancestry) {
        this.location = location;
        this.ancestry = ancestry;
    }
}
