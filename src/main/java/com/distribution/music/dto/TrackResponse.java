package com.distribution.music.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class TrackResponse {
    private Long id;
    private Integer trackNumber;
    private String title;
    private String featuringArtists;
    private String composer;
    private String author;
    private String arranger;
    private String producer;
    private String isrc;
    private String lyricsUrl;
    private String audioFileUrl;
    private Integer durationSeconds;
    private String audioFormat;
    private Integer sampleRate;
    private Integer bitDepth;
    private Double integratedLufs;
}
