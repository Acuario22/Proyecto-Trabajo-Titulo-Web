package com.gestionreportes.dashboard.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.Contact;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuración de Swagger / OpenAPI 3 para Spring Boot 3.4.
 *
 * springdoc-openapi-starter-webmvc-ui 2.7.x es compatible con Spring Boot 3.4.x.
 * El error HTTP 500 en /v3/api-docs suele aparecer cuando:
 *   a) El controlador expone directamente una entidad JPA con lazily-loaded relations (ciclo JSON).
 *   b) Falta un Bean de tipo OpenAPI que permita a springdoc inicializar el contexto.
 *
 * Resolución: todos los endpoints deben retornar DTOs (nunca entidades JPA) +
 * esta clase garantiza la inicialización correcta del contexto OpenAPI.
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI dashboardOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Dashboard Reportes Urbanos API")
                        .version("1.0.0")
                        .description("API REST para la gestión y consulta de reportes ciudadanos de infraestructura urbana")
                        .contact(new Contact()
                                .name("Proyecto Trabajo de Título")
                                .email("contacto@gestionreportes.cl")));
    }
}
