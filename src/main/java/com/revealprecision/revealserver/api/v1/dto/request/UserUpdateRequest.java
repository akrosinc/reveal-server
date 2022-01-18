package com.revealprecision.revealserver.api.v1.dto.request;

import com.sun.istack.Nullable;
import lombok.*;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.util.Set;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserUpdateRequest {

    @NotBlank(message = "must not be empty")
    private String firstName;

    @NotBlank(message = "must not be empty")
    private String lastName;

    @Email
    @Nullable
    @Size(min = 3, message = "minimum number of characters is 3")
    private String email;

    @NotNull
    private Set<UUID> organizations;

    @NotNull
    private Set<String> securityGroups;
}
