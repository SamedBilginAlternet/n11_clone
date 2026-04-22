import { apiFetch } from '../../shared/api/client';
import { Notification } from './types';

export const notificationApi = {
  list: () => apiFetch<Notification[]>('/notifications'),

  unreadCount: () => apiFetch<{ count: number }>('/notifications/unread-count'),

  markRead: (id: number) =>
    apiFetch<Notification>(`/notifications/${id}/read`, { method: 'PATCH' }),

  remove: (id: number) => apiFetch<void>(`/notifications/${id}`, { method: 'DELETE' }),
};
