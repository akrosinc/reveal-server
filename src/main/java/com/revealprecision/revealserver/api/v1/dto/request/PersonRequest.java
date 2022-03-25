package com.revealprecision.revealserver.api.v1.dto.request;

import com.revealprecision.revealserver.enums.GenderEnum;
import java.time.LocalDate;
import java.util.UUID;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import lombok.Data;

@Data
public class PersonRequest {
//TODO: minimum data requirements fort this person object must be specified.
  private UUID identifier;
  private boolean active;
  @Valid
  private PersonName name;
  @NotNull
  private GenderEnum gender;
  private LocalDate birthDate;
  private String[] groups;

}

