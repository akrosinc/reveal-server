package com.revealprecision.revealserver.api.v1.dto.request;

import lombok.*;
import org.hibernate.validator.constraints.Length;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserPasswordRequest {

    @Length(min = 5)
    private String password;
    private boolean tempPassword;
}
