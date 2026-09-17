package com.distribution.music.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
public class AudioValidationService {

    @Value("${app.audio.min-sample-rate:44100}")
    private int minSampleRate;

    @Value("${app.audio.lufs-warning-threshold:-14.0}")
    private double lufsWarningThreshold;

    // Repère la ligne "I:         -13.5 LUFS" du résumé ebur128 de ffmpeg
    private static final Pattern LUFS_PATTERN = Pattern.compile("I:\\s*(-?\\d+\\.?\\d*)\\s*LUFS");

    public AudioAnalysisResult analyze(File wavFile) {
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        int sampleRate = 0;
        int bitDepth = 0;
        int durationSeconds = 0;
        Double lufs = null;

        try {
            AudioFileFormat fileFormat = AudioSystem.getAudioFileFormat(wavFile);
            AudioFormat format = fileFormat.getFormat();

            if (fileFormat.getType() != AudioFileFormat.Type.WAVE) {
                errors.add("Le fichier n'est pas un WAV valide");
            }

            sampleRate = (int) format.getSampleRate();
            bitDepth = format.getSampleSizeInBits();
            long frameLength = fileFormat.getFrameLength();
            if (frameLength > 0 && format.getFrameRate() > 0) {
                durationSeconds = (int) (frameLength / format.getFrameRate());
            }

            if (sampleRate < minSampleRate) {
                errors.add("Fréquence d'échantillonnage insuffisante : " + sampleRate + "Hz (minimum " + minSampleRate + "Hz)");
            }
            if (bitDepth != 16 && bitDepth != 24) {
                errors.add("Profondeur de bits non supportée : " + bitDepth + "-bit (attendu 16-bit ou 24-bit)");
            }
        } catch (UnsupportedAudioFileException e) {
            errors.add("Fichier audio illisible ou format non WAV");
        } catch (IOException e) {
            log.error("Erreur de lecture du fichier audio", e);
            errors.add("Erreur lors de la lecture du fichier audio");
        }

        // Inutile de lancer ffmpeg si le format de base est déjà invalide
        if (errors.isEmpty()) {
            lufs = measureIntegratedLoudness(wavFile);
            if (lufs != null && lufs > lufsWarningThreshold) {
                warnings.add(String.format(
                        "Volume trop fort : %.1f LUFS intégrés (recommandé : %.1f LUFS ou moins)",
                        lufs, lufsWarningThreshold));
            }
        }

        return new AudioAnalysisResult("WAVE", sampleRate, bitDepth, durationSeconds, lufs, errors, warnings);
    }

    private Double measureIntegratedLoudness(File file) {
        try {
            ProcessBuilder pb = new ProcessBuilder(
                    "ffmpeg", "-i", file.getAbsolutePath(), "-af", "ebur128=peak=true", "-f", "null", "-");
            pb.redirectErrorStream(true);
            Process process = pb.start();

            String output = new String(process.getInputStream().readAllBytes());
            boolean finished = process.waitFor(60, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                log.warn("Timeout lors de l'analyse LUFS de {}", file.getName());
                return null;
            }

            Double lastValue = null;
            Matcher matcher = LUFS_PATTERN.matcher(output);
            while (matcher.find()) {
                lastValue = Double.parseDouble(matcher.group(1));
            }
            return lastValue;
        } catch (IOException e) {
            log.warn("Impossible de mesurer le LUFS (ffmpeg indisponible ?) : {}", e.getMessage());
            return null;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Analyse LUFS interrompue : {}", e.getMessage());
            return null;
        }
    }
}
