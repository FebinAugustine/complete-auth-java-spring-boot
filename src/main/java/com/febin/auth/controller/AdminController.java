package com.febin.auth.controller;

import com.febin.auth.dto.UserResponse;
import com.febin.auth.entity.Role;
import com.febin.auth.entity.User;
import com.febin.auth.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final UserService userService;

    public AdminController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/users")
    public ResponseEntity<List<UserResponse>> getAllUsers() {
        List<User> users = userService.findAllUsers();
        List<UserResponse> userResponses = users.stream()
                .map(user -> {
                    UserResponse resp = new UserResponse();
                    resp.setId(user.getId());
                    resp.setUsername(user.getUsername());
                    resp.setEmail(user.getEmail());
                    resp.setRoles(user.getRoles().stream().map(Role::getName).collect(Collectors.toSet()));
                    return resp;
                })
                .collect(Collectors.toList());
        return ResponseEntity.ok(userResponses);
    }

    @PostMapping("/users/{id}/disable")
    public ResponseEntity<?> disableUser(@PathVariable Long id, Authentication authentication) {
        User adminUser = (User) authentication.getPrincipal();
        userService.disableUserAccount(id, adminUser);
        return ResponseEntity.ok(Map.of("message", "User account has been disabled successfully."));
    }
}
