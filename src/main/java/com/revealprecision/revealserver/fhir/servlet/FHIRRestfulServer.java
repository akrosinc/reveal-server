package com.revealprecision.revealserver.fhir.servlet;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.server.RestfulServer;
import com.revealprecision.revealserver.fhir.providers.PersonResourceProvider;
import com.revealprecision.revealserver.fhir.providers.PlanResourceProvider;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.context.support.SpringBeanAutowiringSupport;

@WebServlet(name = "fhir-facade", urlPatterns = {"/fhir/*"})
@Component
public class FHIRRestfulServer extends RestfulServer{
    //Initialize

    @Autowired
    PlanResourceProvider planResourceProvider;

    @Autowired
    PersonResourceProvider personResourceProvider;

    @Override
    protected void initialize() {
        //create a context for the appropriate version
        setFhirContext(FhirContext.forR4());
        //Register Resource Providers - COMING SOON
        registerProvider(planResourceProvider);
        registerProvider(personResourceProvider);

    }

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        SpringBeanAutowiringSupport.processInjectionBasedOnCurrentContext(config.getServletContext());
    }

}
