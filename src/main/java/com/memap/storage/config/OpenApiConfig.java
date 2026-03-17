package com.memap.storage.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {
  private static final String BEARER_AUTH = "bearerAuth";
  private static final String COOKIE_AUTH = "cookieAuth";

  @Bean
  public OpenAPI openAPI(@Value("${open.api.title}") String title,
      @Value("${open.api.version}") String version,
      @Value("${open.api.description}") String description,
      @Value("${open.api.serverUrl}") String serverUrl,
      @Value("${open.api.serverName}") String serverName) {
    return new OpenAPI()
        .info(new Info().title(title).version(version).description(description))
        .servers(List.of(new Server().url(serverUrl).description(serverName)))
        .addSecurityItem(new SecurityRequirement().addList(BEARER_AUTH))
        .addSecurityItem(new SecurityRequirement().addList(COOKIE_AUTH))
        .components(new Components()
            .addSecuritySchemes(BEARER_AUTH,
                new SecurityScheme()
                    .type(SecurityScheme.Type.HTTP)
                    .scheme("bearer")
                    .bearerFormat("JWT")
                    .description("Enter JWT token"))
            .addSecuritySchemes(COOKIE_AUTH,
                new SecurityScheme()
                    .type(SecurityScheme.Type.APIKEY)
                    .in(SecurityScheme.In.COOKIE)
                    .name("access_token")));
  }

  @Bean
  public GroupedOpenApi groupedOpenApi() {
    return GroupedOpenApi.builder()
        .group("storage-service-docs")
        .packagesToScan("com.memap.storage.controller")
        .build();
  }
}
