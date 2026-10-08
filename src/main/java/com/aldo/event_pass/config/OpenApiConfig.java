package com.aldo.event_pass.config;

import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.servers.Server;

@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "Event Pass API",
                version = "1.0",
                description = "API REST sistema para compra y venta de boletos desarrollado con Spring Boot",
                contact = @Contact(
                        name = "Aldo",
                        email = "aldorgithub@gmail.com"
                )
        ),
        security = {
            @SecurityRequirement(name = "bearerAuth")
        },
        servers = {
            @Server(url = "http://localhost:8080", description = "Servidor local")
        }
)

@SecurityScheme(
    name = "bearerAuth",
    type = SecuritySchemeType.HTTP,
    scheme = "bearer",
    bearerFormat = "JWT"
)
public class OpenApiConfig {
}