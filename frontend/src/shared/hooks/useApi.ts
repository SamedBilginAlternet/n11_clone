import { useCallback, useEffect, useRef, useState } from 'react';

export interface UseApiState<T> {
  data: T | undefined;
  error: unknown;
  loading: boolean;
  refetch: () => Promise<void>;
  setData: (updater: T | ((prev: T | undefined) => T)) => void;
}

/**
 * Stateful wrapper around an async function. Replaces the useState(data,
 * loading, error) triple that every page was doing by hand. Aborts stale
 * responses when the caller remounts or dependencies change.
 */
export function useApi<T>(
  fn: () => Promise<T>,
  deps: React.DependencyList = [],
  options: { enabled?: boolean } = {},
): UseApiState<T> {
  const { enabled = true } = options;
  const [data, setDataRaw] = useState<T | undefined>(undefined);
  const [error, setError] = useState<unknown>(undefined);
  const [loading, setLoading] = useState<boolean>(enabled);
  const activeCallIdRef = useRef(0);

  const refetch = useCallback(async () => {
    const callId = ++activeCallIdRef.current;
    setLoading(true);
    setError(undefined);
    try {
      const result = await fn();
      if (callId === activeCallIdRef.current) {
        setDataRaw(result);
      }
    } catch (err) {
      if (callId === activeCallIdRef.current) {
        setError(err);
      }
    } finally {
      if (callId === activeCallIdRef.current) {
        setLoading(false);
      }
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, deps);

  useEffect(() => {
    if (enabled) void refetch();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [enabled, ...deps]);

  const setData = useCallback((updater: T | ((prev: T | undefined) => T)) => {
    setDataRaw((prev) =>
      typeof updater === 'function' ? (updater as (p: T | undefined) => T)(prev) : updater,
    );
  }, []);

  return { data, error, loading, refetch, setData };
}
