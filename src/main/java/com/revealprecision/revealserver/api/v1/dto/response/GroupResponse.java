package com.revealprecision.revealserver.api.v1.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.revealprecision.revealserver.enums.GroupTypeEnum;
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
}
