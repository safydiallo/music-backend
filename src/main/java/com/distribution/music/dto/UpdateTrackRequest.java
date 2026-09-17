package com.distribution.music.dto;

import lombok.Data;

@Data
public class UpdateTrackRequest {
    private Integer trackNumber;
    private String title;
    private String featuringArtists;
    private String composer;
    private String author;
    private String arranger;
    private String producer;
    private String lyricsUrl;
    // Optionnel : permet de corriger manuellement l'ISRC auto-généré
    private String isrc;
}
