import { createBrowserRouter, Navigate } from 'react-router-dom';
import { Layout } from './components/Layout';
import { HomePage } from './pages/HomePage';
import { LoginPage } from './pages/LoginPage';
import { RegisterPage } from './pages/RegisterPage';
import { BasketPage } from './pages/BasketPage';
import { ProductDetailPage } from './pages/ProductDetailPage';
import { useAuthStore } from './stores/auth';

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
