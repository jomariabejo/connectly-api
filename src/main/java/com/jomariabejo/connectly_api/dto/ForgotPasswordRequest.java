package com.jomariabejo.connectly_api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Address to send a reset link or OTP to.")
public class ForgotPasswordRequest {
    @Schema(description = "The response is identical whether or not this address is registered", example = "user@example.com")
    @NotBlank(message = "Email is required")
    @Email(message = "Malformed email address")
    private String email;
}
