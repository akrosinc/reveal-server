package com.revealprecision.revealserver.api.v1.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;
import java.util.Set;
import java.util.UUID;
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
public class GlobalUserResponse {
  private UUID identifier;
  private UUID sid;
  private String firstName;
  private String lastName;
  private String username;
  private String email;
  private List<String> instances;
  private Set<String> securityGroups;
}
