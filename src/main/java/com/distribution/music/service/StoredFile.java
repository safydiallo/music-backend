package com.distribution.music.service;

import java.nio.file.Path;

/**
 * Résultat de l'enregistrement d'un fichier sur disque :
 * l'URL publique (servie via /uploads/**) et le chemin absolu réel
 * (nécessaire pour l'analyser, ex. avec ffmpeg).
 */
public record StoredFile(String publicUrl, Path absolutePath) {
}
