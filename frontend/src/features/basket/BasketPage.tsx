import { Link } from 'react-router-dom';
import { ShoppingCart, Trash2, Minus, Plus, ArrowRight } from 'lucide-react';
import { motion } from 'framer-motion';
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
    } catch (err) { toast.error(errorMessage(err, 'Guncelleme basarisiz.')); }
  };

  const handleRemove = async (itemId: number) => {
    try {
      const b = await basketApi.removeItem(itemId);
      setBasket(b);
      await refetch();
    } catch (err) { toast.error(errorMessage(err, 'Silme basarisiz.')); }
  };

  const handleClear = async () => {
    try {
      await basketApi.clear();
      toast.info('Sepet temizlendi.');
      await refetch();
      setBasket(null);
    } catch (err) { toast.error(errorMessage(err)); }
  };

  if (loading && !basket) return <div className="flex justify-center py-20"><div className="h-8 w-8 animate-spin rounded-full border-4 border-primary border-t-transparent" /></div>;

  if (!basket || basket.items.length === 0) {
    return (
      <motion.div initial={{ opacity: 0, scale: 0.95 }} animate={{ opacity: 1, scale: 1 }}>
        <Card className="mx-auto max-w-md p-12 text-center">
          <div className="mx-auto mb-4 flex h-20 w-20 items-center justify-center rounded-full bg-muted">
            <ShoppingCart className="h-10 w-10 text-muted-foreground" />
          </div>
          <h1 className="text-xl font-bold">Sepetin bos</h1>
          <p className="mt-1 text-sm text-muted-foreground">Alisverise baslamak icin ana sayfaya don.</p>
          <Link to="/">
            <Button className="mt-6">Alisverise Devam Et</Button>
          </Link>
        </Card>
      </motion.div>
    );
  }

  return (
    <div className="grid gap-6 lg:grid-cols-3">
      <div className="space-y-4 lg:col-span-2">
        <div className="flex items-center justify-between">
          <h1 className="text-xl font-bold">Sepetim ({basket.itemCount} urun)</h1>
          <button onClick={handleClear} className="flex items-center gap-1 text-sm text-destructive hover:underline">
            <Trash2 className="h-3.5 w-3.5" /> Sepeti Bosalt
          </button>
        </div>

        {basket.items.map((item, i) => (
          <motion.div key={item.id} initial={{ opacity: 0, x: -20 }} animate={{ opacity: 1, x: 0 }} transition={{ delay: i * 0.05 }}>
            <Card className="flex gap-4 p-4">
              {item.imageUrl && (
                <img src={item.imageUrl} alt={item.productName} className="h-20 w-20 shrink-0 rounded-lg object-cover" />
              )}
              <div className="flex flex-1 flex-col gap-2">
                <div className="text-sm font-medium line-clamp-2">{item.productName}</div>
                <div className="text-lg font-bold text-primary">{formatTRY(item.productPrice)}</div>
                <div className="flex items-center gap-2">
                  <div className="flex items-center rounded-lg border border-border">
                    <button
                      onClick={() => handleQty(item.id, item.quantity - 1)}
                      className="flex h-8 w-8 items-center justify-center transition-colors hover:bg-muted"
                      disabled={item.quantity <= 1}
                    >
                      <Minus className="h-3.5 w-3.5" />
                    </button>
                    <span className="w-8 text-center text-sm font-medium">{item.quantity}</span>
                    <button
                      onClick={() => handleQty(item.id, item.quantity + 1)}
                      className="flex h-8 w-8 items-center justify-center transition-colors hover:bg-muted"
                    >
                      <Plus className="h-3.5 w-3.5" />
                    </button>
                  </div>
                  <button
                    onClick={() => handleRemove(item.id)}
                    className="ml-auto flex items-center gap-1 text-xs text-destructive hover:underline"
                  >
                    <Trash2 className="h-3.5 w-3.5" /> Kaldir
                  </button>
                </div>
              </div>
              <div className="shrink-0 text-right font-bold">{formatTRY(item.subtotal)}</div>
            </Card>
          </motion.div>
        ))}
      </div>

      {/* Summary */}
      <div>
        <Card className="sticky top-20 space-y-4 p-6">
          <h2 className="text-lg font-bold">Siparis Ozeti</h2>
          <div className="space-y-2 text-sm">
            <div className="flex justify-between text-muted-foreground">
              <span>Ara Toplam</span>
              <span>{formatTRY(basket.total)}</span>
            </div>
            <div className="flex justify-between text-muted-foreground">
              <span>Kargo</span>
              <span className="font-medium text-emerald-600">Ucretsiz</span>
            </div>
          </div>
          <div className="border-t pt-3">
            <div className="flex items-center justify-between">
              <span className="text-lg font-bold">Toplam</span>
              <span className="text-2xl font-extrabold text-primary">{formatTRY(basket.total)}</span>
            </div>
          </div>
          <Link to="/checkout">
            <Button variant="secondary" size="lg" fullWidth className="mt-2">
              Odemeye Gec <ArrowRight className="h-4 w-4" />
            </Button>
          </Link>
        </Card>
      </div>
    </div>
  );
}
