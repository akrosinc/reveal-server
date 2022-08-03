package com.revealprecision.revealserver.messaging.listener;

import com.revealprecision.revealserver.constants.KafkaConstants;
import com.revealprecision.revealserver.messaging.message.UserData;
import com.revealprecision.revealserver.messaging.message.UserDataParentChild;
import com.revealprecision.revealserver.messaging.message.UserLevel;
import com.revealprecision.revealserver.messaging.message.UserPerformanceData;
import com.revealprecision.revealserver.props.KafkaProperties;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserDataListener extends Listener {

  private final KafkaProperties kafkaProperties;

  private final KafkaTemplate<String, UserPerformanceData> performanceDataKafkaTemplate;

  private final KafkaTemplate<String, UserDataParentChild> userDataParentChildKafkaTemplate;

  @KafkaListener(topics = "#{kafkaConfigProperties.topicMap.get('USER_DATA')}", groupId = "reveal_server_group")
  public void listenGroupFoo(UserData message) {
    init();

    List<UserLevel> stringList = new ArrayList<>();
    if (message.getFieldWorker() != null) {
      stringList.add(
          new UserLevel(message.getFieldWorker(), message.getFieldWorker(), 0, "fieldWorker",
              message.getFieldWorkerLabel()));
    }
    if (message.getDeviceUser() != null) {
      stringList.add(
          new UserLevel(message.getDeviceUser().getDeviceUserIdentifier().toString(),
              message.getDeviceUser()
                  .getDeviceUserName(), 0, "deviceUser", message.getDeviceUserLabel()));
    }
    if (!message.getOrgHierarchy().isEmpty()) {
      message.getOrgHierarchy().forEach(orgHierarchies -> {
        stringList.addAll(orgHierarchies.stream().map(
                orgHierarchy -> new UserLevel(orgHierarchy.getOrgId(), orgHierarchy.getName(),
                    orgHierarchy.getLevel(), "organization", message.getOrgLabel()))
            .collect(Collectors.toList()));
        Map<String, Object> fields = message.getFields();
        performanceDataKafkaTemplate.send(
            kafkaProperties.getTopicMap().get(KafkaConstants.USER_PERFORMANCE_DATA),
            new UserPerformanceData(message.getPlanIdentifier(), stringList,
                message.getCaptureTime(),
                fields));
      });
      IntStream.range(0, stringList.size() - 1).forEach(
          i -> {
            if (i + 1 <= stringList.size() - 1) {

              userDataParentChildKafkaTemplate.send(
                  kafkaProperties.getTopicMap().get(KafkaConstants.USER_PARENT_CHILD),
                  new UserDataParentChild(message.getPlanIdentifier(), stringList.get(i + 1),
                      stringList.get(i)));
            }
          }
      );

    }
  }
}
