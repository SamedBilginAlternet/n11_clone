import { Loader2 } from 'lucide-react';
import { cn } from '../utils/cn';

export function Spinner({ size = 20, className }: { size?: number; className?: string }) {
  return (
    <Loader2
      className={cn('animate-spin text-primary', className)}
      style={{ width: size, height: size }}
      role="status"
    />
  );
}
