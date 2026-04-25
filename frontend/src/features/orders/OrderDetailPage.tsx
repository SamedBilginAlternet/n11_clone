import { useEffect, useRef, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { ArrowLeft, CheckCircle2, XCircle, Clock } from 'lucide-react';
import { motion } from 'framer-motion';
import { useApi } from '../../shared/hooks/useApi';
import { formatDateTime, formatTRY } from '../../shared/utils/format';
import { Card } from '../../shared/ui/Card';
import { Spinner } from '../../shared/ui/Spinner';
import { orderApi, paymentApi } from './api';
import { OrderStatusBadge } from './OrderStatusBadge';
import { Payment } from './types';
import { errorMessage } from '../../shared/api/problem';

export function OrderDetailPage() {
  const { id } = useParams<{ id: string }>();
  const orderId = Number(id);
  const { data: order, loading, error, setData } = useApi(
    () => orderApi.get(orderId),
    [orderId],
  );

  const [payment, setPayment] = useState<Payment | null>(null);
  const pollRef = useRef<number | null>(null);

  useEffect(() => {
    if (!order) return;
    if (order.status !== 'PENDING') {
      if (pollRef.current) { window.clearInterval(pollRef.current); pollRef.current = null; }
      paymentApi.getByOrder(order.id).then(setPayment).catch(() => setPayment(null));
      return;
    }
    pollRef.current = window.setInterval(() => {
      orderApi.get(order.id).then(setData).catch(() => {});
    }, 2000);
    return () => { if (pollRef.current) window.clearInterval(pollRef.current); };
  }, [order, setData]);

  if (loading) return <div className="flex justify-center py-20"><Spinner size={32} /></div>;
  if (error) return <div className="rounded-lg border border-destructive/20 bg-destructive/5 p-4 text-sm text-destructive">{errorMessage(error)}</div>;
  if (!order) return null;

  return (
    <motion.div initial={{ opacity: 0 }} animate={{ opacity: 1 }} className="grid gap-6 lg:grid-cols-3">
      <div className="space-y-4 lg:col-span-2">
        {/* Header */}
        <Card className="flex items-center justify-between p-5">
          <div>
            <div className="text-xs text-muted-foreground">Siparis No</div>
            <div className="text-2xl font-bold">#{order.id}</div>
            <div className="mt-1 text-xs text-muted-foreground">{formatDateTime(order.createdAt)}</div>
          </div>
          <OrderStatusBadge status={order.status} />
        </Card>

        {/* Status banners */}
        {order.status === 'PENDING' && (
          <Card className="flex items-center gap-4 border-blue-200 bg-blue-50 p-5">
            <div className="flex h-10 w-10 shrink-0 items-center justify-center rounded-full bg-blue-100">
              <Clock className="h-5 w-5 text-blue-600 animate-pulse" />
            </div>
            <div>
              <div className="font-semibold text-blue-900">Odemen isleniyor...</div>
              <div className="text-xs text-blue-800">
                payment-service siparisini kontrol ediyor. Bu ekran otomatik guncelleniyor.
              </div>
            </div>
          </Card>
        )}

        {order.status === 'PAID' && (
          <Card className="flex items-center gap-4 border-emerald-200 bg-emerald-50 p-5">
            <div className="flex h-10 w-10 shrink-0 items-center justify-center rounded-full bg-emerald-100">
              <CheckCircle2 className="h-5 w-5 text-emerald-600" />
            </div>
            <div>
              <div className="font-semibold text-emerald-900">Odeme basariyla tamamlandi</div>
              {payment && (
                <div className="mt-0.5 text-xs text-emerald-800">
                  Islem no: <code className="rounded bg-white px-1.5 py-0.5">{payment.transactionId}</code>
                </div>
              )}
            </div>
          </Card>
        )}

        {(order.status === 'CANCELLED' || order.status === 'FAILED') && (
          <Card className="flex items-center gap-4 border-red-200 bg-red-50 p-5">
            <div className="flex h-10 w-10 shrink-0 items-center justify-center rounded-full bg-red-100">
              <XCircle className="h-5 w-5 text-red-600" />
            </div>
            <div>
              <div className="font-semibold text-red-900">Siparis alinamadi</div>
              {order.failureReason && <div className="mt-0.5 text-xs text-red-800">{order.failureReason}</div>}
            </div>
          </Card>
        )}

        {/* Items */}
        <Card className="p-5">
          <h2 className="mb-4 font-semibold">Urunler</h2>
          <div className="divide-y">
            {order.items.map((item) => (
              <div key={item.id} className="flex items-center gap-4 py-3 first:pt-0 last:pb-0">
                {item.imageUrl && (
                  <img src={item.imageUrl} alt={item.productName} className="h-14 w-14 shrink-0 rounded-lg object-cover" />
                )}
                <div className="min-w-0 flex-1">
                  <div className="text-sm font-medium line-clamp-1">{item.productName}</div>
                  <div className="text-xs text-muted-foreground">
                    {formatTRY(item.productPrice)} x {item.quantity}
                  </div>
                </div>
                <div className="shrink-0 font-bold">{formatTRY(item.subtotal)}</div>
              </div>
            ))}
          </div>
        </Card>

        {order.shippingAddress && (
          <Card className="p-5">
            <h2 className="mb-1 font-semibold">Teslimat Adresi</h2>
            <div className="text-sm text-muted-foreground">{order.shippingAddress}</div>
          </Card>
        )}
      </div>

      {/* Summary sidebar */}
      <Card className="h-fit p-6">
        <h2 className="mb-4 text-lg font-bold">Siparis Ozeti</h2>
        <div className="space-y-2 text-sm">
          <div className="flex justify-between text-muted-foreground">
            <span>Urun adedi</span>
            <span>{order.items.reduce((s, i) => s + i.quantity, 0)}</span>
          </div>
        </div>
        <div className="mt-4 border-t pt-4">
          <div className="flex items-center justify-between">
            <span className="text-lg font-bold">Toplam</span>
            <span className="text-2xl font-extrabold text-primary">{formatTRY(order.totalAmount)}</span>
          </div>
        </div>
        <Link
          to="/orders"
          className="mt-6 flex items-center justify-center gap-1.5 text-sm font-medium text-primary hover:underline"
        >
          <ArrowLeft className="h-4 w-4" /> Tum siparislerim
        </Link>
      </Card>
    </motion.div>
  );
}
