package com.revealprecision.revealserver.api.v1.dto.factory;

import com.revealprecision.revealserver.api.v1.dto.response.GlobalUserResponse;
import com.revealprecision.revealserver.api.v1.dto.response.UserResponse;
import com.revealprecision.revealserver.persistence.domain.User;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class UserResponseFactory {


  public static UserResponse fromEntity(User user) {
    var organizations = user.getOrganizations().stream()
        .map(OrganizationResponseFactory::fromEntityWithoutChild).collect(
            Collectors.toSet());
    return UserResponse.builder()
        .identifier(user.getIdentifier())
        .sid(user.getSid())
        .firstName(user.getFirstName())
        .lastName(user.getLastName())
        .username(user.getUsername())
        .email(user.getEmail())
        .organizations(organizations)
        .securityGroups(user.getSecurityGroups())
        .build();
  }

  public static Page<UserResponse> fromEntityPage(Page<User> users, Pageable pageable) {
    var response = users.getContent().stream()
        .map(UserResponseFactory::fromEntity)
        .collect(Collectors.toList());
    return new PageImpl<>(response, pageable, users.getTotalElements());
  }

  public static UserResponse fromEntityWithoutOrganizations(User user) {
    return UserResponse.builder()
            .identifier(user.getIdentifier())
            .sid(user.getSid())
            .firstName(user.getFirstName())
            .lastName(user.getLastName())
            .username(user.getUsername())
            .email(user.getEmail())
            .securityGroups(user.getSecurityGroups())
            .build();
  }

  public static Page<GlobalUserResponse> toGlobalUserResponsePage(
      Page<User> users,
      Pageable pageable,
      Map<UUID, List<String>> instanceUserMap) {

    List<GlobalUserResponse> response = users.getContent().stream()
        .map(user -> GlobalUserResponse.builder()
            .identifier(user.getIdentifier())
            .sid(user.getSid())
            .firstName(user.getFirstName())
            .lastName(user.getLastName())
            .username(user.getUsername())
            .email(user.getEmail())
            .instances(instanceUserMap.getOrDefault(user.getIdentifier(), List.of()))
            .build())
        .collect(Collectors.toList());

    return new PageImpl<>(response, pageable, users.getTotalElements());
  }
}
