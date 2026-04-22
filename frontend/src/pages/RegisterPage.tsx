import { FormEvent, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { authApi } from '../api/auth';
import { ApiError } from '../api/client';
import { useAuthStore } from '../stores/auth';

export function RegisterPage() {
  const navigate = useNavigate();
  const setTokens = useAuthStore((s) => s.setTokens);

  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [fullName, setFullName] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [fields, setFields] = useState<Record<string, string>>({});

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault();
    setLoading(true);
    setError(null);
    setFields({});
    try {
      const tokens = await authApi.register({ email, password, fullName });
      setTokens(tokens, email);
      navigate('/');
    } catch (err) {
      if (err instanceof ApiError) {
        setError(err.problem?.detail ?? 'Kayıt başarısız.');
        if (err.problem?.fields) setFields(err.problem.fields);
      } else {
        setError('Beklenmeyen bir hata oluştu.');
      }
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="max-w-md mx-auto">
      <div className="bg-white rounded-lg shadow-sm border border-gray-200 p-6 mt-6">
        <h1 className="text-xl font-bold mb-1">Üye Ol</h1>
        <p className="text-sm text-gray-500 mb-4">
          Hesabınız varsa <Link className="text-n11-purple font-medium" to="/login">giriş yapın</Link>.
        </p>

        <form onSubmit={handleSubmit} className="space-y-4">
          <label className="block">
            <span className="text-sm font-medium text-gray-700">Ad Soyad</span>
            <input
              type="text"
              required
              value={fullName}
              onChange={(e) => setFullName(e.target.value)}
              className="mt-1 w-full rounded border border-gray-300 px-3 py-2 focus:outline-none focus:ring-2 focus:ring-n11-purple"
            />
            {fields.fullName && <p className="text-red-600 text-xs mt-1">{fields.fullName}</p>}
          </label>
          <label className="block">
            <span className="text-sm font-medium text-gray-700">E-posta</span>
            <input
              type="email"
              required
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              className="mt-1 w-full rounded border border-gray-300 px-3 py-2 focus:outline-none focus:ring-2 focus:ring-n11-purple"
            />
            {fields.email && <p className="text-red-600 text-xs mt-1">{fields.email}</p>}
          </label>
          <label className="block">
            <span className="text-sm font-medium text-gray-700">Şifre</span>
            <input
              type="password"
              required
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              className="mt-1 w-full rounded border border-gray-300 px-3 py-2 focus:outline-none focus:ring-2 focus:ring-n11-purple"
            />
            <p className="text-xs text-gray-500 mt-1">
              En az 8 karakter · bir büyük harf · bir rakam · bir özel karakter.
            </p>
            {fields.password && <p className="text-red-600 text-xs mt-1">{fields.password}</p>}
          </label>

          {error && <div className="bg-red-50 text-red-700 p-3 rounded text-sm">{error}</div>}

          <button
            type="submit"
            disabled={loading}
            className="w-full bg-n11-orange text-white py-2 rounded font-medium hover:bg-orange-600 disabled:opacity-60"
          >
            {loading ? 'Hesap oluşturuluyor...' : 'Üye Ol'}
          </button>

          <p className="text-xs text-gray-400">
            Kayıt başarılı olduğunda auth-service arka planda bir
            <code className="bg-gray-100 px-1 mx-1 rounded">UserRegistered</code>
            eventi yayınlar; basket-service bunu tüketip size boş bir sepet oluşturur (saga).
          </p>
        </form>
      </div>
    </div>
  );
}
