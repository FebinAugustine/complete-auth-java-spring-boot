package com.febin.auth.controller;

import com.febin.auth.dto.UserResponse;
import com.febin.auth.entity.Role;
import com.febin.auth.entity.User;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/users")
public class UserController {

    @GetMapping("/me")
    public ResponseEntity<UserResponse> getCurrentUser(Authentication authentication) {
        // Spring Security guarantees that the principal is the UserDetails object
        // that we set in the JwtAuthenticationFilter.
        User currentUser = (User) authentication.getPrincipal();

        UserResponse resp = new UserResponse();
        resp.setId(currentUser.getId());
        resp.setUsername(currentUser.getUsername());
        resp.setEmail(currentUser.getEmail());
        resp.setRoles(currentUser.getRoles().stream().map(Role::getName).collect(Collectors.toSet()));

        return ResponseEntity.ok(resp);
    }
}
