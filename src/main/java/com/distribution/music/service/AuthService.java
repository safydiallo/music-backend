package com.distribution.music.service;

import com.distribution.music.dto.*;
import com.distribution.music.entity.Role;
import com.distribution.music.entity.User;
import com.distribution.music.exception.ApiException;
import com.distribution.music.repository.UserRepository;
import com.distribution.music.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final EmailService emailService;
    private final TokenCacheService tokenCacheService;


    private static final int TOKEN_EXPIRY_HOURS = 24;

    public RegisterResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw ApiException.conflict("Cet email est déjà utilisé");
        }

        String token = UUID.randomUUID().toString();

        User user = User.builder()
                .fullName(request.getFullName())
                .nomArtiste(request.getNomArtiste())
                .genreMusical(request.getGenreMusical())
                .pays(request.getPays())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(Role.ARTIST)
                .enabled(false)
                .verificationToken(token)
                // NOUVEAU : expiration dans 24h
                .verificationTokenExpiresAt(LocalDateTime.now().plusHours(TOKEN_EXPIRY_HOURS))
                .createdAt(LocalDateTime.now())
                .build();

        userRepository.save(user);
        EmailContent emailContent = emailService.sendVerificationEmail(user.getEmail(), token);

        log.info("Nouvel utilisateur inscrit : {}", request.getEmail());
        return new RegisterResponse(
                "Inscription réussie. Vérifie ton email.",
                emailContent.fromName(),
                emailContent.fromAddress(),
                emailContent.to(),
                emailContent.subject(),
                emailContent.body()
        );
    }

    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> ApiException.unauthorized("Email ou mot de passe incorrect"));

        if (!user.isEnabled()) {
            throw ApiException.unauthorized("Compte non vérifié. Vérifie ton email.");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            // Message volontairement vague pour ne pas confirmer l'existence du compte
            log.warn("Tentative de connexion échouée pour : {}", request.getEmail());
            throw ApiException.unauthorized("Email ou mot de passe incorrect");
        }

        String token = jwtUtil.generateToken(user.getEmail(), user.getRole().name());
        log.info("Connexion réussie : {}", user.getEmail());
        return new AuthResponse(token, user.getRole().name(), user.getEmail(), user.getFullName());
    }

    public String verifyEmail(String token) {
        User user = userRepository.findByVerificationToken(token)
                .orElseThrow(() -> ApiException.badRequest("Token invalide"));

        //  vérification de l'expiration
        if (user.getVerificationTokenExpiresAt().isBefore(LocalDateTime.now())) {
            throw ApiException.badRequest("Ce lien de vérification a expiré. Réinscris-toi.");
        }

        user.setEnabled(true);
        user.setVerificationToken(null);
        user.setVerificationTokenExpiresAt(null);
        userRepository.save(user);

        log.info("Compte vérifié : {}", user.getEmail());
        return "Compte vérifié avec succès !";
    }

    public void forgotPassword(ForgotPasswordRequest request) {
        // Réponse identique quel que soit le résultat (évite de confirmer l'existence d'un compte)
        userRepository.findByEmail(request.getEmail()).ifPresent(user -> {
            String token = UUID.randomUUID().toString();
            user.setResetPasswordToken(token);
            user.setResetPasswordTokenExpiresAt(LocalDateTime.now().plusHours(1));
            userRepository.save(user);
            emailService.sendResetPasswordEmail(user.getEmail(), token);
        });
        log.info("Demande de réinitialisation pour : {}", request.getEmail());
    }

    public void resetPassword(ResetPasswordRequest request) {
        User user = userRepository.findByResetPasswordToken(request.getToken())
                .orElseThrow(() -> ApiException.badRequest("Token invalide"));

        if (user.getResetPasswordTokenExpiresAt().isBefore(LocalDateTime.now())) {
            throw ApiException.badRequest("Ce lien a expiré.");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        user.setResetPasswordToken(null);
        user.setResetPasswordTokenExpiresAt(null);
        userRepository.save(user);
    }
    public void logout(String token) {
        if (tokenCacheService.isBlacklisted(token)) {
            throw ApiException.badRequest("Token déjà invalidé");
        }
        // Récupère le temps restant avant expiration du token
        long expiration = jwtUtil.getExpirationTime(token);
        tokenCacheService.blacklistToken(token, expiration);
        log.info("Déconnexion réussie, token blacklisté dans Redis");
    }
}