package com.febin.auth.repository;

import com.febin.auth.entity.OAuthProvider;
import com.febin.auth.entity.User;
import com.febin.auth.entity.UserProvider;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserProviderRepository extends JpaRepository<UserProvider, Long> {
    Optional<UserProvider> findByProviderAndProviderId(OAuthProvider provider, String providerId);
    List<UserProvider> findByUser(User user);
    void deleteByUserAndProvider(User user, OAuthProvider provider);
    void deleteByUserAndProviderId(User user, String providerId);
}
