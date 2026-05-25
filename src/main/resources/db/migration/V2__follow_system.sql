-- ==========================================================
-- Follow System & Privacy Engine Migration
-- ==========================================================

-- ==========================================================
-- Add columns to app_user table
-- ==========================================================

ALTER TABLE app_user 
ADD COLUMN IF NOT EXISTS account_private BOOLEAN DEFAULT FALSE;

ALTER TABLE app_user 
ADD COLUMN IF NOT EXISTS first_name VARCHAR(255);

ALTER TABLE app_user 
ADD COLUMN IF NOT EXISTS last_name VARCHAR(255);

-- ==========================================================
-- Create Follow Table
-- ==========================================================
-- This table stores follow relationships between users.
-- A follow can be pending (awaiting approval for private accounts) or approved.

CREATE TABLE IF NOT EXISTS follow
(
    -- Unique identifier for the follow relationship (primary key)
    id              BIGSERIAL PRIMARY KEY,

    -- ID of the user who is following (foreign key reference to app_user)
    follower_id     BIGINT NOT NULL,

    -- ID of the user being followed (foreign key reference to app_user)
    following_id    BIGINT NOT NULL,

    -- Timestamp for when the follow relationship was created
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    -- Whether the follow has been approved (true for auto-follow or approved requests)
    approved        BOOLEAN DEFAULT FALSE,

    -- Status of the follow request (PENDING, APPROVED, REJECTED)
    -- NULL/empty for backward compatibility with approved boolean
    request_status  VARCHAR(20),

    -- Ensure a user can't follow themselves
    CHECK (follower_id != following_id),

    -- Ensure only one follow relationship per (follower, following) pair
    UNIQUE (follower_id, following_id),

    -- Foreign key constraint to link the follower
    FOREIGN KEY (follower_id) REFERENCES app_user (id) ON DELETE CASCADE,

    -- Foreign key constraint to link the user being followed
    FOREIGN KEY (following_id) REFERENCES app_user (id) ON DELETE CASCADE
);

-- Index for fast follower lookups
CREATE INDEX IF NOT EXISTS idx_follow_following_id_approved ON follow (following_id, approved);

-- Index for fast following lookups
CREATE INDEX IF NOT EXISTS idx_follow_follower_id_approved ON follow (follower_id, approved);

-- Index for pending requests
CREATE INDEX IF NOT EXISTS idx_follow_request_status ON follow (request_status);

-- ==========================================================
-- Create User Settings Table
-- ==========================================================
-- This table stores user-specific settings for follow behavior and privacy preferences.

CREATE TABLE IF NOT EXISTS user_settings
(
    -- Unique identifier for the settings (primary key)
    id                          BIGSERIAL PRIMARY KEY,

    -- Reference to the user (foreign key, unique to ensure one settings per user)
    user_id                     BIGINT NOT NULL UNIQUE,

    -- Whether to automatically approve follow requests (TRUE for public account behavior)
    auto_approve_followers      BOOLEAN DEFAULT TRUE,

    -- Whether to allow other users to follow this user
    allow_following             BOOLEAN DEFAULT TRUE,

    -- Timestamp for when settings were created
    created_at                  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    -- Timestamp for when settings were last updated
    updated_at                  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    -- Foreign key constraint linking settings to a user
    FOREIGN KEY (user_id) REFERENCES app_user (id) ON DELETE CASCADE
);

-- Index for fast user settings lookup
CREATE INDEX IF NOT EXISTS idx_user_settings_user_id ON user_settings (user_id);

-- ==========================================================
-- Update Post table privacy constraint
-- ==========================================================
-- Expand privacy options from ('public', 'private') to support new privacy levels

ALTER TABLE post 
DROP CONSTRAINT IF EXISTS post_privacy_check;

ALTER TABLE post 
ADD CONSTRAINT post_privacy_check 
CHECK (privacy IN ('public', 'private', 'followers_only', 'mutuals_only', 'friends_of_friends'));

-- Set default for existing posts to 'public'
UPDATE post SET privacy = 'public' WHERE privacy NOT IN ('public', 'private', 'followers_only', 'mutuals_only', 'friends_of_friends');
