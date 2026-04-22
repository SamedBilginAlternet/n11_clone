import { apiFetch } from '../../shared/api/client';
import { Review, ReviewPage, ReviewStats } from './types';

export const reviewApi = {
  listByProduct: (productId: number) =>
    apiFetch<ReviewPage>(`/reviews/product/${productId}`, {}, { auth: false }),

  statsByProduct: (productId: number) =>
    apiFetch<ReviewStats>(`/reviews/product/${productId}/stats`, {}, { auth: false }),

  create: (body: { productId: number; rating: number; comment: string }) =>
    apiFetch<Review>('/reviews', { method: 'POST', body: JSON.stringify(body) }),

  deleteMine: (id: number) => apiFetch<void>(`/reviews/${id}`, { method: 'DELETE' }),

  mine: () => apiFetch<Review[]>('/reviews/mine'),
};
