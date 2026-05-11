-- ==========================================================
-- Blocking & Circles System Migration
-- ==========================================================

-- ==========================================================
-- Create Block Table
-- ==========================================================
-- This table stores blocking relationships between users.
-- Bidirectional blocking provides privacy through mutual invisibility.

CREATE TABLE IF NOT EXISTS block
(
    -- Unique identifier for the block (primary key)
    id              BIGSERIAL PRIMARY KEY,

    -- ID of the user who is blocking (foreign key reference to app_user)
    blocker_id      BIGINT NOT NULL,

    -- ID of the user being blocked (foreign key reference to app_user)
    blocked_id      BIGINT NOT NULL,

    -- Timestamp for when the block was created
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    -- Ensure a user can't block themselves
    CHECK (blocker_id != blocked_id),

    -- Ensure only one block relationship per (blocker, blocked) pair
    UNIQUE (blocker_id, blocked_id),

    -- Foreign key constraint to link the blocker
    FOREIGN KEY (blocker_id) REFERENCES app_user (id) ON DELETE CASCADE,

    -- Foreign key constraint to link the blocked user
    FOREIGN KEY (blocked_id) REFERENCES app_user (id) ON DELETE CASCADE
);

-- Index for fast block lookups
CREATE INDEX IF NOT EXISTS idx_block_blocker_id ON block (blocker_id);

-- Index for finding who blocked a user
CREATE INDEX IF NOT EXISTS idx_block_blocked_id ON block (blocked_id);

-- ==========================================================
-- Create Circle Table
-- ==========================================================
-- This table stores custom privacy circles created by users.
-- Circles allow grouping followers for privacy granularity (family, work, close-friends).

CREATE TABLE IF NOT EXISTS circle
(
    -- Unique identifier for the circle (primary key)
    id              BIGSERIAL PRIMARY KEY,

    -- Reference to the user who owns/created the circle (foreign key)
    user_id         BIGINT NOT NULL,

    -- Circle name (e.g., 'family', 'work', 'close-friends')
    name            VARCHAR(100) NOT NULL,

    -- Optional circle description
    description     VARCHAR(500),

    -- Timestamp for when circle was created
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    -- Timestamp for when circle was last updated
    updated_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    -- Foreign key constraint linking circle to owner
    FOREIGN KEY (user_id) REFERENCES app_user (id) ON DELETE CASCADE,

    -- Ensure circle names are unique per user
    UNIQUE (user_id, name)
);

-- Index for fast circle lookups by owner
CREATE INDEX IF NOT EXISTS idx_circle_user_id ON circle (user_id);

-- ==========================================================
-- Create Circle Member Table
-- ==========================================================
-- This table represents the many-to-many relationship between circles and their members.

CREATE TABLE IF NOT EXISTS circle_member
(
    -- Unique identifier (primary key)
    id              BIGSERIAL PRIMARY KEY,

    -- Reference to the circle (foreign key)
    circle_id       BIGINT NOT NULL,

    -- Reference to the member user (foreign key)
    member_id       BIGINT NOT NULL,

    -- Timestamp for when member was added
    added_at        TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    -- Ensure a user can't be added to a circle twice
    UNIQUE (circle_id, member_id),

    -- Foreign key constraint linking to circle
    FOREIGN KEY (circle_id) REFERENCES circle (id) ON DELETE CASCADE,

    -- Foreign key constraint linking to member
    FOREIGN KEY (member_id) REFERENCES app_user (id) ON DELETE CASCADE
);

-- Index for fast member lookups in circle
CREATE INDEX IF NOT EXISTS idx_circle_member_circle_id ON circle_member (circle_id);

-- Index for finding circles a user is a member of
CREATE INDEX IF NOT EXISTS idx_circle_member_member_id ON circle_member (member_id);

-- ==========================================================
-- Create Post Circle Visibility Table
-- ==========================================================
-- This table represents which circles can see a post (for CIRCLES privacy level).

CREATE TABLE IF NOT EXISTS post_circle_visibility
(
    -- Unique identifier (primary key)
    id              BIGSERIAL PRIMARY KEY,

    -- Reference to the post (foreign key)
    post_id         BIGINT NOT NULL,

    -- Reference to the visible circle (foreign key)
    circle_id       BIGINT NOT NULL,

    -- Timestamp for when visibility was created
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    -- Ensure post is visible to circle only once
    UNIQUE (post_id, circle_id),

    -- Foreign key constraint linking to post
    FOREIGN KEY (post_id) REFERENCES post (id) ON DELETE CASCADE,

    -- Foreign key constraint linking to circle
    FOREIGN KEY (circle_id) REFERENCES circle (id) ON DELETE CASCADE
);

-- Index for fast post visibility lookups
CREATE INDEX IF NOT EXISTS idx_post_circle_visibility_post_id ON post_circle_visibility (post_id);

-- Index for finding posts visible to a circle
CREATE INDEX IF NOT EXISTS idx_post_circle_visibility_circle_id ON post_circle_visibility (circle_id);

-- ==========================================================
-- Update Post table privacy constraint
-- ==========================================================
-- Expand privacy options to include CIRCLES privacy level

ALTER TABLE post 
DROP CONSTRAINT IF EXISTS post_privacy_check;

ALTER TABLE post 
ADD CONSTRAINT post_privacy_check 
CHECK (privacy IN ('public', 'private', 'followers_only', 'mutuals_only', 'friends_of_friends', 'circles'));

-- Update existing posts - no change needed, they keep their current privacy level
