package com.revealprecision.revealserver.api.v1.dto.response;

import java.util.UUID;
import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class GroupManagementResponse {
  private UUID identifier;
  private String name;
}
