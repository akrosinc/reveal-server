package com.revealprecision.revealserver.messaging.message.mdalite;

import com.revealprecision.revealserver.messaging.message.Message;
import java.util.HashSet;
import java.util.Set;
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
public class MDALiteSupervisorCddListAggregation extends Message {
  private Set<String> cddNames = new HashSet<>();
}
