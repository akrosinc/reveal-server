package com.revealprecision.revealserver.api.v1.dto.factory;

import com.revealprecision.revealserver.api.v1.dto.response.GeoTreeResponse;
import com.revealprecision.revealserver.api.v1.dto.response.IdentifierNameResponse;
import com.revealprecision.revealserver.api.v1.dto.response.InstanceResponse;
import com.revealprecision.revealserver.api.v1.dto.response.LocationHierarchyResponse;
import com.revealprecision.revealserver.api.v1.dto.response.PlanResponse;
import com.revealprecision.revealserver.persistence.domain.Instance;
import com.revealprecision.revealserver.persistence.domain.Plan;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class InstanceResponseFactory {

  public static InstanceResponse fromEntity(Instance instance, List<GeoTreeResponse> areas,
        Plan plan , List<UUID>  assignedLocations) {

    markSelectedAreas(areas, assignedLocations);

    return InstanceResponse.builder()
        .identifier(instance.getIdentifier())
        .name(instance.getName())
        .plan(PlanResponseFactory.fromEntity(plan))
        .members(instance.getUsers().stream()
            .map(instanceUser -> IdentifierNameResponse.builder()
                .identifier(instanceUser.getUser().getIdentifier())
                .name(instanceUser.getUser().getFirstName() + " " + instanceUser.getUser()
                    .getLastName()).build())
            .collect(Collectors.toList()))
        .areas(areas)
        .datasets(instance.getEntityTags().stream()
            .map(instanceEntityTag -> IdentifierNameResponse.builder()
                .identifier(instanceEntityTag.getEntityTag().getIdentifier())
                .name(instanceEntityTag.getEntityTag().getTag())
                .build())
            .collect(Collectors.toList()))
        .locationHierarchy(List.of(LocationHierarchyResponse.builder()
            .identifier(instance.getLocationHierarchy().getIdentifier().toString())
            .name(instance.getLocationHierarchy().getName()).build()))
        .build();
  }

  private static void markSelectedAreas(List<GeoTreeResponse> areas, List<UUID> selectedIds) {
    if (areas == null) {
      return;
    }
    areas.forEach(area -> {
      area.setSelected(selectedIds.contains(area.getIdentifier()));
      markSelectedAreas(area.getChildren(), selectedIds);
    });
  }
}
