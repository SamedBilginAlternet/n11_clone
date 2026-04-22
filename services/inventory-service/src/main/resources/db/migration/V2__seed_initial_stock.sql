-- Initial stock levels mirroring product-service's V2 seed. Product IDs 1..22
-- correspond one-to-one with product-service rows because both databases are
-- empty at compose-up and seeded in the same order. If product-service's seed
-- grows, add a matching row here in a new V-migration so reviewers can
-- reserve every product out of the box.

INSERT INTO inventory_items (product_id, available_stock, reserved_stock) VALUES
(1,  50, 0),   -- iPhone 15 Pro
(2,  40, 0),   -- Galaxy S24 Ultra
(3, 100, 0),   -- Sony WH-1000XM5
(4,  15, 0),   -- LG OLED C3
(5,  30, 0),   -- MacBook Air M3
(6, 200, 0),   -- Adidas Samba
(7, 150, 0),   -- Nike Air Max 270
(8, 300, 0),   -- Mavi Jean
(9, 500, 0),   -- LCW Triko
(10, 80, 0),   -- Philips Airfryer
(11,200, 0),   -- Karaca Jumbo Çay
(12, 35, 0),   -- IKEA POÄNG
(13,500, 0),   -- Prima Bebek
(14, 25, 0),   -- Chicco beşik
(15,300, 0),   -- Nivea Men
(16,400, 0),   -- Maybelline
(17,250, 0),   -- Kipsta Futbol Topu
(18, 60, 0),   -- Columbia Mont
(19,600, 0),   -- Kürk Mantolu Madonna
(20,800, 0),   -- 1984
(21,400, 0),   -- Eti Popkek
(22,350, 0);   -- Ülker Gofret
