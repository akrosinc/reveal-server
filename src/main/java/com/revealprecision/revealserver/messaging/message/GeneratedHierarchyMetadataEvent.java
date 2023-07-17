package com.revealprecision.revealserver.messaging.message;


import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldNameConstants;

@FieldNameConstants
@EqualsAndHashCode(callSuper = true)
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Data
public class GeneratedHierarchyMetadataEvent extends Message  {

  private int id;

  private String locationIdentifier;
  private String tag;
  private Double value;
  private List<String> ancestry;
  private String parent;
  private Integer geographicLevelNumber;
  private GeneratedHierarchyEvent generatedHierarchy;

}
