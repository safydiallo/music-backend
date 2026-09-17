package com.distribution.music.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.distribution.music.entity.UpdateProfileRequest;
import com.distribution.music.entity.UserProfileResponse;
import com.distribution.music.service.UserService;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Tag(name = "Profil utilisateur", description = "Consultation et mise à jour du profil de l'artiste connecté (infos, photo, réseaux sociaux). Tous les endpoints nécessitent un token JWT (`Authorization: Bearer <token>`) et agissent uniquement sur le compte du token fourni.")
@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @Operation(
            summary = "Récupérer son profil",
            description = "Renvoie les informations complètes de l'artiste connecté : nom, biographie, photo, " +
                    "réseaux sociaux, rôle, etc. Utile pour pré-remplir l'écran d'onboarding ou la page profil."
    )
    @ApiResponse(responseCode = "200", description = "Profil renvoyé")
    @GetMapping("/profile")
    public ResponseEntity<UserProfileResponse> getProfile(Authentication auth) {
        return ResponseEntity.ok(userService.getProfile(auth.getName()));
    }

    @Operation(
            summary = "Mettre à jour son profil",
            description = "Mise à jour partielle : seuls les champs fournis (non `null`) dans le corps de la " +
                    "requête sont modifiés, les autres restent inchangés. Couvre notamment la biographie et les " +
                    "liens réseaux sociaux (Instagram, Spotify, YouTube, Facebook, TikTok) de l'écran d'onboarding."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Profil mis à jour, renvoie le profil complet"),
            @ApiResponse(responseCode = "400", description = "Champ invalide")
    })
    @PutMapping("/profile")
    public ResponseEntity<UserProfileResponse> updateProfile(
            Authentication auth,
            @Valid @RequestBody UpdateProfileRequest request) {
        return ResponseEntity.ok(userService.updateProfile(auth.getName(), request));
    }

    @Operation(
            summary = "Uploader sa photo de profil",
            description = "Reçoit un fichier image (`multipart/form-data`, champ `file`) : JPEG, PNG ou WEBP, " +
                    "5 Mo maximum. Remplace la photo existante (l'ancien fichier est supprimé automatiquement). " +
                    "L'image est ensuite servie publiquement via l'URL renvoyée dans `photoUrl` (ex. `/uploads/photos/xxx.jpg`)."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Photo enregistrée, profil complet renvoyé avec le nouveau `photoUrl`"),
            @ApiResponse(responseCode = "400", description = "Fichier vide, format non supporté ou trop volumineux (> 5 Mo)")
    })
    @PostMapping("/profile/photo")
    public ResponseEntity<UserProfileResponse> uploadProfilePhoto(
            Authentication auth,
            @RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(userService.updateProfilePhoto(auth.getName(), file));
    }

    @Operation(
            summary = "Supprimer son compte",
            description = "Suppression définitive et irréversible du compte de l'artiste connecté."
    )
    @ApiResponse(responseCode = "200", description = "Compte supprimé")
    @DeleteMapping("/account")
    public ResponseEntity<String> deleteAccount(Authentication auth) {
        userService.deleteAccount(auth.getName());
        return ResponseEntity.ok("Compte supprimé avec succès.");
    }
}
