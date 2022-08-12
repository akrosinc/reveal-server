package com.revealprecision.revealserver.api.v1.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class EntityTagResponse {

  private UUID identifier;

  private String tag;

  private String valueType;

  private String definition;

  private String fieldType;

  private LookupEntityTypeResponse lookupEntityType;

  private String resultExpression;

  private String generationFormula;

  private List<String> referenceFields;

  private boolean isResultLiteral;

  private boolean isGenerated;

  private boolean addToMetadata;

  private boolean isAggregate;
}
