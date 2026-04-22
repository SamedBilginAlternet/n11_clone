-- Drop the old single-role column from users
ALTER TABLE users DROP COLUMN IF EXISTS role;

-- New join table for multi-role support
CREATE TABLE user_roles (
    user_id BIGINT      NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    role    VARCHAR(50) NOT NULL,
    PRIMARY KEY (user_id, role)
);
