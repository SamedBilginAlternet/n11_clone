export interface SearchProduct {
  id: number;
  name: string;
  slug: string;
  description: string;
  category: string;
  brand: string;
  price: number;
  discountedPrice: number;
  discountPercentage: number;
  stockQuantity: number;
  imageUrl: string;
  rating: number;
  reviewCount: number;
}

export interface SearchFacets {
  brands: Record<string, number>;
  categories: Record<string, number>;
  price: { min: number; max: number };
}

export interface SearchResponse {
  content: SearchProduct[];
  totalElements: number;
  totalPages: number;
  page: number;
  size: number;
  facets: SearchFacets;
}

export interface SearchParams {
  q?: string;
  category?: string;
  brand?: string;
  minPrice?: number;
  maxPrice?: number;
  minRating?: number;
  sort?: 'relevance' | 'price_asc' | 'price_desc' | 'rating_desc';
  page?: number;
  size?: number;
}
