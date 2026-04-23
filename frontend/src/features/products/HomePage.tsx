import { useSearchParams } from 'react-router-dom';
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

  const categories = categoriesQ.data ?? [];
  const page = productsQ.data;
  const heading = q
    ? `"${q}" için arama sonuçları`
    : category
      ? categories.find((c) => c.slug === category)?.name ?? category
      : 'Günün Fırsatları';

  return (
    <div className="space-y-6">
      <CategoryBar categories={categories} />

      <div className="n11-gradient rounded-xl p-6 text-white relative overflow-hidden">
        <span className="absolute -right-2 -bottom-2 text-7xl opacity-20 select-none" aria-hidden>🐞</span>
        <h1 className="text-2xl font-bold">{heading}</h1>
        <p className="text-sm opacity-90 mt-1">
          {page ? `${page.totalElements} ürün bulundu` : 'Ürünler yükleniyor...'}
        </p>
      </div>

      {productsQ.error ? (
        <div className="bg-red-50 text-red-700 p-3 rounded">{errorMessage(productsQ.error)}</div>
      ) : null}

      {productsQ.loading ? (
        <div className="grid grid-cols-2 md:grid-cols-4 lg:grid-cols-6 gap-3">
          {Array.from({ length: 12 }).map((_, i) => (
            <div key={i} className="aspect-[3/4] bg-white border border-gray-200 rounded-lg animate-pulse" />
          ))}
        </div>
      ) : page && page.content.length === 0 ? (
        <div className="text-center text-gray-500 py-12">Bu kriterlere uygun ürün bulunamadı.</div>
      ) : (
        <div className="grid grid-cols-2 md:grid-cols-4 lg:grid-cols-6 gap-3">
          {page?.content.map((p) => (
            <ProductCard key={p.id} product={p} />
          ))}
        </div>
      )}
    </div>
  );
}
