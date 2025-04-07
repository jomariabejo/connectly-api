-- Optional: Define the Role Enum type in the database if your DBMS supports it (PostgreSQL example).
-- If your DBMS does not support Enum, we will use VARCHAR instead.
-- PostgreSQL ENUM type
CREATE TYPE role_enum AS ENUM ('ADMIN', 'USER');

-- User Table (Custom User entity for Spring Security)
CREATE TABLE IF NOT EXISTS "user"
(
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(255) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    role VARCHAR(255) NOT NULL,
    enabled BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_role FOREIGN KEY(role) REFERENCES role_enum(role)
    );


-- Optional: Create an index on username for faster lookups (you can remove this if not needed)
CREATE INDEX IF NOT EXISTS idx_user_username ON "user" (username);

-- Optional: Create an index on email for faster lookups (you can remove this if not needed)
CREATE INDEX IF NOT EXISTS idx_user_email ON "user" (email);

-- Post Table
CREATE TABLE IF NOT EXISTS post
(
    id BIGSERIAL PRIMARY KEY,              -- Auto-incrementing ID for the post (BIGSERIAL in PostgreSQL)
    title VARCHAR(255) NOT NULL,                        -- Title of the post
    content TEXT,                                       -- Content of the post
    post_type VARCHAR(10) CHECK (post_type IN ('text', 'image', 'video')), -- Type of the post
    metadata JSON DEFAULT '{}',                         -- Additional metadata (JSON format)
    created_by BIGINT NOT NULL,                         -- Foreign Key to User
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,     -- Timestamp for post creation
    privacy VARCHAR(10) DEFAULT 'public' CHECK (privacy IN ('public', 'private')), -- Privacy setting
    FOREIGN KEY(created_by) REFERENCES "user"(id)      -- Foreign key reference to the user who created the post
    );

-- Comment Table
CREATE TABLE IF NOT EXISTS comment
(
    id BIGSERIAL PRIMARY KEY,              -- Auto-incrementing ID for the comment (BIGSERIAL in PostgreSQL)
    text TEXT NOT NULL,                                 -- Comment text
    post_id BIGINT NOT NULL,                            -- Foreign Key to Post
    user_id BIGINT NOT NULL,                            -- Foreign Key to User (commenter)
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,     -- Timestamp for comment creation
    FOREIGN KEY(post_id) REFERENCES post(id),          -- Foreign Key reference to Post
    FOREIGN KEY(user_id) REFERENCES "user"(id)         -- Foreign Key reference to User
    );

-- Like Table
CREATE TABLE IF NOT EXISTS "like"
(
    id BIGSERIAL PRIMARY KEY,              -- Auto-incrementing ID for the like (BIGSERIAL in PostgreSQL)
    user_id BIGINT NOT NULL,                            -- Foreign Key to User (who liked)
    post_id BIGINT NOT NULL,                            -- Foreign Key to Post (liked)
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,     -- Timestamp for like creation
    UNIQUE(user_id, post_id),                           -- Ensure that each user can only like a post once
    FOREIGN KEY(user_id) REFERENCES "user"(id),        -- Foreign Key reference to User
    FOREIGN KEY(post_id) REFERENCES post(id)           -- Foreign Key reference to Post
    );
