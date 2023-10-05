package com.revealprecision.revealserver.persistence.domain;

import java.time.LocalDateTime;
import java.util.UUID;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Setter @Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserConfig {

  @Id
  @GeneratedValue
  private UUID id;

  private String username;

  @Column(name = "createdat")
  private LocalDateTime createdAt;
  @Column(name = "receivedat")
  private LocalDateTime receivedAt;

  private String filename;

  private boolean received;
}
