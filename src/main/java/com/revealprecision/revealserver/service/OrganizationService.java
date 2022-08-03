package com.revealprecision.revealserver.service;

import com.cosium.spring.data.jpa.entity.graph.domain.EntityGraphUtils;
import com.revealprecision.revealserver.api.v1.dto.factory.OrganizationResponseFactory;
import com.revealprecision.revealserver.api.v1.dto.request.OrganizationCriteria;
import com.revealprecision.revealserver.api.v1.dto.request.OrganizationRequest;
import com.revealprecision.revealserver.api.v1.dto.response.OrganizationResponse;
import com.revealprecision.revealserver.enums.EntityStatus;
import com.revealprecision.revealserver.exceptions.NotFoundException;
import com.revealprecision.revealserver.persistence.domain.Organization;
import com.revealprecision.revealserver.persistence.domain.Organization.Fields;
import com.revealprecision.revealserver.persistence.domain.Plan;
import com.revealprecision.revealserver.persistence.domain.PlanAssignment;
import com.revealprecision.revealserver.persistence.domain.PlanLocations;
import com.revealprecision.revealserver.persistence.domain.User;
import com.revealprecision.revealserver.persistence.projection.OrganizationProjection;
import com.revealprecision.revealserver.persistence.repository.OrganizationRepository;
import com.revealprecision.revealserver.persistence.repository.UserRepository;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.apache.commons.collections4.MapUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort.Order;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Service
public class OrganizationService {

  private final OrganizationRepository organizationRepository;
  private final UserRepository userRepository;

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

  public Set<Plan> getPlanAssignmentByOrganizationId(UUID identifier) {
    Organization organization = findById(identifier, true);
    return organization.getPlanAssignments().stream().map(PlanAssignment::getPlanLocations)
        .map(PlanLocations::getPlan).collect(Collectors.toSet());
  }

  public void getAssignedOrganizationsByLocationId(UUID identifier) {
    Organization organization = findById(identifier, true);

  }

  public Organization findById(UUID identifier, boolean _summary) {
    if (_summary) {
      return findByIdWithoutChildren(identifier);
    } else {
      return findByIdWithChildren(identifier);
    }
  }

  public Page<Organization> findAllWithoutTreeView(OrganizationCriteria criteria,
      Pageable pageable) {
    return organizationRepository.getAllByCriteriaWithoutRoot(criteria.getSearch(), pageable);
  }

  public Page<OrganizationResponse> findAllTreeView(OrganizationCriteria criteria,
      Pageable pageable) {
    List<OrganizationProjection> organizations = organizationRepository.searchTreeOrganiztions(
        criteria.getSearch());
    List<OrganizationResponse> content = createTreeView(organizations);
    addChildrenToFoundOrganizations(organizations, content);
    int start = (int) pageable.getOffset();
    int end = Math.min((start + pageable.getPageSize()), content.size());
    if (start > content.size()) {
      return new PageImpl<>(new ArrayList<>(), pageable, content.size());
    }
    if (pageable.getSort().isSorted()) {
      for (Order order : pageable.getSort()) {
        if (order.getProperty().equals("name")) {
          return new PageImpl<>(content.stream().sorted(order.isAscending() ? Comparator.comparing(
                  OrganizationResponse::getName) : Comparator.comparing(
                  OrganizationResponse::getName).reversed()).collect(Collectors.toList())
              .subList(start, end), pageable, content.size());
        }
        if (order.getProperty().equals("active")) {
          return new PageImpl<>(content.stream().sorted(order.isAscending() ? Comparator.comparing(
                  OrganizationResponse::isActive) : Comparator.comparing(
                  OrganizationResponse::isActive).reversed()).collect(Collectors.toList())
              .subList(start, end),
              pageable, content.size());
        }
        if (order.getProperty().equals("type")) {
          return new PageImpl<>(content.stream().sorted(order.isAscending() ? Comparator.comparing(
                  OrganizationResponse::getType,
                  (s1, s2) -> s2.getValueCodableConcept().compareTo(s1.getValueCodableConcept()))
                  : Comparator.comparing(OrganizationResponse::getType,
                          (s1, s2) -> s2.getValueCodableConcept().compareTo(s1.getValueCodableConcept()))
                      .reversed()).collect(Collectors.toList())
              .subList(start, end),
              pageable, content.size());
        }
      }
      return new PageImpl<>(content.subList(start, end), pageable, content.size());
    }
    return new PageImpl<>(content.stream().sorted(Comparator.comparing(
        OrganizationResponse::getName)).collect(Collectors.toList()).subList(start, end), pageable,
        content.size());
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

  private List<OrganizationResponse> createTreeView(List<OrganizationProjection> organizations) {

    List<OrganizationResponse> response = new ArrayList<>();
    Map<OrganizationProjection, Set<OrganizationProjection>> parentChilds = new HashMap<>();
    organizations.forEach(organizationProjection -> {
      Set<OrganizationProjection> children = new HashSet<>();
      for (OrganizationProjection org : organizations) {
        if (org.getParentId() != null) {
          if (org.getParentId().equals(organizationProjection.getIdentifier())) {
            children.add(org);
          }
        }
      }
      parentChilds.put(organizationProjection, children);
    });
    parentChilds.entrySet()
        .removeIf(entry -> (entry.getValue().isEmpty() && entry.getKey().getParentId() != null));

    for (Map.Entry<OrganizationProjection, Set<OrganizationProjection>> entry : parentChilds.entrySet()) {
      if (entry.getKey().getParentId() == null) {
        OrganizationResponse parent = OrganizationResponseFactory.fromProjection(entry.getKey());
        for (OrganizationProjection child : entry.getValue()) {
          parent.getHeadOf()
              .add(OrganizationResponseFactory.buildOrganizationResponse(child, parentChilds));
        }
        response.add(parent);
      }
    }
    return response;
  }

  private void addChildrenToFoundOrganizations(List<OrganizationProjection> projections,
      List<OrganizationResponse> orgs) {
    var identifiers = projections.stream()
        .filter(organizationProjection -> organizationProjection.getLvl() == 1)
        .map(organizationProjection -> UUID.fromString(organizationProjection.getIdentifier()))
        .collect(Collectors.toList());

    List<Organization> organizations = organizationRepository.getAllByIdentifiers(identifiers,
        EntityGraphUtils.fromAttributePaths(Fields.children));
    List<OrganizationResponse> foundOrgsResponse = organizations.stream()
        .map(OrganizationResponseFactory::fromEntityWithChild).collect(
            Collectors.toList());
    Map<UUID, OrganizationResponse> foundOrgsMap = new HashMap<>();
    MapUtils.populateMap(foundOrgsMap, foundOrgsResponse, OrganizationResponse::getIdentifier);

    orgs.stream()
        .forEach(organizationResponse -> setChildrenTreeView(organizationResponse, foundOrgsMap));
  }

  private void setChildrenTreeView(OrganizationResponse organizationResponse,
      Map<UUID, OrganizationResponse> orgsMap) {
    if (orgsMap.containsKey(organizationResponse.getIdentifier())) {
      organizationResponse.setHeadOf(orgsMap.get(organizationResponse.getIdentifier()).getHeadOf());
    } else {
      for (OrganizationResponse child : organizationResponse.getHeadOf()) {
        setChildrenTreeView(child, orgsMap);
      }
    }
  }
}
