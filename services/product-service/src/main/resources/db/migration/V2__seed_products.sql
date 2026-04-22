-- Turkish e-commerce seed data (n11-style demo catalog)

INSERT INTO products (name, slug, description, price, discount_percentage, stock_quantity, image_url, category, brand, rating, review_count) VALUES
-- Elektronik
('Apple iPhone 15 Pro 256GB', 'apple-iphone-15-pro-256gb', 'A17 Pro çipi, 48MP kamera, ProMotion 120Hz ekran.', 79999.00, 10, 50, 'https://picsum.photos/seed/iphone15/400/400', 'elektronik', 'Apple', 4.8, 1245),
('Samsung Galaxy S24 Ultra', 'samsung-galaxy-s24-ultra', 'S Pen dahil, 200MP kamera, Snapdragon 8 Gen 3.', 69999.00, 15, 40, 'https://picsum.photos/seed/galaxys24/400/400', 'elektronik', 'Samsung', 4.7, 890),
('Sony WH-1000XM5 Kulaklık', 'sony-wh-1000xm5', 'Aktif gürültü engelleme, 30 saat pil ömrü.', 14999.00, 20, 100, 'https://picsum.photos/seed/sonywh/400/400', 'elektronik', 'Sony', 4.9, 2100),
('LG OLED C3 55 inç 4K TV', 'lg-oled-c3-55', '4K OLED, 120Hz, HDMI 2.1, webOS.', 54999.00, 12, 15, 'https://picsum.photos/seed/lgtv/400/400', 'elektronik', 'LG', 4.8, 340),
('MacBook Air M3 13 inç', 'macbook-air-m3-13', 'Apple M3 çip, 8GB RAM, 256GB SSD, 18 saat pil.', 52999.00, 8, 30, 'https://picsum.photos/seed/mba/400/400', 'elektronik', 'Apple', 4.8, 567),

-- Moda
('Adidas Samba OG Spor Ayakkabı', 'adidas-samba-og', 'Klasik suni deri, üçlü şerit, beyaz taban.', 3499.00, 25, 200, 'https://picsum.photos/seed/samba/400/400', 'moda', 'Adidas', 4.6, 450),
('Nike Air Max 270 Erkek', 'nike-air-max-270', 'Max Air yastıklama, günlük kullanım.', 4299.00, 20, 150, 'https://picsum.photos/seed/airmax/400/400', 'moda', 'Nike', 4.7, 670),
('Mavi Jean Slim Fit Erkek', 'mavi-jean-slim-erkek', 'Pamuklu slim kesim denim.', 899.00, 30, 300, 'https://picsum.photos/seed/mavi/400/400', 'moda', 'Mavi', 4.4, 230),
('LC Waikiki Kadın Triko Kazak', 'lcw-kadin-triko', 'Yumuşak dokulu, yuvarlak yaka triko.', 399.00, 40, 500, 'https://picsum.photos/seed/lcwtriko/400/400', 'moda', 'LC Waikiki', 4.2, 189),

-- Ev & Yaşam
('Philips Airfryer XXL', 'philips-airfryer-xxl', '1.4kg kapasite, RapidAir teknolojisi.', 6499.00, 18, 80, 'https://picsum.photos/seed/airfryer/400/400', 'ev-yasam', 'Philips', 4.7, 820),
('Karaca Jumbo Çay Makinesi', 'karaca-jumbo-cay', 'Çelik demlikli, otomatik çay makinesi.', 1899.00, 22, 200, 'https://picsum.photos/seed/karaca/400/400', 'ev-yasam', 'Karaca', 4.5, 340),
('IKEA POÄNG Koltuk', 'ikea-poang-koltuk', 'Huş kaplama, rahat koltuk.', 3999.00, 10, 35, 'https://picsum.photos/seed/poang/400/400', 'ev-yasam', 'IKEA', 4.6, 420),

-- Anne & Bebek
('Prima Bebek Bezi 5 Numara', 'prima-bebek-5', '11-16 kg, fırsat paketi 80 adet.', 499.00, 15, 500, 'https://picsum.photos/seed/prima/400/400', 'anne-bebek', 'Prima', 4.7, 980),
('Chicco Next2Me Beşik', 'chicco-next2me', 'Anne yanı beşik, 6 kademe yükseklik.', 7499.00, 12, 25, 'https://picsum.photos/seed/chicco/400/400', 'anne-bebek', 'Chicco', 4.8, 215),

-- Kozmetik
('Nivea Men Cilt Bakım Seti', 'nivea-men-set', '3 ürünlü tıraş sonrası bakım seti.', 299.00, 35, 300, 'https://picsum.photos/seed/nivea/400/400', 'kozmetik', 'Nivea', 4.3, 180),
('Maybelline Lash Sensational Maskara', 'maybelline-lash-sensational', 'Yelpaze fırça, hacim veren maskara.', 249.00, 20, 400, 'https://picsum.photos/seed/maybelline/400/400', 'kozmetik', 'Maybelline', 4.5, 560),

-- Spor & Outdoor
('Decathlon Kipsta Futbol Topu', 'kipsta-futbol-topu', 'Sentetik deri, 5 numara maç topu.', 399.00, 10, 250, 'https://picsum.photos/seed/kipsta/400/400', 'spor-outdoor', 'Kipsta', 4.4, 145),
('Columbia Erkek Outdoor Mont', 'columbia-mont-erkek', 'Omni-Heat astar, su geçirmez.', 3999.00, 30, 60, 'https://picsum.photos/seed/columbia/400/400', 'spor-outdoor', 'Columbia', 4.7, 278),

-- Kitap & Müzik
('Kürk Mantolu Madonna - Sabahattin Ali', 'kurk-mantolu-madonna', 'YKY, karton kapak, 172 sayfa.', 99.00, 25, 600, 'https://picsum.photos/seed/kurk/400/400', 'kitap-muzik', 'YKY', 4.8, 3400),
('1984 - George Orwell', 'orwell-1984', 'Can Yayınları, Celal Üster çevirisi.', 149.00, 20, 800, 'https://picsum.photos/seed/1984/400/400', 'kitap-muzik', 'Can Yayınları', 4.9, 5600),

-- Süpermarket
('Eti Popkek Kakaolu 60g x 24', 'eti-popkek-kakaolu', '24 adet tekli paket.', 239.00, 12, 400, 'https://picsum.photos/seed/popkek/400/400', 'supermarket', 'Eti', 4.6, 290),
('Ülker Çikolatalı Gofret 36 Adet', 'ulker-cikolatali-gofret', 'Kutuluk, 36 adet.', 199.00, 15, 350, 'https://picsum.photos/seed/ulker/400/400', 'supermarket', 'Ülker', 4.5, 420);
