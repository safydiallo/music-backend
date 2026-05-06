package com.distribution.music.service;

import org.springframework.stereotype.Service;

import com.distribution.music.entity.UpdateProfileRequest;
import com.distribution.music.entity.User;
import com.distribution.music.entity.UserProfileResponse;
import com.distribution.music.exception.ApiException;
import com.distribution.music.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    public UserProfileResponse getProfile(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> ApiException.notFound("Utilisateur non trouvé"));

        return toResponse(user);
    }

    public UserProfileResponse updateProfile(String email, UpdateProfileRequest request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> ApiException.notFound("Utilisateur non trouvé"));

        if (request.getFullName() != null) user.setFullName(request.getFullName());
        if (request.getNomArtiste() != null) user.setNomArtiste(request.getNomArtiste());
        if (request.getPays() != null) user.setPays(request.getPays());
        if (request.getGenreMusical() != null) user.setGenreMusical(request.getGenreMusical());
        if (request.getBiographie() != null) user.setBiographie(request.getBiographie());
        if (request.getPhotoUrl() != null) user.setPhotoUrl(request.getPhotoUrl());
        if (request.getInstagram() != null) user.setInstagram(request.getInstagram());
        if (request.getSpotifyUrl() != null) user.setSpotifyUrl(request.getSpotifyUrl());
        if (request.getYoutubeUrl() != null) user.setYoutubeUrl(request.getYoutubeUrl());
        if (request.getFacebookUrl() != null) user.setFacebookUrl(request.getFacebookUrl());
        if (request.getTiktokUrl() != null) user.setTiktokUrl(request.getTiktokUrl());

        userRepository.save(user);
        log.info("Profil mis à jour : {}", email);
        return toResponse(user);
    }

    public void deleteAccount(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> ApiException.notFound("Utilisateur non trouvé"));
        userRepository.delete(user);
        log.info("Compte supprimé : {}", email);
    }

    private UserProfileResponse toResponse(User user) {
        return new UserProfileResponse(
                user.getFullName(),
                user.getNomArtiste(),
                user.getEmail(),
                user.getPays(),
                user.getGenreMusical(),
                user.getBiographie(),
                user.getPhotoUrl(),
                user.getInstagram(),
                user.getSpotifyUrl(),
                user.getYoutubeUrl(),
                user.getFacebookUrl(),
                user.getTiktokUrl(),
                user.getRole().name(),
                user.getCreatedAt()
        );
    }
}
