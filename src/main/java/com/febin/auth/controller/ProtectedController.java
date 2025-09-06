package com.febin.auth.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Example protected endpoint to verify security filter behavior.
 * Requires authentication (ATK cookie) because SecurityConfig requires auth for other endpoints.
 */
@RestController
@RequestMapping("/api/secure")
public class ProtectedController {

    @GetMapping("/hello")
    public ResponseEntity<?> hello() {
        return ResponseEntity.ok(Map.of("message", "Hello — you are authenticated"));
    }
}
