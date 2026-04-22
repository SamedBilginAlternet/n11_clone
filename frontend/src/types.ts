export interface Product {
  id: number;
  name: string;
  slug: string;
  description: string;
  price: number;
  discountedPrice: number;
  discountPercentage: number;
  stockQuantity: number;
  imageUrl: string;
  category: string;
  brand: string;
  rating: number;
  reviewCount: number;
}

export interface PageResult<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}

export interface Category {
  slug: string;
  name: string;
  icon: string;
}

export interface BasketItem {
  id: number;
  productId: number;
  productName: string;
  productPrice: number;
  imageUrl: string | null;
  quantity: number;
  subtotal: number;
}

export interface Basket {
  id: number;
  userEmail: string;
  items: BasketItem[];
  total: number;
  itemCount: number;
}

export interface AuthTokens {
  accessToken: string;
  tokenType: string;
  expiresIn: number;
}

export interface ProblemDetail {
  type?: string;
  title?: string;
  status: number;
  detail?: string;
  instance?: string;
  fields?: Record<string, string>;
}
