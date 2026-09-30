package com.revealprecision.revealserver.persistence.domain;

import com.revealprecision.revealserver.enums.IngestionStage;
import com.revealprecision.revealserver.enums.IngestionType;
import java.time.LocalDateTime;
import java.util.UUID;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IngestionTask {
  @Id
  @GeneratedValue
  private UUID identifier;
  private String taskIdentifier;
  @Enumerated(EnumType.STRING)
  private IngestionType type;
  @Enumerated(EnumType.STRING)
  private IngestionStage stage;
  private String message;
  private LocalDateTime lastUpdated;
}


