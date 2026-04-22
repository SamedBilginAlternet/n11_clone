import { useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { productApi } from './api';
import { basketApi } from '../basket/api';
import { useAuthStore } from '../auth/store';
import { useBasketStore } from '../basket/store';
import { useApi } from '../../shared/hooks/useApi';
import { errorMessage } from '../../shared/api/problem';
import { useToast } from '../../shared/providers/ToastProvider';
import { formatTRY } from '../../shared/utils/format';
import { Badge } from '../../shared/ui/Badge';
import { Button } from '../../shared/ui/Button';
import { Card } from '../../shared/ui/Card';
import { RatingStars } from '../../shared/ui/RatingStars';

export function ProductDetailPage() {
  const { slug } = useParams<{ slug: string }>();
  const navigate = useNavigate();
  const authed = useAuthStore((s) => s.isAuthenticated());
  const setBasket = useBasketStore((s) => s.setBasket);
  const toast = useToast();

  const { data: product, loading, error } = useApi(
    () => productApi.getBySlug(slug!),
    [slug],
    { enabled: !!slug },
  );

  const [quantity, setQuantity] = useState(1);
  const [adding, setAdding] = useState(false);

  const handleAdd = async () => {
    if (!product) return;
    if (!authed) {
      navigate('/login');
      return;
    }
    setAdding(true);
    try {
      const updated = await basketApi.addItem({
        productId: product.id,
        productName: product.name,
        productPrice: product.discountedPrice,
        imageUrl: product.imageUrl,
        quantity,
      });
      setBasket(updated);
      toast.success('Ürün sepete eklendi.');
    } catch (err) {
      toast.error(errorMessage(err, 'Ekleme başarısız.'));
    } finally {
      setAdding(false);
    }
  };

  if (loading) return <div className="text-gray-500">Yükleniyor...</div>;
  if (error) return <div className="bg-red-50 text-red-700 p-3 rounded">{errorMessage(error)}</div>;
  if (!product) return <div className="text-gray-500">Ürün bulunamadı.</div>;

  return (
    <Card className="grid md:grid-cols-2 gap-8 p-6">
      <div className="aspect-square bg-gray-50 rounded overflow-hidden">
        <img src={product.imageUrl} alt={product.name} className="w-full h-full object-cover" />
      </div>

      <div className="flex flex-col gap-3">
        {product.brand && (
          <div className="text-xs uppercase tracking-wide text-gray-400 font-medium">
            {product.brand}
          </div>
        )}
        <h1 className="text-2xl font-bold">{product.name}</h1>
        <div className="flex items-center gap-2 text-sm text-gray-500">
          <RatingStars value={product.rating} />
          <span>{product.rating.toFixed(1)}</span>
          <span>({product.reviewCount} değerlendirme)</span>
        </div>

        <p className="text-gray-700 mt-2 text-sm leading-relaxed">{product.description}</p>

        <div className="mt-4">
          {product.discountPercentage > 0 && (
            <div className="text-sm text-gray-400 line-through">{formatTRY(product.price)}</div>
          )}
          <div className="text-3xl font-bold text-n11-purple">
            {formatTRY(product.discountedPrice)}
          </div>
          {product.discountPercentage > 0 && (
            <Badge tone="warning" className="mt-1">
              %{product.discountPercentage} İNDİRİM
            </Badge>
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

        <Button
          onClick={handleAdd}
          variant="secondary"
          size="lg"
          loading={adding}
          disabled={product.stockQuantity === 0}
        >
          {product.stockQuantity === 0 ? 'Stokta Yok' : 'Sepete Ekle'}
        </Button>
      </div>
    </Card>
  );
}
