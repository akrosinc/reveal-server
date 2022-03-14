package com.revealprecision.revealserver.fhir.util;

import com.revealprecision.revealserver.persistence.domain.Action;
import com.revealprecision.revealserver.persistence.domain.Goal;
import com.revealprecision.revealserver.persistence.domain.Location;
import com.revealprecision.revealserver.persistence.domain.Target;
import java.sql.Date;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.DecimalType;
import org.hl7.fhir.r4.model.Duration;
import org.hl7.fhir.r4.model.Enumeration;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Period;
import org.hl7.fhir.r4.model.PlanDefinition;
import org.hl7.fhir.r4.model.PlanDefinition.PlanDefinitionActionComponent;
import org.hl7.fhir.r4.model.PlanDefinition.PlanDefinitionGoalComponent;
import org.hl7.fhir.r4.model.PlanDefinition.PlanDefinitionGoalTargetComponent;
import org.hl7.fhir.r4.model.Quantity;
import org.hl7.fhir.r4.model.StringType;
import org.hl7.fhir.r4.model.TriggerDefinition;
import org.hl7.fhir.r4.model.TriggerDefinition.TriggerType;
import org.hl7.fhir.r4.model.TriggerDefinition.TriggerTypeEnumFactory;
import org.hl7.fhir.r4.model.UriType;

public class FhirPlanUtil {

  public static PlanDefinition.PlanDefinitionActionComponent getActionComponent(Action action) {

    PlanDefinitionActionComponent planDefinitionActionComponent = new PlanDefinitionActionComponent();

    CodeableConcept subjectCodeableConcept = new CodeableConcept();
    subjectCodeableConcept.setText("location");
    planDefinitionActionComponent.setSubject(subjectCodeableConcept);

    var coding = List.of(new CodeableConcept().setText(action.getIdentifier().toString()));

    List<TriggerDefinition> triggerDefinitions = new ArrayList<>();
    TriggerDefinition triggerDefinition = new TriggerDefinition();
    TriggerTypeEnumFactory typeEnumFactory = new TriggerTypeEnumFactory();
    Enumeration<TriggerType> enumeration = new Enumeration<>(typeEnumFactory);
    enumeration.setValue(TriggerType.NAMEDEVENT);
    triggerDefinition.setTypeElement(enumeration);
    triggerDefinition.setNameElement(new StringType("plan-activation"));
    triggerDefinitions.add(triggerDefinition);
    planDefinitionActionComponent.setTrigger(triggerDefinitions);

    planDefinitionActionComponent.setCode(coding);
    planDefinitionActionComponent.setTitle(action.getTitle());

    Period period = new Period();
    period.setStart(Date.from(action.getTimingPeriodStart().atStartOfDay(
        ZoneId.systemDefault()).toInstant()));

    period.setEnd(Date.from(action.getTimingPeriodStart().atStartOfDay(
        ZoneId.systemDefault()).toInstant()));

    planDefinitionActionComponent.setTiming(period);

    planDefinitionActionComponent.setDescription(action.getDescription());
    planDefinitionActionComponent.setGoalId(List.of(new IdType(action.getGoal().getIdentifier().toString())));


    planDefinitionActionComponent.setDefinition(new UriType().setValue(action.getForm().getIdentifier().toString()));

    return planDefinitionActionComponent;
  }

  public static CodeableConcept getPlanJurisdictions(Location location) {
    CodeableConcept codeableConcept = new CodeableConcept();
    codeableConcept.setId(location.getIdentifier().toString());
    codeableConcept.setText(location.getName());
    return codeableConcept;
  }

  public static PlanDefinitionGoalComponent getPlanDefinitionGoalComponent(Goal goal) {
    PlanDefinitionGoalComponent planDefinitionGoalComponent = new PlanDefinitionGoalComponent();
    planDefinitionGoalComponent.setId(goal.getIdentifier().toString());

    CodeableConcept goalDescription = new CodeableConcept();
    goalDescription.setText(goal.getDescription());
    planDefinitionGoalComponent.setDescription(goalDescription);

    CodeableConcept goalPriority = new CodeableConcept();
    goalPriority.setText(goal.getPriority().toString());
    planDefinitionGoalComponent.setPriority(goalPriority);

    goal.getActions().stream().flatMap(action -> action.getConditions().stream())
        .flatMap(condition -> condition.getTargets().stream())
        .map(FhirPlanUtil::getPlanDefinitionGoalTargetComponent).forEach(planDefinitionGoalComponent::addTarget);
    return planDefinitionGoalComponent;
  }

  public static PlanDefinitionGoalTargetComponent getPlanDefinitionGoalTargetComponent(Target target) {
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
