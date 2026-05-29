package com.safeher.ratingservice.security.filter;

import com.safeher.ratingservice.security.JwtUtils;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@Component @RequiredArgsConstructor @Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtils jwtUtils;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain chain) throws ServletException, IOException {
        try {
            String header = request.getHeader("Authorization");
            if (StringUtils.hasText(header) && header.startsWith("Bearer ")) {
                String token = header.substring(7).trim();
                if (jwtUtils.isValid(token)) {
                    UUID   userId    = jwtUtils.extractUserId(token);
                    String role      = jwtUtils.extractRole(token);
                    String authority = (role != null) ? "ROLE_" + role : "ROLE_USER";

                    var auth = new UsernamePasswordAuthenticationToken(
                            jwtUtils.extractUsername(token),
                            userId,
                            List.of(new SimpleGrantedAuthority(authority)));
                    auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(auth);
                }
            }
        } catch (Exception ex) {
            // Malformed / unexpected token — leave SecurityContext unauthenticated.
            // Spring Security will reject the request at the authorisation layer (401/403).
            log.warn("JWT filter error — proceeding unauthenticated: {}", ex.getMessage());
        }
        chain.doFilter(request, response);
    }
}
