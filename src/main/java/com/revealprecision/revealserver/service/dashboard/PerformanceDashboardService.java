package com.revealprecision.revealserver.service.dashboard;

import com.revealprecision.revealserver.api.v1.dto.models.RowData;
import com.revealprecision.revealserver.constants.KafkaConstants;
import com.revealprecision.revealserver.enums.PlanInterventionTypeEnum;
import com.revealprecision.revealserver.messaging.message.UserLevel;
import com.revealprecision.revealserver.messaging.message.UserParentChildren;
import com.revealprecision.revealserver.persistence.domain.Organization;
import com.revealprecision.revealserver.persistence.domain.Plan;
import com.revealprecision.revealserver.persistence.domain.PlanAssignment;
import com.revealprecision.revealserver.props.KafkaProperties;
import com.revealprecision.revealserver.service.OrganizationService;
import com.revealprecision.revealserver.service.PlanAssignmentService;
import com.revealprecision.revealserver.service.PlanService;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.streams.StoreQueryParameters;
import org.apache.kafka.streams.state.QueryableStoreTypes;
import org.apache.kafka.streams.state.ReadOnlyKeyValueStore;
import org.springframework.kafka.config.StreamsBuilderFactoryBean;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class PerformanceDashboardService {


  private final PlanService planService;

  private final PlanAssignmentService planAssignmentService;

  private final OrganizationService organizationService;

  private final IrsPerformanceDashboardService irsPerformanceDashboardService;

  private ReadOnlyKeyValueStore<String, UserParentChildren> userParentChildren;

  private final StreamsBuilderFactoryBean getKafkaStreams;
  private final KafkaProperties kafkaProperties;
  boolean datastoresInitialized = false;


  public List<RowData> getDataForReport(UUID planIdentifier, String id) {

    Plan plan = planService.getPlanByIdentifier(planIdentifier);

    List<PlanAssignment> planAssignmentsByPlanIdentifier = planAssignmentService.getPlanAssignmentsByPlanIdentifier(
        planIdentifier);
    Map<String,UserLevel> userLevels = null;
    if (id == null) {
      userLevels = planAssignmentsByPlanIdentifier.stream()
          .map(this::getOrganization)
          .filter(Objects::nonNull)
          .map(highestLevelOrg -> new UserLevel(highestLevelOrg.getIdentifier().toString(),
              highestLevelOrg.getName(), 0, "organization", highestLevelOrg.getType().name()))
          .collect(Collectors.toMap(UserLevel::getUserId,userLevel -> userLevel,(a,b)->b));
    } else {
      UserParentChildren userParentChildren = this.userParentChildren.get(
          planIdentifier + "_" + id);
      if (userParentChildren != null) {
        userLevels = userParentChildren.getChildren().stream().collect(Collectors.toMap(UserLevel::getUserId,userLevel -> userLevel));
      }
    }
    initialDataStores(plan);
    return getRowDatas(plan, new HashSet<>(userLevels.values()), id);
  }

  private Organization getOrganization(PlanAssignment planAssignment) {
    Organization loopOrg = organizationService.findByIdWithChildren(
        planAssignment.getOrganization().getIdentifier());
    Organization highestLevelOrg = null;
    while (loopOrg != null) {
      if (loopOrg.getParent() == null) {
        highestLevelOrg = loopOrg;
      }
      loopOrg = loopOrg.getParent();
    }

    return highestLevelOrg;
  }

  public List<RowData> getDatedRowDatas(UUID planIdentifier, String id) {

    Plan plan = planService.getPlanByIdentifier(planIdentifier);

    initialDataStores(plan);

    switch (PlanInterventionTypeEnum.valueOf(plan.getInterventionType().getCode())) {
      case IRS:
        return irsPerformanceDashboardService.getDetailedPerformanceColumnData(
            plan, id);
      case IRS_LITE:
      case MDA:
      case MDA_LITE:
      default:
        return null;
    }
  }

  private RowData getPerformanceColumnData(
      Plan plan, PlanInterventionTypeEnum planInterventionTypeEnum, UserLevel userLevel,
      String parentUserLevelId) {

    switch (planInterventionTypeEnum) {
      case IRS:
        return irsPerformanceDashboardService.getRowData(
            plan, userLevel, parentUserLevelId);
      case IRS_LITE:
      case MDA:
      case MDA_LITE:
      default:
        return null;
    }
  }


  private List<RowData> getRowDatas(Plan plan, Set<UserLevel> userLevels,
      String parentUserLevelId) {

    PlanInterventionTypeEnum planInterventionTypeEnum = PlanInterventionTypeEnum.valueOf(
        plan.getInterventionType().getCode());
    if (userLevels == null) {
      return null;
    }
    return userLevels.stream()
        .map(userLevel -> getPerformanceColumnData(
            plan, planInterventionTypeEnum, userLevel, parentUserLevelId)).collect(
            Collectors.toList());
  }


  private void initialDataStores(Plan plan) {
    if (!datastoresInitialized) {
      this.userParentChildren = getKafkaStreams.getKafkaStreams().store(
          StoreQueryParameters.fromNameAndType(kafkaProperties.getStoreMap().get(
                  KafkaConstants.userParentChildren),
              QueryableStoreTypes.keyValueStore()));
      datastoresInitialized = true;
    }
    switch (PlanInterventionTypeEnum.valueOf(plan.getInterventionType().getCode())) {
      case IRS:
        irsPerformanceDashboardService.initialDataStores();
        break;
      case IRS_LITE:
      case MDA:
      case MDA_LITE:
      default:
    }


  }


}
