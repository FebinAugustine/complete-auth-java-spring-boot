package com.febin.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class LinkOAuthAccountRequest {

    @NotBlank(message = "Provider cannot be blank")
    private String provider;

    @NotBlank(message = "Authorization code cannot be blank")
    private String code;
}
