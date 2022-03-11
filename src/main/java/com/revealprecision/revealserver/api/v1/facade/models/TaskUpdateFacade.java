package com.revealprecision.revealserver.api.v1.facade.models;


import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;

@Data
@Setter
@Getter
@Builder
@FieldNameConstants
@JsonInclude(value = Include.NON_NULL)
@AllArgsConstructor
@NoArgsConstructor
public class TaskUpdateFacade implements Serializable {
  private String identifier;
  private String status;
  private String businessStatus;
  private Long serverVersion;
}
