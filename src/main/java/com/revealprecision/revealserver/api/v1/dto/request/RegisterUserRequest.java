package com.revealprecision.revealserver.api.v1.dto.request;

import com.sun.istack.Nullable;
import lombok.*;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.Set;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class RegisterUserRequest {

    @NotBlank(message = "must not be empty")
    private String firstName;

    @NotBlank(message = "must not be empty")
    private String lastName;

    @Email
    @NotBlank(message = "must not be empty")
    private String email;

    @Nullable
    private Set<UUID> organizations;

    @NotNull
    private Set<String> securityGroups;
}
