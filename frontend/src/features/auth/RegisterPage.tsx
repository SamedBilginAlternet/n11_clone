import { FormEvent, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { UserPlus } from 'lucide-react';
import { motion } from 'framer-motion';
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
      toast.success('Hesabin olusturuldu — sepet otomatik kuruluyor (saga).');
      navigate('/');
    } catch (err) {
      setFields(errorFields(err) ?? {});
      toast.error(errorMessage(err, 'Kayit basarisiz.'));
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
            <div className="mx-auto mb-3 flex h-14 w-14 items-center justify-center rounded-full bg-secondary/10">
              <UserPlus className="h-7 w-7 text-secondary" />
            </div>
            <h1 className="text-2xl font-bold tracking-tight">Uye Ol</h1>
            <p className="mt-1 text-sm text-muted-foreground">
              Hesabin varsa{' '}
              <Link className="font-semibold text-primary hover:underline" to="/login">
                giris yap
              </Link>
              .
            </p>
          </div>

          <form onSubmit={handleSubmit} className="space-y-4">
            <Input
              name="fullName"
              label="Ad Soyad"
              required
              placeholder="Adin Soyadin"
              value={fullName}
              onChange={(e) => setFullName(e.target.value)}
              error={fields.fullName}
            />
            <Input
              name="email"
              type="email"
              label="E-posta"
              required
              placeholder="ornek@mail.com"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              error={fields.email}
            />
            <Input
              name="password"
              type="password"
              label="Sifre"
              required
              placeholder="********"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              hint="En az 8 karakter, bir buyuk harf, bir rakam, bir ozel karakter."
              error={fields.password}
            />
            <Button type="submit" variant="secondary" fullWidth loading={loading} size="lg">
              <UserPlus className="h-4 w-4" /> Uye Ol
            </Button>

            <p className="text-center text-xs text-muted-foreground">
              Kayit olduğunda <code className="rounded bg-muted px-1.5 py-0.5 text-[11px]">UserRegistered</code> olayi
              yayinlanir; basket-service tuketip sana bos sepet olusturur.
            </p>
          </form>
        </Card>
      </motion.div>
    </div>
  );
}
