import { Outlet } from 'react-router-dom';
import { Navbar } from './Navbar';
import { motion, AnimatePresence } from 'framer-motion';

export function Layout() {
  return (
    <div className="flex min-h-screen flex-col bg-background">
      <Navbar />
      <AnimatePresence mode="wait">
        <motion.main
          className="mx-auto w-full max-w-7xl flex-1 px-4 py-6 sm:px-6 lg:px-8"
          initial={{ opacity: 0, y: 12 }}
          animate={{ opacity: 1, y: 0 }}
          exit={{ opacity: 0, y: -12 }}
          transition={{ duration: 0.2 }}
        >
          <Outlet />
        </motion.main>
      </AnimatePresence>
      <footer className="border-t border-border bg-card mt-auto">
        <div className="mx-auto flex max-w-7xl flex-col gap-3 px-4 py-8 sm:flex-row sm:items-center sm:justify-between sm:px-6 lg:px-8">
          <div className="text-sm text-muted-foreground">&copy; 2026 n11 Clone &mdash; Egitim amacli demo.</div>
          <div className="flex flex-wrap gap-x-4 gap-y-1 text-xs text-muted-foreground/60">
            <span>10 Mikroservis</span>
            <span>Saga Pattern</span>
            <span>RabbitMQ</span>
            <span>JWT</span>
            <span>Spring Boot</span>
            <span>React</span>
          </div>
        </div>
      </footer>
    </div>
  );
}
