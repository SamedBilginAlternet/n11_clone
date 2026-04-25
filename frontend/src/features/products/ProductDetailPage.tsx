import { useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { ShoppingCart, Minus, Plus, Package } from 'lucide-react';
import { motion } from 'framer-motion';
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
import { Spinner } from '../../shared/ui/Spinner';
import { ReviewList } from '../reviews/ReviewList';

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
    if (!authed) { navigate('/login'); return; }
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
      toast.success('Urun sepete eklendi.');
    } catch (err) {
      toast.error(errorMessage(err, 'Ekleme basarisiz.'));
    } finally {
      setAdding(false);
    }
  };

  if (loading) return <div className="flex items-center justify-center py-20"><Spinner size={32} /></div>;
  if (error) return <div className="rounded-lg border border-destructive/20 bg-destructive/5 p-4 text-sm text-destructive">{errorMessage(error)}</div>;
  if (!product) return <div className="py-16 text-center text-muted-foreground">Urun bulunamadi.</div>;

  return (
    <motion.div initial={{ opacity: 0 }} animate={{ opacity: 1 }} className="space-y-6">
      <Card className="overflow-hidden">
        <div className="grid gap-0 md:grid-cols-2">
          {/* Image */}
          <div className="aspect-square overflow-hidden bg-muted">
            <img src={product.imageUrl} alt={product.name} className="h-full w-full object-cover" />
          </div>

          {/* Info */}
          <div className="flex flex-col gap-4 p-6 sm:p-8">
            {product.brand && (
              <div className="text-xs font-semibold uppercase tracking-wider text-muted-foreground">
                {product.brand}
              </div>
            )}
            <h1 className="text-2xl font-bold tracking-tight sm:text-3xl">{product.name}</h1>

            <div className="flex items-center gap-2 text-sm text-muted-foreground">
              <RatingStars value={product.rating} />
              <span className="font-medium">{product.rating.toFixed(1)}</span>
              <span>({product.reviewCount} degerlendirme)</span>
            </div>

            <p className="text-sm leading-relaxed text-muted-foreground">{product.description}</p>

            {/* Price */}
            <div className="mt-2 rounded-xl bg-muted/50 p-4">
              {product.discountPercentage > 0 && (
                <div className="mb-1 text-sm text-muted-foreground line-through">{formatTRY(product.price)}</div>
              )}
              <div className="text-3xl font-extrabold text-emerald-600">
                {formatTRY(product.discountedPrice)}
              </div>
              {product.discountPercentage > 0 && (
                <Badge tone="warning" className="mt-2">%{product.discountPercentage} INDIRIM</Badge>
              )}
            </div>

            {/* Quantity */}
            <div className="flex items-center gap-3">
              <span className="text-sm font-medium">Adet:</span>
              <div className="flex items-center rounded-lg border border-border">
                <button
                  onClick={() => setQuantity(Math.max(1, quantity - 1))}
                  className="flex h-9 w-9 items-center justify-center transition-colors hover:bg-muted"
                  disabled={quantity <= 1}
                >
                  <Minus className="h-4 w-4" />
                </button>
                <span className="w-10 text-center text-sm font-medium">{quantity}</span>
                <button
                  onClick={() => setQuantity(Math.min(product.stockQuantity, quantity + 1))}
                  className="flex h-9 w-9 items-center justify-center transition-colors hover:bg-muted"
                >
                  <Plus className="h-4 w-4" />
                </button>
              </div>
              <span className="flex items-center gap-1 text-xs text-muted-foreground">
                <Package className="h-3.5 w-3.5" /> Stok: {product.stockQuantity}
              </span>
            </div>

            {/* Add to cart */}
            <Button
              onClick={handleAdd}
              variant="success"
              size="lg"
              loading={adding}
              disabled={product.stockQuantity === 0}
              className="mt-2"
            >
              <ShoppingCart className="h-5 w-5" />
              {product.stockQuantity === 0 ? 'Stokta Yok' : 'Sepete Ekle'}
            </Button>
          </div>
        </div>
      </Card>

      {/* Reviews */}
      <ReviewList productId={product.id} />
    </motion.div>
  );
}
