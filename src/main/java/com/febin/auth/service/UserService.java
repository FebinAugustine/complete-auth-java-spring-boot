package com.febin.auth.service;

import com.febin.auth.entity.*;
import com.febin.auth.exception.InvalidPasswordException;
import com.febin.auth.exception.InvalidTokenException;
import com.febin.auth.repository.RoleRepository;
import com.febin.auth.repository.UserProviderRepository;
import com.febin.auth.repository.UserRepository;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class UserService implements UserDetailsService {
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserProviderRepository userProviderRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final ClientRegistrationRepository clientRegistrationRepository;
    private final RestTemplate restTemplate;

    public UserService(UserRepository userRepository,
                       RoleRepository roleRepository,
                       UserProviderRepository userProviderRepository,
                       @Lazy PasswordEncoder passwordEncoder,
                       EmailService emailService,
                       ClientRegistrationRepository clientRegistrationRepository) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.userProviderRepository = userProviderRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
        this.clientRegistrationRepository = clientRegistrationRepository;
        this.restTemplate = new RestTemplate();
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return findByUsernameOrEmail(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with username or email: " + username));
    }

    @Transactional
    public User registerUser(String username, String email, String rawPassword) {
        if (userRepository.existsByUsername(username)) throw new RuntimeException("Username already taken");
        if (userRepository.existsByEmail(email)) throw new RuntimeException("Email already in use");

        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(rawPassword));
        user.setVerificationCode(UUID.randomUUID().toString());
        user.setAccountStatus(AccountStatus.UNVERIFIED);

        Role userRole = roleRepository.findByName("ROLE_USER").orElseThrow(() -> new RuntimeException("Role not found"));
        user.setRoles(Set.of(userRole));
        
        User savedUser = userRepository.save(user);
        
        emailService.sendAccountVerificationEmail(savedUser.getEmail(), savedUser.getUsername(), savedUser.getVerificationCode());

        return savedUser;
    }

    @Transactional
    public void verifyUser(String code) {
        User user = userRepository.findByVerificationCode(code)
                .orElseThrow(() -> new InvalidTokenException("Invalid verification code."));

        user.setAccountStatus(AccountStatus.ACTIVE);
        user.setVerificationCode(null);
        userRepository.save(user);
    }

    @Transactional
    public void resetPassword(User user, String currentPassword, String newPassword) {
        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new InvalidPasswordException("Current password does not match");
        }
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    @Transactional
    public void generateAndSendPasswordResetCode(String email) {
        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            String code = String.format("%06d", new Random().nextInt(999999));
            user.setPasswordResetCode(code);
            user.setPasswordResetCodeExpiresAt(Instant.now().plusSeconds(600)); // 10 minutes
            userRepository.save(user);
            emailService.sendPasswordResetEmail(user.getEmail(), user.getUsername(), code);
        }
    }

    @Transactional
    public void resetPasswordWithCode(String code, String newPassword) {
        User user = userRepository.findByPasswordResetCode(code)
                .orElseThrow(() -> new InvalidTokenException("Invalid password reset code"));

        if (user.getPasswordResetCodeExpiresAt().isBefore(Instant.now())) {
            throw new InvalidTokenException("Password reset code has expired");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        user.setPasswordResetCode(null);
        user.setPasswordResetCodeExpiresAt(null);
        userRepository.save(user);
    }

    public List<User> findAllUsers() {
        return userRepository.findAll();
    }

    public User findUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with id: " + id));
    }

    public List<Role> findAllRoles() {
        return roleRepository.findAll();
    }

    @Transactional
    public void disableUserAccount(Long userIdToDisable, User adminUser) {
        if (userIdToDisable.equals(adminUser.getId())) {
            throw new IllegalArgumentException("Admin cannot disable their own account.");
        }
        User userToDisable = userRepository.findById(userIdToDisable)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with id: " + userIdToDisable));

        userToDisable.setAccountStatus(AccountStatus.DISABLED);
        userRepository.save(userToDisable);
    }

    @Transactional
    public void enableUserAccount(Long userIdToEnable) {
        User userToEnable = userRepository.findById(userIdToEnable)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with id: " + userIdToEnable));

        userToEnable.setAccountStatus(AccountStatus.ACTIVE);
        userRepository.save(userToEnable);
    }

    @Transactional
    public void updateUserRoles(Long userIdToUpdate, Set<String> roleNames, User adminUser) {
        if (userIdToUpdate.equals(adminUser.getId())) {
            throw new IllegalArgumentException("Admin cannot change their own roles.");
        }

        User userToUpdate = userRepository.findById(userIdToUpdate)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with id: " + userIdToUpdate));

        Set<Role> newRoles = roleRepository.findByNameIn(roleNames);
        if (newRoles.size() != roleNames.size()) {
            throw new IllegalArgumentException("One or more invalid role names provided.");
        }

        userToUpdate.setRoles(newRoles);
        userRepository.save(userToUpdate);
    }

    @Transactional
    public void deleteUserAccount(Long idToDelete, User requestingUser) {
        // First, ensure the user to be deleted actually exists.
        User userToDelete = userRepository.findById(idToDelete)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with id: " + idToDelete));

        boolean isAdmin = requestingUser.getRoles().stream().anyMatch(role -> role.getName().equals("ROLE_ADMIN"));

        if (isAdmin) {
            if (idToDelete.equals(requestingUser.getId())) {
                throw new IllegalArgumentException("Admin cannot delete their own account.");
            }
            userRepository.delete(userToDelete);
        } else {
            if (!idToDelete.equals(requestingUser.getId())) {
                throw new org.springframework.security.access.AccessDeniedException("User can only delete their own account.");
            }
            userRepository.delete(userToDelete);
        }
    }

    @Transactional
    public void linkOAuthAccount(User user, String provider, String code) {
        ClientRegistration clientRegistration = clientRegistrationRepository.findByRegistrationId(provider.toLowerCase());
        if (clientRegistration == null) {
            throw new IllegalArgumentException("Unknown provider: " + provider);
        }

        // 1. Exchange authorization code for access token
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
        map.add("client_id", clientRegistration.getClientId());
        map.add("client_secret", clientRegistration.getClientSecret());
        map.add("code", code);
        map.add("grant_type", "authorization_code");
        map.add("redirect_uri", clientRegistration.getRedirectUri());

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(map, headers);
        Map<String, Object> tokenResponse = restTemplate.exchange(clientRegistration.getProviderDetails().getTokenUri(), HttpMethod.POST, request, new ParameterizedTypeReference<Map<String, Object>>() {}).getBody();
        String accessToken = (String) tokenResponse.get("access_token");

        // 2. Fetch user info from provider
        HttpHeaders userInfoHeaders = new HttpHeaders();
        userInfoHeaders.setBearerAuth(accessToken);
        HttpEntity<Void> userInfoRequest = new HttpEntity<>(userInfoHeaders);
        Map<String, Object> userInfo = restTemplate.exchange(clientRegistration.getProviderDetails().getUserInfoEndpoint().getUri(), HttpMethod.GET, userInfoRequest, new ParameterizedTypeReference<Map<String, Object>>() {}).getBody();

        String providerId = userInfo.get(clientRegistration.getProviderDetails().getUserInfoEndpoint().getUserNameAttributeName()).toString();
        String email = (String) userInfo.get("email");

        // 3. Security Validations
        if (!user.getEmail().equalsIgnoreCase(email)) {
            throw new IllegalArgumentException("OAuth account email does not match your account email.");
        }

        OAuthProvider oAuthProvider = OAuthProvider.valueOf(provider.toUpperCase());
        addProviderToUser(user, oAuthProvider, providerId);
    }

    @Transactional
    public void unlinkOAuthAccount(User user, String provider) {
        OAuthProvider oAuthProvider = OAuthProvider.valueOf(provider.toUpperCase());
        removeProviderFromUser(user, oAuthProvider);
    }

    public Optional<User> findByUsernameOrEmail(String usernameOrEmail) {
        Optional<User> byUsername = userRepository.findByUsername(usernameOrEmail);
        if (byUsername.isPresent()) return byUsername;
        return userRepository.findByEmail(usernameOrEmail);
    }

    public User addProviderToUser(User user, OAuthProvider provider, String providerId) {
        Optional<UserProvider> existing = userProviderRepository.findByProviderAndProviderId(provider, providerId);
        if (existing.isPresent() && !existing.get().getUser().getId().equals(user.getId())) {
            throw new RuntimeException("This " + provider + " account is already linked to another user.");
        }
        if (userProviderRepository.existsByUserAndProvider(user, provider)) {
            throw new RuntimeException("You have already linked a " + provider + " account.");
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
        user.setAccountStatus(AccountStatus.ACTIVE);

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
