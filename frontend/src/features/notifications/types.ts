export type NotificationType = 'WELCOME' | 'ORDER_CONFIRMED' | 'ORDER_CANCELLED' | 'SYSTEM';

export interface Notification {
  id: number;
  type: NotificationType;
  title: string;
  message: string;
  read: boolean;
  createdAt: string;
}
