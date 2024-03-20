package com.revealprecision.revealserver.api.v1.dto.factory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.revealprecision.revealserver.api.v1.dto.response.EntityMetadataResponse;
import com.revealprecision.revealserver.api.v1.dto.response.LocationPropertyResponse;
import com.revealprecision.revealserver.api.v1.dto.response.LocationResponse;
import com.revealprecision.revealserver.enums.SummaryEnum;
import com.revealprecision.revealserver.persistence.domain.Location;
import com.revealprecision.revealserver.persistence.es.HierarchyDetailsElastic;
import com.revealprecision.revealserver.persistence.es.LocationElastic;
import com.revealprecision.revealserver.persistence.projection.PlanLocationDetails;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.common.document.DocumentField;
import org.elasticsearch.search.SearchHit;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class LocationResponseFactory {

  public static LocationResponse fromEntity(Location location) {
    return LocationResponse.builder().identifier(location.getIdentifier())
        .type(location.getType()).geometry(location.getGeometry()).properties(
            LocationPropertyResponse.builder().name(location.getName()).status(location.getStatus())
                .externalId(location.getExternalId())
                .geographicLevel(location.getGeographicLevel().getName()).build()).build();
  }

  public static LocationResponse fromEntityWithChildCount(Location location, Long childrenNumber) {
    return LocationResponse.builder()
        .identifier(location.getIdentifier())
        .type(location.getType())
        .geometry(location.getGeometry())
        .properties(
            LocationPropertyResponse.builder()
                .name(location.getName())
                .status(location.getStatus())
                .externalId(location.getExternalId())
                .geographicLevel(location.getGeographicLevel().getName())
                .childrenNumber(childrenNumber)
                .build())
        .build();
  }

  public static LocationResponse fromEntitySummary(Location location) {
    return LocationResponse.builder().identifier(location.getIdentifier())
        .type(location.getType()).properties(
            LocationPropertyResponse.builder().name(location.getName()).status(location.getStatus())
                .externalId(location.getExternalId())
                .geographicLevel(location.getGeographicLevel().getName()).build()).build();
  }

  public static Page<LocationResponse> fromEntityPage(Page<Location> locations, Pageable pageable,
      SummaryEnum summary) {
    var locationsResponseContent = locations.getContent().stream()
        .map(summary.equals(SummaryEnum.TRUE) ? LocationResponseFactory::fromEntitySummary
            : LocationResponseFactory::fromEntity).collect(
            Collectors.toList());
    return new PageImpl<>(locationsResponseContent, pageable, locations.getTotalElements());
  }

  public static LocationResponse fromPlanLocationDetails(PlanLocationDetails planLocationDetails,
      UUID parentIdentifier) {
    return LocationResponse.builder()
        .identifier(planLocationDetails.getLocation().getIdentifier())
        .type(planLocationDetails.getLocation().getType())
        .geometry(planLocationDetails.getLocation().getGeometry())
        .properties(
            LocationPropertyResponse.builder()
                .name(planLocationDetails.getLocation().getName())
                .status(planLocationDetails.getLocation().getStatus())
                .externalId(planLocationDetails.getLocation().getExternalId())
                .geographicLevel(planLocationDetails.getLocation().getGeographicLevel().getName())
                .numberOfTeams(planLocationDetails.getAssignedTeams())
                .assigned(planLocationDetails.getAssignedLocations() > 0)
                .parentIdentifier(planLocationDetails.getParentLocation() == null ? parentIdentifier
                    : planLocationDetails.getParentLocation().getIdentifier())
                .childrenNumber(planLocationDetails.getChildrenNumber())
                .build())
        .build();
  }

  public static LocationResponse fromElasticModel(LocationElastic locationElastic,
      HierarchyDetailsElastic hierarchyDetailsElastic, List<EntityMetadataResponse> entityMetadataResponses) {
    LocationResponse feature = LocationResponse.builder()
        .geometry(locationElastic.getGeometry())
        .identifier(UUID.fromString(locationElastic.getId()))
        .type("Feature")
        .properties(LocationPropertyResponse.builder()
            .name(locationElastic.getName())
            .metadata(entityMetadataResponses)
            .geographicLevel(locationElastic.getLevel())
            .build())

        .build();

    if (hierarchyDetailsElastic != null) {
      try {
        if (hierarchyDetailsElastic.getParent() != null) {
          feature.getProperties().setParent(UUID.fromString(hierarchyDetailsElastic.getParent()));
        }
        if (hierarchyDetailsElastic.getAncestry() != null && !hierarchyDetailsElastic.getAncestry()
            .isEmpty()) {
          List<String> newAncestry = new ArrayList<>();
          newAncestry.add(locationElastic.getId());
          newAncestry.addAll(hierarchyDetailsElastic.getAncestry());
          feature.setAncestry(newAncestry);
        }

        feature.getProperties()
            .setGeographicLevelNodeNumber(hierarchyDetailsElastic.getGeographicLevelNumber());

      } catch (IllegalArgumentException e) {
        log.error("Cannot set parent Id of location {}", feature.getIdentifier(), e);
      }
    } else {
      feature.getProperties()
          .setGeographicLevelNodeNumber(0);
    }

    return feature;
  }

  public static LocationResponse fromSearchHit(SearchHit hit, Set<String> parents,
      String hierarchyId) throws JsonProcessingException {
    ObjectMapper mapper = new ObjectMapper();
    String source = hit.getSourceAsString();


    hit.getFields().get("meta");
    List<EntityMetadataResponse> collect = new ArrayList<>();
    log.trace("fromSearchHit processing meta");
    if (hit.getFields().size() > 0) {
      if (hit.getFields().containsKey("meta")) {

        DocumentField meta = hit.getFields().get("meta");

        collect = meta.getValues().stream().map(val -> {
          Double valueNumber1 = (Double) ((HashMap<?, ?>) val).get("valueNumber");
          return EntityMetadataResponse.builder()
              .value(valueNumber1 != null ? valueNumber1 : ((HashMap<?, ?>) val).get("value"))
              .type((String) ((HashMap<?, ?>) val).get("tag"))
              .fieldType((String) ((HashMap<?, ?>) val).get("fieldType"))
              .build();
        }).collect(Collectors.toList());

      }

    }
    log.trace("fromSearchHit processed meta");

    LocationElastic locationElastic = mapper.readValue(source, LocationElastic.class);

    log.trace("fromSearchHit unmarshalling location response");

    if (locationElastic.getHierarchyDetailsElastic() != null) {
      Set<String> id = new HashSet<>(List.of(locationElastic.getId()));
      if (locationElastic.getHierarchyDetailsElastic().get(hierarchyId).getAncestry() != null) {
        id.addAll(locationElastic.getHierarchyDetailsElastic().get(hierarchyId).getAncestry());
      }
      if (parents != null){
        parents.addAll(id);
      }
      log.trace("fromSearchHit returning response");
      return fromElasticModel(locationElastic,
          locationElastic.getHierarchyDetailsElastic().get(hierarchyId),collect);
    } else {
      log.trace("fromSearchHit returning response");
      return fromElasticModel(locationElastic, null,collect);
    }
  }
}
