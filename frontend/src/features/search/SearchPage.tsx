import { useSearchParams } from 'react-router-dom';
import { useApi } from '../../shared/hooks/useApi';
import { errorMessage } from '../../shared/api/problem';
import { searchApi } from './api';
import { SearchResultCard } from './SearchResultCard';
import { FacetSidebar } from './FacetSidebar';
import { SearchParams } from './types';

export function SearchPage() {
  const [params, setParams] = useSearchParams();

  const parsed: SearchParams = {
    q: params.get('q') ?? undefined,
    category: params.get('category') ?? undefined,
    brand: params.get('brand') ?? undefined,
    minPrice: params.get('minPrice') ? Number(params.get('minPrice')) : undefined,
    maxPrice: params.get('maxPrice') ? Number(params.get('maxPrice')) : undefined,
    minRating: params.get('minRating') ? Number(params.get('minRating')) : undefined,
    sort: (params.get('sort') as SearchParams['sort']) ?? 'relevance',
    page: params.get('page') ? Number(params.get('page')) : 0,
    size: 24,
  };

  const { data, loading, error } = useApi(
    () => searchApi.search(parsed),
    [
      parsed.q,
      parsed.category,
      parsed.brand,
      parsed.minPrice,
      parsed.maxPrice,
      parsed.minRating,
      parsed.sort,
      parsed.page,
    ],
  );

  const setSort = (sort: string) => {
    const next = new URLSearchParams(params);
    next.set('sort', sort);
    next.delete('page');
    setParams(next);
  };

  const setPage = (page: number) => {
    const next = new URLSearchParams(params);
    next.set('page', String(page));
    setParams(next);
  };

  const heading = parsed.q ? `"${parsed.q}" için sonuçlar` : 'Tüm Ürünler';

  return (
    <div className="grid md:grid-cols-[260px_1fr] gap-6">
      <FacetSidebar facets={data?.facets} />

      <section className="space-y-4 min-w-0">
        <div className="flex items-center justify-between gap-4 flex-wrap">
          <div>
            <h1 className="text-xl font-bold">{heading}</h1>
            <p className="text-sm text-gray-500 mt-1">
              {data ? `${data.totalElements} ürün bulundu` : 'Aranıyor...'}
              {parsed.q && (
                <span className="ml-2 text-xs text-gray-400">
                  · Elasticsearch · fuzzy matching açık
                </span>
              )}
            </p>
          </div>
          <label className="flex items-center gap-2 text-sm">
            Sırala:
            <select
              value={parsed.sort}
              onChange={(e) => setSort(e.target.value)}
              className="border border-gray-300 rounded px-2 py-1 text-sm"
            >
              <option value="relevance">İlgi düzeyi</option>
              <option value="price_asc">Fiyat: Artan</option>
              <option value="price_desc">Fiyat: Azalan</option>
              <option value="rating_desc">En yüksek puan</option>
            </select>
          </label>
        </div>

        {error ? (
          <div className="bg-red-50 text-red-700 p-3 rounded">{errorMessage(error)}</div>
        ) : null}

        {loading ? (
          <div className="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-4 gap-3">
            {Array.from({ length: 12 }).map((_, i) => (
              <div
                key={i}
                className="aspect-[3/4] bg-white border border-gray-200 rounded-lg animate-pulse"
              />
            ))}
          </div>
        ) : data && data.content.length === 0 ? (
          <div className="text-center text-gray-500 py-12 bg-white rounded-lg border border-gray-200">
            Bu kriterlere uygun ürün bulunamadı.
          </div>
        ) : (
          <>
            <div className="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-4 gap-3">
              {data?.content.map((p) => (
                <SearchResultCard key={p.id} product={p} />
              ))}
            </div>

            {data && data.totalPages > 1 && (
              <div className="flex items-center justify-center gap-2 pt-4">
                <button
                  onClick={() => setPage(Math.max(0, (parsed.page ?? 0) - 1))}
                  disabled={(parsed.page ?? 0) === 0}
                  className="px-3 py-1.5 border border-gray-300 rounded disabled:opacity-50"
                >
                  ← Önceki
                </button>
                <span className="text-sm text-gray-600">
                  Sayfa {data.page + 1} / {data.totalPages}
                </span>
                <button
                  onClick={() => setPage(Math.min(data.totalPages - 1, (parsed.page ?? 0) + 1))}
                  disabled={(parsed.page ?? 0) >= data.totalPages - 1}
                  className="px-3 py-1.5 border border-gray-300 rounded disabled:opacity-50"
                >
                  Sonraki →
                </button>
              </div>
            )}
          </>
        )}
      </section>
    </div>
  );
}
