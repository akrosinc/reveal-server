package com.revealprecision.revealserver.messaging.listener;

import com.revealprecision.revealserver.api.v1.controller.TestController.HdssIndividualProjection;
import com.revealprecision.revealserver.api.v1.controller.querying.KafkaGenerateIndividualTasksController.ListObj;
import com.revealprecision.revealserver.enums.ActionTitleEnum;
import com.revealprecision.revealserver.integration.mail.EmailService;
import com.revealprecision.revealserver.messaging.message.EventTrackerMessage;
import com.revealprecision.revealserver.persistence.domain.Action;
import com.revealprecision.revealserver.persistence.domain.Goal;
import com.revealprecision.revealserver.persistence.repository.ActionRepository;
import com.revealprecision.revealserver.persistence.repository.GoalRepository;
import com.revealprecision.revealserver.persistence.repository.HdssCompoundsRepository;
import com.revealprecision.revealserver.service.TaskService;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
@Profile("KafkaMessaging & (Listening | hdss-processing)")
public class HdssProcessingListener extends Listener {

  public static final String POSITIVE = "Positive";
  private final HdssCompoundsRepository hdssCompoundsRepository;

  public static String INDIVIDUAL = "individual";

  public static String COMPOUND = "compound";

  public static String HOUSEHOLD = "household";

  public static String RDT = "rdt";

  private final TaskService taskService;

  private final EmailService emailService;

  private final GoalRepository goalRepository;

  private final ActionRepository actionRepository;

  @KafkaListener(topics = "#{kafkaConfigProperties.topicMap.get('HDSS_PROCESSING')}", groupId = "reveal_server_group")
  public void etl(EventTrackerMessage eventTrackerMessage) {
    log.info("Received Message in group foo: {}", eventTrackerMessage.toString());
    Map<String, List<Object>> observations = eventTrackerMessage.getObservations();
    String individual = getValue(observations, INDIVIDUAL);
    String compound = getValue(observations, COMPOUND);
    String household = getValue(observations, HOUSEHOLD);
    String rdt = getValue(observations, RDT);
    String owner = eventTrackerMessage.getSupervisor();
    if (individual != null && rdt != null && rdt.equals(POSITIVE)) {

      UUID indexStructure = hdssCompoundsRepository.getStructureByIndividualId(
          individual);

      String indexHousehold = hdssCompoundsRepository.getHouseHoldByIndividualId(
          individual);

      List<String> compoundId = hdssCompoundsRepository.getDistinctCompoundsByHouseholdId(
          indexHousehold);

      List<UUID> allStructuresInCompound = hdssCompoundsRepository.getDistinctStructuresByCompoundId(
          compoundId);

      HdssIndividualProjection indexIndividual = hdssCompoundsRepository.getIndividualByIndividualId(
          individual);

      List<HdssIndividualProjection> allIndividualsInCompound = hdssCompoundsRepository.getAllIndividualsByCompoundId(
          compound);

      List<String> allHouseholdsInCompound = hdssCompoundsRepository.getDistinctHouseholdsByCompoundId(
          compoundId);

      allStructuresInCompound.remove(indexStructure);

      emailService.sendEmail("tbahadur@akros.com", "individual " + individual,
          indexStructure.toString().concat("-").concat(indexHousehold).concat("\r\n\r\n")
              .concat(allStructuresInCompound.stream().map(UUID::toString).collect(
                  Collectors.joining(","))).concat("\r\n\r\n")
              .concat(String.join(",", allHouseholdsInCompound)));

      UUID planIdentifier = UUID.fromString("fbbac8f8-9ce0-46ca-bfbf-02915152554a");

      List<Goal> goalsByPlan_identifier = goalRepository.findGoalsByPlan_Identifier(planIdentifier);

      List<Action> actions = goalsByPlan_identifier.stream()
          .flatMap(
              goal -> actionRepository.findActionsByGoal_Identifier(goal.getIdentifier()).stream())
          .collect(
              Collectors.toList());

      submitTasks(owner, List.of(indexStructure), planIdentifier, actions, ActionTitleEnum.INDEX_CASE);

      submitTasks(owner, List.of(UUID.fromString(indexIndividual.getId())), planIdentifier, actions, ActionTitleEnum.INDEX_CASE_MEMBER);

      submitTasks(owner, allStructuresInCompound, planIdentifier, actions, ActionTitleEnum.RCD);

      submitTasks(owner, allIndividualsInCompound.stream().map(individualObj->UUID.fromString(individualObj.getId())).collect(
          Collectors.toList()), planIdentifier, actions, ActionTitleEnum.RCD_MEMBER);
    }
  }

  private void submitTasks(String owner, List<UUID> entityIds, UUID planIdentifier, List<Action> actions, ActionTitleEnum actionEnum) {

    Optional<Action> optionalAction = actions.stream()
        .filter(action -> action.getTitle().equals(actionEnum.getActionTitle()))
        .findAny();

    if (optionalAction.isPresent()) {
      ListObj uuidsObj = new ListObj();
      uuidsObj.setUuids(entityIds);
      taskService.generateIndividualTaskWithOwner(planIdentifier, optionalAction.get().getIdentifier(),
          uuidsObj, owner);
    }
  }

  private String getValue(Map<String, List<Object>> observations, String key) {
    if (observations.containsKey(key)) {
      List<Object> objects = observations.get(key);
      if (objects.size() > 0) {
        Object o = objects.get(0);
        try {
          String string = (String) o;
          return string;
        } catch (ClassCastException e) {
          log.error("cannot cast to String: {}", o);
        }
      }
      return null;
    }
    return null;
  }

}
