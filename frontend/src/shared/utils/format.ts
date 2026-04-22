const TRY_FORMATTER = new Intl.NumberFormat('tr-TR', {
  style: 'currency',
  currency: 'TRY',
  maximumFractionDigits: 2,
});

export const formatTRY = (n: number): string => TRY_FORMATTER.format(n);

export const formatDate = (iso: string | Date): string => {
  const d = typeof iso === 'string' ? new Date(iso) : iso;
  return d.toLocaleDateString('tr-TR', { day: '2-digit', month: 'long', year: 'numeric' });
};

export const formatDateTime = (iso: string | Date): string => {
  const d = typeof iso === 'string' ? new Date(iso) : iso;
  return d.toLocaleString('tr-TR', {
    day: '2-digit',
    month: 'short',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  });
};
