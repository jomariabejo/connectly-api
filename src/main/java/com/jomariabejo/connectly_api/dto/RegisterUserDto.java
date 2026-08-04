package com.jomariabejo.connectly_api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(description = "New account details. The account starts disabled until the emailed verification token is used.")
public class RegisterUserDto {
    @NotBlank(message = "Email is mandatory")
    @Email(message = "Email should be valid")
    @Schema(description = "Must be unique across accounts", example = "test@gmail.com", requiredMode = Schema.RequiredMode.REQUIRED)
    private String email;

    @NotBlank(message = "Password is mandatory")
    @Schema(description = "Stored BCrypt-hashed", example = "admin123", requiredMode = Schema.RequiredMode.REQUIRED)
    private String password;

    @NotBlank(message = "Username is mandatory")
    @Schema(description = "Must be unique across accounts", example = "helloworld", requiredMode = Schema.RequiredMode.REQUIRED)
    private String username;
}