import { useState } from 'react'
import { useParams, Link, useNavigate } from 'react-router-dom'
import { useQuery, useQueryClient } from '@tanstack/react-query'
import { placesApi } from '@/api/places.api'
import { ratingsApi } from '@/api/ratings.api'
import { queryKeys } from '@/lib/queryClient'
import { ScoreDistributionBar } from '@/components/rating/ScoreDistributionBar'
import { ReviewCard } from '@/components/rating/ReviewCard'
import { Button, Spinner, EmptyState, ErrorState, Tag } from '@/components/ui'
import { useAuthStore } from '@/store/auth.store'
import { CATEGORY_LABELS, CATEGORY_ICONS, scoreLabel, isPositiveTag, formatScore } from '@/utils'
import type { RatingSortBy } from '@/types'

const SORT_OPTIONS: { value: RatingSortBy; label: string }[] = [
  { value: 'NEWEST',        label: 'Newest' },
  { value: 'MOST_HELPFUL',  label: 'Most helpful' },
  { value: 'HIGHEST_SCORE', label: 'Highest' },
  { value: 'LOWEST_SCORE',  label: 'Lowest' },
]

export function PlaceDetailPage() {
  const { id } = useParams<{ id: string }>()
  const navigate = useNavigate()
  const { isAuthenticated } = useAuthStore()
  const queryClient = useQueryClient()
  const [sortBy, setSortBy] = useState<RatingSortBy>('NEWEST')
  const [page, setPage] = useState(0)

  const { data: place, isLoading: placeLoading, error: placeError } = useQuery({
    queryKey: queryKeys.places.detail(id!),
    queryFn: () => placesApi.getById(id!),
    enabled: !!id,
  })

  const { data: summary } = useQuery({
    queryKey: queryKeys.ratings.summary(id!),
    queryFn: () => ratingsApi.getSummary(id!),
    enabled: !!id,
  })

  const { data: reviewsPage, isLoading: reviewsLoading } = useQuery({
    queryKey: queryKeys.ratings.byPlace(id!, sortBy, page),
    queryFn: () => ratingsApi.getByPlace(id!, sortBy, page, 10),
    enabled: !!id,
  })

  const reviews = reviewsPage?.content ?? []

  // Aggregate tags from visible reviews
  const tagCounts: Record<string, number> = {}
  reviews.forEach(r => r.tags?.forEach(t => { tagCounts[t] = (tagCounts[t] ?? 0) + 1 }))
  const sortedTags = Object.entries(tagCounts).sort((a, b) => b[1] - a[1]).slice(0, 10)

  if (placeLoading) {
    return (
      <div className="flex items-center justify-center h-64">
        <Spinner size="lg" className="text-brand-400" />
      </div>
    )
  }

  if (placeError || !place) {
    return <ErrorState message="Place not found" retry={() => queryClient.invalidateQueries({ queryKey: queryKeys.places.detail(id!) })} />
  }

  return (
    <div className="max-w-2xl mx-auto px-4 py-6">
      {/* Back */}
      <button onClick={() => navigate(-1)} className="flex items-center gap-1 text-sm text-gray-500 hover:text-gray-300 mb-4 transition-colors">
        <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
          <path strokeLinecap="round" strokeLinejoin="round" d="M15 19l-7-7 7-7" />
        </svg>
        Back
      </button>

      {/* Hero card */}
      <div className="bg-brand-400 rounded-2xl p-5 mb-4 text-white">
        <div className="flex items-start justify-between gap-3">
          <div className="flex-1 min-w-0">
            <div className="flex items-center gap-2 mb-1">
              <span className="text-base">{CATEGORY_ICONS[place.category]}</span>
              <span className="text-brand-950/70 text-xs font-medium">{CATEGORY_LABELS[place.category]}</span>
              {place.verified && (
                <span className="bg-white/20 text-white text-xs px-2 py-0.5 rounded-full font-medium">Verified</span>
              )}
            </div>
            <h1 className="text-xl font-semibold text-white mb-1">{place.name}</h1>
            {place.address && <p className="text-brand-950/70 text-sm truncate">{place.address}</p>}
            {place.city && <p className="text-brand-950/70 text-xs">{place.city}{place.country ? `, ${place.country}` : ''}</p>}
          </div>
          <div className="flex flex-col items-center flex-shrink-0">
            <div className="text-4xl font-bold text-white leading-none mb-1">
              {formatScore(place.safetyScore)}
            </div>
            <p className="text-brand-950/70 text-xs">{place.totalRatings} review{place.totalRatings !== 1 ? 's' : ''}</p>
            <p className="text-white text-xs font-medium mt-0.5">{scoreLabel(place.safetyScore)}</p>
          </div>
        </div>
      </div>

      {/* Score breakdown */}
      {summary && summary.totalRatings > 0 && (
        <div className="bg-gray-900 rounded-xl border border-gray-800 p-4 mb-4">
          <h2 className="text-sm font-medium text-gray-300 mb-3">Score breakdown</h2>
          <ScoreDistributionBar summary={summary} />
        </div>
      )}

      {/* Tags cloud */}
      {sortedTags.length > 0 && (
        <div className="bg-gray-900 rounded-xl border border-gray-800 p-4 mb-4">
          <h2 className="text-sm font-medium text-gray-300 mb-2">What people say</h2>
          <div className="flex flex-wrap gap-2">
            {sortedTags.map(([tag, count]) => (
              <div key={tag} className="flex items-center gap-1">
                <Tag label={tag} positive={isPositiveTag(tag)} size="md" />
                <span className="text-xs text-gray-500">({count})</span>
              </div>
            ))}
          </div>
        </div>
      )}

      {/* Reviews header */}
      <div className="flex items-center justify-between mb-3">
        <div className="flex items-center gap-2 overflow-x-auto">
          {SORT_OPTIONS.map(opt => (
            <button
              key={opt.value}
              onClick={() => { setSortBy(opt.value); setPage(0) }}
              className={`flex-shrink-0 px-3 py-1.5 rounded-lg text-xs font-medium transition-colors ${
                sortBy === opt.value
                  ? 'bg-brand-400 text-brand-950'
                  : 'bg-gray-900 text-gray-400 hover:bg-gray-800'
              }`}
            >
              {opt.label}
            </button>
          ))}
        </div>

        <div className="flex items-center gap-2 flex-shrink-0">
          {isAuthenticated && (
            <Link to={`/chat/${id}`}>
              <Button size="sm" variant="secondary">
                <svg className="w-3.5 h-3.5" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
                  <path strokeLinecap="round" strokeLinejoin="round" d="M8 10h.01M12 10h.01M16 10h.01M9 16H5a2 2 0 01-2-2V6a2 2 0 012-2h14a2 2 0 012 2v8a2 2 0 01-2 2h-5l-5 5v-5z" />
                </svg>
                Ask AI
              </Button>
            </Link>
          )}
          {isAuthenticated ? (
            <Link to={`/place/${id}/review`}>
              <Button size="sm">Write a review</Button>
            </Link>
          ) : (
            <Link to="/login">
              <Button size="sm" variant="secondary">Sign in to review</Button>
            </Link>
          )}
        </div>
      </div>

      {/* Reviews list */}
      {reviewsLoading ? (
        <div className="flex justify-center py-8"><Spinner size="lg" className="text-brand-400" /></div>
      ) : reviews.length === 0 ? (
        <EmptyState
          title="No reviews yet"
          description="Be the first to share your safety experience at this place"
          action={isAuthenticated ? (
            <Link to={`/place/${id}/review`}>
              <Button size="sm">Write the first review</Button>
            </Link>
          ) : undefined}
        />
      ) : (
        <>
          <div className="flex flex-col gap-3">
            {reviews.map(r => (
              <ReviewCard key={r.id} rating={r} placeId={id} />
            ))}
          </div>

          {/* Pagination */}
          {reviewsPage && !reviewsPage.last && (
            <div className="flex justify-center mt-4">
              <Button variant="secondary" size="sm" onClick={() => setPage(p => p + 1)}>
                Load more reviews
              </Button>
            </div>
          )}
        </>
      )}

      {/* Description */}
      {place.description && (
        <div className="bg-gray-900 rounded-xl border border-gray-800 p-4 mt-4">
          <h2 className="text-sm font-medium text-gray-300 mb-2">About</h2>
          <p className="text-sm text-gray-400 leading-relaxed">{place.description}</p>
        </div>
      )}
    </div>
  )
}
