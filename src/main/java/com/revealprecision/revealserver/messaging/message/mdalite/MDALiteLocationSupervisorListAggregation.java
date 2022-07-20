package com.revealprecision.revealserver.messaging.message.mdalite;

import com.revealprecision.revealserver.messaging.message.Message;
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
public class MDALiteLocationSupervisorListAggregation extends Message {
  private Map<String,String> supervisorNames = new HashMap<>();
}
