package com.revealprecision.revealserver.messaging.message;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
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
public class TaskAggregate extends Message {


  private List<TaskLocationPair> taskIds = new ArrayList<>();

  public static List<TaskLocationPair> removeAllExcept(List<TaskLocationPair> taskIds, String id){
    return taskIds.stream().filter(taskId -> !taskId.getId()
        .equals(id)).collect(Collectors.toList());
  }
}
