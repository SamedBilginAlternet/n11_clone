CREATE TABLE inventory_items (
    product_id       BIGINT PRIMARY KEY,
    available_stock  INT NOT NULL CHECK (available_stock >= 0),
    reserved_stock   INT NOT NULL DEFAULT 0 CHECK (reserved_stock >= 0)
);

CREATE TABLE reservations (
    id          BIGSERIAL PRIMARY KEY,
    order_id    BIGINT NOT NULL,
    product_id  BIGINT NOT NULL REFERENCES inventory_items(product_id),
    quantity    INT NOT NULL CHECK (quantity > 0),
    created_at  TIMESTAMP NOT NULL
);

CREATE INDEX idx_reservations_order ON reservations (order_id);
