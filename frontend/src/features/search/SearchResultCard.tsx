import { Link } from 'react-router-dom';
import { SearchProduct } from './types';
import { formatTRY } from '../../shared/utils/format';
import { RatingStars } from '../../shared/ui/RatingStars';

export function SearchResultCard({ product }: { product: SearchProduct }) {
  const hasDiscount = product.discountPercentage > 0;
  return (
    <Link
      to={`/product/${product.slug}`}
      className="group flex flex-col overflow-hidden rounded-xl border border-border bg-card shadow-sm transition-all duration-300 hover:-translate-y-1 hover:shadow-lg"
    >
      <div className="relative aspect-square overflow-hidden bg-muted">
        <img
          src={product.imageUrl}
          alt={product.name}
          loading="lazy"
          className="h-full w-full object-cover transition-transform duration-500 group-hover:scale-110"
        />
        {hasDiscount && (
          <span className="absolute left-2 top-2 rounded-full bg-destructive px-2.5 py-1 text-[11px] font-bold text-white shadow">
            %{product.discountPercentage}
          </span>
        )}
      </div>
      <div className="flex flex-1 flex-col gap-1 p-3">
        {product.brand && (
          <div className="text-[11px] font-semibold uppercase tracking-wider text-muted-foreground">
            {product.brand}
          </div>
        )}
        <div className="line-clamp-2 min-h-[2.5rem] text-sm font-medium text-foreground">{product.name}</div>
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
