import { apiFetch } from './client';
import { Basket } from '../types';

export const basketApi = {
  get: () => apiFetch<Basket>('/basket'),

  addItem: (item: {
    productId: number;
    productName: string;
    productPrice: number;
    imageUrl?: string;
    quantity: number;
  }) => apiFetch<Basket>('/basket/items', { method: 'POST', body: JSON.stringify(item) }),

  updateItem: (itemId: number, quantity: number) =>
    apiFetch<Basket>(`/basket/items/${itemId}`, { method: 'PUT', body: JSON.stringify({ quantity }) }),

  removeItem: (itemId: number) =>
    apiFetch<Basket>(`/basket/items/${itemId}`, { method: 'DELETE' }),

  clear: () => apiFetch<void>('/basket', { method: 'DELETE' }),
};
