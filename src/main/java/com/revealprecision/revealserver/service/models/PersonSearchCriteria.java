package com.revealprecision.revealserver.service.models;

import com.revealprecision.revealserver.enums.GenderEnum;
import com.revealprecision.revealserver.enums.GroupTypeEnum;
import java.util.UUID;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Builder
@Getter
@Setter

public class PersonSearchCriteria {

  String firstName;
  String lastName;
  GenderEnum gender;
  String locationName;
  UUID locationIdentifier;
  String groupName;
  UUID groupIdentifier;
  String birthdate;
  String fromDate;
  String toDate;
}
