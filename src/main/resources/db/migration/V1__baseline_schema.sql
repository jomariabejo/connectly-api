-- ==========================================================
-- V1 -- baseline schema
-- ==========================================================
-- Replaces the old src/main/resources/schema.sql, which spring.sql.init.mode=never meant
-- was never executed. Hibernate generated the live schema instead, and schema.sql drifted
-- out of sync with the entities.
--
-- The schema is now owned here. spring.jpa.hibernate.ddl-auto defaults to `validate`, so
-- Hibernate checks the entities against these tables and refuses to start if they disagree.
--
-- Column types and names below match what the entity mappings produce.

-- ----------------------------------------------------------
-- role
-- ----------------------------------------------------------
CREATE TABLE IF NOT EXISTS role
(
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(255) UNIQUE NOT NULL,
    description VARCHAR(255)
);

-- Seeded, not optional: registration assigns USER to every new account. Without these rows
-- accounts would be created with no authorities, which is exactly the bug this replaces.
INSERT INTO role (name, description)
VALUES ('ADMIN', 'Administrator with full access to all features'),
       ('USER', 'Regular user with restricted access')
ON CONFLICT (name) DO NOTHING;

-- ----------------------------------------------------------
-- app_user
-- ----------------------------------------------------------
CREATE TABLE IF NOT EXISTS app_user
(
    id                        BIGSERIAL PRIMARY KEY,
    username                  VARCHAR(255) UNIQUE NOT NULL,
    password                  VARCHAR(255)        NOT NULL,
    email                     VARCHAR(255) UNIQUE NOT NULL,
    first_name                VARCHAR(255),
    last_name                 VARCHAR(255),
    enabled                   BOOLEAN   DEFAULT FALSE,

    -- Nullable: cleared once the address is verified.
    verification_token        VARCHAR(255) UNIQUE,
    expiry_date               TIMESTAMP,

    created_at                TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at                TIMESTAMP,

    -- Soft delete. deleted_at IS NULL means active; every listing query filters on it.
    deleted_at                TIMESTAMP,
    is_active                 BOOLEAN   DEFAULT TRUE,
    scheduled_deletion_at     TIMESTAMP,
    auto_reactivation_enabled BOOLEAN   DEFAULT TRUE
);

CREATE INDEX IF NOT EXISTS idx_user_username ON app_user (username);
CREATE INDEX IF NOT EXISTS idx_user_email ON app_user (email);
-- The hottest of the four: every paginated query filters on deleted_at.
CREATE INDEX IF NOT EXISTS idx_user_deleted_at ON app_user (deleted_at);
CREATE INDEX IF NOT EXISTS idx_user_scheduled_deletion_at ON app_user (scheduled_deletion_at);

-- ----------------------------------------------------------
-- user_roles
-- ----------------------------------------------------------
CREATE TABLE IF NOT EXISTS user_roles
(
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    PRIMARY KEY (user_id, role_id),
    FOREIGN KEY (user_id) REFERENCES app_user (id) ON DELETE CASCADE,
    FOREIGN KEY (role_id) REFERENCES role (id) ON DELETE CASCADE
);

-- ----------------------------------------------------------
-- post
-- ----------------------------------------------------------
CREATE TABLE IF NOT EXISTS post
(
    id         BIGSERIAL PRIMARY KEY,
    title      VARCHAR(255),
    content    VARCHAR(255),
    post_type  VARCHAR(255),
    metadata   JSONB     DEFAULT '{}'::jsonb,
    created_by BIGINT,
    privacy    VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (created_by) REFERENCES app_user (id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_post_created_by ON post (created_by);

-- ----------------------------------------------------------
-- comment
-- ----------------------------------------------------------
CREATE TABLE IF NOT EXISTS comment
(
    id         BIGSERIAL PRIMARY KEY,
    text       VARCHAR(255),
    post_id    BIGINT,
    user_id    BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (post_id) REFERENCES post (id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES app_user (id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_comment_post_id ON comment (post_id);
CREATE INDEX IF NOT EXISTS idx_comment_user_id ON comment (user_id);

-- ----------------------------------------------------------
-- post_like
-- ----------------------------------------------------------
CREATE TABLE IF NOT EXISTS post_like
(
    id         BIGSERIAL PRIMARY KEY,
    user_id    BIGINT,
    post_id    BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    -- What makes POST /{postId}/likes/toggle safe: a user can like a post at most once.
    UNIQUE (user_id, post_id),
    FOREIGN KEY (user_id) REFERENCES app_user (id) ON DELETE CASCADE,
    FOREIGN KEY (post_id) REFERENCES post (id) ON DELETE CASCADE
);

-- ----------------------------------------------------------
-- verification_token
-- ----------------------------------------------------------
CREATE TABLE IF NOT EXISTS verification_token
(
    id          BIGSERIAL PRIMARY KEY,
    token       VARCHAR(255),
    user_id     BIGINT NOT NULL,
    expiry_date TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES app_user (id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_verification_token_user_id ON verification_token (user_id);

-- ----------------------------------------------------------
-- password_reset_token
-- ----------------------------------------------------------
CREATE TABLE IF NOT EXISTS password_reset_token
(
    id            BIGSERIAL PRIMARY KEY,
    token         VARCHAR(255) UNIQUE,
    otp           VARCHAR(255),
    token_type    VARCHAR(255),
    user_id       BIGINT,
    created_at    TIMESTAMP,
    expiry_date   TIMESTAMP,
    is_used       BOOLEAN DEFAULT FALSE NOT NULL,
    attempt_count INTEGER DEFAULT 0     NOT NULL,
    FOREIGN KEY (user_id) REFERENCES app_user (id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_password_reset_token_user_id ON password_reset_token (user_id);
CREATE INDEX IF NOT EXISTS idx_password_reset_token_expiry_date ON password_reset_token (expiry_date);

-- ----------------------------------------------------------
-- shedlock
-- ----------------------------------------------------------
-- Backs ShedLock, so a @Scheduled job runs once across all replicas rather than once each.
CREATE TABLE IF NOT EXISTS shedlock
(
    name       VARCHAR(64)  NOT NULL PRIMARY KEY,
    lock_until TIMESTAMP    NOT NULL,
    locked_at  TIMESTAMP    NOT NULL,
    locked_by  VARCHAR(255) NOT NULL
);
