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
                description = "API REST para compra y venta de boletos, desarrollada con Spring Boot.\n\n"
                        + "Roles: USER (compra y consulta de sus boletos), ADMIN (gestión de eventos y acceso total) "
                        + "y STAFF (solo marcar boletos como usados; no puede comprar ni consultar compras).\n\n"
                        + "El campo estado de los eventos es un estado efectivo calculado con las fechas: "
                        + "un evento PUBLICADO pasa a INICIADO al llegar fechaInicio y a FINALIZADO al terminar, "
                        + "aunque en base de datos siga PUBLICADO. El catálogo público devuelve PUBLICADO e INICIADO.\n\n"
                        + "Errores de negocio usan ApiError (status, message, timestamp, errors). "
                        + "Los fallos de Stripe devuelven 502 BAD_GATEWAY.\n\n"
                        + "Una reserva inicial dura 15 minutos; al crear la sesión de checkout Stripe se extiende a 30 minutos.",
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