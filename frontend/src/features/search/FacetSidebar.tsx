import { useSearchParams } from 'react-router-dom';
import { Card } from '../../shared/ui/Card';
import { formatTRY } from '../../shared/utils/format';
import { RatingStars } from '../../shared/ui/RatingStars';
import { SearchFacets } from './types';

interface Props {
  facets: SearchFacets | undefined;
}

/**
 * URL is the source of truth for filter state — clicking a facet mutates the
 * query string, which re-runs the useApi search hook in SearchPage. No local
 * state needed here.
 */
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

  const hasAnyFilter =
    activeCategory || activeBrand || activeMinRating || minPrice || maxPrice;

  const clearAll = () => {
    const next = new URLSearchParams(params);
    ['category', 'brand', 'minRating', 'minPrice', 'maxPrice', 'page'].forEach((k) =>
      next.delete(k),
    );
    setParams(next);
  };

  const brandEntries = Object.entries(facets?.brands ?? {});
  const categoryEntries = Object.entries(facets?.categories ?? {});

  return (
    <aside className="space-y-3">
      {hasAnyFilter && (
        <button
          onClick={clearAll}
          className="text-sm text-n11-purple hover:underline font-medium"
        >
          ✕ Tüm filtreleri temizle
        </button>
      )}

      {categoryEntries.length > 0 && (
        <Card className="p-3">
          <h3 className="font-semibold text-sm mb-2">Kategori</h3>
          <div className="space-y-1">
            {categoryEntries.map(([name, count]) => (
              <label key={name} className="flex items-center gap-2 text-sm cursor-pointer">
                <input
                  type="radio"
                  name="category-facet"
                  checked={activeCategory === name}
                  onChange={() =>
                    setParam('category', activeCategory === name ? undefined : name)
                  }
                />
                <span className="flex-1">{name}</span>
                <span className="text-xs text-gray-400">{count}</span>
              </label>
            ))}
          </div>
        </Card>
      )}

      {brandEntries.length > 0 && (
        <Card className="p-3">
          <h3 className="font-semibold text-sm mb-2">Marka</h3>
          <div className="space-y-1 max-h-60 overflow-y-auto">
            {brandEntries.map(([name, count]) => (
              <label key={name} className="flex items-center gap-2 text-sm cursor-pointer">
                <input
                  type="radio"
                  name="brand-facet"
                  checked={activeBrand === name}
                  onChange={() => setParam('brand', activeBrand === name ? undefined : name)}
                />
                <span className="flex-1">{name}</span>
                <span className="text-xs text-gray-400">{count}</span>
              </label>
            ))}
          </div>
        </Card>
      )}

      <Card className="p-3">
        <h3 className="font-semibold text-sm mb-2">Fiyat</h3>
        {facets && (
          <div className="text-xs text-gray-500 mb-2">
            Ürünlerde: {formatTRY(facets.price.min)} – {formatTRY(facets.price.max)}
          </div>
        )}
        <div className="flex gap-2">
          <input
            type="number"
            min={0}
            placeholder="Min"
            value={minPrice}
            onChange={(e) => setParam('minPrice', e.target.value || undefined)}
            className="w-full border border-gray-300 rounded px-2 py-1 text-sm"
          />
          <input
            type="number"
            min={0}
            placeholder="Max"
            value={maxPrice}
            onChange={(e) => setParam('maxPrice', e.target.value || undefined)}
            className="w-full border border-gray-300 rounded px-2 py-1 text-sm"
          />
        </div>
      </Card>

      <Card className="p-3">
        <h3 className="font-semibold text-sm mb-2">Puan</h3>
        <div className="space-y-1">
          {[4, 3, 2, 1].map((min) => (
            <label key={min} className="flex items-center gap-2 text-sm cursor-pointer">
              <input
                type="radio"
                name="rating-facet"
                checked={activeMinRating === String(min)}
                onChange={() =>
                  setParam('minRating', activeMinRating === String(min) ? undefined : String(min))
                }
              />
              <RatingStars value={min} size={12} />
              <span className="text-xs text-gray-500">ve üzeri</span>
            </label>
          ))}
        </div>
      </Card>
    </aside>
  );
}
