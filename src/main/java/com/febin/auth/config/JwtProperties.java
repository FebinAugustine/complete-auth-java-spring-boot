package com.febin.auth.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {

    /**
     * Secret key used to sign JWT tokens.
     * Must be at least 64 chars for HMAC algorithms.
     */
    private String secret = "change_me_default_secret_please_replace";

    /**
     * Access token validity in milliseconds.
     * Default: 15 minutes.
     */
    private long accessTokenValidityMs = 900_000;

    /**
     * Refresh token validity in milliseconds.
     * Default: 7 days.
     */
    private long refreshTokenValidityMs = 604_800_000;

    // Getters and setters
    public String getSecret() {
        return secret;
    }
    public void setSecret(String secret) {
        this.secret = secret;
    }

    public long getAccessTokenValidityMs() {
        return accessTokenValidityMs;
    }
    public void setAccessTokenValidityMs(long accessTokenValidityMs) {
        this.accessTokenValidityMs = accessTokenValidityMs;
    }

    public long getRefreshTokenValidityMs() {
        return refreshTokenValidityMs;
    }
    public void setRefreshTokenValidityMs(long refreshTokenValidityMs) {
        this.refreshTokenValidityMs = refreshTokenValidityMs;
    }
}