import { Link, useSearchParams } from 'react-router-dom';
import { Category } from '../../types';
import { cn } from '../../shared/utils/cn';

export function CategoryBar({ categories }: { categories: Category[] }) {
  const [params] = useSearchParams();
  const active = params.get('category');

  return (
    <div className="hide-scrollbar -mx-4 flex gap-2 overflow-x-auto px-4 pb-1 md:mx-0 md:px-0">
      <Chip to="/search" active={!active}>Tumu</Chip>
      {categories.map((c) => (
        <Chip key={c.slug} to={`/search?category=${c.slug}`} active={active === c.slug}>
          {c.icon && <span className="mr-1">{c.icon}</span>}
          {c.name}
        </Chip>
      ))}
    </div>
  );
}

function Chip({ to, active, children }: { to: string; active: boolean; children: React.ReactNode }) {
  return (
    <Link
      to={to}
      className={cn(
        'inline-flex shrink-0 items-center whitespace-nowrap rounded-full border px-4 py-2 text-sm font-medium transition-all duration-200',
        active
          ? 'border-primary bg-primary text-primary-foreground shadow-md'
          : 'border-border bg-card text-foreground hover:border-primary/40 hover:shadow-sm',
      )}
    >
      {children}
    </Link>
  );
}
