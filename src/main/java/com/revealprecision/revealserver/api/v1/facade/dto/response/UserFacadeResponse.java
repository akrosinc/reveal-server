package com.revealprecision.revealserver.api.v1.facade.dto.response;

import java.util.List;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class UserFacadeResponse {

  private String baseEntityId; //dropped BaseEntity  object just need baseEntityId

  private String username;

  private String firstName;

  private String lastName;

  private String status;

  private List<String> roles;

  private List<String> permissions;

  private String preferredName;

}