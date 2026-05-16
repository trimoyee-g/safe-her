import { useState } from 'react'
import { formatDistanceToNow } from 'date-fns'
import type { RatingResponse } from '@/types'
import { Avatar, Tag } from '@/components/ui'
import { ScoreBadge } from '@/components/ui/ScoreBadge'
import { ratingsApi } from '@/api/ratings.api'
import { useAuthStore } from '@/store/auth.store'
import { isPositiveTag } from '@/utils'
import { useQueryClient } from '@tanstack/react-query'
import { queryKeys } from '@/lib/queryClient'
import clsx from 'clsx'

interface ReviewCardProps {
  rating: RatingResponse
  placeId?: string
  showPlace?: boolean
  onDelete?: (id: string) => void
}

export function ReviewCard({ rating, placeId, onDelete }: ReviewCardProps) {
  const { isAuthenticated, userId, isAdmin } = useAuthStore()
  const queryClient = useQueryClient()
  const [helpfulCount, setHelpfulCount] = useState(rating.helpfulCount)
  const [markedHelpful, setMarkedHelpful] = useState(rating.markedHelpfulByMe ?? false)
  const [reporting, setReporting] = useState(false)
  const [reported, setReported] = useState(false)

  const isOwner = userId === rating.userId
  const canDelete = isOwner || isAdmin()

  const handleHelpful = async () => {
    if (!isAuthenticated) return
    try {
      await ratingsApi.markHelpful(rating.id)
      if (markedHelpful) {
        setHelpfulCount(c => c - 1)
      } else {
        setHelpfulCount(c => c + 1)
      }
      setMarkedHelpful(!markedHelpful)
    } catch { /* silent */ }
  }

  const handleReport = async () => {
    if (!isAuthenticated || reported) return
    setReporting(true)
    try {
      await ratingsApi.report(rating.id)
      setReported(true)
    } finally {
      setReporting(false)
    }
  }

  const handleDelete = async () => {
    if (!canDelete) return
    if (!window.confirm('Delete this review?')) return
    try {
      await ratingsApi.delete(rating.id)
      if (placeId) {
        queryClient.invalidateQueries({ queryKey: queryKeys.ratings.byPlace(placeId, 'NEWEST', 0) })
        queryClient.invalidateQueries({ queryKey: queryKeys.ratings.summary(placeId) })
      }
      onDelete?.(rating.id)
    } catch { /* silent */ }
  }

  const authorName = rating.anonymous ? 'Anonymous' : (rating.authorDisplayName ?? 'SafeHer user')

  return (
    <div className="bg-white rounded-xl border border-gray-100 p-4">
      {/* Header */}
      <div className="flex items-start gap-3 mb-3">
        <Avatar
          name={rating.anonymous ? undefined : rating.authorDisplayName}
          src={rating.anonymous ? undefined : rating.authorAvatarUrl}
          anonymous={rating.anonymous}
          size="md"
        />
        <div className="flex-1 min-w-0">
          <p className={clsx('text-sm font-medium', rating.anonymous ? 'text-gray-500 italic' : 'text-gray-900')}>
            {authorName}
          </p>
          <p className="text-xs text-gray-400">
            {formatDistanceToNow(new Date(rating.createdAt), { addSuffix: true })}
          </p>
        </div>
        <ScoreBadge score={rating.score} size="sm" />
      </div>

      {/* Content */}
      {rating.title && (
        <p className="text-sm font-medium text-gray-800 mb-1">{rating.title}</p>
      )}
      {rating.body && (
        <p className="text-sm text-gray-700 leading-relaxed mb-3">{rating.body}</p>
      )}

      {/* Tags */}
      {rating.tags && rating.tags.length > 0 && (
        <div className="flex flex-wrap gap-1.5 mb-3">
          {rating.tags.map(tag => (
            <Tag key={tag} label={tag} positive={isPositiveTag(tag)} size="sm" />
          ))}
        </div>
      )}

      {/* Footer actions */}
      <div className="flex items-center justify-between pt-2 border-t border-gray-50">
        <button
          onClick={handleHelpful}
          disabled={!isAuthenticated || isOwner}
          className={clsx(
            'flex items-center gap-1.5 text-xs px-2 py-1 rounded-lg transition-colors',
            markedHelpful
              ? 'bg-brand-50 text-brand-700 border border-brand-200'
              : 'text-gray-500 hover:bg-gray-100 border border-transparent',
            (!isAuthenticated || isOwner) && 'cursor-default opacity-50'
          )}
        >
          <svg className="w-3.5 h-3.5" fill={markedHelpful ? 'currentColor' : 'none'}
            viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
            <path strokeLinecap="round" strokeLinejoin="round"
              d="M14 10h4.764a2 2 0 011.789 2.894l-3.5 7A2 2 0 0115.263 21h-4.017c-.163 0-.326-.02-.485-.06L7 20m7-10V5a2 2 0 00-2-2h-.095c-.5 0-.905.405-.905.905 0 .714-.211 1.412-.608 2.006L7 11v9m7-10h-2M7 20H5a2 2 0 01-2-2v-6a2 2 0 012-2h2.5" />
          </svg>
          Helpful {helpfulCount > 0 && `(${helpfulCount})`}
        </button>

        <div className="flex items-center gap-2">
          {canDelete && (
            <button onClick={handleDelete}
              className="text-xs text-red-500 hover:text-red-700 transition-colors">
              Delete
            </button>
          )}
          {isAuthenticated && !isOwner && (
            <button
              onClick={handleReport}
              disabled={reporting || reported}
              className="text-xs text-gray-400 hover:text-gray-600 transition-colors disabled:opacity-50"
            >
              {reported ? 'Reported' : 'Report'}
            </button>
          )}
        </div>
      </div>
    </div>
  )
}
