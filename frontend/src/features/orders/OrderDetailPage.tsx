import { useEffect, useRef, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
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
  const { data: order, loading, error, refetch, setData } = useApi(
    () => orderApi.get(orderId),
    [orderId],
  );

  const [payment, setPayment] = useState<Payment | null>(null);
  const pollRef = useRef<number | null>(null);

  // Poll while PENDING — saga updates the status out-of-band via events.
  useEffect(() => {
    if (!order) return;
    if (order.status !== 'PENDING') {
      if (pollRef.current) {
        window.clearInterval(pollRef.current);
        pollRef.current = null;
      }
      // Also fetch payment record so we can show the transaction ID.
      paymentApi.getByOrder(order.id).then(setPayment).catch(() => setPayment(null));
      return;
    }

    pollRef.current = window.setInterval(() => {
      orderApi.get(order.id).then(setData).catch(() => {});
    }, 2000);

    return () => {
      if (pollRef.current) window.clearInterval(pollRef.current);
    };
  }, [order, setData]);

  if (loading) return <div className="text-gray-500">Sipariş yükleniyor...</div>;
  if (error) return <div className="bg-red-50 text-red-700 p-3 rounded">{errorMessage(error)}</div>;
  if (!order) return null;

  return (
    <div className="grid md:grid-cols-3 gap-6">
      <div className="md:col-span-2 space-y-4">
        <Card className="p-4 flex items-center justify-between">
          <div>
            <div className="text-xs text-gray-500">Sipariş No</div>
            <div className="font-bold text-lg">#{order.id}</div>
            <div className="text-xs text-gray-500 mt-1">{formatDateTime(order.createdAt)}</div>
          </div>
          <OrderStatusBadge status={order.status} />
        </Card>

        {order.status === 'PENDING' && (
          <Card className="p-4 flex items-center gap-3 bg-blue-50 border-blue-200">
            <Spinner className="text-n11-purple" />
            <div>
              <div className="font-semibold text-blue-900">Ödemen işleniyor...</div>
              <div className="text-xs text-blue-800">
                payment-service siparişini kontrol ediyor, sonuç saga üzerinden gelecek. Bu ekran
                otomatik güncelleniyor.
              </div>
            </div>
          </Card>
        )}

        {order.status === 'PAID' && (
          <Card className="p-4 bg-green-50 border-green-200 text-green-900">
            <div className="font-semibold">Ödemen başarıyla tamamlandı ✓</div>
            {payment && (
              <div className="text-xs mt-1">
                İşlem no: <code className="bg-white px-1 rounded">{payment.transactionId}</code>
              </div>
            )}
          </Card>
        )}

        {(order.status === 'CANCELLED' || order.status === 'FAILED') && (
          <Card className="p-4 bg-red-50 border-red-200 text-red-900">
            <div className="font-semibold">Sipariş alınamadı</div>
            {order.failureReason && <div className="text-xs mt-1">{order.failureReason}</div>}
            <button
              onClick={() => void refetch()}
              className="text-xs underline text-red-700 mt-2"
            >
              Tekrar dene
            </button>
          </Card>
        )}

        <Card className="p-4">
          <h2 className="font-semibold mb-2">Ürünler</h2>
          <div className="space-y-3">
            {order.items.map((item) => (
              <div key={item.id} className="flex gap-3 pb-3 border-b last:border-b-0 last:pb-0">
                {item.imageUrl && (
                  <img src={item.imageUrl} alt={item.productName} className="w-14 h-14 rounded object-cover" />
                )}
                <div className="flex-1 min-w-0">
                  <div className="text-sm font-medium line-clamp-1">{item.productName}</div>
                  <div className="text-xs text-gray-500">
                    {formatTRY(item.productPrice)} × {item.quantity}
                  </div>
                </div>
                <div className="font-bold">{formatTRY(item.subtotal)}</div>
              </div>
            ))}
          </div>
        </Card>

        {order.shippingAddress && (
          <Card className="p-4">
            <h2 className="font-semibold mb-1">Teslimat Adresi</h2>
            <div className="text-sm text-gray-700">{order.shippingAddress}</div>
          </Card>
        )}
      </div>

      <Card className="p-4 h-fit">
        <h2 className="font-bold mb-3">Sipariş Özeti</h2>
        <div className="flex justify-between text-sm text-gray-600 mb-1">
          <span>Ürün adedi</span>
          <span>{order.items.reduce((s, i) => s + i.quantity, 0)}</span>
        </div>
        <div className="border-t pt-2 mt-2 flex justify-between font-bold">
          <span>Toplam</span>
          <span className="text-n11-purple text-lg">{formatTRY(order.totalAmount)}</span>
        </div>
        <Link
          to="/orders"
          className="block mt-4 text-center text-sm text-n11-purple font-medium hover:underline"
        >
          ← Tüm siparişlerim
        </Link>
      </Card>
    </div>
  );
}
