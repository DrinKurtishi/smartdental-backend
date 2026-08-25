package com.smartdental.security.jwt;

import com.smartdental.config.JwtProperties;
import com.smartdental.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/** Issues and validates HS256 access/refresh tokens for local + OAuth2 sessions. */
@Slf4j
@Service
@RequiredArgsConstructor
public class JwtService {

    private final JwtProperties jwtProperties;

    /**
     * Builds the signing key once at startup so a missing/unresolved JWT_SECRET (or one too
     * short for HS256) fails the Spring context immediately, instead of on the first login.
     */
    @PostConstruct
    void validateSigningKey() {
        signingKey();
    }

    private Key signingKey() {
        return Keys.hmacShaKeyFor(jwtProperties.secret().getBytes(StandardCharsets.UTF_8));
    }

    public String generateAccessToken(User user) {
        List<String> roles = user.getRoles().stream().map(Enum::name).collect(Collectors.toList());
        Date now = new Date();
        Date expiry = new Date(now.getTime() + jwtProperties.accessTokenExpirationMs());
        return Jwts.builder()
                .issuer(jwtProperties.issuer())
                .subject(user.getId().toString())
                .claim("email", user.getEmail())
                .claim("roles", roles)
                .claim("name", user.getFullName())
                .issuedAt(now)
                .expiration(expiry)
                .signWith(signingKey())
                .compact();
    }

    public String generateRefreshToken(User user) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + jwtProperties.refreshTokenExpirationMs());
        return Jwts.builder()
                .issuer(jwtProperties.issuer())
                .subject(user.getId().toString())
                .claim("type", "refresh")
                .issuedAt(now)
                .expiration(expiry)
                .signWith(signingKey())
                .compact();
    }

    public Claims parseClaims(String token) {
        return Jwts.parser().verifyWith((javax.crypto.SecretKey) signingKey()).build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public boolean isTokenValid(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (ExpiredJwtException e) {
            log.debug("JWT expired: {}", e.getMessage());
            return false;
        } catch (JwtException | IllegalArgumentException e) {
            log.debug("JWT invalid: {}", e.getMessage());
            return false;
        }
    }

    public UUID extractUserId(String token) {
        return UUID.fromString(parseClaims(token).getSubject());
    }

    public String extractEmail(String token) {
        return parseClaims(token).get("email", String.class);
    }
}
