package com.febin.auth.controller;

import com.febin.auth.dto.RoleResponse;
import com.febin.auth.dto.UpdateUserRolesRequest;
import com.febin.auth.dto.UserResponse;
import com.febin.auth.entity.Role;
import com.febin.auth.entity.User;
import com.febin.auth.service.UserService;
import jakarta.validation.Valid;
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
                .map(this::convertToUserResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(userResponses);
    }

    @GetMapping("/users/{id}")
    public ResponseEntity<UserResponse> getUserById(@PathVariable Long id) {
        User user = userService.findUserById(id);
        return ResponseEntity.ok(convertToUserResponse(user));
    }

    @GetMapping("/roles")
    public ResponseEntity<List<RoleResponse>> getAllRoles() {
        List<Role> roles = userService.findAllRoles();
        List<RoleResponse> roleResponses = roles.stream()
                .map(role -> new RoleResponse(role.getId(), role.getName()))
                .collect(Collectors.toList());
        return ResponseEntity.ok(roleResponses);
    }

    @PutMapping("/users/{id}/roles")
    public ResponseEntity<?> updateUserRoles(@PathVariable Long id, @Valid @RequestBody UpdateUserRolesRequest request, Authentication authentication) {
        User adminUser = (User) authentication.getPrincipal();
        userService.updateUserRoles(id, request.getRoles(), adminUser);
        return ResponseEntity.ok(Map.of("message", "User roles updated successfully."));
    }

    @PostMapping("/users/{id}/disable")
    public ResponseEntity<?> disableUser(@PathVariable Long id, Authentication authentication) {
        User adminUser = (User) authentication.getPrincipal();
        userService.disableUserAccount(id, adminUser);
        return ResponseEntity.ok(Map.of("message", "User account has been disabled successfully."));
    }

    @PostMapping("/users/{id}/enable")
    public ResponseEntity<?> enableUser(@PathVariable Long id) {
        userService.enableUserAccount(id);
        return ResponseEntity.ok(Map.of("message", "User account has been enabled successfully."));
    }

    @DeleteMapping("/users/{id}")
    public ResponseEntity<?> deleteUserById(@PathVariable Long id, Authentication authentication) {
        User adminUser = (User) authentication.getPrincipal();
        userService.deleteUserAccount(id, adminUser);
        return ResponseEntity.ok(Map.of("message", "User account has been successfully deleted."));
    }

    private UserResponse convertToUserResponse(User user) {
        UserResponse resp = new UserResponse();
        resp.setId(user.getId());
        resp.setUsername(user.getUsername());
        resp.setEmail(user.getEmail());
        resp.setRoles(user.getRoles().stream().map(Role::getName).collect(Collectors.toSet()));
        return resp;
    }
}
