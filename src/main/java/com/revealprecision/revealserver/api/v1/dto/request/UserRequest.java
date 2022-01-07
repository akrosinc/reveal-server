package com.revealprecision.revealserver.api.v1.dto.request;

import com.sun.istack.Nullable;
import java.util.Set;
import java.util.UUID;
import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.validator.constraints.Length;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class UserRequest {

  @Pattern(regexp = "^[a-z]+([._]?[a-z]+)*$", message = "must match regex")
  @NotBlank(message = "must not be empty")
  private String username;

  @NotBlank(message = "must not be empty")
  private String firstName;

  @NotBlank(message = "must not be empty")
  private String lastName;

  @Email
  @Nullable
  @Size(min = 3, message = "minimum number of characters is 3")
  private String email;

  @NotBlank(message = "must not be empty")
  @Length(min = 5, message = "minimum number of characters is 5")
  private String password;

  @NotNull
  private boolean tempPassword;

  @NotNull
  private Set<UUID> organizations;

  @NotNull
  private Set<String> securityGroups;
}
