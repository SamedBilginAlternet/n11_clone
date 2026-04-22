interface Props {
  value: number;
  size?: number;
  interactive?: boolean;
  onChange?: (rating: number) => void;
}

export function RatingStars({ value, size = 16, interactive = false, onChange }: Props) {
  return (
    <div className="inline-flex items-center gap-0.5">
      {[1, 2, 3, 4, 5].map((i) => {
        const filled = i <= Math.round(value);
        const star = (
          <svg
            key={i}
            width={size}
            height={size}
            viewBox="0 0 24 24"
            fill={filled ? '#F9A825' : 'none'}
            stroke={filled ? '#F9A825' : '#CBD5E0'}
            strokeWidth={1.5}
            strokeLinejoin="round"
          >
            <path d="M12 2l3.09 6.26L22 9.27l-5 4.87 1.18 6.88L12 17.77l-6.18 3.25L7 14.14 2 9.27l6.91-1.01L12 2z" />
          </svg>
        );
        return interactive ? (
          <button
            key={i}
            type="button"
            onClick={() => onChange?.(i)}
            className="cursor-pointer focus:outline-none"
            aria-label={`${i} yıldız`}
          >
            {star}
          </button>
        ) : (
          star
        );
      })}
    </div>
  );
}
