import { ApiError } from './client';

/**
 * Turns any thrown value into a user-readable message. ApiError first (RFC
 * 7807 detail), then native Error.message, then fallback.
 */
export function errorMessage(err: unknown, fallback = 'Bir hata oluştu.'): string {
  if (err instanceof ApiError) return err.problem?.detail ?? err.message ?? fallback;
  if (err instanceof Error) return err.message || fallback;
  return fallback;
}

/** Returns field-level errors if the server sent them (validation failures). */
export function errorFields(err: unknown): Record<string, string> | undefined {
  if (err instanceof ApiError) return err.problem?.fields;
  return undefined;
}
