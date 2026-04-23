package com.revealprecision.revealserver.persistence.projection;

public interface TagYearRangeAggregateDateProjection {
    String getTagIdentifier();
    String getTag();
    Integer getMinYear();
    Integer getMaxYear();
    String getYears();
}