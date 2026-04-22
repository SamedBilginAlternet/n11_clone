import { Link, useNavigate } from 'react-router-dom';
import { useAuthStore } from '../stores/auth';
import { useBasketStore } from '../stores/basket';
import { authApi } from '../api/auth';
import { useState } from 'react';

export function Navbar() {
  const { email, refreshToken, clear, isAuthenticated } = useAuthStore();
  const { basket, setBasket } = useBasketStore();
  const navigate = useNavigate();
  const [query, setQuery] = useState('');

  const handleLogout = async () => {
    try {
      if (refreshToken) await authApi.logout(refreshToken);
    } catch {
      /* ignore — clear local regardless */
    }
    clear();
    setBasket(null);
    navigate('/');
  };

  return (
    <header className="n11-gradient text-white shadow-md sticky top-0 z-20">
      <div className="max-w-7xl mx-auto px-4 py-3 flex items-center gap-4">
        <Link to="/" className="text-2xl font-bold tracking-tight">
          n<span className="text-n11-orange">11</span>
        </Link>

        <form
          className="flex-1 max-w-2xl"
          onSubmit={(e) => {
            e.preventDefault();
            if (query.trim()) navigate(`/?q=${encodeURIComponent(query.trim())}`);
          }}
        >
          <input
            type="search"
            value={query}
            onChange={(e) => setQuery(e.target.value)}
            placeholder="Ürün, kategori veya marka ara..."
            className="w-full rounded-md px-4 py-2 text-gray-900 placeholder-gray-400 focus:outline-none focus:ring-2 focus:ring-n11-orange"
          />
        </form>

        <nav className="flex items-center gap-4 text-sm">
          {isAuthenticated() ? (
            <>
              <span className="hidden md:inline opacity-90">{email}</span>
              <Link
                to="/basket"
                className="relative flex items-center gap-1 px-3 py-1.5 rounded-md hover:bg-white/10"
              >
                🛒 Sepet
                {basket && basket.itemCount > 0 && (
                  <span className="absolute -top-1 -right-1 bg-n11-orange text-xs text-white font-bold rounded-full w-5 h-5 flex items-center justify-center">
                    {basket.itemCount}
                  </span>
                )}
              </Link>
              <button
                onClick={handleLogout}
                className="px-3 py-1.5 rounded-md hover:bg-white/10"
              >
                Çıkış
              </button>
            </>
          ) : (
            <>
              <Link to="/login" className="px-3 py-1.5 rounded-md hover:bg-white/10">
                Giriş Yap
              </Link>
              <Link
                to="/register"
                className="px-3 py-1.5 bg-n11-orange text-white rounded-md hover:bg-orange-500 font-medium"
              >
                Üye Ol
              </Link>
            </>
          )}
        </nav>
      </div>
    </header>
  );
}
