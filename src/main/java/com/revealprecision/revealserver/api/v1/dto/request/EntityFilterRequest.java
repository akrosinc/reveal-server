package com.revealprecision.revealserver.api.v1.dto.request;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class EntityFilterRequest {

  private String fieldIdentifier;
  private String fieldType;
  private RangeFilter range;
  private SearchValue searchValue;
  private String valueType;
  private String tag;
  private List<SearchValue> values;
}
