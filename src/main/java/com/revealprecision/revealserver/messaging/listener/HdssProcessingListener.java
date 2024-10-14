package com.revealprecision.revealserver.messaging.listener;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.revealprecision.revealserver.api.v1.controller.querying.KafkaGenerateIndividualTasksController.ListObj;
import com.revealprecision.revealserver.enums.ActionTitleEnum;
import com.revealprecision.revealserver.integration.mail.EmailService;
import com.revealprecision.revealserver.messaging.message.EventTrackerMessage;
import com.revealprecision.revealserver.persistence.domain.Action;
import com.revealprecision.revealserver.persistence.domain.Goal;
import com.revealprecision.revealserver.persistence.projection.HdssIndividualProjection;
import com.revealprecision.revealserver.persistence.repository.ActionRepository;
import com.revealprecision.revealserver.persistence.repository.GoalRepository;
import com.revealprecision.revealserver.persistence.repository.HdssCompoundsRepository;
import com.revealprecision.revealserver.props.HdssProperties;
import com.revealprecision.revealserver.service.TaskService;
import java.io.Serializable;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
@Profile("KafkaMessaging & (Listening | hdss-processing)")
public class HdssProcessingListener extends Listener {

  public static final String POSITIVE = "Positive";
  private final HdssCompoundsRepository hdssCompoundsRepository;

  public static String INDIVIDUAL = "individual";

  public static String INDIVIDUAL_HOUSEHOLD_COMPOUND = "individual_household_compound";

  public static String COMPOUND = "compound";

  public static String HOUSEHOLD = "household";

  public static String RDT = "rdt";

  private final TaskService taskService;

  private final EmailService emailService;

  private final GoalRepository goalRepository;

  private final ActionRepository actionRepository;

  private final HdssProperties hdssProperties;

  private final ObjectMapper objectMapper;


  @KafkaListener(topics = "#{kafkaConfigProperties.topicMap.get('HDSS_PROCESSING')}", groupId = "reveal_server_group")
  public void etl(EventTrackerMessage eventTrackerMessage) {
    log.info("Received Message in group foo: {}", eventTrackerMessage.toString());
    Map<String, List<Object>> observations = eventTrackerMessage.getObservations();

    String individualHouseholdCompound = getValue(observations, INDIVIDUAL_HOUSEHOLD_COMPOUND);
    log.info("individualHouseholdCompound: {}", individualHouseholdCompound);

    List<IndividualHouseholdCompound> individualHouseholdCompounds;
    ObjectReader reader = objectMapper.readerFor(
        new TypeReference<List<IndividualHouseholdCompound>>() {
        });
    try {
      individualHouseholdCompounds = reader.readValue(individualHouseholdCompound);
      log.info("individualHouseholdCompounds: {}", individualHouseholdCompounds);
      if (individualHouseholdCompounds != null && individualHouseholdCompounds.size() > 0) {
        String individual = individualHouseholdCompounds.get(0).getText();
        String compound = getValue(observations, COMPOUND);
        String household = getValue(observations, HOUSEHOLD);
        String rdt = getValue(observations, RDT);
        String owner = eventTrackerMessage.getSupervisor();
        if (individual != null && rdt != null && rdt.equals(POSITIVE)) {

          UUID indexStructure = hdssCompoundsRepository.getStructureByIndividualId(
              individual);
          log.info("indexStructure: {}", indexStructure);
          String indexHousehold = hdssCompoundsRepository.getHouseHoldByIndividualId(
              individual);
          log.info("indexHousehold: {}", indexHousehold);
          List<String> compoundId = hdssCompoundsRepository.getDistinctCompoundsByHouseholdId(
              indexHousehold);
          log.info("compoundId: {}", compoundId);
          List<UUID> allStructuresInCompound = hdssCompoundsRepository.getDistinctStructuresByCompoundId(
              compoundId);
          log.info("allStructuresInCompound: {}", allStructuresInCompound);
          HdssIndividualProjection indexIndividual = hdssCompoundsRepository.getIndividualByIndividualId(
              individual);
          log.info("indexIndividual: {}", indexIndividual.getIndividualId());
          List<HdssIndividualProjection> allIndividualsInCompound = hdssCompoundsRepository.getAllIndividualsInCompoundId(
              compoundId);
          log.info("allIndividualsInCompound: {}", allIndividualsInCompound.stream().map(
              HdssIndividualProjection::getIndividualId).collect(
              Collectors.joining("|")));
          List<String> allHouseholdsInCompound = hdssCompoundsRepository.getDistinctHouseholdsByCompoundId(
              compoundId);
          log.info("allHouseholdsInCompound: {}", allHouseholdsInCompound);
          allStructuresInCompound.remove(indexStructure);

          UUID planIdentifier1 = eventTrackerMessage.getPlanIdentifier();

          if (planIdentifier1 != null && hdssProperties.getTarget() != null) {

            UUID targetPlan = hdssProperties.getTarget().get(planIdentifier1);

            log.info("Target plan {}", targetPlan);

            if (targetPlan != null) {

              sendEmails(individual, indexStructure, indexHousehold, compoundId,
                  allStructuresInCompound,
                  allHouseholdsInCompound, targetPlan);

              List<Goal> goalsByPlan_identifier = goalRepository.findGoalsByPlan_Identifier(
                  targetPlan);

              List<Action> actions = goalsByPlan_identifier.stream()
                  .flatMap(
                      goal -> actionRepository.findActionsByGoal_Identifier(goal.getIdentifier())
                          .stream())
                  .collect(
                      Collectors.toList());

              log.info("submitting index case  {}", indexStructure);
              submitTasks(owner, List.of(indexStructure), targetPlan, actions,
                  ActionTitleEnum.INDEX_CASE);

              log.info("submitting rcd member {}", allIndividualsInCompound);
              allIndividualsInCompound.stream()
                  .map(individualObj -> UUID.fromString(individualObj.getId()))
                      .forEach(individualId -> submitTasks(owner, List.of(individualId), targetPlan, actions, ActionTitleEnum.RCD_MEMBER));

              log.info("submitting rcd  {}", allStructuresInCompound);
              allStructuresInCompound.forEach(
                  structure -> submitTasks(owner, List.of(structure), targetPlan, actions,
                      ActionTitleEnum.RCD));

              log.info("submitting index case members {}", indexIndividual.getId());
              List.of(UUID.fromString(indexIndividual.getId())).forEach(
                  indexIndividualId -> submitTasks(owner,
                      List.of(indexIndividualId), targetPlan, actions,
                      ActionTitleEnum.INDEX_CASE_MEMBER));


            }
          }
        }
      }

    } catch (JsonProcessingException e) {
      log.error("Err {}", e.getMessage(), e);
    }


  }

