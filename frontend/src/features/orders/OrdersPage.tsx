import { Link } from 'react-router-dom';
import { useApi } from '../../shared/hooks/useApi';
import { formatDateTime, formatTRY } from '../../shared/utils/format';
import { Card } from '../../shared/ui/Card';
import { orderApi } from './api';
import { OrderStatusBadge } from './OrderStatusBadge';
import { errorMessage } from '../../shared/api/problem';

export function OrdersPage() {
  const { data: orders, loading, error } = useApi(() => orderApi.list(), []);

  if (loading) return <div className="text-gray-500">Siparişler yükleniyor...</div>;
  if (error) return <div className="bg-red-50 text-red-700 p-3 rounded">{errorMessage(error)}</div>;

  if (!orders || orders.length === 0) {
    return (
      <Card className="p-12 text-center">
        <div className="text-6xl mb-3">📦</div>
        <h1 className="text-xl font-bold mb-1">Henüz siparişin yok</h1>
        <p className="text-gray-500 mb-4">Ürün eklediğin anda burada görünür.</p>
        <Link to="/" className="inline-block bg-n11-purple text-white px-4 py-2 rounded font-medium hover:bg-purple-700">
          Alışverişe Başla
        </Link>
      </Card>
    );
  }

  return (
    <div className="space-y-4">
      <h1 className="text-xl font-bold">Siparişlerim</h1>
      {orders.map((order) => (
        <Link key={order.id} to={`/orders/${order.id}`}>
          <Card className="p-4 flex items-center gap-4 hover:shadow-md transition-shadow mb-3">
            <div className="flex gap-1 -ml-1">
              {order.items.slice(0, 3).map((item) =>
                item.imageUrl ? (
                  <img
                    key={item.id}
                    src={item.imageUrl}
                    alt=""
                    className="w-12 h-12 object-cover rounded border border-gray-200"
                  />
                ) : null,
              )}
            </div>
            <div className="flex-1 min-w-0">
              <div className="flex items-center gap-2 text-sm text-gray-500">
                <span>Sipariş #{order.id}</span>
                <span>·</span>
                <span>{formatDateTime(order.createdAt)}</span>
              </div>
              <div className="text-sm font-medium line-clamp-1 mt-1">
                {order.items.map((i) => i.productName).join(', ')}
              </div>
              {order.failureReason && (
                <div className="text-xs text-red-600 mt-1">{order.failureReason}</div>
              )}
            </div>
            <div className="flex flex-col items-end gap-1">
              <OrderStatusBadge status={order.status} />
              <div className="font-bold text-n11-purple">{formatTRY(order.totalAmount)}</div>
            </div>
          </Card>
        </Link>
      ))}
    </div>
  );
}
