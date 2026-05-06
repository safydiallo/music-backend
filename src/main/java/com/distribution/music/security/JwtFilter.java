package com.distribution.music.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.distribution.music.service.TokenCacheService;

import java.io.IOException;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final TokenCacheService tokenCacheService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain)
            throws ServletException, IOException {

        String header = request.getHeader("Authorization");

        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7);

            // Vérifie si le token est blacklisté dans Redis
            if (tokenCacheService.isBlacklisted(token)) {
                log.warn("Token blacklisté utilisé sur {}", request.getRequestURI());
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType("application/json");
                response.getWriter().write("{\"message\": \"Token invalidé. Veuillez vous reconnecter.\"}");
                return;
            }
            // Vérifie la validité du token et extrait les informations d'authentification
            if (jwtUtil.isValid(token)) {
                String email = jwtUtil.extractEmail(token);

                //  on utilise le vrai rôle du token
                String role = jwtUtil.extractRole(token);

                var auth = new UsernamePasswordAuthenticationToken(
                        email, null,
                        List.of(new SimpleGrantedAuthority("ROLE_" + role))
                );
                SecurityContextHolder.getContext().setAuthentication(auth);
                log.debug("Utilisateur authentifié : {} avec le rôle {}", email, role);
            } else {
                log.warn("Token JWT invalide reçu sur {}", request.getRequestURI());
            }
        }
        chain.doFilter(request, response);
    }
    
}