  private void sendEmails(String individual, UUID indexStructure, String indexHousehold,
      List<String> compoundId, List<UUID> allStructuresInCompound,
      List<String> allHouseholdsInCompound, UUID targetPlan) {
    if (hdssProperties.isSendToOverrideEmail()) {
      String[] split = hdssProperties.getOverrideEmailList().split(";");
      List<String> collect = Arrays.stream(split).collect(Collectors.toList());
      sendMail(collect, individual, indexStructure, indexHousehold, allStructuresInCompound,
          allHouseholdsInCompound);
    } else {
      for (String compoundItem : compoundId) {
        List<String> userEmailsByCompoundIdAndPlan = hdssCompoundsRepository.getUserEmailsByCompoundIdAndPlan(
            compoundItem, targetPlan);
        if (userEmailsByCompoundIdAndPlan != null && userEmailsByCompoundIdAndPlan.size() > 0) {
          sendMail(userEmailsByCompoundIdAndPlan, individual, indexStructure, indexHousehold,
              allStructuresInCompound, allHouseholdsInCompound);

        } else {
          String[] split = hdssProperties.getDefaultEmailList().split(";");
          List<String> collect = Arrays.stream(split).collect(Collectors.toList());
          sendMail(collect, individual, indexStructure, indexHousehold, allStructuresInCompound,
              allHouseholdsInCompound);
        }
      }
    }
  }

  private void sendMail(List<String> collect, String individual, UUID indexStructure,
      String indexHousehold, List<UUID> allStructuresInCompound,
      List<String> allHouseholdsInCompound) {
    emailService.sendEmail(collect, "individual " + individual,
        indexStructure.toString().concat("-").concat(indexHousehold).concat("\r\n\r\n")
            .concat(allStructuresInCompound.stream().map(UUID::toString).collect(
                Collectors.joining(","))).concat("\r\n\r\n")
            .concat(String.join(",", allHouseholdsInCompound)));
  }


  @Async
  protected void submitTasks(String owner, List<UUID> entityIds, UUID planIdentifier,
      List<Action> actions, ActionTitleEnum actionEnum) {

    Optional<Action> optionalAction = actions.stream()
        .filter(action -> action.getTitle().equals(actionEnum.getActionTitle()))
        .findAny();

    if (optionalAction.isPresent()) {
      ListObj uuidsObj = new ListObj();
      uuidsObj.setUuids(entityIds);
      taskService.generateIndividualTaskWithOwner(planIdentifier,
          optionalAction.get().getIdentifier(),
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

@Data
class IndividualHouseholdCompound implements Serializable {

  private String key;
  private String text;
}
