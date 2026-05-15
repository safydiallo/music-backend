package com.distribution.music.service;

import com.distribution.music.dto.*;
import com.distribution.music.entity.ChangePasswordRequest;
import com.distribution.music.entity.Role;
import com.distribution.music.entity.User;
import com.distribution.music.exception.ApiException;
import com.distribution.music.repository.UserRepository;
import com.distribution.music.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
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
        return new AuthResponse(token, user.getRole().name(), user.getEmail(), user.getFullName(), user.isMustChangePassword());
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
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> ApiException.notFound("Email non trouvé"));

        // Génère un mot de passe temporaire
        String temporaryPassword = generateTemporaryPassword();

        // Remplace l'ancien mot de passe par le nouveau hashé
        user.setPassword(passwordEncoder.encode(temporaryPassword));

        // Marque que l'utilisateur doit changer son mot de passe
        user.setMustChangePassword(true);

        // Supprime les anciens tokens de reset s'il y en a
        user.setResetPasswordToken(null);
        user.setResetPasswordTokenExpiresAt(null);

        userRepository.save(user);

        // Envoie l'email avec le mot de passe temporaire
        emailService.sendTemporaryPasswordEmail(user.getEmail(), temporaryPassword);

        log.info("Mot de passe temporaire envoyé à : {}", user.getEmail());
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


    // Méthode utilitaire pour générer un mot de passe temporaire sécurisé
    private String generateTemporaryPassword() {
        String upper = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        String lower = "abcdefghijklmnopqrstuvwxyz";
        String digits = "0123456789";
        String special = "@#$%!&*";
        String all = upper + lower + digits + special;

        SecureRandom random = new SecureRandom();
        StringBuilder password = new StringBuilder();

        // Garantit au moins un caractère de chaque type
        password.append(upper.charAt(random.nextInt(upper.length())));
        password.append(lower.charAt(random.nextInt(lower.length())));
        password.append(digits.charAt(random.nextInt(digits.length())));
        password.append(special.charAt(random.nextInt(special.length())));

        // Complète jusqu'à 10 caractères
        for (int i = 4; i < 10; i++) {
            password.append(all.charAt(random.nextInt(all.length())));
        }

        // Mélange les caractères
        List<Character> chars = new ArrayList<>();
        for (char c : password.toString().toCharArray()) chars.add(c);
        Collections.shuffle(chars, random);

        StringBuilder result = new StringBuilder();
        for (char c : chars) result.append(c);
        return result.toString();
    }

    // Nouvelle méthode pour changer le mot de passe après connexion
    public void changePassword(String email, ChangePasswordRequest request) {
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw ApiException.badRequest("Les mots de passe ne correspondent pas");
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> ApiException.notFound("Utilisateur non trouvé"));

        // Remplace le mot de passe temporaire par le nouveau
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));

        // Désactive l'obligation de changer le mot de passe
        user.setMustChangePassword(false);

        userRepository.save(user);
        log.info("Mot de passe changé avec succès pour : {}", email);
    }
}