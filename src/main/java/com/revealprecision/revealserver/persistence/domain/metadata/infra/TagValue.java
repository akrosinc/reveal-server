package com.revealprecision.revealserver.persistence.domain.metadata.infra;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import java.io.Serializable;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@JsonInclude(value = Include.NON_NULL)
public class TagValue implements Serializable {

  private String valueString;
  private Integer valueInteger;
  private Double valueDouble;
  private LocalDateTime valueDate;
  private Boolean valueBoolean;
}
