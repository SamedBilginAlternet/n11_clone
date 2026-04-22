import { useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { productApi } from '../api/products';
import { basketApi } from '../api/basket';
import { Product } from '../types';
import { useAuthStore } from '../stores/auth';
import { useBasketStore } from '../stores/basket';
import { ApiError } from '../api/client';

const formatTRY = (n: number) =>
  new Intl.NumberFormat('tr-TR', { style: 'currency', currency: 'TRY', maximumFractionDigits: 2 }).format(n);

export function ProductDetailPage() {
  const { slug } = useParams<{ slug: string }>();
  const navigate = useNavigate();
  const authed = useAuthStore((s) => s.isAuthenticated());
  const setBasket = useBasketStore((s) => s.setBasket);

  const [product, setProduct] = useState<Product | null>(null);
  const [quantity, setQuantity] = useState(1);
  const [loading, setLoading] = useState(true);
  const [adding, setAdding] = useState(false);
  const [message, setMessage] = useState<{ type: 'ok' | 'err'; text: string } | null>(null);

  useEffect(() => {
    if (!slug) return;
    setLoading(true);
    productApi
      .getBySlug(slug)
      .then(setProduct)
      .catch(() => setProduct(null))
      .finally(() => setLoading(false));
  }, [slug]);

  const handleAdd = async () => {
    if (!product) return;
    if (!authed) {
      navigate('/login');
      return;
    }
    setAdding(true);
    setMessage(null);
    try {
      const updated = await basketApi.addItem({
        productId: product.id,
        productName: product.name,
        productPrice: product.discountedPrice,
        imageUrl: product.imageUrl,
        quantity,
      });
      setBasket(updated);
      setMessage({ type: 'ok', text: 'Ürün sepete eklendi.' });
    } catch (err) {
      const text = err instanceof ApiError ? err.problem?.detail ?? err.message : 'Ekleme başarısız.';
      setMessage({ type: 'err', text });
    } finally {
      setAdding(false);
    }
  };

  if (loading) return <div className="text-gray-500">Yükleniyor...</div>;
  if (!product) return <div className="text-gray-500">Ürün bulunamadı.</div>;

  return (
    <div className="grid md:grid-cols-2 gap-8 bg-white rounded-lg border border-gray-200 p-6">
      <div className="aspect-square bg-gray-50 rounded overflow-hidden">
        <img src={product.imageUrl} alt={product.name} className="w-full h-full object-cover" />
      </div>

      <div className="flex flex-col gap-3">
        {product.brand && (
          <div className="text-xs uppercase tracking-wide text-gray-400 font-medium">{product.brand}</div>
        )}
        <h1 className="text-2xl font-bold">{product.name}</h1>
        <div className="flex items-center gap-2 text-sm text-gray-500">
          <span className="text-n11-orange">★</span>
          <span>{product.rating.toFixed(1)}</span>
          <span>({product.reviewCount} değerlendirme)</span>
        </div>

        <p className="text-gray-700 mt-2 text-sm leading-relaxed">{product.description}</p>

        <div className="mt-4">
          {product.discountPercentage > 0 && (
            <div className="text-sm text-gray-400 line-through">{formatTRY(product.price)}</div>
          )}
          <div className="text-3xl font-bold text-n11-purple">{formatTRY(product.discountedPrice)}</div>
          {product.discountPercentage > 0 && (
            <div className="inline-block mt-1 bg-n11-orange text-white text-xs font-bold px-2 py-0.5 rounded">
              %{product.discountPercentage} İNDİRİM
            </div>
          )}
        </div>

        <div className="flex items-center gap-3 mt-4">
          <label className="text-sm font-medium text-gray-700">Adet:</label>
          <input
            type="number"
            min={1}
            max={Math.max(1, product.stockQuantity)}
            value={quantity}
            onChange={(e) => setQuantity(Math.max(1, Number(e.target.value)))}
            className="w-20 border border-gray-300 rounded px-2 py-1"
          />
          <span className="text-xs text-gray-500">Stok: {product.stockQuantity}</span>
        </div>

        <button
          onClick={handleAdd}
          disabled={adding || product.stockQuantity === 0}
          className="mt-4 bg-n11-orange text-white py-3 rounded font-semibold hover:bg-orange-600 disabled:opacity-60"
        >
          {adding ? 'Ekleniyor...' : product.stockQuantity === 0 ? 'Stokta Yok' : 'Sepete Ekle'}
        </button>

        {message && (
          <div
            className={`mt-2 p-2 rounded text-sm ${
              message.type === 'ok' ? 'bg-green-50 text-green-700' : 'bg-red-50 text-red-700'
            }`}
          >
            {message.text}
          </div>
        )}
      </div>
    </div>
  );
}
