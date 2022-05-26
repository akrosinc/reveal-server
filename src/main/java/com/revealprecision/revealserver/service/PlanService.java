package com.revealprecision.revealserver.service;

import com.cosium.spring.data.jpa.entity.graph.domain.EntityGraphUtils;
import com.revealprecision.revealserver.api.v1.dto.factory.PlanEntityFactory;
import com.revealprecision.revealserver.api.v1.dto.request.PlanRequest;
import com.revealprecision.revealserver.enums.ApplicableReportsEnum;
import com.revealprecision.revealserver.enums.EntityStatus;
import com.revealprecision.revealserver.enums.LookupUtil;
import com.revealprecision.revealserver.enums.PlanStatusEnum;
import com.revealprecision.revealserver.enums.ReportTypeEnum;
import com.revealprecision.revealserver.exceptions.ConflictException;
import com.revealprecision.revealserver.exceptions.NotFoundException;
import com.revealprecision.revealserver.messaging.KafkaConstants;
import com.revealprecision.revealserver.messaging.message.Message;
import com.revealprecision.revealserver.messaging.message.PlanUpdateMessage;
import com.revealprecision.revealserver.messaging.message.PlanUpdateType;
import com.revealprecision.revealserver.persistence.domain.Action;
import com.revealprecision.revealserver.persistence.domain.Condition;
import com.revealprecision.revealserver.persistence.domain.Form;
import com.revealprecision.revealserver.persistence.domain.Goal;
import com.revealprecision.revealserver.persistence.domain.Location;
import com.revealprecision.revealserver.persistence.domain.LocationHierarchy;
import com.revealprecision.revealserver.persistence.domain.LookupEntityType;
import com.revealprecision.revealserver.persistence.domain.LookupInterventionType;
import com.revealprecision.revealserver.persistence.domain.Plan;
import com.revealprecision.revealserver.persistence.domain.Plan.Fields;
import com.revealprecision.revealserver.persistence.repository.PlanRepository;
import com.revealprecision.revealserver.props.KafkaProperties;
import com.revealprecision.revealserver.util.UserUtils;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.util.Pair;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class PlanService {

  private final PlanRepository planRepository;
  private final FormService formService;
  private final LocationBulkService locationBulkService;
  private final LocationHierarchyService locationHierarchyService;
  private final LookupInterventionTypeService lookupInterventionTypeService;
  private final LookupEntityTypeService lookupEntityTypeService;
  private final KafkaTemplate<String, Message> kafkaTemplate;
  private final KafkaProperties kafkaProperties;

  public static boolean isNullOrEmpty(final Collection<?> c) {
    return c == null || c.isEmpty();
  }

  public Plan getPlanByIdentifier(UUID planIdentifier) {
    return planRepository.findById(planIdentifier).orElseThrow(
        () -> new NotFoundException(Pair.of(Fields.identifier, planIdentifier), Plan.class));
  }


  public List<Location> findLocationsForPlan(UUID planIdentifier) {
    return planRepository.findLocationsForPlan(planIdentifier);
  }

  public Page<Plan> getAll(String search, Pageable pageable) {
    return planRepository.getAll(search, pageable, EntityGraphUtils.fromAttributePaths(
        String.join(".", Fields.goals, Goal.Fields.actions, Action.Fields.conditions,
            Condition.Fields.targets)));
  }

  public long getAllCount(String search) {
    return planRepository.getAllCount(search);
  }

  public Page<Plan> getPlansForReports(String reportType, Pageable pageable) {
    if(reportType.isBlank()) {
      return planRepository.findPlansByInterventionType(reportType, pageable);
    }else{
      ApplicableReportsEnum applicableReportsEnum = null;
      ReportTypeEnum reportTypeEnum = LookupUtil.lookup(ReportTypeEnum.class, reportType);
      for(ApplicableReportsEnum applicableReport : ApplicableReportsEnum.values()) {
        if(applicableReport.getReportName().contains(reportType)) {
          applicableReportsEnum = applicableReport;
          break;
        }
      }
      return planRepository.findPlansByInterventionType(applicableReportsEnum.name(), pageable);
    }
  }

  public void createPlan(PlanRequest planRequest) {

    LookupInterventionType interventionType = lookupInterventionTypeService.findByIdentifier(
        planRequest.getInterventionType());
    LocationHierarchy locationHierarchy = locationHierarchyService.findByIdentifier(
        planRequest.getLocationHierarchy());

    Set<UUID> forms = new HashSet<>();
    List<LookupEntityType> allLookUpEntityTypes = lookupEntityTypeService.getAllLookUpEntityTypes();
    if (!isNullOrEmpty(planRequest.getGoals())) {
      planRequest.getGoals().forEach(goalRequest -> {
        if (!isNullOrEmpty(goalRequest.getActions())) {
          goalRequest.getActions()
              .forEach(actionRequest -> forms.add(actionRequest.getFormIdentifier()));
        }
      });
    }

    Map<UUID, Form> foundForms = formService.findByIdentifiers(forms);

    Plan plan = PlanEntityFactory.toEntity(planRequest, interventionType, locationHierarchy,
        foundForms, allLookUpEntityTypes);
    plan.setEntityStatus(EntityStatus.ACTIVE);

    savePlan(plan);
  }

  public void activatePlan(UUID planIdentifier) {
    Plan plan = getPlanByIdentifier(planIdentifier);
    if (locationBulkService.areRelationshipsGenerated()) {
      plan.setStatus(PlanStatusEnum.ACTIVE);
      savePlan(plan);
      PlanUpdateMessage planUpdateMessage = new PlanUpdateMessage();
      planUpdateMessage.setPlanIdentifier(plan.getIdentifier());
      planUpdateMessage.setPlanUpdateType(PlanUpdateType.ACTIVATE);
      planUpdateMessage.setOwnerId(UserUtils.getCurrentPrincipleName());

      kafkaTemplate.send(kafkaProperties.getTopicMap().get(KafkaConstants.PLAN_UPDATE),planUpdateMessage);
    } else {
      throw new ConflictException("Relationships still generating for this plan.");
    }
  }

  public void updatePlan(PlanRequest request, UUID identifier) {
    Plan plan = getPlanByIdentifier(identifier);
    LocationHierarchy hierarchy = locationHierarchyService.findByIdentifier(
        request.getLocationHierarchy());
    LookupInterventionType interventionType = lookupInterventionTypeService.findByIdentifier(
        request.getInterventionType());
    plan.update(request, hierarchy, interventionType);
    savePlan(plan);
  }

  private void savePlan(Plan plan) {
    Plan savedPlan = planRepository.save(plan);
  }
}