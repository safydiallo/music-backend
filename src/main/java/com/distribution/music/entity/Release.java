package com.distribution.music.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "releases")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Release {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "artist_id", nullable = false)
    private User artist;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReleaseType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReleaseStatus status;

    private String title;
    private String genre;
    private String subGenre;
    private String language;
    private boolean explicit;

    private LocalDate releaseDate;

    private String upc;
    private String copyrightP; // ℗ 2025 Nom de l'artiste / Label
    private String copyrightC; // © 2025 Nom de l'artiste

    private boolean distributionWorldwide;
    @Column(length = 1000)
    private String excludedTerritories; // codes pays séparés par des virgules

    private String coverUrl;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime submittedAt;
}
