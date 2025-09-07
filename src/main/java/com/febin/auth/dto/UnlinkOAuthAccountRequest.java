package com.febin.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class UnlinkOAuthAccountRequest {

    @NotBlank(message = "Provider cannot be blank")
    private String provider;
}
