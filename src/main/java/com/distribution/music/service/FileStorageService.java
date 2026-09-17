package com.distribution.music.service;

import com.distribution.music.exception.ApiException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
public class FileStorageService {

    @Value("${app.upload.dir}")
    private String uploadDir;

    private static final List<String> ALLOWED_TYPES = List.of("image/jpeg", "image/png", "image/webp");
    private static final long MAX_SIZE = 5 * 1024 * 1024; // 5 Mo

    private static final List<String> ALLOWED_AUDIO_TYPES =
            List.of("audio/wav", "audio/x-wav", "audio/vnd.wave", "audio/wave", "audio/x-pn-wav");
    private static final long MAX_AUDIO_SIZE = 200L * 1024 * 1024; // 200 Mo

    private static final List<String> ALLOWED_COVER_TYPES = List.of("image/jpeg", "image/png");
    private static final long MAX_COVER_SIZE = 15L * 1024 * 1024; // 15 Mo
    private static final int MIN_COVER_DIMENSION = 3000; // px

    public String storeProfilePhoto(MultipartFile file) {
        if (file.isEmpty()) {
            throw ApiException.badRequest("Fichier vide");
        }
        if (!ALLOWED_TYPES.contains(file.getContentType())) {
            throw ApiException.badRequest("Format non supporté. Utilise JPEG, PNG ou WEBP.");
        }
        if (file.getSize() > MAX_SIZE) {
            throw ApiException.badRequest("Fichier trop volumineux (max 5 Mo).");
        }

        try {
            Path photosDir = Paths.get(uploadDir, "photos");
            Files.createDirectories(photosDir);

            String extension = switch (file.getContentType()) {
                case "image/png" -> ".png";
                case "image/webp" -> ".webp";
                default -> ".jpg";
            };
            String filename = UUID.randomUUID() + extension;
            Path target = photosDir.resolve(filename);

            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);

            return "/uploads/photos/" + filename;
        } catch (IOException e) {
            log.error("Erreur lors de l'enregistrement de la photo", e);
            throw new RuntimeException("Impossible d'enregistrer le fichier", e);
        }
    }

    public StoredFile storeTrackAudio(MultipartFile file) {
        if (file.isEmpty()) {
            throw ApiException.badRequest("Fichier vide");
        }

        String filename = file.getOriginalFilename();
        boolean looksLikeWav =
                (file.getContentType() != null && ALLOWED_AUDIO_TYPES.contains(file.getContentType()))
                || (filename != null && filename.toLowerCase().endsWith(".wav"));
        if (!looksLikeWav) {
            throw ApiException.badRequest("Format non supporté. Seul le WAV est accepté.");
        }
        if (file.getSize() > MAX_AUDIO_SIZE) {
            throw ApiException.badRequest("Fichier trop volumineux (max 200 Mo).");
        }

        try {
            Path audioDir = Paths.get(uploadDir, "audio");
            Files.createDirectories(audioDir);

            String uniqueName = UUID.randomUUID() + ".wav";
            Path target = audioDir.resolve(uniqueName);

            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);

            return new StoredFile("/uploads/audio/" + uniqueName, target);
        } catch (IOException e) {
            log.error("Erreur lors de l'enregistrement du fichier audio", e);
            throw new RuntimeException("Impossible d'enregistrer le fichier audio", e);
        }
    }

    public String storeCover(MultipartFile file) {
        if (file.isEmpty()) {
            throw ApiException.badRequest("Fichier vide");
        }
        if (!ALLOWED_COVER_TYPES.contains(file.getContentType())) {
            throw ApiException.badRequest("Format non supporté. Utilise JPG ou PNG.");
        }
        if (file.getSize() > MAX_COVER_SIZE) {
            throw ApiException.badRequest("Fichier trop volumineux (max 15 Mo).");
        }

        byte[] bytes;
        BufferedImage image;
        try {
            bytes = file.getBytes();
            image = ImageIO.read(new ByteArrayInputStream(bytes));
        } catch (IOException e) {
            log.error("Erreur de lecture de la pochette", e);
            throw new RuntimeException("Impossible de lire le fichier image", e);
        }

        if (image == null) {
            throw ApiException.badRequest("Fichier image illisible ou corrompu");
        }

        int width = image.getWidth();
        int height = image.getHeight();

        if (width < MIN_COVER_DIMENSION || height < MIN_COVER_DIMENSION) {
            throw ApiException.badRequest(
                    "Résolution insuffisante : " + width + "x" + height + "px (minimum "
                            + MIN_COVER_DIMENSION + "x" + MIN_COVER_DIMENSION + "px)");
        }
        if (width != height) {
            throw ApiException.badRequest(
                    "La pochette doit être carrée (ratio 1:1) : reçu " + width + "x" + height + "px");
        }

        try {
            Path coversDir = Paths.get(uploadDir, "covers");
            Files.createDirectories(coversDir);

            String extension = "image/png".equals(file.getContentType()) ? ".png" : ".jpg";
            String filename = UUID.randomUUID() + extension;
            Path target = coversDir.resolve(filename);

            Files.write(target, bytes);

            return "/uploads/covers/" + filename;
        } catch (IOException e) {
            log.error("Erreur lors de l'enregistrement de la pochette", e);
            throw new RuntimeException("Impossible d'enregistrer le fichier", e);
        }
    }

    public void deleteFile(Path path) {
        try {
            Files.deleteIfExists(path);
        } catch (IOException e) {
            log.warn("Impossible de supprimer le fichier {}", path, e);
        }
    }

    /**
     * Supprime un fichier à partir de son URL publique (ex. "/uploads/audio/xxx.wav"),
     * utile pour nettoyer l'ancien fichier lors d'un remplacement (re-upload).
     */
    public void deletePublicFile(String publicUrl) {
        if (publicUrl == null || !publicUrl.startsWith("/uploads/")) {
            return;
        }
        Path path = Paths.get(uploadDir, publicUrl.substring("/uploads/".length()));
        deleteFile(path);
    }
}
