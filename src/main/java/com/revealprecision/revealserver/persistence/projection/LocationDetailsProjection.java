package com.revealprecision.revealserver.persistence.projection;


import org.postgresql.util.PGobject;

import java.util.List;
import java.util.UUID;

public interface  LocationDetailsProjection {
    String getLocationId();
    Long getChildrenCount();
    String getParentLocationId();
    String getPopulationData();
    Boolean getAssigned();
    List<String> getAncestry();
}
