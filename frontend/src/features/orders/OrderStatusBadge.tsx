import { Badge } from '../../shared/ui/Badge';
import { OrderStatus } from './types';

const LABELS: Record<OrderStatus, string> = {
  PENDING: 'İşleniyor',
  PAID: 'Onaylandı',
  CANCELLED: 'İptal Edildi',
  FAILED: 'Başarısız',
};

const TONES: Record<OrderStatus, 'info' | 'success' | 'warning' | 'danger'> = {
  PENDING: 'info',
  PAID: 'success',
  CANCELLED: 'warning',
  FAILED: 'danger',
};

export function OrderStatusBadge({ status }: { status: OrderStatus }) {
  return <Badge tone={TONES[status]}>{LABELS[status]}</Badge>;
}
