import { ProblemDetail } from '../../types';
import { useAuthStore } from '../../features/auth/store';

// In Docker, nginx proxies /api to the gateway; in local dev, Vite proxies /api
// to http://localhost:8000. Either way, relative /api works.
const API_BASE = (import.meta.env.VITE_API_BASE as string | undefined) ?? '/api';

export class ApiError extends Error {
  status: number;
  problem?: ProblemDetail;

  constructor(status: number, message: string, problem?: ProblemDetail) {
    super(message);
    this.status = status;
    this.problem = problem;
  }
}

async function parseProblem(res: Response): Promise<ProblemDetail | undefined> {
  try {
    const ct = res.headers.get('content-type') ?? '';
    if (ct.includes('problem+json') || ct.includes('application/json')) {
      return (await res.json()) as ProblemDetail;
    }
  } catch {
    /* body wasn't JSON */
  }
  return undefined;
}

export async function apiFetch<T>(
  path: string,
  init: RequestInit = {},
  opts: { auth?: boolean } = { auth: true },
): Promise<T> {
  const headers = new Headers(init.headers);
  if (init.body && !headers.has('Content-Type')) {
    headers.set('Content-Type', 'application/json');
  }

  if (opts.auth) {
    const token = useAuthStore.getState().accessToken;
    if (token) headers.set('Authorization', `Bearer ${token}`);
  }

  const res = await fetch(`${API_BASE}${path}`, { ...init, headers, credentials: 'include' });

  if (res.status === 204) return undefined as T;

  if (!res.ok) {
    const problem = await parseProblem(res);
    throw new ApiError(res.status, problem?.detail ?? res.statusText, problem);
  }

  const text = await res.text();
  return (text ? JSON.parse(text) : undefined) as T;
}
