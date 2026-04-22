import { HTMLAttributes } from 'react';

export function Card({ className = '', ...rest }: HTMLAttributes<HTMLDivElement>) {
  return (
    <div
      {...rest}
      className={`bg-white rounded-lg border border-gray-200 shadow-sm ${className}`}
    />
  );
}
