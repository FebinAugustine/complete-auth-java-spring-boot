package com.febin.auth.controller;

import com.febin.auth.dto.UserResponse;
import com.febin.auth.entity.Role;
import com.febin.auth.entity.User;
import com.febin.auth.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.stream.Collectors;

/**
 * Simple user endpoints.
 * - GET /api/user/me  -> returns current user's profile (from SecurityContext)
 */
@RestController
@RequestMapping("/api/user")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/me")
    public ResponseEntity<UserResponse> me() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null) {
            return ResponseEntity.status(401).build();
        }

        User user = userService.findByUsernameOrEmail(auth.getName()).orElseThrow(() -> new RuntimeException("User not found"));

        UserResponse resp = new UserResponse();
        resp.setId(user.getId());
        resp.setUsername(user.getUsername());
        resp.setEmail(user.getEmail());
        resp.setRoles(user.getRoles().stream().map(Role::getName).collect(Collectors.toSet()));
        return ResponseEntity.ok(resp);
    }
}