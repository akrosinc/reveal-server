package com.revealprecision.revealserver.messaging.message;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldNameConstants;

@FieldNameConstants
@Builder
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Data
public class GeneratedHierarchyEvent extends Message {

  private String id;

  private String name;

  private JsonNode entityQuery;

  private List<String> nodeOrder;

}
