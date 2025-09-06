package com.febin.auth.oauth;

import com.febin.auth.entity.User;
import com.febin.auth.service.AuthService;
import com.febin.auth.service.UserService;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Optional;

@Component
public class OAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {

    private final UserService userService;
    private final AuthService authService;
    private final String redirectUrl;

    public OAuth2LoginSuccessHandler(UserService userService, AuthService authService,
                                     org.springframework.core.env.Environment env) {
        this.userService = userService;
        this.authService = authService;
        this.redirectUrl = env.getProperty("app.oauth2.success-redirect", "/");
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        if (!(authentication instanceof OAuth2AuthenticationToken)) {
            response.sendRedirect(redirectUrl);
            return;
        }

        OAuth2AuthenticationToken oauthToken = (OAuth2AuthenticationToken) authentication;
        // Extract principal's attributes to find the linked local user
        org.springframework.security.oauth2.core.user.OAuth2User principal =
                (org.springframework.security.oauth2.core.user.OAuth2User) oauthToken.getPrincipal();

        // Prefer to find local user by email attribute (or use registrationId + id lookup via user_providers)
        String email = (String) principal.getAttributes().get("email");
        Optional<User> userOpt = email == null ? Optional.empty() : userService.findByUsernameOrEmail(email);

        // fallback: try to find by provider+providerId
        if (userOpt.isEmpty()) {
            String provider = oauthToken.getAuthorizedClientRegistrationId();
            Object providerId = principal.getAttribute("sub");
            if (providerId == null) providerId = principal.getAttribute("id");
            if (providerId != null) {
                // convert provider+providerId to enum and query userProvider repository via userService
                com.febin.auth.entity.OAuthProvider provEnum = provider.equalsIgnoreCase("github")
                        ? com.febin.auth.entity.OAuthProvider.GITHUB
                        : com.febin.auth.entity.OAuthProvider.GOOGLE;
                // userService doesn't expose find by provider, but registerOrUpdateOAuthUser will return user if exists;
                userOpt = Optional.of(userService.registerOrUpdateOAuthUser(provEnum, String.valueOf(providerId), email, (String) principal.getAttribute("name")));
            }
        }

        // If user still missing, we assume CustomOAuth2UserService already created the user in registerOrUpdateOAuthUser.
        // Try again
        if (userOpt.isEmpty() && email != null) {
            userOpt = userService.findByUsernameOrEmail(email);
        }

        if (userOpt.isPresent()) {
            User user = userOpt.get();
            // create tokens and set cookies
            authService.processOAuthPostLogin(user, response);
        }

        // Redirect to frontend
        response.sendRedirect(redirectUrl);
    }
}
