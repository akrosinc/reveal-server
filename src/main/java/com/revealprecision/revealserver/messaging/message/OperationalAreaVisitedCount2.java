package com.revealprecision.revealserver.messaging.message;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@Data
@AllArgsConstructor
@NoArgsConstructor
public class OperationalAreaVisitedCount2 extends Message {
 private Map<UUID, OperationalAreaVisitedCount3> operationalObj = new HashMap<>();
 private Long operationalAreaVisitedCount;

 @Setter
 @Getter
 @Data
 @AllArgsConstructor
 @NoArgsConstructor
 public static class OperationalAreaVisitedCount3{

  Map<String,Long> counts = new HashMap<>();
  boolean operationalAreaIsVisited = false;
 }
}
