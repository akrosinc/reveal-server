package com.revealprecision.revealserver.fhir.providers;

import ca.uhn.fhir.rest.annotation.IdParam;
import ca.uhn.fhir.rest.annotation.Read;
import ca.uhn.fhir.rest.server.IResourceProvider;
import com.revealprecision.revealserver.fhir.util.FhirActionUtil;
import com.revealprecision.revealserver.persistence.domain.Goal;
import com.revealprecision.revealserver.persistence.domain.Location;
import com.revealprecision.revealserver.persistence.domain.Plan;
import com.revealprecision.revealserver.persistence.domain.Target;
import com.revealprecision.revealserver.service.PlanService;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import javax.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.DecimalType;
import org.hl7.fhir.r4.model.Duration;
import org.hl7.fhir.r4.model.Enumerations.PublicationStatus;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.PlanDefinition;
import org.hl7.fhir.r4.model.PlanDefinition.PlanDefinitionGoalComponent;
import org.hl7.fhir.r4.model.PlanDefinition.PlanDefinitionGoalTargetComponent;
import org.hl7.fhir.r4.model.Quantity;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.StringType;
import org.hl7.fhir.r4.model.UsageContext;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PlanResourceProvider implements IResourceProvider {

  private final PlanService planService;

  @Override
  public Class<? extends IBaseResource> getResourceType() {
    return PlanDefinition.class;
  }

  @Read()
  @Transactional
  public PlanDefinition read(@IdParam IdType theId) {

    Plan plan = planService.getPlanByIdentifier(UUID.fromString(theId.getIdPart()));

    PlanDefinition planDefinition = new PlanDefinition();

    planDefinition.setName(plan.getName());

    Set<Goal> revealGoals = plan.getGoals();
    revealGoals.stream().flatMap(goals -> goals.getActions().stream())
        .map(FhirActionUtil::getActionComponent).forEach(planDefinition::addAction);

    revealGoals.stream().map(this::getPlanDefinitionGoalComponent).forEach(planDefinition::addGoal);

    planDefinition.setStatus(PublicationStatus.fromCode(plan.getStatus().toString().toLowerCase()));

    planDefinition.setIdentifier(
        List.of(new Identifier().setValue(plan.getIdentifier().toString())));

    List<UsageContext> useContexts = new ArrayList<>();

    Coding coding = new Coding();
    coding.setCode("interventionType");
    UsageContext useContext = new UsageContext();
    useContext.setCode(coding);
    Reference reference = new Reference();
    reference.setDisplay(plan.getInterventionType().getCode());
    useContext.setValue(reference);

    useContexts.add(useContext);
    planDefinition.setUseContext(useContexts);

    planService.findLocationsForPlan(plan.getIdentifier()).stream().map(this::getPlanJurisdictions)
        .forEach(planDefinition::addJurisdiction);
    return planDefinition;
  }

  private CodeableConcept getPlanJurisdictions(Location location) {
    CodeableConcept codeableConcept = new CodeableConcept();
    codeableConcept.setId(location.getIdentifier().toString());
    codeableConcept.setText(location.getName());
    return codeableConcept;
  }

  private PlanDefinitionGoalComponent getPlanDefinitionGoalComponent(Goal goal) {
    PlanDefinitionGoalComponent goal1 = new PlanDefinitionGoalComponent();
    goal1.setId(goal.getIdentifier().toString());

    CodeableConcept goalDescription = new CodeableConcept();
    goalDescription.setText(goal.getDescription());
    goal1.setDescription(goalDescription);

    CodeableConcept goalPriority = new CodeableConcept();
    goalPriority.setText(goal.getPriority().toString());
    goal1.setPriority(goalPriority);

    goal.getActions().stream().flatMap(action -> action.getConditions().stream())
        .flatMap(condition -> condition.getTargets().stream())
        .map(this::getPlanDefinitionGoalTargetComponent).forEach(goal1::addTarget);
    return goal1;
  }

  private PlanDefinitionGoalTargetComponent getPlanDefinitionGoalTargetComponent(Target target) {
    PlanDefinitionGoalTargetComponent goalTargetComponent = new PlanDefinitionGoalTargetComponent();

    CodeableConcept goalTargetMeasure = new CodeableConcept();
    goalTargetMeasure.setText(target.getMeasure());
    goalTargetComponent.setMeasure(goalTargetMeasure);

    Duration goalTargetDuration = new Duration();
    DecimalType duration = new DecimalType();
    duration.setValue(target.getDue().toEpochDay() - LocalDate.now().toEpochDay());
    StringType unitString = new StringType();
    unitString.setValue("days");
    goalTargetDuration.setProperty("value", duration);
    goalTargetDuration.setProperty("unit", unitString);
    goalTargetComponent.setDue(goalTargetDuration);

    Quantity targetQuantity = new Quantity();
    DecimalType detailValue = new DecimalType();
    detailValue.setValue(target.getValue());
    StringType comparator = new StringType();
    comparator.setValue(target.getComparator());
    StringType detailUnitString = new StringType();
    detailUnitString.setValue(target.getUnit().toString());
    targetQuantity.setProperty("unit", detailUnitString);
    targetQuantity.setProperty("comparator", comparator);
    targetQuantity.setProperty("value", detailValue);
    goalTargetComponent.setDetail(targetQuantity);

    return goalTargetComponent;
  }
}
