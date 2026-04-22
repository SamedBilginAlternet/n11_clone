import { FormEvent, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
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
      toast.success('Giriş başarılı. Hoş geldin!');
      navigate('/');
    } catch (err) {
      toast.error(errorMessage(err, 'Giriş başarısız.'));
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="max-w-md mx-auto">
      <Card className="p-6 mt-6">
        <h1 className="text-xl font-bold mb-1">Giriş Yap</h1>
        <p className="text-sm text-gray-500 mb-4">
          Hesabın yoksa{' '}
          <Link className="text-n11-purple font-medium" to="/register">
            buradan
          </Link>{' '}
          üye olabilirsin.
        </p>

        <form onSubmit={handleSubmit} className="space-y-4">
          <Input
            name="email"
            type="email"
            label="E-posta"
            required
            value={email}
            onChange={(e) => setEmail(e.target.value)}
          />
          <Input
            name="password"
            type="password"
            label="Şifre"
            required
            value={password}
            onChange={(e) => setPassword(e.target.value)}
          />
          <Button type="submit" fullWidth loading={loading}>
            Giriş Yap
          </Button>
        </form>
      </Card>
    </div>
  );
}
