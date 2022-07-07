package com.revealprecision.revealserver.messaging.message;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class LocationPersonBusinessStateCountAggregate extends Message {

  private Map<String, List<String>> structureBusinessStatePersonMap = new HashMap<>();

  private Map<String, Long> structureBusinessStateCountMap = new HashMap<>();

  private Map<String, String> personBusinessStatusMap = new HashMap<>();

  private boolean isTreated = false;

  private UUID locationHierarchy;

  private UUID ancestor;

  private List<UUID> ancestry;

  private Set<String> treatedLocations = new HashSet<>();

  private String locationIdentifier;
}
