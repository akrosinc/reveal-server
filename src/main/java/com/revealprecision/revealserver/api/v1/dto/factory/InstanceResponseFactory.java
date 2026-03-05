package com.revealprecision.revealserver.api.v1.dto.factory;

import com.revealprecision.revealserver.api.v1.dto.response.EntityTagResponse;
import com.revealprecision.revealserver.api.v1.dto.response.InstanceResponse;
import com.revealprecision.revealserver.api.v1.dto.response.LocationHierarchyResponse;
import com.revealprecision.revealserver.api.v1.dto.response.LocationPropertyResponse;
import com.revealprecision.revealserver.api.v1.dto.response.LocationResponse;
import com.revealprecision.revealserver.api.v1.dto.response.MemberResponse;
import com.revealprecision.revealserver.persistence.domain.Instance;
import java.util.List;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class InstanceResponseFactory {

  public static InstanceResponse fromEntity(Instance instance) {
    return InstanceResponse.builder()
        .identifier(instance.getIdentifier())
        .name(instance.getName())
        .members(instance.getUsers().stream()
            .map(instanceUser -> MemberResponse.builder()
                .identifier(instanceUser.getUser().getIdentifier())
                .name(instanceUser.getUser().getFirstName() + " " + instanceUser.getUser()
                    .getLastName()).build())
            .collect(Collectors.toList()))
        .areas(instance.getLocations().stream()
            .map(instanceLocation -> LocationResponse.builder()
                .identifier(instanceLocation.getLocation().getIdentifier())
                .properties(LocationPropertyResponse.builder()
                    .name(instanceLocation.getLocation().getName()).build())
                .build())
            .collect(Collectors.toList()))
        .datasets(instance.getEntityTags().stream()
            .map(instanceEntityTag -> EntityTagResponseFactory.fromEntity(
                instanceEntityTag.getEntityTag()))
            .collect(Collectors.toList()))
        .locationHierarchy(List.of(LocationHierarchyResponse.builder()
            .identifier(instance.getLocationHierarchy().getIdentifier().toString())
            .name(instance.getLocationHierarchy().getName()).build()))
        .build();
  }

  public static Page<InstanceResponse> fromInstancePage(Page<Instance> instances, Pageable pageable) {
    var instancesContent = instances.getContent().stream()
        .map(InstanceResponseFactory::fromEntity).collect(Collectors.toList());
    return new PageImpl<>(instancesContent, pageable,
        instances.getTotalElements());
  }
}
