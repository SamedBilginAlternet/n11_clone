import { useSearchParams } from 'react-router-dom';
import { X } from 'lucide-react';
import { Card } from '../../shared/ui/Card';
import { formatTRY } from '../../shared/utils/format';
import { RatingStars } from '../../shared/ui/RatingStars';
import { SearchFacets } from './types';

interface Props {
  facets: SearchFacets | undefined;
}

export function FacetSidebar({ facets }: Props) {
  const [params, setParams] = useSearchParams();

  const setParam = (key: string, value: string | undefined) => {
    const next = new URLSearchParams(params);
    if (value === undefined || value === '') next.delete(key);
    else next.set(key, value);
    next.delete('page');
    setParams(next, { replace: false });
  };

  const activeCategory = params.get('category') ?? '';
  const activeBrand = params.get('brand') ?? '';
  const activeMinRating = params.get('minRating') ?? '';
  const minPrice = params.get('minPrice') ?? '';
  const maxPrice = params.get('maxPrice') ?? '';

  const hasAnyFilter = activeCategory || activeBrand || activeMinRating || minPrice || maxPrice;

  const clearAll = () => {
    const next = new URLSearchParams(params);
    ['category', 'brand', 'minRating', 'minPrice', 'maxPrice', 'page'].forEach((k) => next.delete(k));
    setParams(next);
  };

  const brandEntries = Object.entries(facets?.brands ?? {});
  const categoryEntries = Object.entries(facets?.categories ?? {});

  return (
    <aside className="space-y-4">
      {hasAnyFilter && (
        <button
          onClick={clearAll}
          className="flex items-center gap-1.5 text-sm font-medium text-primary hover:underline"
        >
          <X className="h-3.5 w-3.5" /> Tum filtreleri temizle
        </button>
      )}

      {categoryEntries.length > 0 && (
        <Card className="p-4">
          <h3 className="mb-3 text-sm font-semibold">Kategori</h3>
          <div className="space-y-1.5">
            {categoryEntries.map(([name, count]) => (
              <label key={name} className="flex cursor-pointer items-center gap-2 text-sm">
                <input
                  type="radio"
                  name="category-facet"
                  checked={activeCategory === name}
                  onChange={() => setParam('category', activeCategory === name ? undefined : name)}
                  className="accent-primary"
                />
                <span className="flex-1">{name}</span>
                <span className="text-xs text-muted-foreground">{count}</span>
              </label>
            ))}
          </div>
        </Card>
      )}

      {brandEntries.length > 0 && (
        <Card className="p-4">
          <h3 className="mb-3 text-sm font-semibold">Marka</h3>
          <div className="max-h-60 space-y-1.5 overflow-y-auto">
            {brandEntries.map(([name, count]) => (
              <label key={name} className="flex cursor-pointer items-center gap-2 text-sm">
                <input
                  type="radio"
                  name="brand-facet"
                  checked={activeBrand === name}
                  onChange={() => setParam('brand', activeBrand === name ? undefined : name)}
                  className="accent-primary"
                />
                <span className="flex-1">{name}</span>
                <span className="text-xs text-muted-foreground">{count}</span>
              </label>
            ))}
          </div>
        </Card>
      )}

      <Card className="p-4">
        <h3 className="mb-3 text-sm font-semibold">Fiyat</h3>
        {facets && (
          <div className="mb-2 text-xs text-muted-foreground">
            {formatTRY(facets.price.min)} &ndash; {formatTRY(facets.price.max)}
          </div>
        )}
        <div className="flex gap-2">
          <input
            type="number"
            min={0}
            placeholder="Min"
            value={minPrice}
            onChange={(e) => setParam('minPrice', e.target.value || undefined)}
            className="h-9 w-full rounded-lg border border-border bg-background px-2 text-sm focus:outline-none focus:ring-2 focus:ring-ring"
          />
          <input
            type="number"
            min={0}
            placeholder="Max"
            value={maxPrice}
            onChange={(e) => setParam('maxPrice', e.target.value || undefined)}
            className="h-9 w-full rounded-lg border border-border bg-background px-2 text-sm focus:outline-none focus:ring-2 focus:ring-ring"
          />
        </div>
      </Card>

      <Card className="p-4">
        <h3 className="mb-3 text-sm font-semibold">Puan</h3>
        <div className="space-y-1.5">
          {[4, 3, 2, 1].map((min) => (
            <label key={min} className="flex cursor-pointer items-center gap-2 text-sm">
              <input
                type="radio"
                name="rating-facet"
                checked={activeMinRating === String(min)}
                onChange={() => setParam('minRating', activeMinRating === String(min) ? undefined : String(min))}
                className="accent-primary"
              />
              <RatingStars value={min} size={12} />
              <span className="text-xs text-muted-foreground">ve uzeri</span>
            </label>
          ))}
        </div>
      </Card>
    </aside>
  );
}
