CREATE TABLE baskets (
    id          BIGSERIAL PRIMARY KEY,
    user_email  VARCHAR(255) NOT NULL UNIQUE,
    created_at  TIMESTAMP NOT NULL,
    updated_at  TIMESTAMP NOT NULL
);

CREATE INDEX idx_baskets_user_email ON baskets (user_email);
