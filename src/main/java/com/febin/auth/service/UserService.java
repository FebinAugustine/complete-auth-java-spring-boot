package com.febin.auth.service;

import com.febin.auth.entity.*;
import com.febin.auth.repository.RoleRepository;
import com.febin.auth.repository.UserProviderRepository;
import com.febin.auth.repository.UserRepository;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class UserService implements UserDetailsService {
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserProviderRepository userProviderRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository,
                       RoleRepository roleRepository,
                       UserProviderRepository userProviderRepository,
                       @Lazy PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.userProviderRepository = userProviderRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return findByUsernameOrEmail(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with username or email: " + username));
    }

    public User registerUser(String username, String email, String rawPassword) {
        if (userRepository.existsByUsername(username)) throw new RuntimeException("Username already taken");
        if (userRepository.existsByEmail(email)) throw new RuntimeException("Email already in use");

        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(rawPassword));

        Role userRole = roleRepository.findByName("ROLE_USER").orElseThrow(() -> new RuntimeException("Role not found"));
        user.setRoles(Set.of(userRole));
        return userRepository.save(user);
    }

    public Optional<User> findByUsernameOrEmail(String usernameOrEmail) {
        Optional<User> byUsername = userRepository.findByUsername(usernameOrEmail);
        if (byUsername.isPresent()) return byUsername;
        return userRepository.findByEmail(usernameOrEmail);
    }

    public User addProviderToUser(User user, OAuthProvider provider, String providerId) {
        Optional<UserProvider> existing = userProviderRepository.findByProviderAndProviderId(provider, providerId);
        if (existing.isPresent() && !existing.get().getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Provider already linked to another user");
        }
        UserProvider up = new UserProvider(user, provider, providerId);
        userProviderRepository.save(up);
        return user;
    }

    public void removeProviderFromUser(User user, OAuthProvider provider) {
        userProviderRepository.deleteByUserAndProvider(user, provider);
    }

    public List<UserProvider> getUserProviders(User user) {
        return userProviderRepository.findByUser(user);
    }

    public User registerOrUpdateOAuthUser(OAuthProvider provider, String providerId, String email, String name) {
        Optional<UserProvider> byProvider = userProviderRepository.findByProviderAndProviderId(provider, providerId);
        if (byProvider.isPresent()) return byProvider.get().getUser();

        if (email != null && !email.isBlank()) {
            Optional<User> byEmail = userRepository.findByEmail(email);
            if (byEmail.isPresent()) {
                User existing = byEmail.get();
                addProviderToUser(existing, provider, providerId);
                return existing;
            }
        }

        String usernameCandidate = generateUniqueUsername(email, provider, providerId);
        String randomPassword = passwordEncoder.encode(UUID.randomUUID().toString());

        User user = new User();
        user.setUsername(usernameCandidate);
        user.setEmail(email == null ? usernameCandidate + "@noemail.local" : email);
        user.setPassword(randomPassword);

        Role userRole = roleRepository.findByName("ROLE_USER").orElseThrow(() -> new RuntimeException("Role not found"));
        user.setRoles(Set.of(userRole));

        User saved = userRepository.save(user);
        userProviderRepository.save(new UserProvider(saved, provider, providerId));
        return saved;
    }

    private String generateUniqueUsername(String email, OAuthProvider provider, String providerId) {
        String base;
        if (email != null && email.contains("@")) base = email.split("@")[0];
        else base = provider.name().toLowerCase() + "_" + providerId;
        String cand = base;
        int suffix = 0;
        while (userRepository.existsByUsername(cand)) {
            suffix++;
            cand = base + "_" + suffix;
        }
        return cand;
    }
}
