package com.distribution.music.controller;

import com.distribution.music.dto.*;
import com.distribution.music.entity.ChangePasswordRequest;
import com.distribution.music.exception.ApiException;
import com.distribution.music.security.JwtUtil;
import com.distribution.music.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Authentification", description = "Inscription, connexion, vérification d'email et gestion du mot de passe. Ces endpoints sont publics (pas de token requis), sauf `/logout` et `/change-password`.")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final JwtUtil jwtUtil;

    @Operation(
            summary = "Inscrire un nouvel artiste",
            description = "Crée un compte artiste (rôle ARTIST) désactivé par défaut, puis envoie un email " +
                    "de vérification contenant un lien vers `GET /api/auth/verify?token=...`. " +
                    "Le compte ne pourra pas se connecter tant que l'email n'est pas vérifié."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Inscription réussie, email de vérification envoyé"),
            @ApiResponse(responseCode = "400", description = "Champs invalides (email mal formé, mot de passe trop faible, etc.)"),
            @ApiResponse(responseCode = "409", description = "Un compte existe déjà avec cet email")
    })
    @SecurityRequirements
    @PostMapping("/register")
    public ResponseEntity<RegisterResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.ok(authService.register(request));
    }

    @Operation(
            summary = "Se connecter",
            description = "Vérifie l'email/mot de passe et renvoie un JWT à utiliser dans l'en-tête " +
                    "`Authorization: Bearer <token>` pour tous les endpoints protégés. " +
                    "Échoue si le compte n'est pas encore vérifié par email."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Connexion réussie, token JWT renvoyé"),
            @ApiResponse(responseCode = "401", description = "Email/mot de passe incorrect, ou compte non vérifié")
    })
    @SecurityRequirements
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @Operation(
            summary = "Vérifier l'adresse email",
            description = "Appelé via le lien reçu par email après inscription (`token` = celui généré à " +
                    "l'inscription). Active le compte (`enabled = true`) s'il est valide et non expiré."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Compte activé avec succès"),
            @ApiResponse(responseCode = "400", description = "Token invalide, déjà utilisé ou expiré")
    })
    @SecurityRequirements
    @GetMapping("/verify")
    public ResponseEntity<String> verify(
            @Parameter(description = "Token de vérification reçu par email") @RequestParam String token) {
        return ResponseEntity.ok(authService.verifyEmail(token));
    }

    @Operation(
            summary = "Demander une réinitialisation de mot de passe",
            description = "Envoie un email contenant un lien vers `POST /api/auth/reset-password` avec un token " +
                    "temporaire. Ne révèle pas si l'email existe ou non en base (réponse identique dans les deux cas)."
    )
    @ApiResponse(responseCode = "200", description = "Email envoyé (si le compte existe)")
    @SecurityRequirements
    @PostMapping("/forgot-password")
    public ResponseEntity<String> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        authService.forgotPassword(request);
        return ResponseEntity.ok("Email de réinitialisation envoyé.");
    }

    @Operation(
            summary = "Réinitialiser le mot de passe",
            description = "Définit un nouveau mot de passe à partir du token reçu par email via `/forgot-password`."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Mot de passe mis à jour"),
            @ApiResponse(responseCode = "400", description = "Token invalide ou expiré")
    })
    @SecurityRequirements
    @PostMapping("/reset-password")
    public ResponseEntity<String> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request);
        return ResponseEntity.ok("Mot de passe mis à jour avec succès.");
    }

    @Operation(
            summary = "Se déconnecter",
            description = "Invalide le token JWT courant (ajouté à une liste noire) pour qu'il ne puisse plus " +
                    "être réutilisé, même s'il n'est pas encore expiré."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Déconnexion réussie"),
            @ApiResponse(responseCode = "400", description = "En-tête Authorization manquant ou mal formé")
    })
    @PostMapping("/logout")
    public ResponseEntity<String> logout(
            @Parameter(description = "En-tête `Authorization: Bearer <token>`", required = true)
            @RequestHeader("Authorization") String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw ApiException.badRequest("Token manquant ou invalide");
        }
        String token = authHeader.substring(7);
        authService.logout(token);
        return ResponseEntity.ok("Déconnexion réussie.");
    }

    @Operation(
            summary = "Changer son mot de passe (utilisateur connecté)",
            description = "Remplace le mot de passe de l'utilisateur actuellement authentifié. " +
                    "`newPassword` et `confirmPassword` doivent être identiques."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Mot de passe modifié"),
            @ApiResponse(responseCode = "400", description = "Mot de passe trop faible ou confirmation différente")
    })
    @PostMapping("/change-password")
    public ResponseEntity<String> changePassword(
            @Parameter(description = "En-tête `Authorization: Bearer <token>`", required = true)
            @RequestHeader("Authorization") String authHeader,
            @Valid @RequestBody ChangePasswordRequest request) {
        String token = authHeader.substring(7);
        String email = jwtUtil.extractEmail(token);
        authService.changePassword(email, request);
        return ResponseEntity.ok("Mot de passe modifié avec succès.");
    }
}
