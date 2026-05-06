package com.distribution.music.entity;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class UserProfileResponse {
    private String fullName;
    private String nomArtiste;
    private String email;
    private String pays;
    private String genreMusical;
    private String instagram;
    private String photoUrl;
    private String biographie;
    private String spotifyUrl;
    private String youtubeUrl;
    private String facebookUrl;
    private String tiktokUrl;
    private String role;
    private LocalDateTime createdAt;
}
