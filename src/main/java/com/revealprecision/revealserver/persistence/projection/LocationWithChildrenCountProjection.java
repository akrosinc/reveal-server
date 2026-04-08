package com.revealprecision.revealserver.persistence.projection;

import com.revealprecision.revealserver.persistence.domain.Location;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
public class LocationWithChildrenCountProjection {
    private Location location;
    private Long childrenCount;
    private UUID parentIdentifier;

    public LocationWithChildrenCountProjection(Location location, Long childrenCount, UUID parentIdentifier){
        this.location = location;
        this.childrenCount = childrenCount;
        this.parentIdentifier = parentIdentifier;
    }
}
