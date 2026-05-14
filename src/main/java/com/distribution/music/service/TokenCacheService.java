package com.distribution.music.service;

import com.distribution.music.entity.BlacklistedToken;
import com.distribution.music.repository.BlacklistedTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class TokenCacheService {

    private final RedisTemplate<String, String> redisTemplate;
    private final BlacklistedTokenRepository blacklistedTokenRepository;

    private static final String BLACKLIST_PREFIX = "blacklist:";

    public void blacklistToken(String token, long expirationMs) {
        // Persistance en DB : source de vérité fiable
        blacklistedTokenRepository.save(
            BlacklistedToken.builder()
                .token(token)
                .blacklistedAt(LocalDateTime.now())
                .build()
        );

        // Cache Redis en supplément (optionnel)
        try {
            redisTemplate.opsForValue().set(BLACKLIST_PREFIX + token, "1", expirationMs, TimeUnit.MILLISECONDS);
        } catch (RedisConnectionFailureException e) {
            log.warn("Redis indisponible — token blacklisté uniquement en DB");
        }
    }

    public boolean isBlacklisted(String token) {
        // Redis en premier (plus rapide)
        try {
            return Boolean.TRUE.equals(redisTemplate.hasKey(BLACKLIST_PREFIX + token));
        } catch (RedisConnectionFailureException e) {
            log.debug("Redis indisponible — vérification du blacklist en DB");
            return blacklistedTokenRepository.existsByToken(token);
        }
    }
}
