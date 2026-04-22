import { createBrowserRouter, Navigate } from 'react-router-dom';
import { Layout } from '../layout/Layout';
import { HomePage } from '../features/products/HomePage';
import { ProductDetailPage } from '../features/products/ProductDetailPage';
import { LoginPage } from '../features/auth/LoginPage';
import { RegisterPage } from '../features/auth/RegisterPage';
import { BasketPage } from '../features/basket/BasketPage';
import { CheckoutPage } from '../features/orders/CheckoutPage';
import { OrdersPage } from '../features/orders/OrdersPage';
import { OrderDetailPage } from '../features/orders/OrderDetailPage';
import { SearchPage } from '../features/search/SearchPage';
import { useAuthStore } from '../features/auth/store';

function RequireAuth({ children }: { children: JSX.Element }) {
  const authed = useAuthStore((s) => s.isAuthenticated());
  return authed ? children : <Navigate to="/login" replace />;
}

export const router = createBrowserRouter([
  {
    path: '/',
    element: <Layout />,
    children: [
      { index: true, element: <HomePage /> },
      { path: 'search', element: <SearchPage /> },
      { path: 'login', element: <LoginPage /> },
      { path: 'register', element: <RegisterPage /> },
      { path: 'product/:slug', element: <ProductDetailPage /> },
      {
        path: 'basket',
        element: (
          <RequireAuth>
            <BasketPage />
          </RequireAuth>
        ),
      },
      {
        path: 'checkout',
        element: (
          <RequireAuth>
            <CheckoutPage />
          </RequireAuth>
        ),
      },
      {
        path: 'orders',
        element: (
          <RequireAuth>
            <OrdersPage />
          </RequireAuth>
        ),
      },
      {
        path: 'orders/:id',
        element: (
          <RequireAuth>
            <OrderDetailPage />
          </RequireAuth>
        ),
      },
    ],
  },
]);

export { RequireAuth };
