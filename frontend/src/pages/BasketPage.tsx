import { useEffect } from 'react';
import { Link } from 'react-router-dom';
import { basketApi } from '../api/basket';
import { useBasketStore } from '../stores/basket';

const formatTRY = (n: number) =>
  new Intl.NumberFormat('tr-TR', { style: 'currency', currency: 'TRY', maximumFractionDigits: 2 }).format(n);

export function BasketPage() {
  const { basket, loading, setBasket, setLoading } = useBasketStore();

  const reload = async () => {
    setLoading(true);
    try {
      const b = await basketApi.get();
      setBasket(b);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    void reload();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const handleQty = async (itemId: number, quantity: number) => {
    if (quantity < 1) return;
    const b = await basketApi.updateItem(itemId, quantity);
    setBasket(b);
  };

  const handleRemove = async (itemId: number) => {
    const b = await basketApi.removeItem(itemId);
    setBasket(b);
  };

  const handleClear = async () => {
    await basketApi.clear();
    setBasket({ ...(basket ?? { id: 0, userEmail: '', items: [], total: 0, itemCount: 0 }), items: [], total: 0, itemCount: 0 });
  };

  if (loading && !basket) return <div className="text-gray-500">Sepet yükleniyor...</div>;

  if (!basket || basket.items.length === 0) {
    return (
      <div className="bg-white rounded-lg border border-gray-200 p-12 text-center">
        <div className="text-6xl mb-3">🛒</div>
        <h1 className="text-xl font-bold mb-1">Sepetiniz boş</h1>
        <p className="text-gray-500 mb-4">Alışverişe başlamak için ana sayfaya dönün.</p>
        <Link to="/" className="inline-block bg-n11-purple text-white px-4 py-2 rounded font-medium hover:bg-purple-700">
          Alışverişe Devam Et
        </Link>
      </div>
    );
  }

  return (
    <div className="grid md:grid-cols-3 gap-6">
      <div className="md:col-span-2 space-y-3">
        <div className="flex items-center justify-between">
          <h1 className="text-xl font-bold">Sepetim ({basket.itemCount} ürün)</h1>
          <button onClick={handleClear} className="text-sm text-red-600 hover:underline">
            Sepeti Boşalt
          </button>
        </div>

        {basket.items.map((item) => (
          <div key={item.id} className="bg-white rounded-lg border border-gray-200 p-3 flex gap-3">
            {item.imageUrl && (
              <img src={item.imageUrl} alt={item.productName} className="w-20 h-20 object-cover rounded" />
            )}
            <div className="flex-1 min-w-0">
              <div className="font-medium text-sm line-clamp-2">{item.productName}</div>
              <div className="text-n11-purple font-bold mt-1">{formatTRY(item.productPrice)}</div>
              <div className="flex items-center gap-2 mt-2">
                <button
                  onClick={() => handleQty(item.id, item.quantity - 1)}
                  className="w-7 h-7 rounded border border-gray-300 hover:bg-gray-50"
                  disabled={item.quantity <= 1}
                >
                  −
                </button>
                <span className="w-8 text-center">{item.quantity}</span>
                <button
                  onClick={() => handleQty(item.id, item.quantity + 1)}
                  className="w-7 h-7 rounded border border-gray-300 hover:bg-gray-50"
                >
                  +
                </button>
                <button
                  onClick={() => handleRemove(item.id)}
                  className="ml-auto text-sm text-red-600 hover:underline"
                >
                  Kaldır
                </button>
              </div>
            </div>
            <div className="text-right font-bold">{formatTRY(item.subtotal)}</div>
          </div>
        ))}
      </div>

      <aside className="bg-white rounded-lg border border-gray-200 p-4 h-fit sticky top-20">
        <h2 className="font-bold mb-3">Sipariş Özeti</h2>
        <div className="flex justify-between text-sm text-gray-600 mb-2">
          <span>Ara Toplam</span>
          <span>{formatTRY(basket.total)}</span>
        </div>
        <div className="flex justify-between text-sm text-gray-600 mb-2">
          <span>Kargo</span>
          <span className="text-green-600 font-medium">Ücretsiz</span>
        </div>
        <div className="border-t pt-2 mt-2 flex justify-between font-bold">
          <span>Toplam</span>
          <span className="text-n11-purple text-lg">{formatTRY(basket.total)}</span>
        </div>
        <button className="w-full mt-4 bg-n11-orange text-white py-2.5 rounded font-semibold hover:bg-orange-600">
          Ödemeye Geç
        </button>
        <p className="text-xs text-gray-400 mt-2">
          Ödeme akışı demo kapsamında değil — sepet saga mimarisini göstermek için yeterli.
        </p>
      </aside>
    </div>
  );
}
