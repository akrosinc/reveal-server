package com.revealprecision.revealserver.api.v1.dto.response;

import java.util.List;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Setter @Getter
@Builder
public class ComplexTagDto {

  private String id;

  private String hierarchyId;

  private String hierarchyType;

  private String tagName;

  private List<TagWithFormulaSymbol> tags;

  private String formula;

  @Builder
  @Setter @Getter
  public static class TagWithFormulaSymbol{

    private String name;

    private String symbol;
  }
}
