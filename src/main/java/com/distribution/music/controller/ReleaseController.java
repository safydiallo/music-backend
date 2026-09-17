package com.distribution.music.controller;

import com.distribution.music.dto.*;
import com.distribution.music.service.ReleaseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * Gestion des sorties musicales (Single / EP / Album) et de leurs pistes.
 *
 * Règles générales valables pour tous les endpoints ci-dessous :
 * - Authentification requise (`Authorization: Bearer <token>`).
 * - Un artiste ne peut voir/modifier que ses propres sorties (403 sinon).
 * - Toute modification (métadonnées, pistes, fichiers) est refusée (400) une fois la sortie
 *   passée au statut SUBMITTED — seule la lecture (`GET`) reste possible.
 */
@Tag(name = "Sorties musicales", description = "Création et gestion des sorties (Single/EP/Album) : brouillon, métadonnées, pistes, upload audio/pochette, soumission. Endpoints protégés par JWT ; chaque artiste n'accède qu'à ses propres sorties.")
@RestController
@RequestMapping("/api/releases")
@RequiredArgsConstructor
public class ReleaseController {

    private final ReleaseService releaseService;

    @Operation(
            summary = "Créer une nouvelle sortie (brouillon)",
            description = "Démarre une nouvelle distribution. Pour un `SINGLE`, l'unique piste est créée " +
                    "automatiquement (avec ISRC auto-généré) ; pour un `EP`/`ALBUM`, la sortie démarre sans " +
                    "piste — elles sont ajoutées ensuite via `POST /{id}/tracks`."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Brouillon créé"),
            @ApiResponse(responseCode = "400", description = "Type de sortie manquant ou invalide")
    })
    @PostMapping
    public ResponseEntity<ReleaseResponse> createDraft(
            Authentication auth, @Valid @RequestBody CreateReleaseRequest request) {
        return ResponseEntity.ok(releaseService.createDraft(auth.getName(), request));
    }

    @Operation(
            summary = "Lister ses sorties",
            description = "Renvoie toutes les sorties de l'artiste connecté (brouillons et soumises), triées " +
                    "des plus récemment modifiées aux plus anciennes. Renvoie `[]` si l'artiste n'a encore rien " +
                    "créé — utile pour afficher l'état vide du tableau de bord."
    )
    @ApiResponse(responseCode = "200", description = "Liste des sorties (peut être vide)")
    @GetMapping
    public ResponseEntity<List<ReleaseResponse>> listReleases(Authentication auth) {
        return ResponseEntity.ok(releaseService.listReleases(auth.getName()));
    }

    @Operation(
            summary = "Voir le détail d'une sortie",
            description = "Renvoie toutes les métadonnées de la sortie ainsi que la liste complète de ses " +
                    "pistes. Le champ `warnings` est recalculé à chaque appel (ex. avertissement si la date " +
                    "de sortie est à moins de 7 jours)."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Détail de la sortie"),
            @ApiResponse(responseCode = "403", description = "Cette sortie appartient à un autre artiste"),
            @ApiResponse(responseCode = "404", description = "Sortie introuvable")
    })
    @GetMapping("/{id}")
    public ResponseEntity<ReleaseDetailResponse> getRelease(
            Authentication auth,
            @Parameter(description = "Identifiant de la sortie") @PathVariable Long id) {
        return ResponseEntity.ok(releaseService.getRelease(id, auth.getName()));
    }

    @Operation(
            summary = "Mettre à jour les métadonnées (sauvegarde du brouillon)",
            description = "Mise à jour partielle : seuls les champs fournis (non `null`) sont modifiés — à " +
                    "appeler à chaque étape du formulaire pour la sauvegarde automatique du brouillon. " +
                    "Valide et normalise `upc` (12-13 chiffres, unique), `excludedTerritories` (codes pays " +
                    "ISO 2 lettres séparés par des virgules, ex. `FR,US`) et déclenche un avertissement non " +
                    "bloquant si `releaseDate` est à moins de 7 jours."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Sortie mise à jour"),
            @ApiResponse(responseCode = "400", description = "Champ invalide, ou sortie déjà soumise (non modifiable)"),
            @ApiResponse(responseCode = "403", description = "Cette sortie appartient à un autre artiste"),
            @ApiResponse(responseCode = "404", description = "Sortie introuvable"),
            @ApiResponse(responseCode = "409", description = "Ce code UPC/EAN est déjà utilisé par une autre sortie")
    })
    @PutMapping("/{id}")
    public ResponseEntity<ReleaseDetailResponse> updateRelease(
            Authentication auth,
            @Parameter(description = "Identifiant de la sortie") @PathVariable Long id,
            @RequestBody UpdateReleaseRequest request) {
        return ResponseEntity.ok(releaseService.updateRelease(id, auth.getName(), request));
    }

    @Operation(
            summary = "Supprimer une sortie",
            description = "Supprime définitivement la sortie ainsi que toutes ses pistes (et leurs fichiers " +
                    "audio associés). Uniquement possible tant que la sortie est en brouillon (`DRAFT`)."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Sortie supprimée"),
            @ApiResponse(responseCode = "400", description = "Sortie déjà soumise (non supprimable)"),
            @ApiResponse(responseCode = "403", description = "Cette sortie appartient à un autre artiste"),
            @ApiResponse(responseCode = "404", description = "Sortie introuvable")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRelease(
            Authentication auth,
            @Parameter(description = "Identifiant de la sortie") @PathVariable Long id) {
        releaseService.deleteRelease(id, auth.getName());
        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "Soumettre la sortie",
            description = "Verrouille la sortie (passage du statut `DRAFT` à `SUBMITTED`) et bloque toute " +
                    "modification ultérieure. Refusé si le titre est manquant, si aucune piste n'existe, ou " +
                    "si une piste n'a pas encore de fichier audio uploadé."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Sortie soumise"),
            @ApiResponse(responseCode = "400", description = "Sortie incomplète (titre, piste ou audio manquant) ou déjà soumise"),
            @ApiResponse(responseCode = "403", description = "Cette sortie appartient à un autre artiste"),
            @ApiResponse(responseCode = "404", description = "Sortie introuvable")
    })
    @PostMapping("/{id}/submit")
    public ResponseEntity<ReleaseDetailResponse> submitRelease(
            Authentication auth,
            @Parameter(description = "Identifiant de la sortie") @PathVariable Long id) {
        return ResponseEntity.ok(releaseService.submitRelease(id, auth.getName()));
    }

    @Operation(
            summary = "Ajouter une piste (EP/Album uniquement)",
            description = "Ajoute une nouvelle piste à un EP ou un Album (impossible sur un Single, qui a " +
                    "toujours exactement une piste). Le numéro de piste est auto-incrémenté si non fourni. " +
                    "L'ISRC est généré automatiquement, sauf si `isrc` est fourni manuellement " +
                    "(format `CC-XXX-YY-NNNNN`, validé et vérifié unique)."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Piste créée"),
            @ApiResponse(responseCode = "400", description = "Sortie de type SINGLE, sortie déjà soumise, ou ISRC au format invalide"),
            @ApiResponse(responseCode = "403", description = "Cette sortie appartient à un autre artiste"),
            @ApiResponse(responseCode = "404", description = "Sortie introuvable"),
            @ApiResponse(responseCode = "409", description = "Cet ISRC est déjà utilisé par une autre piste")
    })
    @PostMapping("/{id}/tracks")
    public ResponseEntity<TrackResponse> addTrack(
            Authentication auth,
            @Parameter(description = "Identifiant de la sortie") @PathVariable Long id,
            @RequestBody CreateTrackRequest request) {
        return ResponseEntity.ok(releaseService.addTrack(id, auth.getName(), request));
    }

    @Operation(
            summary = "Modifier une piste",
            description = "Mise à jour partielle des métadonnées d'une piste (titre, featuring, compositeur, " +
                    "auteur, arrangeur, producteur, paroles, ISRC). Seuls les champs fournis sont modifiés."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Piste mise à jour"),
            @ApiResponse(responseCode = "400", description = "Sortie déjà soumise, ou ISRC au format invalide"),
            @ApiResponse(responseCode = "403", description = "Cette sortie appartient à un autre artiste"),
            @ApiResponse(responseCode = "404", description = "Sortie ou piste introuvable"),
            @ApiResponse(responseCode = "409", description = "Cet ISRC est déjà utilisé par une autre piste")
    })
    @PutMapping("/{id}/tracks/{trackId}")
    public ResponseEntity<TrackResponse> updateTrack(
            Authentication auth,
            @Parameter(description = "Identifiant de la sortie") @PathVariable Long id,
            @Parameter(description = "Identifiant de la piste") @PathVariable Long trackId,
            @RequestBody UpdateTrackRequest request) {
        return ResponseEntity.ok(releaseService.updateTrack(id, trackId, auth.getName(), request));
    }

    @Operation(
            summary = "Supprimer une piste (EP/Album uniquement)",
            description = "Impossible de supprimer l'unique piste d'un Single, ni une piste d'une sortie déjà soumise."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Piste supprimée"),
            @ApiResponse(responseCode = "400", description = "Sortie de type SINGLE ou déjà soumise"),
            @ApiResponse(responseCode = "403", description = "Cette sortie appartient à un autre artiste"),
            @ApiResponse(responseCode = "404", description = "Sortie ou piste introuvable")
    })
    @DeleteMapping("/{id}/tracks/{trackId}")
    public ResponseEntity<Void> deleteTrack(
            Authentication auth,
            @Parameter(description = "Identifiant de la sortie") @PathVariable Long id,
            @Parameter(description = "Identifiant de la piste") @PathVariable Long trackId) {
        releaseService.deleteTrack(id, trackId, auth.getName());
        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "Uploader le fichier audio d'une piste",
            description = "Reçoit un fichier WAV (`multipart/form-data`, champ `file`), 200 Mo maximum. " +
                    "Validation bloquante (400) : doit être un WAV valide, fréquence d'échantillonnage " +
                    "≥ 44.1kHz, profondeur 16 ou 24-bit. Validation non bloquante : le volume intégré est " +
                    "mesuré (LUFS, via ffmpeg) et un avertissement est renvoyé dans `warnings` s'il dépasse " +
                    "-14 LUFS — l'upload réussit quand même. Un nouvel upload remplace proprement le fichier précédent."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Audio analysé et enregistré (voir `warnings` pour d'éventuelles alertes non bloquantes)"),
            @ApiResponse(responseCode = "400", description = "Fichier non-WAV, fréquence/bit-depth invalide, ou sortie déjà soumise"),
            @ApiResponse(responseCode = "403", description = "Cette sortie appartient à un autre artiste"),
            @ApiResponse(responseCode = "404", description = "Sortie ou piste introuvable")
    })
    @PostMapping("/{id}/tracks/{trackId}/audio")
    public ResponseEntity<TrackAudioUploadResponse> uploadTrackAudio(
            Authentication auth,
            @Parameter(description = "Identifiant de la sortie") @PathVariable Long id,
            @Parameter(description = "Identifiant de la piste") @PathVariable Long trackId,
            @RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(releaseService.uploadTrackAudio(id, trackId, auth.getName(), file));
    }

    @Operation(
            summary = "Uploader la pochette de la sortie",
            description = "Reçoit une image (`multipart/form-data`, champ `file`) : JPG ou PNG, 15 Mo maximum. " +
                    "Doit être carrée (ratio 1:1) et faire au moins 3000x3000px, sinon rejetée (400) avec un " +
                    "message précisant le problème (résolution ou ratio). Le recadrage carré se fait côté " +
                    "frontend ; ce endpoint valide juste le résultat final. Un nouvel upload remplace proprement " +
                    "la pochette précédente."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Pochette enregistrée, sortie complète renvoyée avec le nouveau `coverUrl`"),
            @ApiResponse(responseCode = "400", description = "Format non supporté, résolution insuffisante, pas carrée, ou sortie déjà soumise"),
            @ApiResponse(responseCode = "403", description = "Cette sortie appartient à un autre artiste"),
            @ApiResponse(responseCode = "404", description = "Sortie introuvable")
    })
    @PostMapping("/{id}/cover")
    public ResponseEntity<ReleaseDetailResponse> uploadCover(
            Authentication auth,
            @Parameter(description = "Identifiant de la sortie") @PathVariable Long id,
            @RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(releaseService.uploadCover(id, auth.getName(), file));
    }
}
