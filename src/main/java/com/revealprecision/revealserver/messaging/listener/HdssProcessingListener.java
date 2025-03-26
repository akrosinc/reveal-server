package com.revealprecision.revealserver.messaging.listener;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.revealprecision.revealserver.api.v1.controller.querying.KafkaGenerateIndividualTasksController.ListObj;
import com.revealprecision.revealserver.enums.ActionTitleEnum;
import com.revealprecision.revealserver.integration.mail.EmailService;
import com.revealprecision.revealserver.messaging.message.EventTrackerMessage;
import com.revealprecision.revealserver.persistence.domain.Action;
import com.revealprecision.revealserver.persistence.domain.Goal;
import com.revealprecision.revealserver.persistence.domain.Task;
import com.revealprecision.revealserver.persistence.projection.HdssIndividualProjection;
import com.revealprecision.revealserver.persistence.repository.ActionRepository;
import com.revealprecision.revealserver.persistence.repository.GoalRepository;
import com.revealprecision.revealserver.persistence.repository.HdssCompoundsRepository;
import com.revealprecision.revealserver.props.HdssProperties;
import com.revealprecision.revealserver.service.TaskService;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.mail.MessagingException;
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
@Profile("KafkaMessaging & (Listening | hdss-processing) & Email")
public class HdssProcessingListener extends Listener {

  public static final String POSITIVE = "Positive";
  private final HdssCompoundsRepository hdssCompoundsRepository;

  public static String INDIVIDUAL = "individual";

  public static String INDIVIDUAL_HOUSEHOLD_COMPOUND_SEARCH = "individual_household_compound_search";

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

    String individualHouseholdCompound = getValue(observations,
        INDIVIDUAL_HOUSEHOLD_COMPOUND_SEARCH);

