package com.revealprecision.revealserver.api.v1.dto.request;

import java.util.Set;
import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class UserRequest {

  @NotBlank(message = "must not be empty")
  private String userName;

  @NotBlank(message = "must not be empty")
  private String firstName;

  @NotBlank(message = "must not be empty")
  private String lastName;

  @NotBlank(message = "must not be empty")
  @Email
  private String email;

  @NotBlank(message = "must not be empty")
  private String password;

  @NotNull
  private boolean tempPassword;

  @NotNull
  private Set<String> organizations;

  @NotNull
  private Set<String> securityGroups;
}
