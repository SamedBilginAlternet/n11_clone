import { create } from 'zustand';
import { persist } from 'zustand/middleware';
import { AuthTokens } from '../../types';

interface AuthState {
  accessToken: string | null;
  email: string | null;
  setTokens: (tokens: AuthTokens, email: string) => void;
  clear: () => void;
  isAuthenticated: () => boolean;
}

export const useAuthStore = create<AuthState>()(
  persist(
    (set, get) => ({
      accessToken: null,
      email: null,
      setTokens: (tokens, email) =>
        set({ accessToken: tokens.accessToken, email }),
      clear: () => set({ accessToken: null, email: null }),
      isAuthenticated: () => !!get().accessToken,
    }),
    { name: 'n11-auth' },
  ),
);
