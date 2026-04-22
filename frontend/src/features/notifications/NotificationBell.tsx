import { useCallback, useEffect, useRef, useState } from 'react';
import { notificationApi } from './api';
import { Notification, NotificationType } from './types';
import { useAuthStore } from '../auth/store';
import { useNotificationSocket } from './useNotificationSocket';
import { formatDateTime } from '../../shared/utils/format';

const ICONS: Record<NotificationType, string> = {
  WELCOME: '👋',
  ORDER_CONFIRMED: '✅',
  ORDER_CANCELLED: '⚠️',
  SYSTEM: 'ℹ️',
};

export function NotificationBell() {
  const authed = useAuthStore((s) => s.isAuthenticated());
  const [open, setOpen] = useState(false);
  const [unread, setUnread] = useState(0);
  const [items, setItems] = useState<Notification[]>([]);
  const [loading, setLoading] = useState(false);
  const containerRef = useRef<HTMLDivElement>(null);

  // Fetch unread count once on mount (no polling — WebSocket handles updates)
  useEffect(() => {
    if (!authed) {
      setUnread(0);
      setItems([]);
      return;
    }
    notificationApi
      .unreadCount()
      .then((r) => setUnread(r.count))
      .catch(() => {});
  }, [authed]);

  // Real-time notifications via WebSocket
  const handleNewNotification = useCallback((n: Notification) => {
    setUnread((c) => c + 1);
    setItems((prev) => [n, ...prev]);
  }, []);

  useNotificationSocket(handleNewNotification);

  // Click outside to close
  useEffect(() => {
    if (!open) return;
    const onClick = (e: MouseEvent) => {
      if (containerRef.current && !containerRef.current.contains(e.target as Node)) {
        setOpen(false);
      }
    };
    window.addEventListener('click', onClick);
    return () => window.removeEventListener('click', onClick);
  }, [open]);

  const toggle = async () => {
    const next = !open;
    setOpen(next);
    if (next) {
      setLoading(true);
      try {
        const data = await notificationApi.list();
        setItems(data);
      } finally {
        setLoading(false);
      }
    }
  };

  const handleMarkRead = async (n: Notification) => {
    if (n.read) return;
    try {
      await notificationApi.markRead(n.id);
      setItems((list) => list.map((x) => (x.id === n.id ? { ...x, read: true } : x)));
      setUnread((c) => Math.max(0, c - 1));
    } catch {
      /* ignore */
    }
  };

  if (!authed) return null;

  return (
    <div ref={containerRef} className="relative">
      <button
        onClick={toggle}
        className="relative flex items-center px-3 py-1.5 rounded-md hover:bg-white/10"
        aria-label="Bildirimler"
      >
        🔔
        {unread > 0 && (
          <span className="absolute -top-1 -right-1 bg-red-500 text-xs text-white font-bold rounded-full w-5 h-5 flex items-center justify-center">
            {unread > 9 ? '9+' : unread}
          </span>
        )}
      </button>

      {open && (
        <div className="absolute right-0 mt-2 w-80 max-h-96 overflow-y-auto bg-white rounded-lg shadow-xl border border-gray-200 text-gray-900 z-30">
          <div className="px-3 py-2 border-b border-gray-100 flex items-center justify-between">
            <span className="font-semibold text-sm">Bildirimler</span>
            {unread > 0 && (
              <span className="text-xs text-n11-purple">{unread} okunmamış</span>
            )}
          </div>
          {loading ? (
            <div className="p-4 text-center text-sm text-gray-500">Yükleniyor...</div>
          ) : items.length === 0 ? (
            <div className="p-6 text-center text-sm text-gray-500">Bildirim yok.</div>
          ) : (
            items.map((n) => (
              <button
                key={n.id}
                onClick={() => void handleMarkRead(n)}
                className={`w-full text-left px-3 py-3 border-b border-gray-100 hover:bg-gray-50 flex gap-2 ${
                  !n.read ? 'bg-purple-50' : ''
                }`}
              >
                <span className="text-lg leading-none">{ICONS[n.type]}</span>
                <div className="flex-1 min-w-0">
                  <div className="font-medium text-sm flex items-center gap-1">
                    {n.title}
                    {!n.read && <span className="w-2 h-2 rounded-full bg-n11-purple" />}
                  </div>
                  <div className="text-xs text-gray-600 line-clamp-2 mt-0.5">{n.message}</div>
                  <div className="text-xs text-gray-400 mt-0.5">{formatDateTime(n.createdAt)}</div>
                </div>
              </button>
            ))
          )}
        </div>
      )}
    </div>
  );
}
