package com.revealprecision.revealserver.api.v1.dto.response;

import com.revealprecision.revealserver.enums.TaskPriorityEnum;
import com.revealprecision.revealserver.persistence.domain.LookupTaskStatus;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class LookupTaskStatusResponse {

  private UUID identifier;

  private String name;

  private String code;

}
