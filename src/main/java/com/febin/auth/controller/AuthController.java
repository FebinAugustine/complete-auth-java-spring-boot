package com.febin.auth.controller;

import com.febin.auth.dto.LoginRequest;
import com.febin.auth.dto.SignupRequest;
import com.febin.auth.entity.AccountStatus;
import com.febin.auth.entity.User;
import com.febin.auth.service.AuthService;
import com.febin.auth.service.UserService;
import com.febin.auth.util.CookieUtil;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

/**
 * Authentication endpoints:
 * - POST /api/auth/signup
 * - GET  /api/auth/verify
 * - POST /api/auth/login
 * - POST /api/auth/refresh
 * - POST /api/auth/logout
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
    public ResponseEntity<?> signup(@Valid @RequestBody SignupRequest req) {
        userService.registerUser(req.getUsername(), req.getEmail(), req.getPassword());
        return ResponseEntity.ok(Map.of("message", "Signup successful. Please check your email to verify your account."));
    }

    @GetMapping("/verify")
    public ResponseEntity<?> verifyAccount(@RequestParam("code") String code) {
        userService.verifyUser(code);
        return ResponseEntity.ok(Map.of("message", "Your account has been successfully verified. You can now log in."));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest req, HttpServletResponse response) {
        try {
            authService.login(req, response);
            return ResponseEntity.ok(Map.of("message", "Logged in"));
        } catch (DisabledException e) {
            // User is disabled, check for the reason
            Optional<User> userOpt = userService.findByUsernameOrEmail(req.getUsernameOrEmail());
            if (userOpt.isPresent()) {
                User user = userOpt.get();
                if (user.getAccountStatus() == AccountStatus.UNVERIFIED) {
                    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Please verify your email before logging in."));
                } else if (user.getAccountStatus() == AccountStatus.DISABLED) {
                    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Your account has been disabled. Please contact support."));
                }
            }
            // Fallback for any other disabled state
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Account is disabled."));
        } catch (BadCredentialsException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Invalid username or password"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "An unexpected error occurred."));
        }
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(HttpServletRequest request, HttpServletResponse response) {
        String refreshTokenValue = cookieUtil.getCookie(request, CookieUtil.REFRESH_TOKEN_COOKIE)
                .map(Cookie::getValue)
                .orElseThrow(() -> new RuntimeException("Refresh token not found"));
        authService.refresh(refreshTokenValue, response);
        return ResponseEntity.ok(Map.of("message", "Tokens refreshed"));
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletRequest request, HttpServletResponse response) {
        String refreshTokenValue = cookieUtil.getCookie(request, CookieUtil.REFRESH_TOKEN_COOKIE)
                .map(Cookie::getValue)
                .orElse(null);

        if (refreshTokenValue != null) {
            authService.logout(refreshTokenValue, response);
        } else {
            cookieUtil.deleteCookie(response, CookieUtil.ACCESS_TOKEN_COOKIE, "Lax");
            cookieUtil.deleteCookie(response, CookieUtil.REFRESH_TOKEN_COOKIE, "Lax");
        }

        return ResponseEntity.ok(Map.of("message", "Logged out"));
    }
}
