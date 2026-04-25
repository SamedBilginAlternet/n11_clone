import { Link, useNavigate } from 'react-router-dom';
import { useState } from 'react';
import { Search, ShoppingCart, Package, LogOut, Menu, X, User } from 'lucide-react';
import { useAuthStore } from '../features/auth/store';
import { useBasketStore } from '../features/basket/store';
import { authApi } from '../features/auth/api';
import { NotificationBell } from '../features/notifications/NotificationBell';
import { useToast } from '../shared/providers/ToastProvider';
import { cn } from '../shared/utils/cn';

export function Navbar() {
  const { email, clear, isAuthenticated } = useAuthStore();
  const { basket, setBasket } = useBasketStore();
  const navigate = useNavigate();
  const toast = useToast();
  const [query, setQuery] = useState('');
  const [mobileOpen, setMobileOpen] = useState(false);

  const handleLogout = async () => {
    try { await authApi.logout(); } catch { /* ignore */ }
    clear();
    setBasket(null);
    toast.info('Cikis yapildi.');
    navigate('/');
  };

  const authed = isAuthenticated();

  return (
    <header className="sticky top-0 z-50 border-b border-white/10 bg-gradient-to-r from-violet-700 via-purple-600 to-violet-700 text-white shadow-lg backdrop-blur supports-[backdrop-filter]:bg-violet-700/95">
      <div className="mx-auto flex h-16 max-w-7xl items-center gap-4 px-4">
        {/* Logo */}
        <Link to="/" className="flex shrink-0 items-center gap-1.5 text-2xl font-extrabold tracking-tight">
          <span className="text-3xl leading-none" role="img" aria-label="n11">🐞</span>
          n<span className="text-amber-400">11</span>
        </Link>

        {/* Search */}
        <form
          className="hidden flex-1 sm:flex max-w-2xl"
          onSubmit={(e) => {
            e.preventDefault();
            const q = query.trim();
            navigate(q ? `/search?q=${encodeURIComponent(q)}` : '/search');
          }}
        >
          <div className="relative flex-1">
            <Search className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-gray-400" />
            <input
              type="search"
              value={query}
              onChange={(e) => setQuery(e.target.value)}
              placeholder="Urun, kategori veya marka ara..."
              className="h-10 w-full rounded-lg bg-white/95 pl-10 pr-4 text-sm text-gray-900 placeholder-gray-400 shadow-inner transition-all focus:outline-none focus:ring-2 focus:ring-amber-400"
            />
          </div>
        </form>

        {/* Desktop nav */}
        <nav className="hidden items-center gap-1 md:flex">
          {authed ? (
            <>
              <span className="max-w-[140px] truncate px-2 text-sm text-white/80">{email}</span>
              <NavLink to="/orders"><Package className="h-4 w-4" /> Siparisler</NavLink>
              <NotificationBell />
              <NavLink to="/basket" className="relative">
                <ShoppingCart className="h-4 w-4" />
                Sepet
                {basket && basket.itemCount > 0 && (
                  <span className="absolute -right-1 -top-1 flex h-5 w-5 items-center justify-center rounded-full bg-amber-400 text-[10px] font-bold text-gray-900 shadow">
                    {basket.itemCount}
                  </span>
                )}
              </NavLink>
              <button onClick={handleLogout} className="flex items-center gap-1.5 rounded-lg px-3 py-2 text-sm transition-colors hover:bg-white/10">
                <LogOut className="h-4 w-4" /> Cikis
              </button>
            </>
          ) : (
            <>
              <NavLink to="/login"><User className="h-4 w-4" /> Giris Yap</NavLink>
              <Link
                to="/register"
                className="ml-1 rounded-lg bg-amber-400 px-4 py-2 text-sm font-semibold text-gray-900 shadow transition-all hover:bg-amber-300 hover:shadow-md active:scale-95"
              >
                Uye Ol
              </Link>
            </>
          )}
        </nav>

        {/* Mobile toggle */}
        <button
          className="ml-auto rounded-lg p-2 hover:bg-white/10 md:hidden"
          onClick={() => setMobileOpen(!mobileOpen)}
        >
          {mobileOpen ? <X className="h-5 w-5" /> : <Menu className="h-5 w-5" />}
        </button>
      </div>

      {/* Mobile menu */}
      <div
        className={cn(
          'overflow-hidden transition-all duration-300 md:hidden',
          mobileOpen ? 'max-h-96 border-t border-white/10' : 'max-h-0',
        )}
      >
        <div className="space-y-1 px-4 pb-4 pt-2">
          {/* Mobile search */}
          <form
            className="mb-2 sm:hidden"
            onSubmit={(e) => {
              e.preventDefault();
              const q = query.trim();
              navigate(q ? `/search?q=${encodeURIComponent(q)}` : '/search');
              setMobileOpen(false);
            }}
          >
            <div className="relative">
              <Search className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-gray-400" />
              <input
                type="search"
                value={query}
                onChange={(e) => setQuery(e.target.value)}
                placeholder="Ara..."
                className="h-10 w-full rounded-lg bg-white/95 pl-10 pr-4 text-sm text-gray-900 placeholder-gray-400"
              />
            </div>
          </form>

          {authed ? (
            <>
              <MobileLink to="/orders" onClick={() => setMobileOpen(false)}>
                <Package className="h-4 w-4" /> Siparislerim
              </MobileLink>
              <MobileLink to="/basket" onClick={() => setMobileOpen(false)}>
                <ShoppingCart className="h-4 w-4" /> Sepetim
                {basket && basket.itemCount > 0 && (
                  <span className="ml-auto rounded-full bg-amber-400 px-2 py-0.5 text-xs font-bold text-gray-900">
                    {basket.itemCount}
                  </span>
                )}
              </MobileLink>
              <button
                onClick={() => { handleLogout(); setMobileOpen(false); }}
                className="flex w-full items-center gap-2 rounded-lg px-3 py-2.5 text-sm hover:bg-white/10"
              >
                <LogOut className="h-4 w-4" /> Cikis Yap
              </button>
            </>
          ) : (
            <>
              <MobileLink to="/login" onClick={() => setMobileOpen(false)}>
                <User className="h-4 w-4" /> Giris Yap
              </MobileLink>
              <MobileLink to="/register" onClick={() => setMobileOpen(false)}>
                Uye Ol
              </MobileLink>
            </>
          )}
        </div>
      </div>
    </header>
  );
}

function NavLink({ to, className, children }: { to: string; className?: string; children: React.ReactNode }) {
  return (
    <Link to={to} className={cn('flex items-center gap-1.5 rounded-lg px-3 py-2 text-sm transition-colors hover:bg-white/10', className)}>
      {children}
    </Link>
  );
}

function MobileLink({ to, onClick, children }: { to: string; onClick?: () => void; children: React.ReactNode }) {
  return (
    <Link to={to} onClick={onClick} className="flex items-center gap-2 rounded-lg px-3 py-2.5 text-sm hover:bg-white/10">
      {children}
    </Link>
  );
}
