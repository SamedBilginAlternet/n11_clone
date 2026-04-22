import { apiFetch } from '../../shared/api/client';
import { SearchParams, SearchResponse } from './types';

export const searchApi = {
  search: (params: SearchParams) => {
    const qs = new URLSearchParams();
    (Object.entries(params) as [keyof SearchParams, unknown][]).forEach(([k, v]) => {
      if (v !== undefined && v !== null && v !== '') qs.set(k, String(v));
    });
    const suffix = qs.toString() ? `?${qs}` : '';
    return apiFetch<SearchResponse>(`/search${suffix}`, {}, { auth: false });
  },
};
