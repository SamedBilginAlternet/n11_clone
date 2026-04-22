import { FormEvent, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { authApi } from './api';
import { useAuthStore } from './store';
import { Button } from '../../shared/ui/Button';
import { Card } from '../../shared/ui/Card';
import { Input } from '../../shared/ui/Input';
import { errorFields, errorMessage } from '../../shared/api/problem';
import { useToast } from '../../shared/providers/ToastProvider';

export function RegisterPage() {
  const navigate = useNavigate();
  const setTokens = useAuthStore((s) => s.setTokens);
  const toast = useToast();

  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [fullName, setFullName] = useState('');
  const [loading, setLoading] = useState(false);
  const [fields, setFields] = useState<Record<string, string>>({});

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault();
    setLoading(true);
    setFields({});
    try {
      const tokens = await authApi.register({ email, password, fullName });
      setTokens(tokens, email);
      toast.success('Hesabın oluşturuldu — sepet otomatik kuruluyor (saga).');
      navigate('/');
    } catch (err) {
      setFields(errorFields(err) ?? {});
      toast.error(errorMessage(err, 'Kayıt başarısız.'));
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="max-w-md mx-auto">
      <Card className="p-6 mt-6">
        <h1 className="text-xl font-bold mb-1">Üye Ol</h1>
        <p className="text-sm text-gray-500 mb-4">
          Hesabın varsa{' '}
          <Link className="text-n11-purple font-medium" to="/login">
            giriş yap
          </Link>
          .
        </p>

        <form onSubmit={handleSubmit} className="space-y-4">
          <Input
            name="fullName"
            label="Ad Soyad"
            required
            value={fullName}
            onChange={(e) => setFullName(e.target.value)}
            error={fields.fullName}
          />
          <Input
            name="email"
            type="email"
            label="E-posta"
            required
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            error={fields.email}
          />
          <Input
            name="password"
            type="password"
            label="Şifre"
            required
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            hint="En az 8 karakter · bir büyük harf · bir rakam · bir özel karakter."
            error={fields.password}
          />

          <Button type="submit" variant="secondary" fullWidth loading={loading}>
            Üye Ol
          </Button>

          <p className="text-xs text-gray-400">
            Kayıt olduğunda <code className="bg-gray-100 px-1 rounded">UserRegistered</code> olayı
            yayınlanır; basket-service tüketip sana boş sepet oluşturur (saga koreografisi).
          </p>
        </form>
      </Card>
    </div>
  );
}
