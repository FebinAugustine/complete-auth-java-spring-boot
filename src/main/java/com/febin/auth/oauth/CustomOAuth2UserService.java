package com.febin.auth.oauth;

import com.febin.auth.entity.OAuthProvider;
import com.febin.auth.entity.User;
import com.febin.auth.service.UserService;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class CustomOAuth2UserService implements OAuth2UserService<OAuth2UserRequest, OAuth2User>{

    private final UserService userService;
    private final DefaultOAuth2UserService delegate = new DefaultOAuth2UserService();
    private final RestTemplate restTemplate = new RestTemplate();

    public CustomOAuth2UserService(UserService userService) {
        this.userService = userService;
    }

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oauth2User = delegate.loadUser(userRequest);
        String registrationId = userRequest.getClientRegistration().getRegistrationId(); // "google" or "github"
        Map<String, Object> attributes = oauth2User.getAttributes();

        String providerId = null;
        String email = null;
        String name = null;

        if ("google".equalsIgnoreCase(registrationId)) {
            providerId = (String) attributes.get("sub");
            email = (String) attributes.get("email");
            name = (String) attributes.get("name");
        } else if ("github".equalsIgnoreCase(registrationId)) {
            Object idObj = attributes.get("id");
            providerId = idObj == null ? null : String.valueOf(idObj);
            email = (String) attributes.get("email");
            name = (String) attributes.get("name");

            if (email == null || email.isBlank()) {
                // call GitHub API /user/emails for verified primary email
                String token = userRequest.getAccessToken().getTokenValue();
                try {
                    HttpHeaders headers = new HttpHeaders();
                    headers.setBearerAuth(token);
                    headers.setAccept(java.util.List.of(MediaType.APPLICATION_JSON));
                    HttpEntity<Void> entity = new HttpEntity<>(headers);
                    ResponseEntity<Object[]> resp = restTemplate.exchange("https://api.github.com/user/emails",
                            HttpMethod.GET, entity, Object[].class);
                    if (resp.getStatusCode().is2xxSuccessful() && resp.getBody() != null) {
                        for (Object item : resp.getBody()) {
                            if (item instanceof Map) {
                                @SuppressWarnings("unchecked")
                                Map<String, Object> m = (Map<String, Object>) item;
                                Boolean verified = Boolean.valueOf(String.valueOf(m.getOrDefault("verified", false)));
                                Boolean primary = Boolean.valueOf(String.valueOf(m.getOrDefault("primary", false)));
                                if (Boolean.TRUE.equals(verified) && Boolean.TRUE.equals(primary)) {
                                    email = (String) m.get("email");
                                    break;
                                }
                            }
                        }
                        if ((email == null || email.isBlank()) && resp.getBody().length > 0) {
                            for (Object item : resp.getBody()) {
                                if (item instanceof Map) {
                                    @SuppressWarnings("unchecked")
                                    Map<String, Object> m = (Map<String, Object>) item;
                                    Boolean verified = Boolean.valueOf(String.valueOf(m.getOrDefault("verified", false)));
                                    if (Boolean.TRUE.equals(verified)) {
                                        email = (String) m.get("email");
                                        break;
                                    }
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    // log in real app; continue with fallback
                }
            }

            if (email == null && attributes.get("login") != null) {
                email = attributes.get("login") + "@users.noreply.github.com";
            }
        } else {
            Object idObj = attributes.get(userRequest.getClientRegistration().getProviderDetails()
                    .getUserInfoEndpoint().getUserNameAttributeName());
            providerId = idObj == null ? UUID.randomUUID().toString() : idObj.toString();
            if (attributes.containsKey("email")) email = (String) attributes.get("email");
            if (attributes.containsKey("name")) name = (String) attributes.get("name");
        }

        OAuthProvider providerEnum = OAuthProvider.LOCAL;
        if ("google".equalsIgnoreCase(registrationId)) providerEnum = OAuthProvider.GOOGLE;
        else if ("github".equalsIgnoreCase(registrationId)) providerEnum = OAuthProvider.GITHUB;

        // If an authenticated user exists (linking flow), attach provider to that user
        Authentication currentAuth = SecurityContextHolder.getContext().getAuthentication();
        if (currentAuth != null && currentAuth.isAuthenticated() && !"anonymousUser".equals(currentAuth.getPrincipal())) {
            String principalName = currentAuth.getName();
            Optional<User> cur = userService.findByUsernameOrEmail(principalName);
            if (cur.isPresent()) {
                userService.addProviderToUser(cur.get(), providerEnum, providerId);
                return oauth2User;
            }
        }

        // Else register or link by email (if verified)
        userService.registerOrUpdateOAuthUser(providerEnum, providerId, email, name);

        return oauth2User;
    }
}
