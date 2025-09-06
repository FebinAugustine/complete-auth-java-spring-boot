package com.febin.auth.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@ConfigurationProperties(prefix = "app.rate-limit")
public class RateLimitProperties {

    /**
     * Whether rate limiting is enabled.
     */
    private boolean enabled = true;

    /**
     * Default rate limit configuration if endpoint not specified.
     */
    private EndpointConfig defaultConfig = new EndpointConfig();

    /**
     * Per-endpoint overrides (keyed by endpoint path).
     */
    private Map<String, EndpointConfig> endpoints = new HashMap<>();

    /**
     * For login endpoint: whether to also use per-account (username/email) keying.
     */
    private boolean usePerAccountForLogin = true;

    /**
     * List of whitelisted IPs that bypass rate limiting.
     */
    private List<String> whitelistIps;

    public static class EndpointConfig {
        private long capacity = 10;
        private long refillTokens = 10;
        private long refillPeriodSeconds = 60;

        public long getCapacity() {
            return capacity;
        }
        public void setCapacity(long capacity) {
            this.capacity = capacity;
        }

        public long getRefillTokens() {
            return refillTokens;
        }
        public void setRefillTokens(long refillTokens) {
            this.refillTokens = refillTokens;
        }

        public long getRefillPeriodSeconds() {
            return refillPeriodSeconds;
        }
        public void setRefillPeriodSeconds(long refillPeriodSeconds) {
            this.refillPeriodSeconds = refillPeriodSeconds;
        }
    }

    // Getters and setters
    public boolean isEnabled() {
        return enabled;
    }
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public EndpointConfig getDefaultConfig() {
        return defaultConfig;
    }
    public void setDefaultConfig(EndpointConfig defaultConfig) {
        this.defaultConfig = defaultConfig;
    }

    public Map<String, EndpointConfig> getEndpoints() {
        return endpoints;
    }
    public void setEndpoints(Map<String, EndpointConfig> endpoints) {
        this.endpoints = endpoints;
    }

    public boolean isUsePerAccountForLogin() {
        return usePerAccountForLogin;
    }
    public void setUsePerAccountForLogin(boolean usePerAccountForLogin) {
        this.usePerAccountForLogin = usePerAccountForLogin;
    }

    public List<String> getWhitelistIps() {
        return whitelistIps;
    }
    public void setWhitelistIps(List<String> whitelistIps) {
        this.whitelistIps = whitelistIps;
    }
}
