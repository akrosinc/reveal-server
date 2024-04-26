package com.revealprecision.revealserver.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Configuration;

@Configuration
@SecurityScheme(
    name = "keycloakauth", // can be set to anything
    type = SecuritySchemeType.HTTP,
    scheme = "bearer"
)
@OpenAPIDefinition(
    info = @Info(title = "Reveal Server"
        , version = "${springdoc.version}"
        , description = "Reveal Server forms the backend processing of the Reveal Platform"
        , license = @License(name = "Reveal Precision", url = "https://www.revealprecision.com")
        , contact = @Contact(name = "Akros Inc. ", email = "info@akros.com", url = "https://www.akros.com"
    )),
    security = @SecurityRequirement(name = "keycloakauth") // references the name defined in the line 3
)
//@OpenAPIDefinition(
//    info = @Info(title = "Reveal Server"
//        , version = "${springdoc.version}"
//        , description = "Reveal Server forms the backend processing of the Reveal Platform"
//        , license = @License(name = "Reveal Precision", url = "https://www.revealprecision.com")
//        , contact = @Contact(name = "Akros Inc. ", email = "info@akros.com", url = "https://www.akros.com")
//    ),
//
//    security = @SecurityRequirement(name = "keycloakauth") // references the name defined in the line 3,
//    ,servers = {@Server(url = "https://api-my-local.akros.digital")}
//)
public class SwaggerConfig {


}
