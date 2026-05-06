package com.distribution.music.entity;

import lombok.Data;

@Data
public class UpdateProfileRequest {
    private String fullName;
    private String nomArtiste;
    private String pays;
    private String genreMusical;
    private String biographie;
    private String photoUrl;
    private String instagram;
    private String spotifyUrl;
    private String youtubeUrl;
    private String facebookUrl;
    private String tiktokUrl;
}
