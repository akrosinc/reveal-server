package com.revealprecision.revealserver.service.models;

import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class PersonSearchCriteria {
  String firstName;
  String lastName;
  String gender;
  String location;
  String group;
  String birthdate;
  String fromDate;
  String toDate;
}
