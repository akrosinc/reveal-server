package com.revealprecision.revealserver.fhir.servlet;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.server.RestfulServer;
import com.revealprecision.revealserver.fhir.providers.CodeSystemResourceProvider;
import com.revealprecision.revealserver.fhir.providers.LocationResourceProvider;
import com.revealprecision.revealserver.fhir.providers.PersonResourceProvider;
import com.revealprecision.revealserver.fhir.providers.PlanResourceProvider;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.web.context.support.SpringBeanAutowiringSupport;

@WebServlet(name = "fhir-facade", urlPatterns = {"/fhir/*"})
@Component
@RequiredArgsConstructor
@Profile("FHIR")
public class FHIRRestfulServer extends RestfulServer {

  private final PlanResourceProvider planResourceProvider;
  private final PersonResourceProvider personResourceProvider;
  private final CodeSystemResourceProvider codeSystemResourceProvider;
  private final LocationResourceProvider locationResourceProvider;

  @Override
  protected void initialize() {
    setFhirContext(FhirContext.forR4());
    registerProvider(planResourceProvider);
    registerProvider(personResourceProvider);
    registerProvider(codeSystemResourceProvider);
    registerProvider(locationResourceProvider);

  }

  @Override
  public void init(ServletConfig config) throws ServletException {
    super.init(config);
    SpringBeanAutowiringSupport.processInjectionBasedOnCurrentContext(config.getServletContext());
  }

}
