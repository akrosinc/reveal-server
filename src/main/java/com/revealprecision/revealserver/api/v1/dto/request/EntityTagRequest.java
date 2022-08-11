package com.revealprecision.revealserver.api.v1.dto.request;

import com.revealprecision.revealserver.enums.LookupEntityTypeCodeEnum;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
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
public class EntityTagRequest {

  @NotBlank(message = "must not be blank")
  private String tag;

  @NotBlank(message = "must not be blank")
  private String valueType;

  private String definition;

  @NotNull
  private LookupEntityTypeCodeEnum entityType;

  private Map<String,String> formFieldNames;

  private boolean generated;
  private List<String> referencedFields;
  private List<String> aggregationMethod;
  private String generationFormula;
  private String scope;
  private String resultExpression;
  private boolean isResultLiteral;
  private boolean addToMetadata;

  private boolean isAggregate;

}
