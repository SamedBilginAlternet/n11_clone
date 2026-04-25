import { Link } from 'react-router-dom';
import { Package, ChevronRight } from 'lucide-react';
import { motion } from 'framer-motion';
import { useApi } from '../../shared/hooks/useApi';
import { formatDateTime, formatTRY } from '../../shared/utils/format';
import { Card } from '../../shared/ui/Card';
import { Button } from '../../shared/ui/Button';
import { orderApi } from './api';
import { OrderStatusBadge } from './OrderStatusBadge';
import { errorMessage } from '../../shared/api/problem';

export function OrdersPage() {
  const { data: orders, loading, error } = useApi(() => orderApi.list(), []);

  if (loading) return <div className="flex justify-center py-20"><div className="h-8 w-8 animate-spin rounded-full border-4 border-primary border-t-transparent" /></div>;
  if (error) return <div className="rounded-lg border border-destructive/20 bg-destructive/5 p-4 text-sm text-destructive">{errorMessage(error)}</div>;

  if (!orders || orders.length === 0) {
    return (
      <Card className="mx-auto max-w-md p-12 text-center">
        <div className="mx-auto mb-4 flex h-20 w-20 items-center justify-center rounded-full bg-muted">
          <Package className="h-10 w-10 text-muted-foreground" />
        </div>
        <h1 className="text-xl font-bold">Henuz siparisin yok</h1>
        <p className="mt-1 text-sm text-muted-foreground">Urun ekledigin anda burada gorunur.</p>
        <Link to="/"><Button className="mt-6">Alisverise Basla</Button></Link>
      </Card>
    );
  }

  return (
    <div className="space-y-4">
      <h1 className="text-xl font-bold">Siparislerim</h1>
      {orders.map((order, i) => (
        <motion.div key={order.id} initial={{ opacity: 0, y: 10 }} animate={{ opacity: 1, y: 0 }} transition={{ delay: i * 0.05 }}>
          <Link to={`/orders/${order.id}`}>
            <Card className="flex items-center gap-4 p-4 transition-all hover:shadow-md hover:-translate-y-0.5">
              <div className="flex -space-x-2">
                {order.items.slice(0, 3).map((item) =>
                  item.imageUrl ? (
                    <img
                      key={item.id}
                      src={item.imageUrl}
                      alt=""
                      className="h-12 w-12 rounded-lg border-2 border-card object-cover"
                    />
                  ) : null,
                )}
              </div>
              <div className="min-w-0 flex-1">
                <div className="flex items-center gap-2 text-xs text-muted-foreground">
                  <span>Siparis #{order.id}</span>
                  <span>&middot;</span>
                  <span>{formatDateTime(order.createdAt)}</span>
                </div>
                <div className="mt-1 line-clamp-1 text-sm font-medium">
                  {order.items.map((i) => i.productName).join(', ')}
                </div>
                {order.failureReason && (
                  <div className="mt-1 text-xs text-destructive">{order.failureReason}</div>
                )}
              </div>
              <div className="flex shrink-0 flex-col items-end gap-1.5">
                <OrderStatusBadge status={order.status} />
                <div className="font-bold text-primary">{formatTRY(order.totalAmount)}</div>
              </div>
              <ChevronRight className="h-5 w-5 shrink-0 text-muted-foreground/50" />
            </Card>
          </Link>
        </motion.div>
      ))}
    </div>
  );
}
