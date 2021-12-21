package com.revealprecision.revealserver.api.v1.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import java.util.Set;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserResponse {

    private UUID identifier;
    private String userName;
    private String firstName;
    private String lastName;
    private String email;
    private Set<OrganizationResponse> organizations;
    private Set<String> securityGroups;
}
