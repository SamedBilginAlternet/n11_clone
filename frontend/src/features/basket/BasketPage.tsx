import { Link } from 'react-router-dom';
import { basketApi } from './api';
import { useBasketStore } from './store';
import { useApi } from '../../shared/hooks/useApi';
import { formatTRY } from '../../shared/utils/format';
import { Button } from '../../shared/ui/Button';
import { Card } from '../../shared/ui/Card';
import { useToast } from '../../shared/providers/ToastProvider';
import { errorMessage } from '../../shared/api/problem';

export function BasketPage() {
  const setBasket = useBasketStore((s) => s.setBasket);
  const toast = useToast();

  const { data: basket, loading, refetch } = useApi(() => basketApi.get(), []);

  const handleQty = async (itemId: number, qty: number) => {
    if (qty < 1) return;
    try {
      const b = await basketApi.updateItem(itemId, qty);
      setBasket(b);
      await refetch();
    } catch (err) {
      toast.error(errorMessage(err, 'Güncelleme başarısız.'));
    }
  };

  const handleRemove = async (itemId: number) => {
    try {
      const b = await basketApi.removeItem(itemId);
      setBasket(b);
      await refetch();
    } catch (err) {
      toast.error(errorMessage(err, 'Silme başarısız.'));
    }
  };

  const handleClear = async () => {
    try {
      await basketApi.clear();
      toast.info('Sepet temizlendi.');
      await refetch();
      setBasket(null);
    } catch (err) {
      toast.error(errorMessage(err));
    }
  };

  if (loading && !basket) return <div className="text-gray-500">Sepet yükleniyor...</div>;

  if (!basket || basket.items.length === 0) {
    return (
      <Card className="p-12 text-center">
        <div className="text-6xl mb-3">🛒</div>
        <h1 className="text-xl font-bold mb-1">Sepetin boş</h1>
        <p className="text-gray-500 mb-4">Alışverişe başlamak için ana sayfaya dön.</p>
        <Link
          to="/"
          className="inline-block bg-n11-purple text-white px-4 py-2 rounded font-medium hover:bg-purple-700"
        >
          Alışverişe Devam Et
        </Link>
      </Card>
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
          <Card key={item.id} className="p-3 flex gap-3">
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
          </Card>
        ))}
      </div>

      <Card className="p-4 h-fit sticky top-20">
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
        <Link to="/checkout">
          <Button variant="secondary" size="lg" fullWidth className="mt-4">
            Ödemeye Geç
          </Button>
        </Link>
      </Card>
    </div>
  );
}
