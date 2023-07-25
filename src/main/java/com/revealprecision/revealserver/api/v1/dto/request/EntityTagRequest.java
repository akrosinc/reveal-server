package com.revealprecision.revealserver.api.v1.dto.request;

import java.util.List;
import javax.validation.constraints.NotBlank;
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

  private String identifier;

  private String tag;

  @NotBlank(message = "must not be blank")
  private String valueType;

  private String definition;

  private List<String> aggregationMethod;

  private boolean isAggregate;

  private List<EntityTagItem> tags;


}
