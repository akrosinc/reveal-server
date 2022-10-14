package com.revealprecision.revealserver.persistence.domain.logging;

import com.fasterxml.jackson.databind.JsonNode;
import com.vladmihalcea.hibernate.type.json.JsonBinaryType;
import java.io.Serializable;
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
import lombok.ToString;
import lombok.experimental.FieldNameConstants;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;

@FieldNameConstants
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class HttpLoggingModel implements Serializable {

  private UUID identifier;
  private String traceId;
  private String spanId;
  private String httpMethod;
  private String httpCode;

  private JsonNode httpHeaders;
  private String path;

  private LocalDateTime requestTime;

  private LocalDateTime responseTime;

  private JsonNode request;

  private JsonNode response;

  private String requestor;

  private String jwtKid;

}
