package com.febin.auth.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Setter
@Getter
@Entity
@Table(name = "user_providers",
        uniqueConstraints = {@UniqueConstraint(name = "uk_user_providers_provider_providerid", columnNames = {"provider", "provider_id"})},
        indexes = {@Index(name = "idx_user_providers_user_id", columnList = "user_id")})
public class UserProvider {

    // getters/setters
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "provider", nullable = false)
    private OAuthProvider provider;

    @Column(name = "provider_id", nullable = false, length = 255)
    private String providerId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    public UserProvider() {}

    public UserProvider(User user, OAuthProvider provider, String providerId) {
        this.user = user;
        this.provider = provider;
        this.providerId = providerId;
        this.createdAt = Instant.now();
    }

}
