import { FormEvent, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { LogIn } from 'lucide-react';
import { motion } from 'framer-motion';
import { authApi } from './api';
import { useAuthStore } from './store';
import { Button } from '../../shared/ui/Button';
import { Card } from '../../shared/ui/Card';
import { Input } from '../../shared/ui/Input';
import { errorMessage } from '../../shared/api/problem';
import { useToast } from '../../shared/providers/ToastProvider';

export function LoginPage() {
  const navigate = useNavigate();
  const setTokens = useAuthStore((s) => s.setTokens);
  const toast = useToast();

  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [loading, setLoading] = useState(false);

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault();
    setLoading(true);
    try {
      const tokens = await authApi.login({ email, password });
      setTokens(tokens, email);
      toast.success('Giris basarili. Hos geldin!');
      navigate('/');
    } catch (err) {
      toast.error(errorMessage(err, 'Giris basarisiz.'));
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="flex min-h-[60vh] items-center justify-center">
      <motion.div
        initial={{ opacity: 0, y: 20 }}
        animate={{ opacity: 1, y: 0 }}
        className="w-full max-w-md"
      >
        <Card className="p-8">
          <div className="mb-6 text-center">
            <div className="mx-auto mb-3 flex h-14 w-14 items-center justify-center rounded-full bg-primary/10">
              <LogIn className="h-7 w-7 text-primary" />
            </div>
            <h1 className="text-2xl font-bold tracking-tight">Giris Yap</h1>
            <p className="mt-1 text-sm text-muted-foreground">
              Hesabin yoksa{' '}
              <Link className="font-semibold text-primary hover:underline" to="/register">
                buradan
              </Link>{' '}
              uye olabilirsin.
            </p>
          </div>

          <form onSubmit={handleSubmit} className="space-y-4">
            <Input
              name="email"
              type="email"
              label="E-posta"
              required
              placeholder="ornek@mail.com"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
            />
            <Input
              name="password"
              type="password"
              label="Sifre"
              required
              placeholder="********"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
            />
            <Button type="submit" fullWidth loading={loading} size="lg">
              <LogIn className="h-4 w-4" /> Giris Yap
            </Button>
          </form>
        </Card>
      </motion.div>
    </div>
  );
}
