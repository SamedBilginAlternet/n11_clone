import { Link } from 'react-router-dom';
import { ShoppingCart } from 'lucide-react';
import { Product } from '../../types';
import { formatTRY } from '../../shared/utils/format';
import { RatingStars } from '../../shared/ui/RatingStars';

export function ProductCard({ product }: { product: Product }) {
  const hasDiscount = product.discountPercentage > 0;
  return (
    <Link
      to={`/product/${product.slug}`}
      className="group relative flex flex-col overflow-hidden rounded-xl border border-border bg-card shadow-sm transition-all duration-300 hover:-translate-y-1 hover:shadow-xl"
    >
      {/* Image */}
      <div className="relative aspect-square overflow-hidden bg-muted">
        <img
          src={product.imageUrl}
          alt={product.name}
          loading="lazy"
          className="h-full w-full object-cover transition-transform duration-500 group-hover:scale-110"
        />
        {hasDiscount && (
          <span className="absolute left-2 top-2 rounded-full bg-destructive px-2.5 py-1 text-[11px] font-bold text-white shadow-lg">
            %{product.discountPercentage}
          </span>
        )}
        <div className="absolute inset-0 bg-gradient-to-t from-black/20 to-transparent opacity-0 transition-opacity group-hover:opacity-100" />
        <div className="absolute bottom-2 right-2 flex h-9 w-9 items-center justify-center rounded-full bg-primary text-primary-foreground opacity-0 shadow-lg transition-all duration-300 group-hover:opacity-100 group-hover:translate-y-0 translate-y-2">
          <ShoppingCart className="h-4 w-4" />
        </div>
      </div>

      {/* Content */}
      <div className="flex flex-1 flex-col gap-1 p-3">
        {product.brand && (
          <div className="text-[11px] font-semibold uppercase tracking-wider text-muted-foreground">
            {product.brand}
          </div>
        )}
        <div className="line-clamp-2 min-h-[2.5rem] text-sm font-medium text-foreground">
          {product.name}
        </div>
        <div className="mt-1 flex items-center gap-1 text-xs text-muted-foreground">
          <RatingStars value={product.rating} size={12} />
          <span>({product.reviewCount})</span>
        </div>
        <div className="mt-auto pt-2">
          {hasDiscount ? (
            <>
              <div className="text-xs text-muted-foreground line-through">{formatTRY(product.price)}</div>
              <div className="text-lg font-bold text-emerald-600">{formatTRY(product.discountedPrice)}</div>
            </>
          ) : (
            <div className="text-lg font-bold text-emerald-600">{formatTRY(product.price)}</div>
          )}
        </div>
      </div>
    </Link>
  );
}
