package com.revealprecision.revealserver.persistence.projection;

import java.time.LocalDate;
import java.util.UUID;

public interface TagYearAggregateDateProjection {
    String getTagIdentifier();
    String getTag();
    Integer getYear();
}