package com.revealprecision.revealserver.service.models;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.springframework.web.bind.annotation.RequestParam;

@Builder
@Getter
@Setter
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
