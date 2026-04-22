import { FormEvent, useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useApi } from '../../shared/hooks/useApi';
import { useToast } from '../../shared/providers/ToastProvider';
import { errorMessage } from '../../shared/api/problem';
import { formatTRY } from '../../shared/utils/format';
import { Button } from '../../shared/ui/Button';
import { Card } from '../../shared/ui/Card';
import { Input } from '../../shared/ui/Input';
import { basketApi } from '../basket/api';
import { useBasketStore } from '../basket/store';
import { orderApi } from './api';

export function CheckoutPage() {
  const navigate = useNavigate();
  const toast = useToast();
  const setBasket = useBasketStore((s) => s.setBasket);
  const { data: basket, loading } = useApi(() => basketApi.get(), []);

  const [address, setAddress] = useState('');
  const [submitting, setSubmitting] = useState(false);

  useEffect(() => {
    if (!loading && basket && basket.items.length === 0) {
      toast.info('Sepet boş olduğu için ana sayfaya yönlendirildin.');
      navigate('/');
    }
  }, [loading, basket, navigate, toast]);

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault();
    if (!basket) return;
    setSubmitting(true);
    try {
      const order = await orderApi.checkout({
        shippingAddress: address,
        items: basket.items.map((i) => ({
          productId: i.productId,
          productName: i.productName,
          productPrice: i.productPrice,
          imageUrl: i.imageUrl ?? undefined,
          quantity: i.quantity,
        })),
      });
      toast.success(`Sipariş #${order.id} alındı — ödeme işleniyor.`);
      // Optimistic: clear local basket — the saga will clear the DB one once
      // payment succeeds. If payment fails, we'll refetch on Orders page.
      setBasket(null);
      navigate(`/orders/${order.id}`);
    } catch (err) {
      toast.error(errorMessage(err, 'Sipariş oluşturulamadı.'));
    } finally {
      setSubmitting(false);
    }
  };

  if (loading) return <div className="text-gray-500">Yükleniyor...</div>;
  if (!basket || basket.items.length === 0) return null;

  return (
    <div className="grid md:grid-cols-3 gap-6">
      <Card className="md:col-span-2 p-6">
        <h1 className="text-xl font-bold mb-4">Ödeme ve Teslimat</h1>

        <form onSubmit={handleSubmit} className="space-y-4">
          <Input
            name="address"
            label="Teslimat Adresi"
            required
            value={address}
            onChange={(e) => setAddress(e.target.value)}
            hint="Mahalle, cadde, bina ve kapı no ile birlikte gir."
          />

          <div className="bg-purple-50 border border-purple-200 rounded p-3 text-xs text-purple-900">
            <strong>Demo ödeme:</strong> e-posta adresinde "fail" geçiyorsa ya da tutar 100.000 TL'yi
            aşıyorsa payment-service ödemeyi reddeder ve saga sipariş durumunu <code>CANCELLED</code>'a
            çeker. Aksi halde ödeme onaylanır ve sepetin otomatik boşalır.
          </div>

          <Button type="submit" variant="secondary" size="lg" fullWidth loading={submitting}>
            {formatTRY(basket.total)} Tutarında Ödemeyi Başlat
          </Button>
        </form>
      </Card>

      <Card className="p-4 h-fit">
        <h2 className="font-bold mb-3">Sipariş Özeti ({basket.itemCount} ürün)</h2>
        <div className="space-y-2 mb-3">
          {basket.items.map((i) => (
            <div key={i.id} className="flex justify-between text-sm">
              <span className="truncate pr-2">
                {i.productName} <span className="text-gray-400">× {i.quantity}</span>
              </span>
              <span className="font-medium">{formatTRY(i.subtotal)}</span>
            </div>
          ))}
        </div>
        <div className="border-t pt-2 flex justify-between font-bold">
          <span>Toplam</span>
          <span className="text-n11-purple text-lg">{formatTRY(basket.total)}</span>
        </div>
      </Card>
    </div>
  );
}
