package com.febin.auth.service;

import com.febin.auth.dto.LoginRequest;
import com.febin.auth.entity.RefreshToken;
import com.febin.auth.entity.User;
import com.febin.auth.repository.RefreshTokenRepository;
import com.febin.auth.util.CookieUtil;
import com.febin.auth.util.JwtUtil;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
public class AuthService {

    private final AuthenticationConfiguration authenticationConfiguration;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtUtil jwtUtil;
    private final CookieUtil cookieUtil;

    public AuthService(AuthenticationConfiguration authenticationConfiguration,
                       RefreshTokenRepository refreshTokenRepository,
                       JwtUtil jwtUtil,
                       CookieUtil cookieUtil) {
        this.authenticationConfiguration = authenticationConfiguration;
        this.refreshTokenRepository = refreshTokenRepository;
        this.jwtUtil = jwtUtil;
        this.cookieUtil = cookieUtil;
    }

    @Transactional
    public User login(LoginRequest loginRequest, HttpServletResponse response) throws Exception {
        AuthenticationManager authenticationManager = authenticationConfiguration.getAuthenticationManager();
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginRequest.getUsernameOrEmail(), loginRequest.getPassword()));
        User user = (User) authentication.getPrincipal();
        processOAuthPostLogin(user, response);
        return user;
    }

    @Transactional
    public void refresh(String refreshTokenValue, HttpServletResponse response) {
        RefreshToken refreshToken = refreshTokenRepository.findByToken(refreshTokenValue)
                .orElseThrow(() -> new RuntimeException("Refresh token not found"));

        if (refreshToken.isRevoked() || refreshToken.getExpiryDate().isBefore(Instant.now())) {
            throw new RuntimeException("Refresh token is expired or revoked");
        }

        User user = refreshToken.getUser();
        processOAuthPostLogin(user, response);
        refreshToken.setRevoked(true);
        refreshTokenRepository.save(refreshToken);
    }

    @Transactional
    public void logout(String refreshTokenValue, HttpServletResponse response) {
        revokeRefreshToken(refreshTokenValue);
        cookieUtil.deleteCookie(response, CookieUtil.ACCESS_TOKEN_COOKIE, "Lax");
        cookieUtil.deleteCookie(response, CookieUtil.REFRESH_TOKEN_COOKIE, "Lax");
    }

    /**
     * Helper used after OAuth login: create access + refresh tokens and set cookies for the response.
     */
    public void processOAuthPostLogin(User user, HttpServletResponse response) {
        String accessToken = jwtUtil.generateAccessToken(user);
        String refreshToken = createAndStoreRefreshToken(user);

        boolean secure = true; // in dev override to false via config
        String sameSite = "Lax";
        Cookie accessCookie = cookieUtil.createCookie(CookieUtil.ACCESS_TOKEN_COOKIE, accessToken,
                (int) (jwtUtil.getAccessTokenValidityMs() / 1000), true, secure, sameSite);
        Cookie refreshCookie = cookieUtil.createCookie(CookieUtil.REFRESH_TOKEN_COOKIE, refreshToken,
                (int) (jwtUtil.getRefreshTokenValidityMs() / 1000), true, secure, sameSite);

        cookieUtil.addCookie(response, accessCookie, sameSite);
        cookieUtil.addCookie(response, refreshCookie, sameSite);
    }

    private String createAndStoreRefreshToken(User user) {
        RefreshToken rt = new RefreshToken();
        rt.setToken(UUID.randomUUID().toString());
        rt.setUser(user);
        rt.setExpiryDate(Instant.now().plusMillis(jwtUtil.getRefreshTokenValidityMs()));
        rt.setRevoked(false);
        refreshTokenRepository.save(rt);
        return rt.getToken();
    }

    public void revokeRefreshToken(String token) {
        refreshTokenRepository.findByToken(token).ifPresent(rt -> {
            rt.setRevoked(true);
            refreshTokenRepository.save(rt);
        });
    }
}
