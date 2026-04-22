import { apiFetch } from '../../shared/api/client';
import { AuthTokens } from '../../types';

export const authApi = {
  register: (body: { email: string; password: string; fullName: string }) =>
    apiFetch<AuthTokens>('/auth/register', { method: 'POST', body: JSON.stringify(body) }, { auth: false }),

  login: (body: { email: string; password: string }) =>
    apiFetch<AuthTokens>('/auth/login', { method: 'POST', body: JSON.stringify(body) }, { auth: false }),

  refresh: () =>
    apiFetch<AuthTokens>('/auth/refresh', { method: 'POST' }, { auth: false }),

  logout: () =>
    apiFetch<void>('/auth/logout', { method: 'POST' }, { auth: false }),
};
