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

  /**
   * Optional task details from the client (limitCounter, pass). Null from callers that only update
   * status (e.g. event processing, cleanup), in which case existing details are left untouched.
   */
  private TaskFacade.Details details;
}
