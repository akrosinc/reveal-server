package com.revealprecision.revealserver.service;

import com.cosium.spring.data.jpa.entity.graph.domain.EntityGraphUtils;
import com.revealprecision.revealserver.api.v1.dto.request.OrganizationCriteria;
import com.revealprecision.revealserver.api.v1.dto.request.OrganizationRequest;
import com.revealprecision.revealserver.exceptions.NotFoundException;
import com.revealprecision.revealserver.persistence.domain.Organization;
import com.revealprecision.revealserver.persistence.domain.Organization.Fields;
import com.revealprecision.revealserver.persistence.repository.OrganizationRepository;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;

@Service
public class OrganizationService {

  OrganizationRepository organizationRepository;

  @Autowired
  public OrganizationService(OrganizationRepository organizationRepository) {
    this.organizationRepository = organizationRepository;
  }

  public Organization createOrganization(OrganizationRequest organizationRequest) {
    return organizationRepository.save(Organization.builder()
        .name(organizationRequest.getName())
        .parent((organizationRequest.getPartOf() == null) ? null
            : findByIdWithChildren(organizationRequest.getPartOf()))
        .type(organizationRequest.getType())
        .active(organizationRequest.isActive())
        .build());
  }

  public Organization findById(UUID identifier, boolean _summary) {
    if (_summary) {
      return findByIdWithoutChildren(identifier);
    } else {
      return findByIdWithChildren(identifier);
    }
  }

  public Page<Organization> findAll(OrganizationCriteria criteria, Pageable pageable) {
    if (criteria.isRoot()) {
      return organizationRepository.getAllByCriteriaWithRoot(criteria.getName(),
          (criteria.getType() == null) ? null : criteria.getType().name(), pageable);
    } else {
      return organizationRepository.getAllByCriteriaWithoutRoot(criteria.getName(),
          (criteria.getType() == null) ? null : criteria.getType().name(), pageable);
    }
  }

  public long getCountFindAll(OrganizationCriteria criteria) {
    if (criteria.isRoot()) {
      return organizationRepository.getCountByCriteriaWithRoot(criteria.getName(),
          (criteria.getType() == null) ? null : criteria.getType().name());
    } else {
      return organizationRepository.getCountByCriteriaWithoutRoot(criteria.getName(),
          (criteria.getType() == null) ? null : criteria.getType().name());
    }
  }

  public Organization findByIdWithChildren(UUID identifier) {
    return organizationRepository.findById(identifier,
            EntityGraphUtils.fromAttributePaths(Fields.children))
        .orElseThrow(() -> new NotFoundException(
            Pair.of(Fields.identifier, identifier),
            Organization.class));
  }

  public Organization findByIdWithoutChildren(UUID identifier) {
    return organizationRepository.findById(identifier)
        .orElseThrow(() -> new NotFoundException(
            Pair.of(Fields.identifier, identifier),
            Organization.class));
  }

  public Organization updateOrganization(UUID identifier, OrganizationRequest organizationRequest) {
    Organization organization = findByIdWithoutChildren(identifier);
    if (organizationRequest.getPartOf() != null) {
      Organization parent = findByIdWithoutChildren(organizationRequest.getPartOf());
      organization.update(organizationRequest, parent);
    } else {
      organization.update(organizationRequest, null);
    }

    return organizationRepository.save(organization);
  }

  public void deleteOrganization(UUID identifier) {
    Organization organization = findByIdWithoutChildren(identifier);
    organizationRepository.delete(organization);
  }
}
