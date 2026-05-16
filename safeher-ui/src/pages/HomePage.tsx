import { useState, useCallback, useDeferredValue } from 'react'
import { useQuery } from '@tanstack/react-query'
import { MapView } from '@/components/map/MapView'
import { PlaceCard } from '@/components/place/PlaceCard'
import { Spinner, EmptyState } from '@/components/ui'
import { placesApi } from '@/api/places.api'
import { queryKeys } from '@/lib/queryClient'
import type { PlaceCategory, PlaceSummaryResponse } from '@/types'
import { CATEGORY_ICONS, CATEGORY_LABELS } from '@/utils'

const QUICK_CATEGORIES: PlaceCategory[] = [
  'TRANSIT_STATION', 'PARK', 'RESTAURANT', 'SHOPPING_MALL', 'STREET', 'MARKET',
]

export function HomePage() {
  const [mapCenter, setMapCenter] = useState<{ lat: number; lng: number; radius: number }>({
    lat: 22.5726,   // Kolkata default
    lng: 88.3639,
    radius: 5000,
  })
  const [selectedCategory, setSelectedCategory] = useState<PlaceCategory | undefined>()
  const [selectedPlace, setSelectedPlace] = useState<PlaceSummaryResponse | null>(null)
  const [view, setView] = useState<'map' | 'list'>('map')

  const deferredCenter = useDeferredValue(mapCenter)

  const { data: geoResults, isFetching } = useQuery({
    queryKey: queryKeys.places.geoSearch(
      deferredCenter.lat, deferredCenter.lng, deferredCenter.radius, selectedCategory
    ),
    queryFn: () => placesApi.geoSearch({
      latitude: deferredCenter.lat,
      longitude: deferredCenter.lng,
      radiusMeters: Math.min(deferredCenter.radius, 25_000),
      category: selectedCategory,
      size: 50,
    }),
    staleTime: 30_000,
  })

  const places = geoResults?.content ?? []

  const handleBoundsChange = useCallback((lat: number, lng: number, radiusMeters: number) => {
    setMapCenter({ lat, lng, radius: radiusMeters })
  }, [])

  return (
    <div className="h-[calc(100vh-56px)] flex flex-col">
      {/* Category filter strip */}
      <div className="bg-white border-b border-gray-100 px-4 py-2 flex items-center gap-2 overflow-x-auto scrollbar-none">
        <button
          onClick={() => setSelectedCategory(undefined)}
          className={`flex-shrink-0 px-3 py-1.5 rounded-full text-xs font-medium transition-colors border ${
            !selectedCategory
              ? 'bg-brand-400 text-white border-brand-400'
              : 'bg-white text-gray-600 border-gray-200 hover:border-gray-300'
          }`}
        >
          All places
        </button>
        {QUICK_CATEGORIES.map(cat => (
          <button
            key={cat}
            onClick={() => setSelectedCategory(selectedCategory === cat ? undefined : cat)}
            className={`flex-shrink-0 flex items-center gap-1.5 px-3 py-1.5 rounded-full text-xs font-medium transition-colors border ${
              selectedCategory === cat
                ? 'bg-brand-400 text-white border-brand-400'
                : 'bg-white text-gray-600 border-gray-200 hover:border-gray-300'
            }`}
          >
            <span>{CATEGORY_ICONS[cat]}</span>
            <span>{CATEGORY_LABELS[cat]}</span>
          </button>
        ))}

        {/* View toggle */}
        <div className="ml-auto flex-shrink-0 flex items-center gap-1 bg-gray-100 rounded-lg p-0.5">
          <button
            onClick={() => setView('map')}
            className={`px-3 py-1 text-xs font-medium rounded-md transition-colors ${
              view === 'map' ? 'bg-white text-gray-900 shadow-sm' : 'text-gray-500'
            }`}
          >
            Map
          </button>
          <button
            onClick={() => setView('list')}
            className={`px-3 py-1 text-xs font-medium rounded-md transition-colors ${
              view === 'list' ? 'bg-white text-gray-900 shadow-sm' : 'text-gray-500'
            }`}
          >
            List
          </button>
        </div>
      </div>

      {/* Main content */}
      <div className="flex-1 overflow-hidden">
        {view === 'map' ? (
          <div className="relative h-full">
            <MapView
              places={places}
              onBoundsChange={handleBoundsChange}
              onPlaceClick={setSelectedPlace}
              selectedPlaceId={selectedPlace?.id}
              height="100%"
            />

            {/* Nearby panel overlaid at bottom */}
            {places.length > 0 && (
              <div className="absolute bottom-0 left-0 right-0 bg-white rounded-t-2xl shadow-lg max-h-72 overflow-y-auto">
                <div className="sticky top-0 bg-white px-4 pt-3 pb-2 border-b border-gray-100 flex items-center justify-between">
                  <div className="flex items-center gap-2">
                    <div className="w-8 h-1 bg-gray-300 rounded-full mx-auto" />
                  </div>
                  <p className="text-xs font-medium text-gray-600 absolute left-1/2 -translate-x-1/2">
                    {places.length} nearby places
                    {isFetching && <Spinner size="sm" className="inline ml-1.5" />}
                  </p>
                </div>
                <div className="p-3 flex flex-col gap-2">
                  {places.map(p => (
                    <PlaceCard key={p.id} place={p} compact />
                  ))}
                </div>
              </div>
            )}
          </div>
        ) : (
          <div className="h-full overflow-y-auto">
            <div className="max-w-2xl mx-auto px-4 py-4">
              <div className="flex items-center justify-between mb-3">
                <p className="text-sm font-medium text-gray-700">
                  {places.length} places found
                </p>
                {isFetching && <Spinner size="sm" />}
              </div>
              {places.length === 0 && !isFetching ? (
                <EmptyState
                  title="No places found"
                  description="Try changing the category or moving the map to a different area"
                />
              ) : (
                <div className="flex flex-col gap-2">
                  {places.map(p => <PlaceCard key={p.id} place={p} />)}
                </div>
              )}
            </div>
          </div>
        )}
      </div>
    </div>
  )
}
