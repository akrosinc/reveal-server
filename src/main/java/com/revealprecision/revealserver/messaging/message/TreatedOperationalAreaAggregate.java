package com.revealprecision.revealserver.messaging.message;

import java.util.HashMap;
import java.util.HashSet;
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
public class TreatedOperationalAreaAggregate extends Message {

  UUID identifier;

  UUID ancestorIdentifier;

  Map<UUID, String> aggregate = new HashMap<>();

  Map<String, Set<UUID>> aggregatedLocations = new HashMap<>();

  Map<String, Long> aggregatedLocationCount = new HashMap<>();

  Set<String> treatedLocations = new HashSet<>();

  Long treatLocationCount = 0L;

  private UUID locationHierarchy;

  private boolean isTreated = false;


}
