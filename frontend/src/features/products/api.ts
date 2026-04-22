import { apiFetch } from '../../shared/api/client';
import { Category, PageResult, Product } from '../../types';

export const productApi = {
  list: (params: { category?: string; q?: string; page?: number; size?: number } = {}) => {
    const qs = new URLSearchParams();
    if (params.category) qs.set('category', params.category);
    if (params.q) qs.set('q', params.q);
    if (params.page != null) qs.set('page', String(params.page));
    if (params.size != null) qs.set('size', String(params.size));
    const suffix = qs.toString() ? `?${qs}` : '';
    return apiFetch<PageResult<Product>>(`/products${suffix}`, {}, { auth: false });
  },

  getById: (id: number) => apiFetch<Product>(`/products/${id}`, {}, { auth: false }),

  getBySlug: (slug: string) => apiFetch<Product>(`/products/slug/${slug}`, {}, { auth: false }),

  categories: () => apiFetch<Category[]>('/products/categories', {}, { auth: false }),
};
