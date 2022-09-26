package com.revealprecision.revealserver.messaging.listener;

import com.revealprecision.revealserver.constants.KafkaConstants;
import com.revealprecision.revealserver.enums.PlanInterventionTypeEnum;
import com.revealprecision.revealserver.messaging.message.UserData;
import com.revealprecision.revealserver.messaging.message.UserDataParentChild;
import com.revealprecision.revealserver.messaging.message.UserLevel;
import com.revealprecision.revealserver.messaging.message.UserPerformanceData;
import com.revealprecision.revealserver.persistence.domain.PerformanceEventTracker;
import com.revealprecision.revealserver.persistence.domain.PerformanceUserType;
import com.revealprecision.revealserver.persistence.domain.Plan;
import com.revealprecision.revealserver.persistence.repository.PerformanceEventTrackerRepository;
import com.revealprecision.revealserver.persistence.repository.PerformanceUserTypeRepository;
import com.revealprecision.revealserver.props.KafkaProperties;
import com.revealprecision.revealserver.service.PlanService;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
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
  private final PlanService planService;
  private final PerformanceEventTrackerRepository performanceEventTrackerRepository;
  private final PerformanceUserTypeRepository performanceUserTypeRepository;

  private final KafkaTemplate<String, UserPerformanceData> performanceDataKafkaTemplate;

  private final KafkaTemplate<String, UserDataParentChild> userDataParentChildKafkaTemplate;

//  @KafkaListener(topics = "#{kafkaConfigProperties.topicMap.get('USER_DATA')}", groupId = "reveal_server_group")
  public void listenGroupFoo(UserData message) {
    log.info("Received Message {}", message);
    init();

    PerformanceEventTracker previousPerformanceEvent = null;
    if (message.getSubmissionId() != null) {
      previousPerformanceEvent = performanceEventTrackerRepository.findPerformanceEventTrackerBySubmissionId(
          message.getSubmissionId());
    }

    processUserData(message,false);
    if (previousPerformanceEvent != null) {
      processUserData(previousPerformanceEvent.getUserData(),true);
      previousPerformanceEvent.setUserData(message);
      performanceEventTrackerRepository.save(previousPerformanceEvent);
    } else {
      if (message.getSubmissionId() != null) {
        PerformanceEventTracker performanceEventTracker = new PerformanceEventTracker();
        performanceEventTracker.setSubmissionId(message.getSubmissionId());
        performanceEventTracker.setUserData(message);
        performanceEventTracker.setPlanIdentifier(message.getPlanIdentifier());
        performanceEventTrackerRepository.save(performanceEventTracker);
      }
    }
  }

  private void processUserData(UserData message, boolean isUndo) {
    Plan plan = planService.findPlanByIdentifier(message.getPlanIdentifier());

    if (plan.getInterventionType().getCode().equals(PlanInterventionTypeEnum.IRS_LITE.name())){
      processUserDataIRSLite( message,  isUndo);
    } else {

      if (message.getFieldWorker() != null) {
        saveUser(message.getFieldWorker(), "fieldWorker", message.getPlanIdentifier());
      }
      if (message.getDeviceUser() != null) {
        saveUser(message.getDeviceUser().getDeviceUserName(), "deviceUser",
            message.getPlanIdentifier());
      }

      if (message.getDistrict() != null) {
        saveUser(message.getDistrict(), "district", message.getPlanIdentifier());

      }
    }

  }

  private void saveUser(String user, String type, UUID planIdentifier) {
    PerformanceUserType performanceUserTypeByPlanIdentifierAndUser = performanceUserTypeRepository.findPerformanceUserTypeByPlanIdentifierAndUserString(
        planIdentifier, user);
    if (performanceUserTypeByPlanIdentifierAndUser == null) {
      performanceUserTypeRepository.save(
          PerformanceUserType.builder().planIdentifier(planIdentifier).userString(user).typeString(type).build());
    }
  }

  private void processUserDataIRSLite(UserData message, boolean isUndo) {
    List<UserLevel> stringList = new ArrayList<>();
    if (message.getFieldWorker() != null) {
      stringList.add(
          new UserLevel(message.getFieldWorker(), message.getFieldWorker(), 0, "fieldWorker",
              message.getFieldWorkerLabel()));
    }
    if (message.getDeviceUser() != null) {
      stringList.add(new UserLevel(message.getDeviceUser().getDeviceUserIdentifier().toString(),
          message.getDeviceUser().getDeviceUserName(), 0, "deviceUser",
          message.getDeviceUserLabel()));
    }

    if (!message.getOrgHierarchy().isEmpty()) {
      message.getOrgHierarchy().forEach(orgHierarchies -> {
        stringList.addAll(orgHierarchies.stream().map(
                orgHierarchy -> new UserLevel(orgHierarchy.getOrgId(), orgHierarchy.getName(),
                    orgHierarchy.getLevel(), "organization", message.getOrgLabel()))
            .collect(Collectors.toList()));

      });

      if (message.getDistrict() != null) {
        stringList.add(new UserLevel(message.getDistrict(), message.getDistrict(), 0, "district",
            message.getDistrictLabel()));

      }

      Map<String, Object> fields = message.getFields();
      performanceDataKafkaTemplate.send(
          kafkaProperties.getTopicMap().get(KafkaConstants.USER_PERFORMANCE_DATA),
          new UserPerformanceData(message.getSubmissionId(), message.getPlanIdentifier(),
              stringList, message.getCaptureTime(), fields, isUndo));

      IntStream.range(0, stringList.size() - 1).forEach(i -> {
        if (i + 1 <= stringList.size() - 1) {

          userDataParentChildKafkaTemplate.send(
              kafkaProperties.getTopicMap().get(KafkaConstants.USER_PARENT_CHILD),
              new UserDataParentChild(message.getPlanIdentifier(), stringList.get(i + 1),
                  stringList.get(i)));

        }

      });
    }
    if (stringList.get(stringList.size() - 1).getType().equals("district")) {
      userDataParentChildKafkaTemplate.send(
          kafkaProperties.getTopicMap().get(KafkaConstants.USER_PARENT_CHILD),
          new UserDataParentChild(message.getPlanIdentifier(),
              new UserLevel("highest", "highest", 0, "highest", "highest"),
              stringList.get(stringList.size() - 1)));
    }


  }
}
