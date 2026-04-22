import { Client } from '@stomp/stompjs';
import { useEffect, useRef } from 'react';
import { useAuthStore } from '../auth/store';
import { Notification } from './types';

export function useNotificationSocket(onNotification: (n: Notification) => void) {
  const token = useAuthStore((s) => s.accessToken);
  const callbackRef = useRef(onNotification);
  callbackRef.current = onNotification;

  useEffect(() => {
    if (!token) return;

    const protocol = window.location.protocol === 'https:' ? 'wss' : 'ws';
    const wsUrl = `${protocol}://${window.location.host}/ws/notifications`;

    const client = new Client({
      brokerURL: wsUrl,
      connectHeaders: { Authorization: `Bearer ${token}` },
      reconnectDelay: 5000,
      heartbeatIncoming: 10000,
      heartbeatOutgoing: 10000,
      onConnect: () => {
        client.subscribe('/user/queue/notifications', (message) => {
          const notification: Notification = JSON.parse(message.body);
          callbackRef.current(notification);
        });
      },
    });

    client.activate();

    return () => {
      client.deactivate();
    };
  }, [token]);
}
