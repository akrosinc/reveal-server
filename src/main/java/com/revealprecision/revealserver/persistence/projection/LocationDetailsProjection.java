package com.revealprecision.revealserver.persistence.projection;

import com.revealprecision.revealserver.persistence.domain.Location;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

public interface  LocationDetailsProjection {
    String getLocationId();
    Long getChildrenCount();
    String getParentLocationId();


}
