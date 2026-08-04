package com.jomariabejo.connectly_api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Completes a password reset. Supply either token (email link flow) or otp (code flow), not both.")
public class ResetPasswordRequest {
    @Schema(description = "Token from the reset email; leave null when using an OTP", example = "your-reset-token-here")
    private String token;

    @Schema(description = "One-time code from the reset email; leave null when using a token", example = "123456")
    private String otp;

    @Schema(description = "New password, at least security.password.validation.min-length characters", example = "NewSecurePassword123!")
    private String newPassword;
}
