import { useSearchParams } from 'react-router-dom';
import { Sparkles } from 'lucide-react';

function HeroBanner({ heading, count }: { heading: string; count?: number }) {
  return (
    <div className="relative overflow-hidden rounded-2xl bg-gradient-to-br from-violet-700 via-purple-600 to-indigo-700 p-8 text-white shadow-xl sm:p-10">
      <div className="absolute -right-6 -top-6 h-40 w-40 rounded-full bg-white/5 blur-2xl" />
      <div className="absolute -bottom-8 -left-8 h-48 w-48 rounded-full bg-amber-400/10 blur-3xl" />
      <div className="relative">
        <div className="flex items-center gap-2">
          <Sparkles className="h-6 w-6 text-amber-400" />
          <h1 className="text-2xl font-extrabold tracking-tight sm:text-3xl">{heading}</h1>
        </div>
        <p className="mt-2 text-sm text-white/80">
          {count !== undefined ? `${count} urun bulundu` : 'Urunler yukleniyor...'}
        </p>
      </div>
    </div>
  );
}
import { productApi } from './api';
import { ProductCard } from './ProductCard';
import { CategoryBar } from './CategoryBar';
import { useApi } from '../../shared/hooks/useApi';
import { errorMessage } from '../../shared/api/problem';

export function HomePage() {
  const [params] = useSearchParams();
  const category = params.get('category') ?? undefined;
  const q = params.get('q') ?? undefined;

  const categoriesQ = useApi(() => productApi.categories(), []);
  const productsQ = useApi(
    () => productApi.list({ category, q, page: 0, size: 24 }),
    [category, q],
  );

  const categories = (categoriesQ.data ?? []) as any[];
  const page = productsQ.data as any;
  const heading: string = q
    ? `"${q}" icin arama sonuclari`
    : category
      ? (categories.find((c: any) => c.slug === category)?.name ?? category)
      : 'Gunun Firsatlari';

  return (
    <div className="space-y-6">
      <CategoryBar categories={categories as any} />

      {/* Hero banner */}
      <HeroBanner heading={heading} count={page?.totalElements} />

      {/* Error */}
      {productsQ.error ? (
        <div className="rounded-lg border border-destructive/20 bg-destructive/5 p-4 text-sm text-destructive">
          {String(errorMessage(productsQ.error))}
        </div>
      ) : null}

      {/* Skeleton loading */}
      {productsQ.loading ? (
        <div className="grid grid-cols-2 gap-4 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-5">
          {Array.from({ length: 10 }).map((_, i) => (
            <div key={i} className="animate-pulse space-y-3">
              <div className="aspect-square rounded-xl bg-muted" />
              <div className="h-3 w-3/4 rounded bg-muted" />
              <div className="h-3 w-1/2 rounded bg-muted" />
              <div className="h-5 w-1/3 rounded bg-muted" />
            </div>
          ))}
        </div>
      ) : page && page.content.length === 0 ? (
        <div className="py-16 text-center">
          <div className="mx-auto mb-4 text-6xl">🔍</div>
          <p className="text-lg font-medium text-muted-foreground">Bu kriterlere uygun urun bulunamadi.</p>
        </div>
      ) : (
        <div className="grid grid-cols-2 gap-4 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-5">
          {page?.content.map((p: any) => (
            <ProductCard key={p.id} product={p} />
          ))}
        </div>
      )}
    </div>
  );
}
