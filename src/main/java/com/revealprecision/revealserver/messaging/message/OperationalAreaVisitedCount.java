package com.revealprecision.revealserver.messaging.message;

import java.util.List;
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
public class OperationalAreaVisitedCount extends Message {
  Long count;
  List<String> listOfKeys;
}
