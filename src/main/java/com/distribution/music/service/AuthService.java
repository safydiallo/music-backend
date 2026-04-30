package com.distribution.music.service;

import com.distribution.music.dto.*;
import com.distribution.music.entity.Role;
import com.distribution.music.entity.User;
import com.distribution.music.repository.UserRepository;
import com.distribution.music.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final EmailService emailService;

    public String register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email déjà utilisé");
        }

        String token = UUID.randomUUID().toString();

        User user = User.builder()
                .fullName(request.getFullName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(Role.ARTIST)
                .enabled(false)
                .verificationToken(token)
                .createdAt(LocalDateTime.now())
                .build();

        userRepository.save(user);
        emailService.sendVerificationEmail(user.getEmail(), token);

        return "Inscription réussie. Vérifie ton email.";
    }

    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

        if (!user.isEnabled()) {
            throw new RuntimeException("Compte non vérifié. Vérifie ton email.");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new RuntimeException("Mot de passe incorrect");
        }

        String token = jwtUtil.generateToken(user.getEmail(), user.getRole().name());
        return new AuthResponse(token, user.getRole().name(), user.getEmail(), user.getFullName());
    }

    public String verifyEmail(String token) {
        User user = userRepository.findByVerificationToken(token)
                .orElseThrow(() -> new RuntimeException("Token invalide"));

        user.setEnabled(true);
        user.setVerificationToken(null);
        userRepository.save(user);

        return "Compte vérifié avec succès !";
    }
}