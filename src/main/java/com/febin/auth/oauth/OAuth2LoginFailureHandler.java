package com.febin.auth.oauth;

import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

@Component
public class OAuth2LoginFailureHandler extends SimpleUrlAuthenticationFailureHandler {

    private final String redirectUrl;

    public OAuth2LoginFailureHandler(org.springframework.core.env.Environment env) {
        this.redirectUrl = env.getProperty("app.oauth2.failure-redirect", "/login?error");
    }

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
                                        org.springframework.security.core.AuthenticationException exception)
            throws IOException, ServletException {
        // Optionally log exception
        response.sendRedirect(redirectUrl);
    }
}
