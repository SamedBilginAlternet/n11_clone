CREATE TABLE products (
    id                   BIGSERIAL PRIMARY KEY,
    name                 VARCHAR(255) NOT NULL,
    slug                 VARCHAR(255) NOT NULL UNIQUE,
    description          TEXT,
    price                NUMERIC(12, 2) NOT NULL,
    discount_percentage  INT NOT NULL DEFAULT 0,
    stock_quantity       INT NOT NULL DEFAULT 0,
    image_url            VARCHAR(1024),
    category             VARCHAR(100) NOT NULL,
    brand                VARCHAR(100),
    rating               DOUBLE PRECISION DEFAULT 0,
    review_count         INT DEFAULT 0
);

CREATE INDEX idx_products_category ON products (category);
CREATE INDEX idx_products_slug ON products (slug);
