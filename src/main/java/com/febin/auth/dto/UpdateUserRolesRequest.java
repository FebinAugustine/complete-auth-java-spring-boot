package com.febin.auth.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;

import java.util.Set;

@Setter
@Getter
public class UpdateUserRolesRequest {

    @NotEmpty(message = "Roles cannot be empty")
    private Set<String> roles;
}
