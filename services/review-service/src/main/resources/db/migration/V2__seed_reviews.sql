-- Seed reviews for popular seeded products (IDs match product-service V2).
-- Distinct (product_id, user_email) pairs satisfy the unique constraint.

INSERT INTO reviews (product_id, user_email, user_name, rating, comment, created_at) VALUES
-- iPhone 15 Pro (id=1)
(1, 'ayse@n11demo.com',    'Ayşe',   5, 'Kamera kalitesi gerçekten çok iyi, pil ömrü de beklediğimden uzun.', NOW() - INTERVAL '12 days'),
(1, 'mehmet@n11demo.com',  'Mehmet', 4, 'Fiyatı biraz yüksek ama performans buna değiyor.',                   NOW() - INTERVAL '8 days'),
(1, 'zeynep@n11demo.com',  'Zeynep', 5, 'Kutudan çıkar çıkmaz kuruldu, ekran harika.',                        NOW() - INTERVAL '3 days'),

-- Samsung Galaxy S24 Ultra (id=2)
(2, 'ahmet@n11demo.com',   'Ahmet',  5, 'S Pen gerçekten iş görüyor, 200MP kamera çok başarılı.',             NOW() - INTERVAL '10 days'),
(2, 'elif@n11demo.com',    'Elif',   4, 'Kutudan çıkan şarj aleti olmaması dışında harika bir telefon.',      NOW() - INTERVAL '5 days'),

-- Sony WH-1000XM5 (id=3)
(3, 'mehmet@n11demo.com',  'Mehmet', 5, 'ANC efsane, uçakta kullanıma muhteşem.',                             NOW() - INTERVAL '20 days'),
(3, 'fatma@n11demo.com',   'Fatma',  5, 'Ses kalitesi rakipsiz, kafadaki konforu da çok iyi.',                NOW() - INTERVAL '15 days'),
(3, 'ayse@n11demo.com',    'Ayşe',   4, 'Pil ömrü iddia edildiği gibi 30 saat kadar geliyor.',                NOW() - INTERVAL '6 days'),

-- LG OLED C3 (id=4)
(4, 'hakan@n11demo.com',   'Hakan',  5, 'Siyahlar gerçek siyah, HDR içerikler muazzam.',                      NOW() - INTERVAL '25 days'),

-- MacBook Air M3 (id=5)
(5, 'ahmet@n11demo.com',   'Ahmet',  5, 'Fansız tasarım sessiz ve serin, iOS geliştirme için ideal.',         NOW() - INTERVAL '14 days'),
(5, 'elif@n11demo.com',    'Elif',   4, '8GB RAM biraz düşük ama günlük kullanım için yetiyor.',              NOW() - INTERVAL '4 days'),

-- Adidas Samba OG (id=6)
(6, 'zeynep@n11demo.com',  'Zeynep', 5, 'Kombinlere çok rahat uyum sağlıyor, konforlu.',                      NOW() - INTERVAL '9 days'),
(6, 'fatma@n11demo.com',   'Fatma',  4, 'İlk birkaç kullanımda biraz vuruyor, sonra yumuşuyor.',              NOW() - INTERVAL '2 days'),

-- Nike Air Max 270 (id=7)
(7, 'hakan@n11demo.com',   'Hakan',  4, 'Yastıklama bacağı yormuyor, günlük kullanım için iyi.',              NOW() - INTERVAL '7 days'),

-- Philips Airfryer XXL (id=10)
(10, 'ayse@n11demo.com',   'Ayşe',   5, 'Kızartma yağı ihtiyacı kalmadı, kapasitesi aile için uygun.',        NOW() - INTERVAL '18 days'),
(10, 'mehmet@n11demo.com', 'Mehmet', 4, 'Temizliği kolay, patates çok güzel oluyor.',                         NOW() - INTERVAL '11 days'),

-- Karaca Jumbo Çay (id=11)
(11, 'fatma@n11demo.com',  'Fatma',  5, 'Demi ayarlaması güzel, her sabah kullanıyoruz.',                     NOW() - INTERVAL '16 days'),

-- Kürk Mantolu Madonna (id=19)
(19, 'zeynep@n11demo.com', 'Zeynep', 5, 'Klasik olması boşuna değil. Baskı kalitesi de iyi.',                 NOW() - INTERVAL '22 days'),

-- 1984 - Orwell (id=20)
(20, 'ahmet@n11demo.com',  'Ahmet',  5, 'Her okuyuşta farklı bir katmanını fark ediyorsunuz. Çeviri başarılı.', NOW() - INTERVAL '30 days'),
(20, 'elif@n11demo.com',   'Elif',   5, 'Distopya klasiği, güncelliğini koruyor.',                              NOW() - INTERVAL '1 day');
