package com.revealprecision.revealserver.api.v1.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@ToString
public class EntityMetadataResponse {
  private Object value;
  private String type;
  private String fieldType;
  private UUID datasetId;

  public EntityMetadataResponse(Object value, String type, String fieldType){
    this.value = value;
    this.type = type;
    this.fieldType = fieldType;
  }
}
