package com.febin.auth.controller;

import com.febin.auth.dto.LoginRequest;
import com.febin.auth.dto.SignupRequest;
import com.febin.auth.dto.UserResponse;
import com.febin.auth.entity.Role;
import com.febin.auth.entity.User;
import com.febin.auth.service.AuthService;
import com.febin.auth.service.UserService;
import com.febin.auth.util.CookieUtil;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.stream.Collectors;

/**
 * Authentication endpoints:
 * - POST /api/auth/signup
 * - POST /api/auth/login      -> authService will set cookies ATK + RTK
 * - POST /api/auth/refresh    -> authService rotates tokens using RTK cookie
 * - POST /api/auth/logout     -> clears cookies + revokes refresh token
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserService userService;
    private final AuthService authService;
    private final CookieUtil cookieUtil;

    public AuthController(UserService userService, AuthService authService, CookieUtil cookieUtil) {
        this.userService = userService;
        this.authService = authService;
        this.cookieUtil = cookieUtil;
    }

    @PostMapping("/signup")
    public ResponseEntity<UserResponse> signup(@Valid @RequestBody SignupRequest req) {
        User user = userService.registerUser(req.getUsername(), req.getEmail(), req.getPassword());
        UserResponse resp = new UserResponse();
        resp.setId(user.getId());
        resp.setUsername(user.getUsername());
        resp.setEmail(user.getEmail());
        resp.setRoles(user.getRoles().stream().map(Role::getName).collect(Collectors.toSet()));
        return ResponseEntity.ok(resp);
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest req, HttpServletResponse response) throws Exception {
        // AuthService.login handles authentication, token creation and cookie setting.
        authService.login(req, response);

        // Return a minimal response; tokens are in HttpOnly cookies.
        return ResponseEntity.ok(java.util.Map.of("message", "Logged in"));
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(HttpServletRequest request, HttpServletResponse response) {
        // Read refresh token from cookie
        String refreshTokenValue = cookieUtil.getCookie(request, CookieUtil.REFRESH_TOKEN_COOKIE)
                .map(Cookie::getValue)
                .orElseThrow(() -> new RuntimeException("Refresh token not found"));

        // AuthService.refresh rotates tokens and sets cookies
        authService.refresh(refreshTokenValue, response);

        return ResponseEntity.ok(java.util.Map.of("message", "Tokens refreshed"));
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletRequest request, HttpServletResponse response) {
        // Get refresh token from cookie and pass to AuthService.logout
        String refreshTokenValue = cookieUtil.getCookie(request, CookieUtil.REFRESH_TOKEN_COOKIE)
                .map(Cookie::getValue)
                .orElse(null);

        if (refreshTokenValue != null) {
            authService.logout(refreshTokenValue, response);
        } else {
            // Even if no refresh token, ensure cookies are cleared on client
            cookieUtil.deleteCookie(response, CookieUtil.ACCESS_TOKEN_COOKIE, "Lax");
            cookieUtil.deleteCookie(response, CookieUtil.REFRESH_TOKEN_COOKIE, "Lax");
        }

        return ResponseEntity.ok(java.util.Map.of("message", "Logged out"));
    }
}
