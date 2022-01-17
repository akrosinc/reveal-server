package com.revealprecision.revealserver.service;

import com.cosium.spring.data.jpa.entity.graph.domain.EntityGraphUtils;
import com.revealprecision.revealserver.api.v1.dto.request.OrganizationCriteria;
import com.revealprecision.revealserver.api.v1.dto.request.OrganizationRequest;
import com.revealprecision.revealserver.enums.EntityStatus;
import com.revealprecision.revealserver.exceptions.NotFoundException;
import com.revealprecision.revealserver.persistence.domain.Organization;
import com.revealprecision.revealserver.persistence.domain.Organization.Fields;
import com.revealprecision.revealserver.persistence.domain.User;
import com.revealprecision.revealserver.persistence.repository.OrganizationRepository;
import com.revealprecision.revealserver.persistence.repository.UserRepository;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Service
public class OrganizationService {

  public final OrganizationRepository organizationRepository;
  public final UserRepository userRepository;

  public Organization createOrganization(OrganizationRequest organizationRequest) {
    Organization organization = Organization.builder()
        .name(organizationRequest.getName())
        .parent((organizationRequest.getPartOf() == null) ? null
            : findByIdWithChildren(organizationRequest.getPartOf()))
        .type(organizationRequest.getType())
        .active(organizationRequest.isActive())
        .build();
    organization.setEntityStatus(EntityStatus.ACTIVE);

    return organizationRepository.save(organization);
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
      return organizationRepository.getAllByCriteriaWithRoot(criteria.getSearch(), pageable);
      //TODO update this method, search should apply on children also
    } else {
      return organizationRepository.getAllByCriteriaWithoutRoot(criteria.getSearch(), pageable);
    }
  }

  public long getCountFindAll(OrganizationCriteria criteria) {
    if (criteria.isRoot()) {
      return organizationRepository.getCountByCriteriaWithRoot(criteria.getSearch());
    } else {
      return organizationRepository.getCountByCriteriaWithoutRoot(criteria.getSearch());
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

  @Transactional
  public void deleteOrganization(UUID identifier) {
    Organization organization = findByIdWithoutChildren(identifier);
    for (User u : organization.getUsers()) {
      u.getOrganizations().remove(organization);
      userRepository.save(u);
    }
    organizationRepository.delete(organization);
  }

  public Set<Organization> findByIdentifiers(Set<UUID> identifiers) {
    return organizationRepository.findByIdentifiers(identifiers);
  }
}
