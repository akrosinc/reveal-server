package com.revealprecision.revealserver.api.v1.dto.factory;

import com.revealprecision.revealserver.api.v1.dto.response.OrganizationResponse;
import com.revealprecision.revealserver.api.v1.dto.response.TypeResponse;
import com.revealprecision.revealserver.persistence.domain.Organization;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class OrganizationResponseFactory {

  public static OrganizationResponse fromEntityWithChild(Organization organization) {
    var children = organization.getChildren().stream()
        .map(child -> fromEntityToChildResponse(child))
        .collect(
            Collectors.toSet());
    return OrganizationResponse.builder()
        .identifier(organization.getIdentifier())
        .active(organization.isActive())
        .name(organization.getName())
        .partOf(
            (organization.getParent() == null) ? null : organization.getParent().getIdentifier())
        .type(TypeResponse.builder()
            .code(organization.getType())
            .valueCodableConcept(organization.getType().getOrganizationType()).build())
        .headOf(children)
        .build();
  }

  public static OrganizationResponse fromEntityWithoutChild(Organization organization) {
    return OrganizationResponse.builder()
        .identifier(organization.getIdentifier())
        .active(organization.isActive())
        .type(TypeResponse.builder()
            .code(organization.getType())
            .valueCodableConcept(organization.getType().getOrganizationType()).build())
        .name(organization.getName())
        .build();
  }

  public static OrganizationResponse fromEntityToChildResponse(Organization organization) {
    var children = organization.getChildren().stream()
        .map(child -> fromEntityToChildResponse(child))
        .collect(
            Collectors.toSet());
    return OrganizationResponse.builder()
        .identifier(organization.getIdentifier())
        .active(organization.isActive())
        .type(TypeResponse.builder()
            .code(organization.getType())
            .valueCodableConcept(organization.getType().getOrganizationType()).build())
        .name(organization.getName())
        .headOf((children.isEmpty()) ? null : children)
        .build();
  }
}
