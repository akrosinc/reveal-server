package com.revealprecision.revealserver.api.v1.dto.factory;

import com.revealprecision.revealserver.api.v1.dto.request.UserRequest;
import com.revealprecision.revealserver.batch.dto.UserBatchDTO;
import com.revealprecision.revealserver.persistence.domain.Organization;
import com.revealprecision.revealserver.persistence.domain.User;
import java.util.Set;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class UserEntityFactory {

  public static User toEntity(UserRequest request, Set<Organization> organizations) {
    return User.builder()
        .username(request.getUsername())
        .email(request.getEmail())
        .firstName(request.getFirstName())
        .lastName(request.getLastName())
        .securityGroups(request.getSecurityGroups())
        .organizations(organizations)
        .build();
  }

  public static User toEntity(UserBatchDTO userBatchDTO) {
    return User.builder()
        .username(userBatchDTO.getUsername())
        .email(userBatchDTO.getEmail())
        .firstName(userBatchDTO.getFirstName())
        .lastName(userBatchDTO.getLastName())
        .securityGroups(userBatchDTO.getSecurityGroups())
        .build();
  }
}
