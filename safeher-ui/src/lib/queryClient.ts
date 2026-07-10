import { QueryClient } from '@tanstack/react-query'

export const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      staleTime: 1000 * 60 * 2,        // 2 min
      gcTime: 1000 * 60 * 10,          // 10 min
      retry: (failureCount, error: unknown) => {
        const status = (error as { response?: { status: number } })?.response?.status
        if (status === 401 || status === 403 || status === 404 || status === 429) return false
        return failureCount < 2
      },
      refetchOnWindowFocus: false,
    },
    mutations: {
      retry: false,
    },
  },
})

// ── Query keys ────────────────────────────────────────────────────────────────
export const queryKeys = {
  places: {
    all: ['places'] as const,
    list: (page: number, size: number) => ['places', 'list', page, size] as const,
    detail: (id: string) => ['places', 'detail', id] as const,
    geoSearch: (lat: number, lng: number, radius: number, category?: string) =>
      ['places', 'geo', lat, lng, radius, category] as const,
    keywordSearch: (query: string, category?: string, city?: string) =>
      ['places', 'search', query, category, city] as const,
    topRated: () => ['places', 'top-rated'] as const,
    myPlaces: (page: number) => ['places', 'mine', page] as const,
    byCategory: (category: string, page: number) => ['places', 'category', category, page] as const,
  },
  ratings: {
    all: ['ratings'] as const,
    byPlace: (placeId: string, sortBy: string, page: number) =>
      ['ratings', 'place', placeId, sortBy, page] as const,
    summary: (placeId: string) => ['ratings', 'summary', placeId] as const,
    byUser: (userId: string, page: number) => ['ratings', 'user', userId, page] as const,
    mine: (page: number) => ['ratings', 'mine', page] as const,
    search: (query: string, placeId?: string) => ['ratings', 'search', query, placeId] as const,
  },
  users: {
    me: ['users', 'me'] as const,
    detail: (id: string) => ['users', id] as const,
    public: (id: string) => ['users', 'public', id] as const,
  },
}
