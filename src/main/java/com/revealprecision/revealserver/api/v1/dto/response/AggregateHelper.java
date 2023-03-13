package com.revealprecision.revealserver.api.v1.dto.response;


import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class AggregateHelper {

  String identifier;
  List<String> ancestry;
  List<EntityMetadataResponse> entityMetadataResponses;
  String geographicLevel;
  List<String> filteredNodeOrder;
}
