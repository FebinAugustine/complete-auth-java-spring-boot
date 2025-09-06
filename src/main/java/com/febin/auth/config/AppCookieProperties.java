package com.febin.auth.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.cookie")
public class AppCookieProperties {

    /**
     * Whether cookies should be marked as Secure.
     * Default: true (should be true in production).
     */
    private boolean secure = true;

    /**
     * SameSite policy for cookies: Strict, Lax, None.
     * Default: Lax.
     */
    private String sameSite = "Lax";

    // Getters and setters
    public boolean isSecure() {
        return secure;
    }
    public void setSecure(boolean secure) {
        this.secure = secure;
    }

    public String getSameSite() {
        return sameSite;
    }
    public void setSameSite(String sameSite) {
        this.sameSite = sameSite;
    }
}
