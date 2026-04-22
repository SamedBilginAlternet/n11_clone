import { create } from 'zustand';
import { Basket } from '../types';

interface BasketState {
  basket: Basket | null;
  loading: boolean;
  setBasket: (basket: Basket | null) => void;
  setLoading: (loading: boolean) => void;
}

export const useBasketStore = create<BasketState>((set) => ({
  basket: null,
  loading: false,
  setBasket: (basket) => set({ basket }),
  setLoading: (loading) => set({ loading }),
}));
