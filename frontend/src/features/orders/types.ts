export type OrderStatus = 'PENDING' | 'PAID' | 'CANCELLED' | 'FAILED';

export interface OrderItem {
  id: number;
  productId: number;
  productName: string;
  productPrice: number;
  imageUrl: string | null;
  quantity: number;
  subtotal: number;
}

export interface Order {
  id: number;
  userEmail: string;
  status: OrderStatus;
  totalAmount: number;
  shippingAddress: string | null;
  failureReason: string | null;
  items: OrderItem[];
  createdAt: string;
}

export interface CheckoutItem {
  productId: number;
  productName: string;
  productPrice: number;
  imageUrl?: string;
  quantity: number;
}

export interface CheckoutRequest {
  shippingAddress: string;
  items: CheckoutItem[];
}

export interface Payment {
  id: number;
  transactionId: string;
  orderId: number;
  amount: number;
  status: 'SUCCEEDED' | 'FAILED';
  failureReason: string | null;
  createdAt: string;
}
