package com.distribution.music.dto;

import lombok.Data;

@Data
public class CreateTrackRequest {
    private Integer trackNumber;
    private String title;
    private String featuringArtists;
    private String composer;
    private String author;
    private String arranger;
    private String producer;
    // Optionnel : ISRC fourni manuellement par l'artiste. Si absent, généré automatiquement.
    private String isrc;
}
