package com.revealprecision.revealserver.fhir.providers;

import ca.uhn.fhir.rest.annotation.IdParam;
import ca.uhn.fhir.rest.annotation.Read;
import ca.uhn.fhir.rest.server.IResourceProvider;
import com.revealprecision.revealserver.fhir.util.FhirPlanUtil;
import com.revealprecision.revealserver.persistence.domain.Goal;
import com.revealprecision.revealserver.persistence.domain.Plan;
import com.revealprecision.revealserver.service.PlanService;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import javax.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Enumerations.PublicationStatus;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.PlanDefinition;
import org.hl7.fhir.r4.model.Reference;
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
        .map(FhirPlanUtil::getActionComponent).forEach(planDefinition::addAction);

    revealGoals.stream().map(FhirPlanUtil::getPlanDefinitionGoalComponent).forEach(planDefinition::addGoal);

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

    planService.findLocationsForPlan(plan.getIdentifier()).stream().map(FhirPlanUtil::getPlanJurisdictions)
        .forEach(planDefinition::addJurisdiction);
    return planDefinition;
  }


}
