package com.distribution.music.config;

import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI musicAppOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Music Backend API")
                        .description("API de gestion de l'application Music")
                        .version("v1")
                        .contact(new Contact()
                                .name("Musique Zig")
                                .email("support@musique-zig.com"))
                        .license(new License().name("Apache 2.0").url("http://springdoc.org")))
                .externalDocs(new ExternalDocumentation()
                        .description("Documentation de l'API")
                        .url("https://github.com/safydiallo/music-backend"));
    }
}
