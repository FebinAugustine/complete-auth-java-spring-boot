package com.febin.auth.controller;

import com.febin.auth.dto.LinkOAuthAccountRequest;
import com.febin.auth.dto.LinkedAccountResponse;
import com.febin.auth.dto.PasswordResetRequest;
import com.febin.auth.dto.UnlinkOAuthAccountRequest;
import com.febin.auth.dto.UserResponse;
import com.febin.auth.entity.Role;
import com.febin.auth.entity.User;
import com.febin.auth.entity.UserProvider;
import com.febin.auth.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated() || !(authentication.getPrincipal() instanceof User)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "No user is currently logged in or session is invalid."));
        }

        User currentUser = (User) authentication.getPrincipal();

        UserResponse resp = new UserResponse();
        resp.setId(currentUser.getId());
        resp.setUsername(currentUser.getUsername());
        resp.setEmail(currentUser.getEmail());
        resp.setRoles(currentUser.getRoles().stream().map(Role::getName).collect(Collectors.toSet()));

        return ResponseEntity.ok(resp);
    }

    @DeleteMapping("/me")
    public ResponseEntity<?> deleteSelf(Authentication authentication) {
        User currentUser = (User) authentication.getPrincipal();
        userService.deleteUserAccount(currentUser.getId(), currentUser);
        return ResponseEntity.ok(Map.of("message", "Your account has been successfully deleted."));
    }

    @GetMapping("/me/linked-accounts")
    public ResponseEntity<?> getLinkedAccounts(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated() || !(authentication.getPrincipal() instanceof User)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "No user is currently logged in or session is invalid."));
        }

        User currentUser = (User) authentication.getPrincipal();
        List<UserProvider> userProviders = userService.getUserProviders(currentUser);
        List<LinkedAccountResponse> linkedAccounts = userProviders.stream()
                .map(provider -> new LinkedAccountResponse(provider.getProvider().name()))
                .collect(Collectors.toList());

        return ResponseEntity.ok(linkedAccounts);
    }

    @PostMapping("/me/password")
    public ResponseEntity<?> resetPassword(Authentication authentication, @Valid @RequestBody PasswordResetRequest passwordResetRequest) {
        User currentUser = (User) authentication.getPrincipal();
        userService.resetPassword(currentUser, passwordResetRequest.getCurrentPassword(), passwordResetRequest.getNewPassword());
        return ResponseEntity.ok(Map.of("message", "Password has been reset successfully"));
    }

    @PostMapping("/me/link-oauth")
    public ResponseEntity<?> linkOAuthAccount(Authentication authentication, @Valid @RequestBody LinkOAuthAccountRequest linkRequest) {
        User currentUser = (User) authentication.getPrincipal();
        userService.linkOAuthAccount(currentUser, linkRequest.getProvider(), linkRequest.getCode());
        return ResponseEntity.ok(Map.of("message", "Account successfully linked to " + linkRequest.getProvider()));
    }

    @PostMapping("/me/unlink-oauth")
    public ResponseEntity<?> unlinkOAuthAccount(Authentication authentication, @Valid @RequestBody UnlinkOAuthAccountRequest unlinkRequest) {
        User currentUser = (User) authentication.getPrincipal();
        userService.unlinkOAuthAccount(currentUser, unlinkRequest.getProvider());
        return ResponseEntity.ok(Map.of("message", "Account successfully unlinked from " + unlinkRequest.getProvider()));
    }
}
