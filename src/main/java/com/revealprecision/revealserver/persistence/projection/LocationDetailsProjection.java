package com.revealprecision.revealserver.persistence.projection;



public interface  LocationDetailsProjection {
    String getLocationId();
    Long getChildrenCount();
    String getParentLocationId();
    String getPopulationData();
}
