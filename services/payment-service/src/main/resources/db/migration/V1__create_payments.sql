CREATE TABLE payment_transactions (
    id              BIGSERIAL PRIMARY KEY,
    transaction_id  VARCHAR(64) NOT NULL UNIQUE,
    order_id        BIGINT NOT NULL,
    user_email      VARCHAR(255) NOT NULL,
    amount          NUMERIC(12, 2) NOT NULL,
    status          VARCHAR(32) NOT NULL,
    failure_reason  VARCHAR(512),
    created_at      TIMESTAMP NOT NULL
);

CREATE INDEX idx_payments_order_id ON payment_transactions (order_id);
CREATE INDEX idx_payments_user_email ON payment_transactions (user_email);
