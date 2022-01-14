package com.revealprecision.revealserver.api.v1.dto.response;

import com.revealprecision.revealserver.enums.ActionSubjectEnum;
import com.revealprecision.revealserver.enums.ActionTypeEnum;
import java.time.LocalDate;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActionResponse {

  private UUID identifier;
  private String title;
  private String description;
  private LocalDate timingPeriodStart;
  private LocalDate timingPeriodEnd;
  private String code;
  private String reason;
  private ActionSubjectEnum subject;
  private ActionTypeEnum type;
  private String definitionUri;
}
