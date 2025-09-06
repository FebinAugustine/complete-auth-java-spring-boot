package com.febin.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class LoginRequest {

    // === Getters / Setters ===
    @NotBlank
    private String usernameOrEmail;

    @NotBlank
    private String password;

}
