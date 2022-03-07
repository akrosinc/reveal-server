package com.revealprecision.revealserver.fhir.providers;

import ca.uhn.fhir.rest.annotation.Search;
import ca.uhn.fhir.rest.server.IResourceProvider;
import com.revealprecision.revealserver.service.EntityTagService;
import java.util.List;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.CodeSystem;
import org.springframework.beans.factory.annotation.Autowired;


public class CodeSystemResource implements IResourceProvider {

  private final EntityTagService entityTagService;

  @Autowired
  public CodeSystemResource(EntityTagService entityTagService){
    this.entityTagService = entityTagService;
  }

  @Override
  public Class<? extends IBaseResource> getResourceType() {
    return CodeSystem.class;
  }

//  @Search
//  public List<CodeSystem> getAllCodeSystems(){
//    CodeSystem codeSystem = new CodeSystem();
//    codeSystem.setUrl()
//  }
}
