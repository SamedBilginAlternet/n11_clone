import { FormEvent, useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { CreditCard, MapPin, Info } from 'lucide-react';
import { motion } from 'framer-motion';
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
      toast.info('Sepet bos oldugu icin ana sayfaya yonlendirildin.');
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
      toast.success(`Siparis #${order.id} alindi — odeme isleniyor.`);
      setBasket(null);
      navigate(`/orders/${order.id}`);
    } catch (err) {
      toast.error(errorMessage(err, 'Siparis olusturulamadi.'));
    } finally {
      setSubmitting(false);
    }
  };

  if (loading) return <div className="flex justify-center py-20"><div className="h-8 w-8 animate-spin rounded-full border-4 border-primary border-t-transparent" /></div>;
  if (!basket || basket.items.length === 0) return null;

  return (
    <motion.div initial={{ opacity: 0 }} animate={{ opacity: 1 }} className="grid gap-6 lg:grid-cols-3">
      <Card className="p-6 sm:p-8 lg:col-span-2">
        <div className="mb-6 flex items-center gap-3">
          <div className="flex h-10 w-10 items-center justify-center rounded-full bg-primary/10">
            <CreditCard className="h-5 w-5 text-primary" />
          </div>
          <h1 className="text-xl font-bold">Odeme ve Teslimat</h1>
        </div>

        <form onSubmit={handleSubmit} className="space-y-5">
          <div className="flex items-start gap-2">
            <MapPin className="mt-1 h-4 w-4 shrink-0 text-muted-foreground" />
            <div className="flex-1">
              <Input
                name="address"
                label="Teslimat Adresi"
                required
                placeholder="Mahalle, cadde, bina ve kapi no..."
                value={address}
                onChange={(e) => setAddress(e.target.value)}
              />
            </div>
          </div>

          <div className="flex items-start gap-3 rounded-xl border border-violet-200 bg-violet-50 p-4 text-xs text-violet-900">
            <Info className="mt-0.5 h-4 w-4 shrink-0" />
            <div>
              <strong>Demo odeme:</strong> E-posta adresinde "fail" geciyorsa veya tutar 100.000 TL'yi
              asarsa payment-service odemeyi reddeder ve saga siparis durumunu CANCELLED'a cekerken,
              aksi halde odeme onaylanir.
            </div>
          </div>

          <Button type="submit" variant="secondary" size="lg" fullWidth loading={submitting}>
            <CreditCard className="h-5 w-5" />
            {formatTRY(basket.total)} Tutarinda Odemeyi Baslat
          </Button>
        </form>
      </Card>

      <Card className="h-fit p-6">
        <h2 className="mb-4 text-lg font-bold">Siparis Ozeti</h2>
        <div className="space-y-3">
          {basket.items.map((i) => (
            <div key={i.id} className="flex items-center justify-between gap-2 text-sm">
              <span className="truncate text-muted-foreground">
                {i.productName} <span className="text-xs">x {i.quantity}</span>
              </span>
              <span className="shrink-0 font-medium">{formatTRY(i.subtotal)}</span>
            </div>
          ))}
        </div>
        <div className="mt-4 border-t pt-4">
          <div className="flex items-center justify-between">
            <span className="text-lg font-bold">Toplam</span>
            <span className="text-2xl font-extrabold text-primary">{formatTRY(basket.total)}</span>
          </div>
        </div>
      </Card>
    </motion.div>
  );
}
