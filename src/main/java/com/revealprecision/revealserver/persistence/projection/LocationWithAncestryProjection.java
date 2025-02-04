package com.revealprecision.revealserver.persistence.projection;


import com.revealprecision.revealserver.persistence.domain.Location;

import java.util.List;
import java.util.UUID;

public interface LocationWithAncestryProjection {
    Location getLocation();
    String getAncestry();
}
