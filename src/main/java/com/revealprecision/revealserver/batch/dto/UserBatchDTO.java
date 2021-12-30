package com.revealprecision.revealserver.batch.dto;

import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserBatchDTO {

  private String username;
  private String firstName;
  private String lastName;
  private String email;
  private String password;
  private Boolean tempPassword;
  private Set<String> securityGroups;
  private Set<String> organizations;
}
