package com.distribution.music.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "tracks")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Track {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "release_id", nullable = false)
    private Release release;

    @Column(nullable = false)
    private Integer trackNumber;

    private String title;
    private String featuringArtists;
    private String composer;
    private String author;
    private String arranger;
    private String producer;

    @Column(unique = true)
    private String isrc;

    private String lyricsUrl;

    // Renseignés à la Phase 3 (upload audio)
    private String audioFileUrl;
    private Integer durationSeconds;
    private String audioFormat;
    private Integer sampleRate;
    private Integer bitDepth;
    private Double integratedLufs;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
