package com.revealprecision.revealserver.api.v1.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.revealprecision.revealserver.enums.GroupTypeEnum;
import java.util.List;
import lombok.*;

import java.util.Set;
import java.util.UUID;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class GroupResponse {

  UUID identifier;

  String name;

  GroupTypeEnum type;

  UUID locationIdentifier;

  Relationships relationships;

  @Data
  @Builder
  public static class Relationships{
    List<PersonResponse> person;
  }
}
