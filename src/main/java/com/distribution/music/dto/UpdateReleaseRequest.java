package com.distribution.music.dto;

import lombok.Data;

import java.time.LocalDate;

@Data
public class UpdateReleaseRequest {
    private String title;
    private String genre;
    private String subGenre;
    private String language;
    private Boolean explicit;
    private LocalDate releaseDate;
    private String upc;
    private String copyrightP;
    private String copyrightC;
    private Boolean distributionWorldwide;
    private String excludedTerritories;
    private String coverUrl;
}
