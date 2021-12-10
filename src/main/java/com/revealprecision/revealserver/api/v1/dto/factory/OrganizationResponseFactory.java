package com.revealprecision.revealserver.api.v1.dto.factory;

import com.revealprecision.revealserver.api.v1.dto.response.OrganizationResponse;
import com.revealprecision.revealserver.api.v1.dto.response.TypeResponse;
import com.revealprecision.revealserver.enums.SummaryEnum;
import com.revealprecision.revealserver.persistence.domain.Organization;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

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
        .partOf(
            (organization.getParent() == null) ? null : organization.getParent().getIdentifier())
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

  public static Page<OrganizationResponse> fromEntityPage(Page<Organization> organizations,
      Pageable pageable, SummaryEnum _summary) {
    var organizationContent = organizations.getContent().stream()
        .map((_summary.equals(SummaryEnum.TRUE))
            ? OrganizationResponseFactory::fromEntityWithoutChild
            : OrganizationResponseFactory::fromEntityWithChild)
        .collect(Collectors.toList());
    return new PageImpl<>(organizationContent, pageable,
        organizations.getTotalElements());
  }
}
