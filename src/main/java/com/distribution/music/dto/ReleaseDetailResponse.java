package com.distribution.music.dto;

import com.distribution.music.entity.ReleaseStatus;
import com.distribution.music.entity.ReleaseType;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
public class ReleaseDetailResponse {
    private Long id;
    private ReleaseType type;
    private ReleaseStatus status;
    private String title;
    private String genre;
    private String subGenre;
    private String language;
    private boolean explicit;
    private LocalDate releaseDate;
    private String upc;
    private String copyrightP;
    private String copyrightC;
    private boolean distributionWorldwide;
    private String excludedTerritories;
    private String coverUrl;
    private List<TrackResponse> tracks;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime submittedAt;
    // Avertissements non bloquants recalculés à chaque lecture (ex. date de sortie trop proche)
    private List<String> warnings;
}
