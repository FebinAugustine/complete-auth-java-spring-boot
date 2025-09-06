package com.febin.auth.util;

import com.febin.auth.entity.Role;
import com.febin.auth.entity.User;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class JwtUtil {

    private final Key key;
    @Getter
    private final long accessTokenValidityMs;
    @Getter
    private final long refreshTokenValidityMs;

    public JwtUtil(@Value("${jwt.secret}") String secret,
                   @Value("${jwt.access-token-validity-ms}") long accessTokenValidityMs,
                   @Value("${jwt.refresh-token-validity-ms}") long refreshTokenValidityMs) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes());
        this.accessTokenValidityMs = accessTokenValidityMs;
        this.refreshTokenValidityMs = refreshTokenValidityMs;
    }

    public String generateAccessToken(String subject, Map<String, Object> claims) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + accessTokenValidityMs);
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(subject)
                .setIssuedAt(now)
                .setExpiration(expiry)
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    public String generateRefreshToken(String subject) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + refreshTokenValidityMs);
        return Jwts.builder()
                .setSubject(subject)
                .setIssuedAt(now)
                .setExpiration(expiry)
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    public Jws<Claims> parseClaims(String token) {
        return Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token);
    }

    public boolean isTokenExpired(String token) {
        try {
            return parseClaims(token).getBody().getExpiration().before(new Date());
        } catch (JwtException e) {
            return true;
        }
    }

    /**
     * Create an access token for the given user.
     * Claims included: id, username, email, roles (String array).
     * Subject is set to the username (change to id if you prefer).
     */
    public String generateAccessToken(User user) {
        if (user == null) throw new IllegalArgumentException("user cannot be null");

        Map<String, Object> claims = new HashMap<>();
        claims.put("id", user.getId());
        claims.put("username", user.getUsername());
        claims.put("email", user.getEmail());

        if (user.getRoles() != null) {
            claims.put("roles", user.getRoles()
                    .stream()
                    .map(Role::getName)
                    .collect(Collectors.toList()));
        }

        String subject = user.getUsername();
        return generateAccessToken(subject, claims);
    }
}
