package com.jomariabejo.connectly_api.dto;


import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(description = "Login credentials. Authentication is by email, not username.")
public class LoginUserDto {

    @NotBlank(message = "Email is mandatory")
    @Email(message = "Email should be valid")
    @Schema(example = "test@gmail.com", requiredMode = Schema.RequiredMode.REQUIRED)
    private String email;

    @NotBlank(message = "Password is mandatory")
    @Schema(example = "admin123", requiredMode = Schema.RequiredMode.REQUIRED)
    private String password;

}
