package com.distribution.music.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(nullable = false)
    private String password;

    private String fullName;

    @Column(nullable = false)
    private String nomArtiste;

    @Column(nullable = false)
    private String pays;

    @Column(nullable = false)
    private String genreMusical;

    @Enumerated(EnumType.STRING)
    private Role role;

    private boolean enabled;

    private String verificationToken;

    private String biographie;

    private String photoUrl;

    //lien vers les réseaux sociaux
    private String instagram;
    private String spotifyUrl;
    private String youtubeUrl;
    private String facebookUrl;
    private String tiktokUrl;

    //  expiration du token (24h)
    private LocalDateTime verificationTokenExpiresAt;

    private LocalDateTime createdAt;

    //Reinitialisation du mot de passe
    private String resetPasswordToken;
    private LocalDateTime resetPasswordTokenExpiresAt;  
}