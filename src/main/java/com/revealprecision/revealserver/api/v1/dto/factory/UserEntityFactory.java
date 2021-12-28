package com.revealprecision.revealserver.api.v1.dto.factory;

import com.revealprecision.revealserver.api.v1.dto.request.UserRequest;
import com.revealprecision.revealserver.batch.dto.UserBatchDTO;
import com.revealprecision.revealserver.persistence.domain.Organization;
import com.revealprecision.revealserver.persistence.domain.User;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.Set;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class UserEntityFactory {

    public static User toEntity(UserRequest request, Set<Organization> organizations) {
        return User.builder()
                .userName(request.getUserName())
                .email(request.getEmail())
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .securityGroups(request.getSecurityGroups())
                .organizations(organizations)
                .build();
    }

    public static User toEntity(UserBatchDTO userBatchDTO) {
        return User.builder()
                .userName(userBatchDTO.getUserName())
                .email(userBatchDTO.getEmail())
                .firstName(userBatchDTO.getFirstName())
                .lastName(userBatchDTO.getLastName())
                .securityGroups(userBatchDTO.getSecurityGroups())
                .build();
    }
}
