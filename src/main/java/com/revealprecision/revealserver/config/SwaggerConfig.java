package com.revealprecision.revealserver.config;

import com.revealprecision.revealserver.props.SwaggerProperties;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

//@Configuration
//@SecurityScheme(
//    name = "keycloakauth", // can be set to anything
//    type = SecuritySchemeType.HTTP,
//    scheme = "bearer"
//)
//@OpenAPIDefinition(
//    info = @Info(title = "Reveal Server"
//        , version = "${springdoc.version}"
//        , description = "Reveal Server forms the backend processing of the Reveal Platform"
//        , license = @License(name = "Reveal Precision", url = "https://www.revealprecision.com")
//        , contact = @Contact(name = "Akros Inc. ", email = "info@akros.com", url = "https://www.akros.com"
//    )),
//    security = @SecurityRequirement(name = "keycloakauth") // references the name defined in the line 3
//)
@Configuration
public class SwaggerConfig {

    @Autowired
    private SwaggerProperties swaggerProperties;

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("Reveal Server")
                .version("${springdoc.version}")
                .description("Reveal Server forms the backend processing of the Reveal Platform")
                .license(new License()
                    .name("Reveal Precision")
                    .url("https://www.revealprecision.com"))
                .contact(new Contact()
                    .name("Akros Inc.")
                    .email("info@akros.com")
                    .url("https://www.akros.com")))
            // Dynamically add the server URL from properties
            .addServersItem(new Server().url(swaggerProperties.getServer()))
            // Add the security requirement
            .addSecurityItem(new SecurityRequirement().addList("keycloakauth"))
            .components(new Components()
                .addSecuritySchemes("keycloakauth",
                    new SecurityScheme()
                        .name("Authorization")
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT") // Optional, helps Swagger UI understand the format
                ));
    }

}