    try {
      log.info("individualHouseholdCompounds: {}", individualHouseholdCompound);
      if (individualHouseholdCompound != null) {
        String compound = getValue(observations, COMPOUND);
        String household = getValue(observations, HOUSEHOLD);
        String rdt = getValue(observations, RDT);
        String owner = eventTrackerMessage.getSupervisor();
        if (rdt != null && rdt.equals(POSITIVE)) {

          UUID indexStructure = hdssCompoundsRepository.getStructureByIndividualId(
              individualHouseholdCompound);
          log.debug("indexStructure: {}", indexStructure);

          String indexHousehold = hdssCompoundsRepository.getHouseHoldByIndividualId(
              individualHouseholdCompound);
          log.debug("indexHousehold: {}", indexHousehold);

          List<String> compoundId = hdssCompoundsRepository.getDistinctCompoundsByHouseholdId(
              indexHousehold);
          log.debug("compoundId: {}", compoundId);

          List<UUID> allStructuresInCompound = hdssCompoundsRepository.getDistinctStructuresByCompoundIdExcludingNullStructures(
              compoundId);
          log.debug("allStructuresInCompound: {}", allStructuresInCompound);

          HdssIndividualProjection indexIndividual = hdssCompoundsRepository.getIndividualByIndividualId(
              individualHouseholdCompound);
          log.debug("indexIndividual: {}", indexIndividual.getIndividualId());

          List<HdssIndividualProjection> allIndividualsInCompound = hdssCompoundsRepository.getAllIndividualsInCompoundIdWithStructure(
              compoundId);
          allIndividualsInCompound.remove(indexIndividual);
          List<HdssIndividualProjection> allIndividualsExcludingIndex = allIndividualsInCompound.stream()
              .filter(compoundIndividual ->
                  {
                    if (!compoundIndividual.getIndividualId()
                        .equals(indexIndividual.getIndividualId())) {
                      return true;
                    }
                    return false;
                  }
              ).collect(Collectors.toList());

          log.debug("allIndividualsInCompound: {}", allIndividualsInCompound.stream().map(
              HdssIndividualProjection::getIndividualId).collect(
              Collectors.joining("|")));

          List<String> allHouseholdsInCompound = hdssCompoundsRepository.getDistinctHouseholdsByCompoundIdWithStructure(
              compoundId);
          log.debug("allHouseholdsInCompound: {}", allHouseholdsInCompound);

          allStructuresInCompound.remove(indexStructure);

          List<Task> allTasksAccrossPlansByBaseEntityIdentifiers = taskService.getTasksAcrossPlansByBaseEntityIdentifiers(
              allIndividualsInCompound.stream().map(HdssIndividualProjection::getId).map(
                      id -> {
                        try {
                          return UUID.fromString(id);
                        } catch (IllegalArgumentException e) {
                          return null;
                        }
                      }
                  ).filter(Objects::nonNull)
                  .collect(
                      Collectors.toList()));

          List<Task> tasksAccrossPlansByBaseEntityIdentifiers = allTasksAccrossPlansByBaseEntityIdentifiers.stream()
              .filter(task -> task.getLookupTaskStatus().getCode().equals(
                  "READY")).collect(Collectors.toList());

          boolean existingIndexCase = tasksAccrossPlansByBaseEntityIdentifiers.stream()
              .anyMatch(task -> {
                LocalDateTime now = LocalDateTime.now();
                LocalDateTime threeDaysAgo = now.minusDays(3);
                if (task.getCreatedDatetime() != null && task.getCreatedDatetime()
                    .isAfter(threeDaysAgo)) {
                  if (task.getAction().getTitle()
                      .equals(ActionTitleEnum.INDEX_CASE_MEMBER.getActionTitle())) {
                    return true;
                  }
                }
                return false;
              });
          boolean existingRCD = tasksAccrossPlansByBaseEntityIdentifiers.stream().anyMatch(task -> {
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime threeDaysAgo = now.minusDays(3);
            if (task.getCreatedDatetime() != null && task.getCreatedDatetime()
                .isAfter(threeDaysAgo)) {
              if (task.getAction().getTitle()
                  .equals(ActionTitleEnum.RCD_MEMBER.getActionTitle())) {
                return true;
              }
            }
            return false;
          });

          UUID planIdentifier1 = eventTrackerMessage.getPlanIdentifier();
          log.debug("submitting plan {}", planIdentifier1);
//          if (hdssProperties.getTarget()!=null){
//            hdssProperties.getTarget().entrySet().stream().forEach(entry->{
//              log.debug("current plan targets {} {} ", entry.getKey(),entry.getValue());
//            });
//
//          }
          if (planIdentifier1 != null && hdssProperties.getTarget() != null) {
            UUID targetPlan = planIdentifier1;
//            UUID targetPlan = hdssProperties.getTarget().get(planIdentifier1);
            log.debug("Target plan {}", targetPlan);
            if (targetPlan != null) {

              sendEmails(individualHouseholdCompound, indexStructure, indexHousehold, compoundId,
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

              if (existingIndexCase) {
                log.debug("submitting index case members {}", indexIndividual.getId());
                List.of(UUID.fromString(indexIndividual.getId())).forEach(
                    indexIndividualId -> submitTasks(owner,
                        List.of(indexIndividualId), targetPlan, actions,
                        ActionTitleEnum.SECONDARY_INDEX_CASE_MEMBER));

                if (existingRCD) {
                  List.of(UUID.fromString(indexIndividual.getId())).forEach(
                      indexIndividualId -> cancelTasks(owner,
                          List.of(indexIndividualId), targetPlan, actions,
                          ActionTitleEnum.RCD_MEMBER));

                  List<Task> active = taskService.getTasksAcrossPlansByBaseEntityIdentifiers(
                      List.of(indexStructure));
                  if (active.size() > 0) {
                    Task task = active.get(0);

                    if (task.getAction().getTitle().equals(ActionTitleEnum.RCD.getActionTitle()) &&
                        task.getLookupTaskStatus().getCode().equals("READY")) {
                      cancelTasks(owner,
                          List.of(task.getBaseEntityIdentifier()), targetPlan, actions,
                          ActionTitleEnum.RCD);
                      submitTasks(owner,
                          List.of(task.getBaseEntityIdentifier()), targetPlan, actions,
                          ActionTitleEnum.SECONDARY_INDEX_CASE);
                    }
                  }
                }
              } else {
                log.debug("submitting index case members {}", indexIndividual.getId());
                List.of(UUID.fromString(indexIndividual.getId())).forEach(
                    indexIndividualId -> submitTasks(owner,
                        List.of(indexIndividualId), targetPlan, actions,
                        ActionTitleEnum.INDEX_CASE_MEMBER));

                log.debug("submitting index case  {}", indexStructure);
                submitTasks(owner, List.of(indexStructure), targetPlan, actions,
                    ActionTitleEnum.INDEX_CASE);

//                log.debug("submitting rcd member {}", allIndividualsExcludingIndex);
//                allIndividualsExcludingIndex.stream()
//                    .map(individualObj -> UUID.fromString(individualObj.getId()))
//                    .forEach(
//                        individualId -> submitTasks(owner, List.of(individualId), targetPlan,
//                            actions,
//                            ActionTitleEnum.RCD_MEMBER));
//
//                log.debug("submitting rcd  {}", allStructuresInCompound);
//                allStructuresInCompound.stream().filter(Objects::nonNull).forEach(
//                    structure -> submitTasks(owner, List.of(structure), targetPlan, actions,
//                        ActionTitleEnum.RCD));
              }
            }
          }
        }
      }
    } catch (Exception e) {
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
    try {
      emailService.sendEmail(collect, "Index Case Notification: " + individual,
          "<p>for structure: ".concat(indexStructure.toString()).concat("</p><br>").concat("<p>")
              .concat(indexHousehold).concat("</p><br>")
              .concat("<p>structures in compounds</p>")
              .concat(allStructuresInCompound.stream().filter(Objects::nonNull).map(UUID::toString)
                  .map(uuid -> "<p>".concat(uuid).concat("</p>")).collect(
                      Collectors.joining("<br>"))).concat("<br>")
              .concat("<p>households in compounds</p>")
              .concat(allHouseholdsInCompound.stream()
                  .map(household -> "<p>".concat(household).concat("</p>")).collect(
                      Collectors.joining("<br>"))));
    } catch (MessagingException e) {
      log.error(e.getMessage(),e);
    }
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
      taskService.generateIndividualTaskWithOwnerDirect(
          planIdentifier,
          optionalAction.get().getIdentifier(),
          uuidsObj, owner);
    }
  }

  @Async
  protected void cancelTasks(String owner, List<UUID> entityIds, UUID planIdentifier,
      List<Action> actions, ActionTitleEnum actionEnum) {

    Optional<Action> optionalAction = actions.stream()
        .filter(action -> action.getTitle().equals(actionEnum.getActionTitle()))
        .findAny();

    if (optionalAction.isPresent()) {
      ListObj uuidsObj = new ListObj();
      uuidsObj.setUuids(entityIds);
      taskService.cancelIndividualTaskWithOwnerDirect(
          planIdentifier,
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
