import { useCallback, useEffect, useRef, useState } from 'react';
import { Bell } from 'lucide-react';
import { notificationApi } from './api';
import { Notification, NotificationType } from './types';
import { useAuthStore } from '../auth/store';
import { useNotificationSocket } from './useNotificationSocket';
import { formatDateTime } from '../../shared/utils/format';
import { cn } from '../../shared/utils/cn';

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

  useEffect(() => {
    if (!authed) { setUnread(0); setItems([]); return; }
    notificationApi.unreadCount().then((r) => setUnread(r.count)).catch(() => {});
  }, [authed]);

  const handleNewNotification = useCallback((n: Notification) => {
    setUnread((c) => c + 1);
    setItems((prev) => [n, ...prev]);
  }, []);

  useNotificationSocket(handleNewNotification);

  useEffect(() => {
    if (!open) return;
    const onClick = (e: MouseEvent) => {
      if (containerRef.current && !containerRef.current.contains(e.target as Node)) setOpen(false);
    };
    window.addEventListener('click', onClick);
    return () => window.removeEventListener('click', onClick);
  }, [open]);

  const toggle = async () => {
    const next = !open;
    setOpen(next);
    if (next) {
      setLoading(true);
      try { setItems(await notificationApi.list()); } finally { setLoading(false); }
    }
  };

  const handleMarkRead = async (n: Notification) => {
    if (n.read) return;
    try {
      await notificationApi.markRead(n.id);
      setItems((list) => list.map((x) => (x.id === n.id ? { ...x, read: true } : x)));
      setUnread((c) => Math.max(0, c - 1));
    } catch { /* ignore */ }
  };

  if (!authed) return null;

  return (
    <div ref={containerRef} className="relative">
      <button
        onClick={toggle}
        className="relative flex h-10 w-10 items-center justify-center rounded-lg transition-colors hover:bg-white/10"
        aria-label="Bildirimler"
      >
        <Bell className="h-5 w-5" />
        {unread > 0 && (
          <span className="absolute -right-0.5 -top-0.5 flex h-5 w-5 items-center justify-center rounded-full bg-red-500 text-[10px] font-bold text-white shadow ring-2 ring-violet-700">
            {unread > 9 ? '9+' : unread}
          </span>
        )}
      </button>

      {open && (
        <div className="absolute right-0 z-50 mt-2 w-80 overflow-hidden rounded-xl border border-border bg-card shadow-2xl">
          <div className="flex items-center justify-between border-b px-4 py-3">
            <span className="text-sm font-semibold text-card-foreground">Bildirimler</span>
            {unread > 0 && (
              <span className="rounded-full bg-primary/10 px-2 py-0.5 text-xs font-medium text-primary">
                {unread} okunmamis
              </span>
            )}
          </div>
          <div className="max-h-80 overflow-y-auto">
            {loading ? (
              <div className="p-6 text-center text-sm text-muted-foreground">Yukleniyor...</div>
            ) : items.length === 0 ? (
              <div className="p-8 text-center text-sm text-muted-foreground">Bildirim yok.</div>
            ) : (
              items.map((n) => (
                <button
                  key={n.id}
                  onClick={() => void handleMarkRead(n)}
                  className={cn(
                    'flex w-full gap-3 border-b border-border/50 px-4 py-3 text-left transition-colors hover:bg-muted/50',
                    !n.read && 'bg-primary/5',
                  )}
                >
                  <span className="mt-0.5 text-lg leading-none">{ICONS[n.type]}</span>
                  <div className="min-w-0 flex-1">
                    <div className="flex items-center gap-1.5 text-sm font-medium">
                      {n.title}
                      {!n.read && <span className="h-2 w-2 rounded-full bg-primary" />}
                    </div>
                    <div className="mt-0.5 line-clamp-2 text-xs text-muted-foreground">{n.message}</div>
                    <div className="mt-1 text-[11px] text-muted-foreground/60">{formatDateTime(n.createdAt)}</div>
                  </div>
                </button>
              ))
            )}
          </div>
        </div>
      )}
    </div>
  );
}
