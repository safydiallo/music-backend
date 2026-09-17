package com.distribution.music.service;

import com.distribution.music.exception.ApiException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

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
}
