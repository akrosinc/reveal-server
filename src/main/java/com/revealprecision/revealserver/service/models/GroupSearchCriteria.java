package com.revealprecision.revealserver.service.models;

import com.revealprecision.revealserver.enums.GroupTypeEnum;
import java.util.UUID;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Builder
@Getter
@Setter
public class GroupSearchCriteria {

  String groupName;
  GroupTypeEnum groupType;
}
