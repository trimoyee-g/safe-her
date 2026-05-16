import { Link } from 'react-router-dom'
import type { PlaceSummaryResponse } from '@/types'
import { ScoreBadge } from '@/components/ui/ScoreBadge'
import { Tag } from '@/components/ui'
import { CATEGORY_LABELS, CATEGORY_ICONS, formatDistance } from '@/utils'
import clsx from 'clsx'

interface PlaceCardProps {
  place: PlaceSummaryResponse
  compact?: boolean
}

export function PlaceCard({ place, compact }: PlaceCardProps) {
  return (
    <Link
      to={`/place/${place.id}`}
      className={clsx(
        'flex items-center gap-3 bg-white rounded-xl border border-gray-100 hover:border-gray-200 hover:shadow-sm transition-all',
        compact ? 'p-3' : 'p-4'
      )}
    >
      {/* Category icon */}
      <div className={clsx(
        'flex-shrink-0 rounded-xl flex items-center justify-center text-lg',
        compact ? 'w-10 h-10' : 'w-12 h-12',
        place.safetyScore >= 4 ? 'bg-brand-50' :
        place.safetyScore >= 2.5 ? 'bg-amber-50' :
        place.safetyScore > 0 ? 'bg-red-50' : 'bg-gray-100'
      )}>
        {CATEGORY_ICONS[place.category]}
      </div>

      {/* Info */}
      <div className="flex-1 min-w-0">
        <div className="flex items-center gap-1.5 mb-0.5">
          <p className="text-sm font-medium text-gray-900 truncate">{place.name}</p>
          {place.verified && (
            <svg className="w-3.5 h-3.5 text-brand-400 flex-shrink-0" fill="currentColor" viewBox="0 0 20 20">
              <path fillRule="evenodd" d="M6.267 3.455a3.066 3.066 0 001.745-.723 3.066 3.066 0 013.976 0 3.066 3.066 0 001.745.723 3.066 3.066 0 012.812 2.812c.051.643.304 1.254.723 1.745a3.066 3.066 0 010 3.976 3.066 3.066 0 00-.723 1.745 3.066 3.066 0 01-2.812 2.812 3.066 3.066 0 00-1.745.723 3.066 3.066 0 01-3.976 0 3.066 3.066 0 00-1.745-.723 3.066 3.066 0 01-2.812-2.812 3.066 3.066 0 00-.723-1.745 3.066 3.066 0 010-3.976 3.066 3.066 0 00.723-1.745 3.066 3.066 0 012.812-2.812zm7.44 5.252a1 1 0 00-1.414-1.414L9 10.586 7.707 9.293a1 1 0 00-1.414 1.414l2 2a1 1 0 001.414 0l4-4z" clipRule="evenodd" />
            </svg>
          )}
        </div>
        <p className="text-xs text-gray-500 truncate">
          {CATEGORY_LABELS[place.category]}
          {place.city && ` · ${place.city}`}
          {place.distanceMeters !== undefined && ` · ${formatDistance(place.distanceMeters)}`}
        </p>
        {!compact && place.totalRatings > 0 && (
          <p className="text-xs text-gray-400 mt-0.5">{place.totalRatings} review{place.totalRatings !== 1 ? 's' : ''}</p>
        )}
      </div>

      {/* Score */}
      <ScoreBadge score={place.safetyScore} size={compact ? 'sm' : 'md'} />
    </Link>
  )
}
