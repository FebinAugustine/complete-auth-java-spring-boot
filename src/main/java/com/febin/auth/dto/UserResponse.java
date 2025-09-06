package com.febin.auth.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Set;

@Setter
@Getter
public class UserResponse {
    // === Getters / Setters ===
    private Long id;
    private String username;
    private String email;
    private Set<String> roles;
    private List<ProviderInfo> providers;

    // === Nested DTO for linked providers ===
    @Setter
    @Getter
    public static class ProviderInfo {
        private String provider;     // e.g., GOOGLE, GITHUB
        private String providerId;   // e.g., GitHub numeric id or Google sub
        private String connectedAt;  // ISO timestamp

        public ProviderInfo() {}

        public ProviderInfo(String provider, String providerId, String connectedAt) {
            this.provider = provider;
            this.providerId = providerId;
            this.connectedAt = connectedAt;
        }

    }

}