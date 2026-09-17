package com.distribution.music.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration Swagger / OpenAPI.
 * Documentation interactive disponible sur /swagger-ui/index.html une fois l'application lancée.
 * Ajoute un bouton "Authorize" (JWT Bearer) permettant de tester directement les endpoints protégés
 * depuis l'interface, sans avoir besoin d'un outil externe (Postman, curl...).
 */
@Configuration
public class OpenApiConfig {

    private static final String BEARER_SCHEME = "bearerAuth";

    @Bean
    public OpenAPI musicOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Musique-Zig API")
                        .description("""
                                API backend de la plateforme de distribution musicale Musique-Zig.

                                Couvre : l'authentification (inscription, connexion, vérification email, mot de passe),
                                le profil artiste (informations, photo, réseaux sociaux), et la gestion des sorties
                                musicales (Single/EP/Album) avec leurs pistes, fichiers audio, pochette et métadonnées.

                                La plupart des endpoints nécessitent un token JWT obtenu via `POST /api/auth/login`,
                                à fournir dans l'en-tête `Authorization: Bearer <token>` (utilise le bouton
                                "Authorize" ci-dessus pour le renseigner une seule fois).
                                """)
                        .version("v1")
                        .contact(new Contact().name("Musique-Zig")))
                .addSecurityItem(new SecurityRequirement().addList(BEARER_SCHEME))
                .components(new Components()
                        .addSecuritySchemes(BEARER_SCHEME, new SecurityScheme()
                                .name(BEARER_SCHEME)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")));
    }
}
