package com.revealprecision.revealserver.fhir.providers;

import ca.uhn.fhir.rest.annotation.IdParam;
import ca.uhn.fhir.rest.annotation.Read;
import ca.uhn.fhir.rest.annotation.Search;
import ca.uhn.fhir.rest.server.IResourceProvider;
import com.revealprecision.revealserver.fhir.properties.FhirServerProperties;
import com.revealprecision.revealserver.persistence.domain.EntityTags;
import com.revealprecision.revealserver.persistence.domain.LookupEntityType;
import com.revealprecision.revealserver.service.EntityTagService;
import com.revealprecision.revealserver.service.LookupEntityTypeService;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.CodeSystem;
import org.hl7.fhir.r4.model.CodeSystem.ConceptDefinitionComponent;
import org.hl7.fhir.r4.model.IdType;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CodeSystemResourceProvider implements IResourceProvider {

  private final EntityTagService entityTagService;

  private final LookupEntityTypeService lookupEntityTypeService;

  private final FhirServerProperties fhirServerProperties;

  @Override
  public Class<? extends IBaseResource> getResourceType() {
    return CodeSystem.class;
  }

  @Read()
  @Transactional
  public CodeSystem read(@IdParam IdType theId) {
    return getCodeSystem(
        lookupEntityTypeService.getLookupEntityTypeByTableName(theId.getIdPart().split("-")[0]));
  }


  @Search
  public List<CodeSystem> getAllCodeSystems() {
    return lookupEntityTypeService.getAllLookUpEntityTypes().stream().map(
        this::getCodeSystem
    ).collect(Collectors.toList());
  }

  private CodeSystem getCodeSystem(LookupEntityType lookupEntityType) {

    CodeSystem codeSystem = new CodeSystem();
    codeSystem.setId(lookupEntityType.getTableName() + "-metadata");
    codeSystem.setUrl(fhirServerProperties.getBaseURL());
    codeSystem.setConcept(
        entityTagService.getEntityTagsByLookupEntityTypeIdentifier(lookupEntityType.getIdentifier())
            .stream().map(this::getConceptDefinitionComponent).collect(Collectors.toList()));
    codeSystem.setDescription("Reveal Codes - " + lookupEntityType.getCode());

    return codeSystem;
  }


  private ConceptDefinitionComponent getConceptDefinitionComponent(EntityTags entityTag) {
    ConceptDefinitionComponent conceptDefinitionComponent = new ConceptDefinitionComponent();
    conceptDefinitionComponent.setCode(entityTag.getTag());
    conceptDefinitionComponent.setDefinition(entityTag.getDefinition());
    conceptDefinitionComponent.setId(entityTag.getIdentifier().toString());
    return conceptDefinitionComponent;
  }
}
