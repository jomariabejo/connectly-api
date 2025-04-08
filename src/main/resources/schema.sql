-- ==========================================================
-- Role Enum Type Definition
-- ==========================================================
-- This section defines an enum type 'role_enum' for user roles.
-- The roles defined are 'ADMIN' and 'USER', which will be used
-- for assigning user roles in the application.

-- Drop the role enum type if it already exists (to avoid errors during re-runs)
DROP TYPE IF EXISTS role_enum;

-- Create the 'role_enum' type with 'ADMIN' and 'USER' as valid values
CREATE TYPE role_enum AS ENUM ('ADMIN', 'USER');

-- ==========================================================
-- User Table (app_user)
-- ==========================================================
-- The 'app_user' table stores information about the users of the application.
-- It contains fields for user credentials (username, password, email),
-- user role, account status (enabled/disabled), and timestamps for account creation and updates.

-- Drop the app_user table if it already exists (to avoid errors during re-runs)
DROP TABLE IF EXISTS app_user;

-- Create the 'app_user' table with the necessary columns
CREATE TABLE IF NOT EXISTS app_user
(
    -- Unique identifier for the user (primary key)
    id         BIGSERIAL PRIMARY KEY,

    -- Username of the user (must be unique and not null)
    username   VARCHAR(255) UNIQUE NOT NULL,

    -- Password for the user (not null)
    password   VARCHAR(255)        NOT NULL,

    -- Email address (must be unique and not null)
    email      VARCHAR(255) UNIQUE NOT NULL,

    -- Role of the user, defined by the 'role_enum' type (not null)
    role       role_enum           NOT NULL,

    -- Account status (enabled or disabled), default is TRUE (enabled)
    enabled    BOOLEAN   DEFAULT TRUE,

    -- Timestamps for tracking when the account was created and last updated
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP
);

-- Create an index on the 'username' column for faster lookups
CREATE INDEX IF NOT EXISTS idx_user_username ON app_user (username);

-- Create an index on the 'email' column for faster lookups
CREATE INDEX IF NOT EXISTS idx_user_email ON app_user (email);

-- ==========================================================
-- Post Table
-- ==========================================================
-- The 'post' table stores posts created by users.
-- Each post has a title, content, type (text, image, or video),
-- metadata in JSON format, a reference to the user who created the post,
-- and a privacy setting (public or private).

-- Create the 'post' table
CREATE TABLE IF NOT EXISTS post
(
    -- Unique identifier for the post (primary key)
    id         BIGSERIAL PRIMARY KEY,

    -- Title of the post (required)
    title      VARCHAR(255) NOT NULL,

    -- Content of the post (optional, could be text, image, or video)
    content    TEXT,

    -- Type of post (text, image, or video)
    post_type  VARCHAR(10) CHECK (post_type IN ('text', 'image', 'video')),

    -- Metadata related to the post, stored as JSONB for better performance
    metadata   JSONB       DEFAULT '{}'::jsonb,

    -- ID of the user who created the post (foreign key reference to app_user)
    created_by BIGINT       NOT NULL,

    -- Timestamp for when the post was created
    created_at TIMESTAMP   DEFAULT CURRENT_TIMESTAMP,

    -- Privacy setting for the post (public or private)
    privacy    VARCHAR(10) DEFAULT 'public' CHECK (privacy IN ('public', 'private')),

    -- Foreign key constraint to link the post to the user who created it
    FOREIGN KEY (created_by) REFERENCES app_user (id)
);

-- ==========================================================
-- Comment Table
-- ==========================================================
-- The 'comment' table stores comments made by users on posts.
-- Each comment is associated with a specific post and user.

-- Create the 'comment' table
CREATE TABLE IF NOT EXISTS comment
(
    -- Unique identifier for the comment (primary key)
    id         BIGSERIAL PRIMARY KEY,

    -- The actual content of the comment (required)
    text       TEXT   NOT NULL,

    -- ID of the post this comment belongs to (foreign key reference to post)
    post_id    BIGINT NOT NULL,

    -- ID of the user who made the comment (foreign key reference to app_user)
    user_id    BIGINT NOT NULL,

    -- Timestamp for when the comment was created
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    -- Foreign key constraint to link the comment to a post
    FOREIGN KEY (post_id) REFERENCES post (id),

    -- Foreign key constraint to link the comment to a user
    FOREIGN KEY (user_id) REFERENCES app_user (id)
);

-- ==========================================================
-- Like Table (post_like)
-- ==========================================================
-- The 'post_like' table stores records of users liking posts.
-- A user can like a post only once, enforced by the unique constraint on user_id and post_id.

-- Create the 'post_like' table (with the name quoted to avoid conflict with the reserved keyword 'LIKE')
CREATE TABLE IF NOT EXISTS post_like
(
    -- Unique identifier for the like (primary key)
    id         BIGSERIAL PRIMARY KEY,

    -- ID of the user who liked the post (foreign key reference to app_user)
    user_id    BIGINT NOT NULL,

    -- ID of the post that was liked (foreign key reference to post)
    post_id    BIGINT NOT NULL,

    -- Timestamp for when the like was created
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    -- Enforce that a user can like a post only once (unique constraint on user_id and post_id)
    UNIQUE (user_id, post_id),

    -- Foreign key constraint to link the like to a user
    FOREIGN KEY (user_id) REFERENCES app_user (id),

    -- Foreign key constraint to link the like to a post
    FOREIGN KEY (post_id) REFERENCES post (id)
);
