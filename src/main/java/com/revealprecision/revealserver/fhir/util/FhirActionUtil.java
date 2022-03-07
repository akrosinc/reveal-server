package com.revealprecision.revealserver.fhir.util;

import com.revealprecision.revealserver.persistence.domain.Action;
import java.sql.Date;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Enumeration;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Period;
import org.hl7.fhir.r4.model.PlanDefinition;
import org.hl7.fhir.r4.model.PlanDefinition.PlanDefinitionActionComponent;
import org.hl7.fhir.r4.model.StringType;
import org.hl7.fhir.r4.model.TriggerDefinition;
import org.hl7.fhir.r4.model.TriggerDefinition.TriggerType;
import org.hl7.fhir.r4.model.TriggerDefinition.TriggerTypeEnumFactory;
import org.hl7.fhir.r4.model.UriType;

public class FhirActionUtil {

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
}
