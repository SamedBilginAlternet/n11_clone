CREATE TABLE basket_items (
    id             BIGSERIAL PRIMARY KEY,
    basket_id      BIGINT NOT NULL REFERENCES baskets(id) ON DELETE CASCADE,
    product_id     BIGINT NOT NULL,
    product_name   VARCHAR(255) NOT NULL,
    product_price  NUMERIC(12, 2) NOT NULL,
    image_url      VARCHAR(1024),
    quantity       INT NOT NULL,
    created_at     TIMESTAMP NOT NULL,
    updated_at     TIMESTAMP NOT NULL
);

CREATE INDEX idx_basket_items_basket_id ON basket_items (basket_id);
