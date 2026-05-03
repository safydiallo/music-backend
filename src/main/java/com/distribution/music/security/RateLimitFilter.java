package com.distribution.music.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Component
public class RateLimitFilter extends OncePerRequestFilter {

    // Max 10 tentatives de login par IP par minute
    private static final int MAX_ATTEMPTS = 10;
    private static final long WINDOW_MS = 60_000;

    private final ConcurrentHashMap<String, AtomicInteger> attempts = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Long> windowStart = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain)
            throws ServletException, IOException {

        if (!request.getRequestURI().equals("/api/auth/login")) {
            chain.doFilter(request, response);
            return;
        }

        String ip = request.getRemoteAddr();
        long now = System.currentTimeMillis();

        windowStart.putIfAbsent(ip, now);
        attempts.putIfAbsent(ip, new AtomicInteger(0));

        // Réinitialiser la fenêtre si la minute est écoulée
        if (now - windowStart.get(ip) > WINDOW_MS) {
            windowStart.put(ip, now);
            attempts.get(ip).set(0);
        }

        int count = attempts.get(ip).incrementAndGet();

        if (count > MAX_ATTEMPTS) {
            log.warn("Rate limit dépassé pour l'IP : {}", ip);
            response.setStatus(429);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\": \"Trop de tentatives. Réessaye dans 1 minute.\"}");
            return;
        }

        chain.doFilter(request, response);
    }
}