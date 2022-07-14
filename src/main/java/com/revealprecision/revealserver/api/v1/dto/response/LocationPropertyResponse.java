package com.revealprecision.revealserver.api.v1.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.revealprecision.revealserver.api.v1.dto.models.ColumnData;
import java.util.List;
import com.revealprecision.revealserver.enums.LocationStatus;
import java.util.Map;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class LocationPropertyResponse {

  private String name;
  private LocationStatus status;
  private UUID externalId;
  private String geographicLevel;
  private Long numberOfTeams;
  private boolean assigned;
  private UUID parentIdentifier;
  private long childrenNumber;
  private Object distCoveragePercent;
  private Object numberOfChildrenTreated;
  private Object numberOfChildrenEligible;
  private Object sprayCoverage;
  private UUID id;
  private Map<String, ColumnData> columnDataMap;
  private List<PersonMainData> persons;
  private List<EntityMetadataResponse> metadata;
}
