package com.revealprecision.revealserver.messaging;

import com.revealprecision.revealserver.messaging.message.Message;
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
public class Submissions extends Message {
  UUID rawEventId;
}
