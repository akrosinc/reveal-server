package com.revealprecision.revealserver.api.v1.dto.request;

import com.revealprecision.revealserver.enums.GenderEnum;
import com.revealprecision.revealserver.enums.NameUseEnum;
import java.time.LocalDate;
import lombok.Builder;
import lombok.Data;

@Data
public class PersonRequest {

  boolean active;
  Name name;
  GenderEnum gender;
  LocalDate birthDate;
  String[] groups;

  @Data
  @Builder
  public static class Name {

    NameUseEnum use;
    String text;
    String family;
    String given;
    String prefix;
    String suffix;
  }


}

