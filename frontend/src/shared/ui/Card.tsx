import { HTMLAttributes, forwardRef } from 'react';
import { cn } from '../utils/cn';

export const Card = forwardRef<HTMLDivElement, HTMLAttributes<HTMLDivElement>>(
  function Card({ className, ...rest }, ref) {
    return (
      <div
        ref={ref}
        className={cn(
          'rounded-xl border border-border bg-card text-card-foreground shadow-sm transition-shadow',
          className,
        )}
        {...rest}
      />
    );
  },
);
