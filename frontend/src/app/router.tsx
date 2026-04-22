import { createBrowserRouter, Navigate } from 'react-router-dom';
import { Layout } from '../layout/Layout';
import { HomePage } from '../features/products/HomePage';
import { ProductDetailPage } from '../features/products/ProductDetailPage';
import { LoginPage } from '../features/auth/LoginPage';
import { RegisterPage } from '../features/auth/RegisterPage';
import { BasketPage } from '../features/basket/BasketPage';
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
    ],
  },
]);

export { RequireAuth };
