import { useSearchParams } from 'react-router-dom';
import { Search as SearchIcon, ChevronLeft, ChevronRight } from 'lucide-react';

function ErrorBanner({ message }: { message: string }) {
  return (
    <div className="rounded-lg border border-destructive/20 bg-destructive/5 p-4 text-sm text-destructive">
      {message}
    </div>
  );
}

function EmptyState() {
  return (
    <div className="flex flex-col items-center py-16">
      <SearchIcon className="mb-4 h-12 w-12 text-muted-foreground/40" />
      <p className="text-lg font-medium text-muted-foreground">Bu kriterlere uygun urun bulunamadi.</p>
    </div>
  );
}
import { useApi } from '../../shared/hooks/useApi';
import { errorMessage } from '../../shared/api/problem';
import { searchApi } from './api';
import { SearchResultCard } from './SearchResultCard';
import { FacetSidebar } from './FacetSidebar';
import { SearchParams } from './types';
import { Button } from '../../shared/ui/Button';

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
    [parsed.q, parsed.category, parsed.brand, parsed.minPrice, parsed.maxPrice, parsed.minRating, parsed.sort, parsed.page],
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

  const heading: string = parsed.q ? `"${parsed.q}" icin sonuclar` : 'Tum Urunler';

  return (
    <div className="grid gap-6 md:grid-cols-[260px_1fr]">
      <FacetSidebar facets={data?.facets} />

      <section className="min-w-0 space-y-4">
        <div className="flex flex-wrap items-center justify-between gap-4">
          <div>
            <h1 className="text-xl font-bold">{heading}</h1>
            <p className="mt-1 text-sm text-muted-foreground">
              {data ? `${String((data as any).totalElements)} urun bulundu` : 'Araniyor...'}
              {parsed.q && (
                <span className="ml-2 text-xs text-muted-foreground/60">
                  Elasticsearch fuzzy matching
                </span>
              )}
            </p>
          </div>
          <select
            value={parsed.sort}
            onChange={(e) => setSort(e.target.value)}
            className="h-10 rounded-lg border border-border bg-card px-3 text-sm transition-colors focus:outline-none focus:ring-2 focus:ring-ring"
          >
            <option value="relevance">Ilgi duzeyi</option>
            <option value="price_asc">Fiyat: Artan</option>
            <option value="price_desc">Fiyat: Azalan</option>
            <option value="rating_desc">En yuksek puan</option>
          </select>
        </div>

        {error ? <ErrorBanner message={errorMessage(error)} /> : null}

        {loading ? (
          <div className="grid grid-cols-2 gap-4 md:grid-cols-3 lg:grid-cols-4">
            {Array.from({ length: 12 }).map((_, i) => (
              <div key={i} className="animate-pulse space-y-3">
                <div className="aspect-square rounded-xl bg-muted" />
                <div className="h-3 w-3/4 rounded bg-muted" />
                <div className="h-5 w-1/3 rounded bg-muted" />
              </div>
            ))}
          </div>
        ) : data && data.content.length === 0 ? (
          <EmptyState />
        ) : (
          <>
            <div className="grid grid-cols-2 gap-4 md:grid-cols-3 lg:grid-cols-4">
              {data?.content.map((p) => (
                <SearchResultCard key={p.id} product={p} />
              ))}
            </div>

            {data && data.totalPages > 1 && (
              <div className="flex items-center justify-center gap-3 pt-6">
                <Button
                  variant="outline"
                  size="sm"
                  onClick={() => setPage(Math.max(0, (parsed.page ?? 0) - 1))}
                  disabled={(parsed.page ?? 0) === 0}
                >
                  <ChevronLeft className="h-4 w-4" /> Onceki
                </Button>
                <span className="text-sm text-muted-foreground">
                  Sayfa {data.page + 1} / {data.totalPages}
                </span>
                <Button
                  variant="outline"
                  size="sm"
                  onClick={() => setPage(Math.min(data.totalPages - 1, (parsed.page ?? 0) + 1))}
                  disabled={(parsed.page ?? 0) >= data.totalPages - 1}
                >
                  Sonraki <ChevronRight className="h-4 w-4" />
                </Button>
              </div>
            )}
          </>
        )}
      </section>
    </div>
  );
}
