import { useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { placesApi } from '@/api/places.api'
import { queryKeys } from '@/lib/queryClient'
import { PlaceCard } from '@/components/place/PlaceCard'
import { Input, Select, Spinner, EmptyState } from '@/components/ui'
import type { PlaceCategory } from '@/types'
import { CATEGORY_LABELS } from '@/utils'
import { useDebounce } from '@/hooks/useDebounce'

const CATEGORY_OPTIONS = [
  { value: '', label: 'All categories' },
  ...Object.entries(CATEGORY_LABELS).map(([value, label]) => ({ value, label })),
]

export function SearchPage() {
  const [query, setQuery] = useState('')
  const [category, setCategory] = useState<PlaceCategory | ''>('')
  const [city, setCity] = useState('')
  const [page, setPage] = useState(0)

  const debouncedQuery = useDebounce(query, 400)
  const debouncedCity  = useDebounce(city, 400)

  const { data, isFetching } = useQuery({
    queryKey: queryKeys.places.keywordSearch(debouncedQuery, category || undefined, debouncedCity || undefined),
    queryFn: () => placesApi.keywordSearch({
      query: debouncedQuery,
      category: (category as PlaceCategory) || undefined,
      city: debouncedCity || undefined,
      page,
      size: 20,
    }),
    enabled: debouncedQuery.length >= 2,
    staleTime: 30_000,
  })

  const places = data?.content ?? []

  return (
    <div className="max-w-2xl mx-auto px-4 py-6">
      <h1 className="text-lg font-semibold text-gray-100 mb-4">Search places</h1>

      {/* Filters */}
      <div className="flex flex-col gap-3 mb-5">
        <div className="relative">
          <svg className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-500"
            fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
            <path strokeLinecap="round" strokeLinejoin="round"
              d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
          </svg>
          <input
            type="search"
            placeholder="Search by name, area..."
            value={query}
            onChange={e => { setQuery(e.target.value); setPage(0) }}
            className="w-full pl-9 pr-4 py-2.5 text-sm border border-gray-700 rounded-xl bg-gray-900 text-gray-100 placeholder-gray-500 focus:outline-none focus:ring-2 focus:ring-brand-400"
          />
        </div>
        <div className="grid grid-cols-2 gap-2">
          <Select
            options={CATEGORY_OPTIONS}
            value={category}
            onChange={e => { setCategory(e.target.value as PlaceCategory); setPage(0) }}
          />
          <Input
            placeholder="City..."
            value={city}
            onChange={e => { setCity(e.target.value); setPage(0) }}
          />
        </div>
      </div>

      {/* Results */}
      {debouncedQuery.length < 2 ? (
        <div className="text-center py-16">
          <div className="w-16 h-16 bg-gray-900 rounded-full flex items-center justify-center mx-auto mb-3">
            <svg className="w-8 h-8 text-gray-500" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5}
                d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
            </svg>
          </div>
          <p className="text-sm text-gray-400">Type at least 2 characters to search</p>
        </div>
      ) : isFetching ? (
        <div className="flex justify-center py-12"><Spinner size="lg" className="text-brand-400" /></div>
      ) : places.length === 0 ? (
        <EmptyState
          title="No places found"
          description={`No results for "${debouncedQuery}"${category ? ` in ${CATEGORY_LABELS[category as PlaceCategory]}` : ''}`}
        />
      ) : (
        <>
          <p className="text-xs text-gray-400 mb-3">
            {data?.totalElements} result{data?.totalElements !== 1 ? 's' : ''} for "{debouncedQuery}"
          </p>
          <div className="flex flex-col gap-2">
            {places.map(p => <PlaceCard key={p.id} place={p} />)}
          </div>
          {data && !data.last && (
            <div className="flex justify-center mt-4">
              <button
                onClick={() => setPage(p => p + 1)}
                className="text-sm text-brand-300 font-medium hover:text-brand-100"
              >
                Load more
              </button>
            </div>
          )}
        </>
      )}
    </div>
  )
}
