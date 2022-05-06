package com.revealprecision.revealserver.persistence.domain.logging;

import com.fasterxml.jackson.databind.JsonNode;
import com.vladmihalcea.hibernate.type.json.JsonBinaryType;
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
import lombok.experimental.FieldNameConstants;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;

@FieldNameConstants
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@TypeDef(name = "jsonb", typeClass = JsonBinaryType.class)
public class HttpLogging {

  @Id
  @GeneratedValue
  private UUID identifier;

  private String traceId;
  private String spanId;
  private String httpMethod;
  private String httpCode;

  @Type(type = "jsonb")
  @Column(columnDefinition = "jsonb")
  private JsonNode httpHeaders;
  private String path;

  private LocalDateTime requestTime;

  private LocalDateTime responseTime;

  @Type(type = "jsonb")
  @Column(columnDefinition = "jsonb")
  private JsonNode request;

  @Type(type = "jsonb")
  @Column(columnDefinition = "jsonb")
  private JsonNode response;

  private String requestor;

  private String jwtKid;

}
