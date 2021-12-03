package com.revealprecision.revealserver.api.dto.factory;

import com.revealprecision.revealserver.api.dto.response.GeographicLevelResponse;
import com.revealprecision.revealserver.persistence.domain.GeographicLevel;

public class GeographicLevelResponseFactory {

    public static GeographicLevelResponse fromEntity(GeographicLevel geographicLevel) {
        return GeographicLevelResponse.builder()
                .identifier(geographicLevel.getIdentifier())
                .name(geographicLevel.getName())
                .title(geographicLevel.getTitle())
                .build();
    }
}
