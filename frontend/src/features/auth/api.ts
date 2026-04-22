import { apiFetch } from './client';
import { AuthTokens } from '../types';

export const authApi = {
  register: (body: { email: string; password: string; fullName: string }) =>
    apiFetch<AuthTokens>('/auth/register', { method: 'POST', body: JSON.stringify(body) }, { auth: false }),

  login: (body: { email: string; password: string }) =>
    apiFetch<AuthTokens>('/auth/login', { method: 'POST', body: JSON.stringify(body) }, { auth: false }),

  refresh: (refreshToken: string) =>
    apiFetch<AuthTokens>('/auth/refresh', { method: 'POST', body: JSON.stringify({ refreshToken }) }, { auth: false }),

  logout: (refreshToken: string) =>
    apiFetch<void>('/auth/logout', { method: 'POST', body: JSON.stringify({ refreshToken }) }, { auth: false }),
};
