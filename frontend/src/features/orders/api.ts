import { apiFetch } from '../../shared/api/client';
import { CheckoutRequest, Order, Payment } from './types';

export const orderApi = {
  checkout: (body: CheckoutRequest) =>
    apiFetch<Order>('/orders/checkout', { method: 'POST', body: JSON.stringify(body) }),

  list: () => apiFetch<Order[]>('/orders'),

  get: (id: number) => apiFetch<Order>(`/orders/${id}`),
};

export const paymentApi = {
  myPayments: () => apiFetch<Payment[]>('/payments'),

  getByOrder: (orderId: number) => apiFetch<Payment>(`/payments/order/${orderId}`),
};
