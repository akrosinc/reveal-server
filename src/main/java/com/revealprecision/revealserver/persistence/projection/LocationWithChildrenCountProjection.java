package com.revealprecision.revealserver.persistence.projection;

import com.revealprecision.revealserver.persistence.domain.Location;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class LocationWithChildrenCountProjection {
    private Location location;
    private Long childrenCount;

    public LocationWithChildrenCountProjection(Location location, Long childrenCount){
        this.location = location;
        this.childrenCount = childrenCount;
    }
}
