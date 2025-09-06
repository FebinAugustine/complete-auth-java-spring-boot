package com.febin.auth.ratelimit;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Simple in-memory token-bucket rate limiting filter.
 * <p>
 * IMPORTANT:
 * - This implementation is suitable for single-instance or local development.
 * - For production (multi-instance), replace with a distributed implementation
 *   (e.g. Bucket4j + Redis, or Redis Lua scripts).
 * <p>
 * Keying: client IP + endpoint path (so each IP has its own bucket per endpoint).
 */
@Component
public class RateLimitFilter extends OncePerRequestFilter {

    // Toggle global enable/disable
    private final boolean enabled;

    // Endpoint-specific configuration (hardcoded defaults here; you can move these to properties)
    private final Map<String, EndpointConfig> endpointConfigs = new ConcurrentHashMap<>();

    // Map of buckets keyed by (ip + ":" + path)
    private final Map<String, TokenBucket> buckets = new ConcurrentHashMap<>();

    public RateLimitFilter(
            @Value("${app.rate-limit.enabled:true}") boolean enabled,
            @Value("${app.rate-limit.endpoints./api/auth/password/strength.capacity:30}") int strengthCapacity,
            @Value("${app.rate-limit.endpoints./api/auth/password/strength.refillTokens:30}") int strengthRefillTokens,
            @Value("${app.rate-limit.endpoints./api/auth/password/strength.refillPeriodSeconds:60}") int strengthRefillPeriodSeconds,
            @Value("${app.rate-limit.endpoints./api/auth/signup.capacity:5}") int signupCapacity,
            @Value("${app.rate-limit.endpoints./api/auth/signup.refillTokens:5}") int signupRefillTokens,
            @Value("${app.rate-limit.endpoints./api/auth/signup.refillPeriodSeconds:300}") int signupRefillPeriodSeconds,
            @Value("${app.rate-limit.endpoints./api/auth/login.capacity:20}") int loginCapacity,
            @Value("${app.rate-limit.endpoints./api/auth/login.refillTokens:20}") int loginRefillTokens,
            @Value("${app.rate-limit.endpoints./api/auth/login.refillPeriodSeconds:60}") int loginRefillPeriodSeconds
    ) {
        this.enabled = enabled;

        // populate endpoint config map
        endpointConfigs.put("/api/auth/password/strength",
                new EndpointConfig(strengthCapacity, strengthRefillTokens, strengthRefillPeriodSeconds));
        endpointConfigs.put("/api/auth/signup",
                new EndpointConfig(signupCapacity, signupRefillTokens, signupRefillPeriodSeconds));
        endpointConfigs.put("/api/auth/login",
                new EndpointConfig(loginCapacity, loginRefillTokens, loginRefillPeriodSeconds));
    }

    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
        return !enabled;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest req, @NonNull HttpServletResponse res, @NonNull FilterChain chain)
            throws ServletException, IOException {

        String path = req.getRequestURI();
        EndpointConfig cfg = endpointConfigs.get(path);

        // If this path isn't rate-limited, allow through
        if (cfg == null) {
            chain.doFilter(req, res);
            return;
        }

        String ip = extractClientIp(req);
        if (!StringUtils.hasText(ip)) ip = "unknown";

        String bucketKey = ip + ":" + path;

        TokenBucket bucket = buckets.computeIfAbsent(bucketKey, k ->
                new TokenBucket(cfg.capacity, cfg.refillTokens, Duration.ofSeconds(cfg.refillPeriodSeconds))
        );

        boolean allowed = bucket.tryConsume();

        if (allowed) {
            // Add headers to inform client about remaining tokens
            res.setHeader("X-RateLimit-Remaining", String.valueOf(bucket.getAvailableTokens()));
            res.setHeader("X-RateLimit-Limit", String.valueOf(cfg.capacity));
            chain.doFilter(req, res);
        } else {
            long waitSeconds = bucket.getSecondsToRefillOne();
            if (waitSeconds <= 0) waitSeconds = 1;
            res.setStatus(429); // Too Many Requests
            res.setHeader("Retry-After", String.valueOf(waitSeconds));
            res.setHeader("X-RateLimit-Retry-After-Seconds", String.valueOf(waitSeconds));
            res.setContentType("application/json");
            String body = String.format("{\"error\":\"Too many requests\",\"retryAfterSeconds\":%d}", waitSeconds);
            res.getWriter().write(body);
        }
    }

    private String extractClientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(xff)) {
            return xff.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    // ------------ Helper classes ------------

    private record EndpointConfig(int capacity, int refillTokens, int refillPeriodSeconds) {
    }

    /**
     * Very small token-bucket implementation.
     * - capacity: max tokens
     * - refillTokens: tokens added each refillPeriod
     * - refillPeriod: interval for refillTokens
     * <p>
     * This implementation is intentionally simple and synchronized per bucket.
     * For heavy load / multi-instance, replace with a distributed bucket (Redis or Bucket4j).
     */
    private static class TokenBucket {
        private final int capacity;
        private final int refillTokens;
        private final Duration refillPeriod;

        private double availableTokens;
        private Instant lastRefill;

        TokenBucket(int capacity, int refillTokens, Duration refillPeriod) {
            this.capacity = Math.max(1, capacity);
            this.refillTokens = Math.max(0, refillTokens);
            this.refillPeriod = refillPeriod != null ? refillPeriod : Duration.ofSeconds(60);
            this.availableTokens = this.capacity;
            this.lastRefill = Instant.now();
        }

        synchronized boolean tryConsume() {
            refillIfNeeded();
            if (availableTokens >= 1.0) {
                availableTokens -= 1.0;
                return true;
            }
            return false;
        }

        synchronized void refillIfNeeded() {
            Instant now = Instant.now();
            long periodsPassed = Duration.between(lastRefill, now).dividedBy(refillPeriod);
            if (periodsPassed <= 0) return;
            double tokensToAdd = periodsPassed * (double) refillTokens;
            availableTokens = Math.min(capacity, availableTokens + tokensToAdd);
            // move lastRefill forward by the consumed periods
            lastRefill = lastRefill.plus(refillPeriod.multipliedBy(periodsPassed));
        }

        synchronized long getSecondsToRefillOne() {
            refillIfNeeded();
            if (availableTokens >= 1.0) return 0;
            Instant nextRefill = lastRefill.plus(refillPeriod);
            long secs = Duration.between(Instant.now(), nextRefill).getSeconds();
            return Math.max(1, secs);
        }

        synchronized int getAvailableTokens() {
            refillIfNeeded();
            return (int) Math.floor(availableTokens);
        }
    }
}
