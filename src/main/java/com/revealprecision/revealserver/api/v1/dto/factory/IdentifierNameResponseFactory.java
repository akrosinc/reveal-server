package com.revealprecision.revealserver.api.v1.dto.factory;

import com.revealprecision.revealserver.api.v1.dto.response.IdentifierNameResponse;
import com.revealprecision.revealserver.persistence.projection.IdentifierNameProjection;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class IdentifierNameResponseFactory {
  public static IdentifierNameResponse toIdentifierNameResponse(IdentifierNameProjection identifierNameProjection) {
    IdentifierNameResponse response = new IdentifierNameResponse();
    response.setIdentifier(identifierNameProjection.getIdentifier());
    response.setName(identifierNameProjection.getName());
    return response;
  }
}
