package com.febin.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class ResetPasswordWithCodeRequest {

    @NotBlank(message = "Code cannot be blank")
    @Size(min = 6, max = 6, message = "Code must be 6 digits")
    private String code;

    @NotBlank(message = "New password cannot be blank")
    @Size(min = 8, message = "New password must be at least 8 characters long")
    private String newPassword;
}
