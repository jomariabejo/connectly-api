-- ==========================================================
-- V1: Core schema required before V2+ migrations
-- (role, users, social posts, verification / password-reset tokens)
-- ==========================================================

CREATE TABLE role
(
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(50) UNIQUE NOT NULL,
    description TEXT
);

INSERT INTO role (name, description)
VALUES ('ADMIN', 'Administrator with full access to all features'),
       ('USER', 'Regular user with restricted access');

CREATE TABLE app_user
(
    id                        BIGSERIAL PRIMARY KEY,
    username                  VARCHAR(255) UNIQUE NOT NULL,
    password                  VARCHAR(255)        NOT NULL,
    email                     VARCHAR(255) UNIQUE NOT NULL,
    enabled                   BOOLEAN   DEFAULT FALSE,
    verificationtoken         VARCHAR(255) UNIQUE,
    created_at                TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at                TIMESTAMP,
    deleted_at                TIMESTAMP,
    is_active                 BOOLEAN   DEFAULT TRUE,
    scheduled_deletion_at     TIMESTAMP,
    auto_reactivation_enabled BOOLEAN   DEFAULT TRUE,
    first_name                VARCHAR(255),
    last_name                 VARCHAR(255),
    account_private           BOOLEAN   DEFAULT FALSE NOT NULL,
    expiry_date               TIMESTAMP
);

CREATE INDEX idx_user_username ON app_user (username);
CREATE INDEX idx_user_email ON app_user (email);
CREATE INDEX idx_user_deleted_at ON app_user (deleted_at);
CREATE INDEX idx_user_scheduled_deletion_at ON app_user (scheduled_deletion_at);

CREATE TABLE user_roles
(
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    PRIMARY KEY (user_id, role_id),
    FOREIGN KEY (user_id) REFERENCES app_user (id) ON DELETE CASCADE,
    FOREIGN KEY (role_id) REFERENCES role (id) ON DELETE CASCADE
);

CREATE TABLE post
(
    id         BIGSERIAL PRIMARY KEY,
    title      VARCHAR(255) NOT NULL,
    content    TEXT,
    post_type  VARCHAR(10) CHECK (post_type IN ('text', 'image', 'video')),
    metadata   JSONB       DEFAULT '{}'::jsonb,
    created_by BIGINT       NOT NULL,
    created_at TIMESTAMP   DEFAULT CURRENT_TIMESTAMP,
    privacy    VARCHAR(10) DEFAULT 'public' CHECK (privacy IN ('public', 'private')),
    FOREIGN KEY (created_by) REFERENCES app_user (id)
);

CREATE TABLE comment
(
    id         BIGSERIAL PRIMARY KEY,
    text       TEXT   NOT NULL,
    post_id    BIGINT NOT NULL,
    user_id    BIGINT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (post_id) REFERENCES post (id),
    FOREIGN KEY (user_id) REFERENCES app_user (id)
);

CREATE TABLE post_like
(
    id         BIGSERIAL PRIMARY KEY,
    user_id    BIGINT NOT NULL,
    post_id    BIGINT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (user_id, post_id),
    FOREIGN KEY (user_id) REFERENCES app_user (id),
    FOREIGN KEY (post_id) REFERENCES post (id)
);

CREATE TABLE verification_token
(
    id          BIGSERIAL PRIMARY KEY,
    token       VARCHAR(255) UNIQUE NOT NULL,
    user_id     BIGINT              NOT NULL UNIQUE,
    expiry_date TIMESTAMP           NOT NULL,
    FOREIGN KEY (user_id) REFERENCES app_user (id) ON DELETE CASCADE
);

CREATE TABLE password_reset_token
(
    id            BIGSERIAL PRIMARY KEY,
    token         VARCHAR(255) UNIQUE,
    otp           VARCHAR(10),
    token_type    VARCHAR(50)  NOT NULL,
    user_id       BIGINT       NOT NULL,
    created_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    expiry_date   TIMESTAMP    NOT NULL,
    is_used       BOOLEAN   DEFAULT FALSE NOT NULL,
    attempt_count INTEGER   DEFAULT 0     NOT NULL,
    FOREIGN KEY (user_id) REFERENCES app_user (id) ON DELETE CASCADE
);

CREATE INDEX idx_password_reset_token_user_id ON password_reset_token (user_id);
CREATE INDEX idx_password_reset_token_expiry_date ON password_reset_token (expiry_date);
