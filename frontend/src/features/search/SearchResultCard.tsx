import { Link } from 'react-router-dom';
import { SearchProduct } from './types';
import { formatTRY } from '../../shared/utils/format';
import { RatingStars } from '../../shared/ui/RatingStars';

export function SearchResultCard({ product }: { product: SearchProduct }) {
  const hasDiscount = product.discountPercentage > 0;
  return (
    <Link
      to={`/product/${product.slug}`}
      className="bg-white rounded-lg border border-gray-200 hover:shadow-lg transition-shadow overflow-hidden flex flex-col group"
    >
      <div className="relative aspect-square bg-gray-50">
        <img
          src={product.imageUrl}
          alt={product.name}
          loading="lazy"
          className="w-full h-full object-cover group-hover:scale-105 transition-transform"
        />
        {hasDiscount && (
          <span className="absolute top-2 left-2 bg-n11-orange text-white text-xs font-bold px-2 py-0.5 rounded">
            %{product.discountPercentage} İNDİRİM
          </span>
        )}
      </div>
      <div className="p-3 flex flex-col flex-1 gap-1">
        {product.brand && (
          <div className="text-[11px] uppercase tracking-wide text-gray-400 font-medium">
            {product.brand}
          </div>
        )}
        <div className="text-sm text-gray-900 line-clamp-2 min-h-[2.5rem]">{product.name}</div>
        <div className="flex items-center gap-1 text-xs text-gray-500 mt-1">
          <RatingStars value={product.rating} size={12} />
          <span>({product.reviewCount})</span>
        </div>
        <div className="mt-auto pt-2">
          {hasDiscount ? (
            <>
              <div className="text-xs text-gray-400 line-through">{formatTRY(product.price)}</div>
              <div className="text-lg font-bold text-n11-purple">{formatTRY(product.discountedPrice)}</div>
            </>
          ) : (
            <div className="text-lg font-bold text-n11-purple">{formatTRY(product.price)}</div>
          )}
        </div>
      </div>
    </Link>
  );
}
