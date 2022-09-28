package com.revealprecision.revealserver.persistence.domain;

import com.revealprecision.revealserver.messaging.message.UserData;
import com.vladmihalcea.hibernate.type.json.JsonBinaryType;
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
import lombok.experimental.FieldNameConstants;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;

@FieldNameConstants
@Entity
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@TypeDef(name = "jsonb", typeClass = JsonBinaryType.class)
public class PerformanceEventTracker  {

  @Id
  @GeneratedValue
  private UUID identifier;

  @Column(unique = true)
  private UUID planIdentifier;

  @Column(unique = true)
  private String submissionId;

  @Type(type = "jsonb")
  @Column(columnDefinition = "jsonb")
  private UserData userData;

}
