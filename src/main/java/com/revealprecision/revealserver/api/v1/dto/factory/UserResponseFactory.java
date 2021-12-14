package com.revealprecision.revealserver.api.v1.dto.factory;

import com.revealprecision.revealserver.api.v1.dto.response.UserResponse;
import com.revealprecision.revealserver.persistence.domain.User;
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
        .map(OrganizationResponseFactory::fromEntityWithChild).collect(
            Collectors.toSet());
    return UserResponse.builder()
        .identifier(user.getIdentifier())
        .firstName(user.getFirstName())
        .lastName(user.getLastName())
        .userName(user.getUserName())
        .email(user.getEmail())
        .organizations(organizations)
        .build();
  }

  public static Page<UserResponse> fromEntityPage(Page<User> users, Pageable pageable) {
    var response = users.getContent().stream()
        .map(UserResponseFactory::fromEntity)
        .collect(Collectors.toList());
    return new PageImpl<>(response, pageable, users.getTotalElements());
  }
}
