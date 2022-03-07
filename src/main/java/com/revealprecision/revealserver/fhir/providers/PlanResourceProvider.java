package com.revealprecision.revealserver.fhir.providers;

import ca.uhn.fhir.rest.annotation.IdParam;
import ca.uhn.fhir.rest.annotation.Read;
import ca.uhn.fhir.rest.server.IResourceProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.revealprecision.revealserver.fhir.util.FhirActionUtil;
import com.revealprecision.revealserver.persistence.domain.Plan;
import com.revealprecision.revealserver.persistence.repository.PlanRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.CodeType;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.PlanDefinition;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.StringType;
import org.hl7.fhir.r4.model.UsageContext;
import org.hl7.fhir.r4.model.codesystems.UsageContextType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class PlanResourceProvider implements IResourceProvider {

  @Autowired
  PlanRepository planRepository;

  @Autowired
  ObjectMapper objectMapper;

  @Override
  public Class<? extends IBaseResource> getResourceType() {
    return PlanDefinition.class;
  }

  @Read()
  @Transactional
  public PlanDefinition read(@IdParam IdType theId) {
    log.info("theId {}", theId);
    log.info("getIdPart {}", theId.getIdPart());
    log.info("planRepository {}", planRepository.findAll());

    Optional<Plan> optionalPlan = planRepository.findById(UUID.fromString(theId.getIdPart()));
    if (optionalPlan.isPresent()) {
      Plan plan = optionalPlan.get();
      PlanDefinition planDefinition = new PlanDefinition();

      planDefinition.setIdentifier(List.of(
          new Identifier().setValue(plan.getIdentifier().toString())));

      planDefinition.setName(plan.getName());

      var actions = plan.getGoals().stream().flatMap(goals -> goals.getActions().stream())
          .map(FhirActionUtil::getActionComponent).collect(
              Collectors.toList());

      planDefinition.setAction(actions);

      CodeableConcept codeableConcept = new CodeableConcept();

      CodeType type = new CodeType();
      type.setValue(UsageContextType.PROGRAM.toCode());

      Coding coding = new Coding();
      coding.setCode("interventionType");

      CodeableConcept usageContextValue = new CodeableConcept();
      usageContextValue.addCoding(new Coding().setCode("IRS"));

      Reference reference = new Reference();
      reference.setDisplay("IRS");

      UsageContext usageContext = new UsageContext();
      usageContext.setProperty("value[x]",new StringType("interventionType"));
      usageContext.setValue(reference);

      List<UsageContext> usageContexts = new ArrayList<>();
      usageContexts.add(usageContext);

      planDefinition.setUseContext(usageContexts);
      return planDefinition;

    }
    return null;
  }
}
