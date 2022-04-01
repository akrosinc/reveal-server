package com.revealprecision.revealserver.api.v1.facade.models;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;

@Setter
@Getter
@Builder
@FieldNameConstants
@JsonInclude(value = Include.NON_NULL)
@AllArgsConstructor
@NoArgsConstructor
public class ClientFacade extends BaseEntity {

  private String firstName;

  private String middleName;

  private String lastName;

  private String birthdate;

  private String deathdate;

  private Boolean birthdateApprox;

  private Boolean deathdateApprox;

  private String gender;

  private String clientType;

  private Map<String, List<String>> relationships;

  private String teamId;

  private String locationId;

}
