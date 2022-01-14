package com.revealprecision.revealserver.api.v1.dto.request;

import java.util.List;
import javax.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.validator.constraints.UniqueElements;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class LocationHierarchyRequest {

  @NotEmpty(message = "location_hierarchy name is required and must not be empty")
  private String name;

  @NotEmpty(message = "node order list  is required and must not be empty")
  @UniqueElements(message = "duplicate nodes in hierarchy")
  private List<String> nodeOrder;

}
