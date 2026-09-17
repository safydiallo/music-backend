package com.distribution.music.service;

import java.util.List;

/**
 * Résultat de l'analyse d'un fichier audio :
 * caractéristiques techniques + listes d'erreurs bloquantes et d'avertissements non bloquants.
 */
public record AudioAnalysisResult(
        String format,
        int sampleRate,
        int bitDepth,
        int durationSeconds,
        Double integratedLufs,
        List<String> errors,
        List<String> warnings
) {
}
