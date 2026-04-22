CREATE TABLE orders (
    id                BIGSERIAL PRIMARY KEY,
    user_email        VARCHAR(255) NOT NULL,
    total_amount      NUMERIC(12, 2) NOT NULL,
    status            VARCHAR(32) NOT NULL,
    shipping_address  VARCHAR(1024),
    failure_reason    VARCHAR(512),
    created_at        TIMESTAMP NOT NULL,
    updated_at        TIMESTAMP NOT NULL
);

CREATE INDEX idx_orders_user_email ON orders (user_email);
CREATE INDEX idx_orders_status ON orders (status);

CREATE TABLE order_items (
    id             BIGSERIAL PRIMARY KEY,
    order_id       BIGINT NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    product_id     BIGINT NOT NULL,
    product_name   VARCHAR(255) NOT NULL,
    product_price  NUMERIC(12, 2) NOT NULL,
    image_url      VARCHAR(1024),
    quantity       INT NOT NULL,
    created_at     TIMESTAMP NOT NULL,
    updated_at     TIMESTAMP NOT NULL
);

CREATE INDEX idx_order_items_order_id ON order_items (order_id);
