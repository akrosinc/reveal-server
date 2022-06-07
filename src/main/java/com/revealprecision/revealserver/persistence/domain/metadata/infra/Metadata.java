package com.revealprecision.revealserver.persistence.domain.metadata.infra;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Setter @Getter
@ToString
public class Metadata implements Serializable {

  private UUID planId;
  private String userId;
  private LocalDateTime createDateTime;
  private LocalDateTime updateDateTime;
  private UUID taskId;

}
