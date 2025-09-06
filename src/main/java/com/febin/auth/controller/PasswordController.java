package com.febin.auth.controller;

import com.febin.auth.dto.ForgotPasswordRequest;
import com.febin.auth.dto.ResetPasswordWithCodeRequest;
import com.febin.auth.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/auth/password")
public class PasswordController {

    private final UserService userService;

    public PasswordController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/forgot")
    public ResponseEntity<?> forgotPassword(@Valid @RequestBody ForgotPasswordRequest forgotPasswordRequest) {
        userService.generateAndSendPasswordResetCode(forgotPasswordRequest.getEmail());
        // Always return a success response to prevent email enumeration attacks
        return ResponseEntity.ok(Map.of("message", "If an account with that email exists, a password reset code has been sent."));
    }

    @PostMapping("/reset")
    public ResponseEntity<?> resetPasswordWithCode(@Valid @RequestBody ResetPasswordWithCodeRequest resetRequest) {
        userService.resetPasswordWithCode(resetRequest.getCode(), resetRequest.getNewPassword());
        return ResponseEntity.ok(Map.of("message", "Password has been reset successfully."));
    }
}
