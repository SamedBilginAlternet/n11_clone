import { Outlet } from 'react-router-dom';
import { Navbar } from './Navbar';

export function Layout() {
  return (
    <div className="min-h-screen flex flex-col">
      <Navbar />
      <main className="flex-1 max-w-7xl w-full mx-auto px-4 py-6">
        <Outlet />
      </main>
      <footer className="bg-n11-dark text-gray-300 text-sm py-6 mt-12">
        <div className="max-w-7xl mx-auto px-4 flex flex-col md:flex-row justify-between gap-3">
          <div>© 2026 n11 Clone — Eğitim amaçlı demo.</div>
          <div className="text-xs opacity-70">
            Mikroservis mimarisi · Saga pattern · JWT · Spring Boot · React
          </div>
        </div>
      </footer>
    </div>
  );
}
