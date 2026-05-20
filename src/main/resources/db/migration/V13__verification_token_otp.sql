-- Flyway Migration V13: OTP support for email verification

ALTER TABLE verification_token
    ADD COLUMN IF NOT EXISTS otp VARCHAR(6),
    ADD COLUMN IF NOT EXISTS attempt_count INT NOT NULL DEFAULT 0;

CREATE INDEX IF NOT EXISTS idx_verification_token_otp ON verification_token(otp);
