CREATE TABLE reviews (
    id          BIGSERIAL PRIMARY KEY,
    product_id  BIGINT NOT NULL,
    user_email  VARCHAR(255) NOT NULL,
    user_name   VARCHAR(255) NOT NULL,
    rating      INT NOT NULL CHECK (rating BETWEEN 1 AND 5),
    comment     VARCHAR(2000) NOT NULL,
    created_at  TIMESTAMP NOT NULL,
    CONSTRAINT uq_review_product_user UNIQUE (product_id, user_email)
);

CREATE INDEX idx_reviews_product_id ON reviews (product_id);
CREATE INDEX idx_reviews_user_email ON reviews (user_email);
