package com.distribution.music.service;

import com.distribution.music.dto.*;
import com.distribution.music.entity.*;
import com.distribution.music.exception.ApiException;
import com.distribution.music.repository.ReleaseRepository;
import com.distribution.music.repository.TrackRepository;
import com.distribution.music.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Year;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReleaseService {

    private final ReleaseRepository releaseRepository;
    private final TrackRepository trackRepository;
    private final UserRepository userRepository;
    private final FileStorageService fileStorageService;
    private final AudioValidationService audioValidationService;

    @Value("${app.isrc.country-code:SN}")
    private String isrcCountryCode;

    @Value("${app.isrc.registrant-code:MZG}")
    private String isrcRegistrantCode;

    @Value("${app.release.min-days-before-release:7}")
    private int minDaysBeforeRelease;

    private static final Pattern ISRC_PATTERN = Pattern.compile("^[A-Z]{2}-?[A-Z0-9]{3}-?\\d{2}-?\\d{5}$");
    private static final Pattern UPC_PATTERN = Pattern.compile("^\\d{12,13}$");
    private static final Pattern COUNTRY_CODE_PATTERN = Pattern.compile("^[A-Z]{2}$");

    // ---------- Releases ----------

    public ReleaseResponse createDraft(String email, CreateReleaseRequest request) {
        User artist = getArtist(email);

        Release release = Release.builder()
                .artist(artist)
                .type(request.getType())
                .status(ReleaseStatus.DRAFT)
                .title(request.getTitle())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        release = releaseRepository.save(release);

        // Un Single a toujours exactement une piste, créée tout de suite
        if (request.getType() == ReleaseType.SINGLE) {
            Track track = Track.builder()
                    .release(release)
                    .trackNumber(1)
                    .title(request.getTitle())
                    .isrc(generateUniqueIsrc())
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();
            trackRepository.save(track);
        }

        log.info("Brouillon de sortie créé : id={}, artiste={}, type={}", release.getId(), email, request.getType());
        return toResponse(release);
    }

    public List<ReleaseResponse> listReleases(String email) {
        User artist = getArtist(email);
        return releaseRepository.findByArtistIdOrderByUpdatedAtDesc(artist.getId())
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public ReleaseDetailResponse getRelease(Long id, String email) {
        return toDetailResponse(getOwnedRelease(id, email));
    }

    public ReleaseDetailResponse updateRelease(Long id, String email, UpdateReleaseRequest request) {
        Release release = getOwnedRelease(id, email);
        requireDraft(release);

        if (request.getTitle() != null) release.setTitle(request.getTitle());
        if (request.getGenre() != null) release.setGenre(request.getGenre());
        if (request.getSubGenre() != null) release.setSubGenre(request.getSubGenre());
        if (request.getLanguage() != null) release.setLanguage(request.getLanguage());
        if (request.getExplicit() != null) release.setExplicit(request.getExplicit());
        if (request.getReleaseDate() != null) release.setReleaseDate(request.getReleaseDate());
        if (request.getUpc() != null) setUpc(release, request.getUpc());
        if (request.getCopyrightP() != null) release.setCopyrightP(request.getCopyrightP());
        if (request.getCopyrightC() != null) release.setCopyrightC(request.getCopyrightC());
        if (request.getDistributionWorldwide() != null) release.setDistributionWorldwide(request.getDistributionWorldwide());
        if (request.getExcludedTerritories() != null) release.setExcludedTerritories(normalizeTerritories(request.getExcludedTerritories()));
        if (request.getCoverUrl() != null) release.setCoverUrl(request.getCoverUrl());

        release.setUpdatedAt(LocalDateTime.now());
        releaseRepository.save(release);

        log.info("Sortie mise à jour (brouillon) : id={}", id);
        return toDetailResponse(release);
    }

    public void deleteRelease(Long id, String email) {
        Release release = getOwnedRelease(id, email);
        requireDraft(release);
        trackRepository.deleteAll(trackRepository.findByReleaseIdOrderByTrackNumberAsc(id));
        releaseRepository.delete(release);
        log.info("Sortie supprimée : id={}", id);
    }

    public ReleaseDetailResponse submitRelease(Long id, String email) {
        Release release = getOwnedRelease(id, email);
        requireDraft(release);

        List<Track> tracks = trackRepository.findByReleaseIdOrderByTrackNumberAsc(id);

        if (release.getTitle() == null || release.getTitle().isBlank()) {
            throw ApiException.badRequest("Le titre est requis avant soumission");
        }
        if (tracks.isEmpty()) {
            throw ApiException.badRequest("Au moins une piste est requise avant soumission");
        }
        for (Track track : tracks) {
            if (track.getAudioFileUrl() == null) {
                throw ApiException.badRequest("Chaque piste doit avoir un fichier audio avant soumission : " + track.getTitle());
            }
        }

        release.setStatus(ReleaseStatus.SUBMITTED);
        release.setSubmittedAt(LocalDateTime.now());
        releaseRepository.save(release);

        log.info("Sortie soumise : id={}", id);
        return toDetailResponse(release);
    }

    // ---------- Tracks ----------

    public TrackResponse addTrack(Long releaseId, String email, CreateTrackRequest request) {
        Release release = getOwnedRelease(releaseId, email);
        requireDraft(release);

        if (release.getType() == ReleaseType.SINGLE) {
            throw ApiException.badRequest("Un Single ne peut avoir qu'une seule piste");
        }

        int trackNumber = request.getTrackNumber() != null
                ? request.getTrackNumber()
                : trackRepository.findByReleaseIdOrderByTrackNumberAsc(releaseId).size() + 1;

        Track track = Track.builder()
                .release(release)
                .trackNumber(trackNumber)
                .title(request.getTitle())
                .featuringArtists(request.getFeaturingArtists())
                .composer(request.getComposer())
                .author(request.getAuthor())
                .arranger(request.getArranger())
                .producer(request.getProducer())
                .isrc(resolveIsrc(request.getIsrc(), null))
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        track = trackRepository.save(track);

        log.info("Piste ajoutée : release={}, piste={}", releaseId, track.getId());
        return toTrackResponse(track);
    }

    public TrackResponse updateTrack(Long releaseId, Long trackId, String email, UpdateTrackRequest request) {
        Release release = getOwnedRelease(releaseId, email);
        requireDraft(release);
        Track track = trackRepository.findByIdAndReleaseId(trackId, releaseId)
                .orElseThrow(() -> ApiException.notFound("Piste non trouvée"));

        if (request.getTrackNumber() != null) track.setTrackNumber(request.getTrackNumber());
        if (request.getTitle() != null) track.setTitle(request.getTitle());
        if (request.getFeaturingArtists() != null) track.setFeaturingArtists(request.getFeaturingArtists());
        if (request.getComposer() != null) track.setComposer(request.getComposer());
        if (request.getAuthor() != null) track.setAuthor(request.getAuthor());
        if (request.getArranger() != null) track.setArranger(request.getArranger());
        if (request.getProducer() != null) track.setProducer(request.getProducer());
        if (request.getLyricsUrl() != null) track.setLyricsUrl(request.getLyricsUrl());
        if (request.getIsrc() != null) track.setIsrc(resolveIsrc(request.getIsrc(), track.getId()));

        track.setUpdatedAt(LocalDateTime.now());
        trackRepository.save(track);
        return toTrackResponse(track);
    }

    public void deleteTrack(Long releaseId, Long trackId, String email) {
        Release release = getOwnedRelease(releaseId, email);
        requireDraft(release);
        if (release.getType() == ReleaseType.SINGLE) {
            throw ApiException.badRequest("Impossible de supprimer l'unique piste d'un Single");
        }
        Track track = trackRepository.findByIdAndReleaseId(trackId, releaseId)
                .orElseThrow(() -> ApiException.notFound("Piste non trouvée"));
        trackRepository.delete(track);
        log.info("Piste supprimée : release={}, piste={}", releaseId, trackId);
    }

    public TrackAudioUploadResponse uploadTrackAudio(Long releaseId, Long trackId, String email, MultipartFile file) {
        Release release = getOwnedRelease(releaseId, email);
        requireDraft(release);
        Track track = trackRepository.findByIdAndReleaseId(trackId, releaseId)
                .orElseThrow(() -> ApiException.notFound("Piste non trouvée"));

        StoredFile stored = fileStorageService.storeTrackAudio(file);
        AudioAnalysisResult analysis = audioValidationService.analyze(stored.absolutePath().toFile());

        if (!analysis.errors().isEmpty()) {
            fileStorageService.deleteFile(stored.absolutePath());
            throw ApiException.badRequest(String.join(" ; ", analysis.errors()));
        }

        // Si la piste avait déjà un audio (re-upload après correction), on nettoie l'ancien fichier
        String previousAudioUrl = track.getAudioFileUrl();

        track.setAudioFileUrl(stored.publicUrl());
        track.setAudioFormat(analysis.format());
        track.setSampleRate(analysis.sampleRate());
        track.setBitDepth(analysis.bitDepth());
        track.setDurationSeconds(analysis.durationSeconds());
        track.setIntegratedLufs(analysis.integratedLufs());
        track.setUpdatedAt(LocalDateTime.now());
        trackRepository.save(track);

        if (previousAudioUrl != null && !previousAudioUrl.equals(stored.publicUrl())) {
            fileStorageService.deletePublicFile(previousAudioUrl);
        }

        log.info("Audio uploadé : release={}, piste={}, {}Hz, {}-bit, {}s, LUFS={}",
                releaseId, trackId, analysis.sampleRate(), analysis.bitDepth(),
                analysis.durationSeconds(), analysis.integratedLufs());

        return new TrackAudioUploadResponse(toTrackResponse(track), analysis.warnings());
    }

    public ReleaseDetailResponse uploadCover(Long releaseId, String email, MultipartFile file) {
        Release release = getOwnedRelease(releaseId, email);
        requireDraft(release);

        String previousCoverUrl = release.getCoverUrl();

        String coverUrl = fileStorageService.storeCover(file);
        release.setCoverUrl(coverUrl);
        release.setUpdatedAt(LocalDateTime.now());
        releaseRepository.save(release);

        if (previousCoverUrl != null && !previousCoverUrl.equals(coverUrl)) {
            fileStorageService.deletePublicFile(previousCoverUrl);
        }

        log.info("Pochette mise à jour : release={}", releaseId);
        return toDetailResponse(release);
    }

    // ---------- Helpers ----------

    private User getArtist(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> ApiException.notFound("Utilisateur non trouvé"));
    }

    private Release getOwnedRelease(Long id, String email) {
        Release release = releaseRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Sortie non trouvée"));
        if (!release.getArtist().getEmail().equals(email)) {
            throw ApiException.forbidden("Tu n'as pas accès à cette sortie");
        }
        return release;
    }

    private void requireDraft(Release release) {
        if (release.getStatus() != ReleaseStatus.DRAFT) {
            throw ApiException.badRequest("Cette sortie a déjà été soumise et ne peut plus être modifiée");
        }
    }

    private String generateUniqueIsrc() {
        String year = String.valueOf(Year.now().getValue()).substring(2);
        String isrc;
        do {
            String designation = String.format("%05d", ThreadLocalRandom.current().nextInt(100000));
            isrc = isrcCountryCode + "-" + isrcRegistrantCode + "-" + year + "-" + designation;
        } while (trackRepository.existsByIsrc(isrc));
        return isrc;
    }

    /**
     * Si l'artiste fournit un ISRC manuel, on le valide et on vérifie son unicité.
     * Sinon, on en génère un automatiquement (comportement par défaut).
     * excludeTrackId : lors d'une modification, exclut la piste elle-même du contrôle d'unicité.
     */
    private String resolveIsrc(String rawIsrc, Long excludeTrackId) {
        if (rawIsrc == null || rawIsrc.isBlank()) {
            return generateUniqueIsrc();
        }

        String cleaned = rawIsrc.trim().toUpperCase();
        if (!ISRC_PATTERN.matcher(cleaned).matches()) {
            throw ApiException.badRequest("Format ISRC invalide (attendu : CC-XXX-YY-NNNNN)");
        }

        String digits = cleaned.replace("-", "");
        String formatted = digits.substring(0, 2) + "-" + digits.substring(2, 5) + "-"
                + digits.substring(5, 7) + "-" + digits.substring(7, 12);

        trackRepository.findByIsrc(formatted).ifPresent(existing -> {
            if (excludeTrackId == null || !existing.getId().equals(excludeTrackId)) {
                throw ApiException.conflict("Cet ISRC est déjà utilisé par une autre piste");
            }
        });

        return formatted;
    }

    private void setUpc(Release release, String rawUpc) {
        if (rawUpc.isBlank()) {
            release.setUpc(null);
            return;
        }
        String cleaned = rawUpc.trim();
        if (!UPC_PATTERN.matcher(cleaned).matches()) {
            throw ApiException.badRequest("Format UPC/EAN invalide (12 ou 13 chiffres attendus)");
        }
        releaseRepository.findByUpc(cleaned).ifPresent(existing -> {
            if (!existing.getId().equals(release.getId())) {
                throw ApiException.conflict("Ce code UPC/EAN est déjà utilisé par une autre sortie");
            }
        });
        release.setUpc(cleaned);
    }

    private String normalizeTerritories(String raw) {
        if (raw.isBlank()) {
            return null;
        }
        Set<String> codes = new LinkedHashSet<>();
        for (String part : raw.split(",")) {
            String code = part.trim().toUpperCase();
            if (code.isEmpty()) continue;
            if (!COUNTRY_CODE_PATTERN.matcher(code).matches()) {
                throw ApiException.badRequest("Code pays invalide : \"" + code + "\" (format ISO 2 lettres attendu, ex : FR, SN, US)");
            }
            codes.add(code);
        }
        return codes.isEmpty() ? null : String.join(",", codes);
    }

    private List<String> computeWarnings(Release release) {
        List<String> warnings = new ArrayList<>();
        if (release.getReleaseDate() != null) {
            long daysUntil = ChronoUnit.DAYS.between(LocalDate.now(), release.getReleaseDate());
            if (daysUntil < minDaysBeforeRelease) {
                warnings.add("Pour garantir la disponibilité sur toutes les plateformes, soumets au moins "
                        + minDaysBeforeRelease + " jours à l'avance (actuellement " + daysUntil + " jour(s) avant la date choisie).");
            }
        }
        return warnings;
    }

    private ReleaseResponse toResponse(Release release) {
        int trackCount = trackRepository.findByReleaseIdOrderByTrackNumberAsc(release.getId()).size();
        return new ReleaseResponse(
                release.getId(), release.getTitle(), release.getType(), release.getStatus(),
                release.getCoverUrl(), trackCount, release.getUpdatedAt()
        );
    }

    private ReleaseDetailResponse toDetailResponse(Release release) {
        List<TrackResponse> tracks = trackRepository.findByReleaseIdOrderByTrackNumberAsc(release.getId())
                .stream().map(this::toTrackResponse).collect(Collectors.toList());

        return new ReleaseDetailResponse(
                release.getId(), release.getType(), release.getStatus(), release.getTitle(),
                release.getGenre(), release.getSubGenre(), release.getLanguage(), release.isExplicit(),
                release.getReleaseDate(), release.getUpc(), release.getCopyrightP(), release.getCopyrightC(),
                release.isDistributionWorldwide(), release.getExcludedTerritories(), release.getCoverUrl(),
                tracks, release.getCreatedAt(), release.getUpdatedAt(), release.getSubmittedAt(),
                computeWarnings(release)
        );
    }

    private TrackResponse toTrackResponse(Track track) {
        return new TrackResponse(
                track.getId(), track.getTrackNumber(), track.getTitle(), track.getFeaturingArtists(),
                track.getComposer(), track.getAuthor(), track.getArranger(), track.getProducer(),
                track.getIsrc(), track.getLyricsUrl(), track.getAudioFileUrl(), track.getDurationSeconds(),
                track.getAudioFormat(), track.getSampleRate(), track.getBitDepth(), track.getIntegratedLufs()
        );
    }
}
