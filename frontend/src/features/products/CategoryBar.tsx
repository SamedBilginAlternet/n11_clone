import { Link, useSearchParams } from 'react-router-dom';
import { Category } from '../../types';

/**
 * Category chips jump into /search with a category filter pre-applied so
 * users land on the faceted results page (Elasticsearch-backed) instead of
 * the home grid.
 */
export function CategoryBar({ categories }: { categories: Category[] }) {
  const [params] = useSearchParams();
  const active = params.get('category');
  return (
    <div className="flex gap-2 overflow-x-auto pb-2 -mx-4 px-4 md:mx-0 md:px-0">
      <Link
        to="/search"
        className={`category-chip whitespace-nowrap ${!active ? 'ring-2 ring-n11-purple text-n11-purple' : ''}`}
      >
        Tümü
      </Link>
      {categories.map((c) => (
        <Link
          key={c.slug}
          to={`/search?category=${c.slug}`}
          className={`category-chip whitespace-nowrap ${
            active === c.slug ? 'ring-2 ring-n11-purple text-n11-purple' : ''
          }`}
        >
          <span className="mr-1">{c.icon}</span>
          {c.name}
        </Link>
      ))}
    </div>
  );
}
