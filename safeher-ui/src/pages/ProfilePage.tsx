import { useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { usersApi } from '@/api/users.api'
import { ratingsApi } from '@/api/ratings.api'
import { placesApi } from '@/api/places.api'
import { queryKeys } from '@/lib/queryClient'
import { useAuthStore } from '@/store/auth.store'
import { Avatar, Button, Spinner, EmptyState } from '@/components/ui'
import { ReviewCard } from '@/components/rating/ReviewCard'
import { PlaceCard } from '@/components/place/PlaceCard'
import { Link, useNavigate } from 'react-router-dom'
import { authApi } from '@/api/auth.api'

type Tab = 'reviews' | 'places' | 'settings'

export function ProfilePage() {
  const { userId, role, logout, refreshToken } = useAuthStore()
  const navigate = useNavigate()
  const [tab, setTab] = useState<Tab>('reviews')

  const { data: user, isLoading } = useQuery({
    queryKey: queryKeys.users.me,
    queryFn: usersApi.getMe,
    enabled: !!userId,
  })

  const { data: reviewsPage } = useQuery({
    queryKey: queryKeys.ratings.mine(0),
    queryFn: () => ratingsApi.myReviews(0, 20),
    enabled: tab === 'reviews' && !!userId,
  })

  const { data: placesPage } = useQuery({
    queryKey: queryKeys.places.myPlaces(0),
    queryFn: () => placesApi.myPlaces(0, 20),
    enabled: tab === 'places' && !!userId,
  })

  const handleLogout = async () => {
    try { await authApi.logout(refreshToken ?? undefined) } finally {
      logout()
      navigate('/login')
    }
  }

  if (isLoading) return (
    <div className="flex justify-center py-20"><Spinner size="lg" className="text-brand-400" /></div>
  )

  if (!user) return null

  return (
    <div className="max-w-2xl mx-auto px-4 py-6">
      {/* Profile header */}
      <div className="bg-brand-400 rounded-2xl p-5 mb-4">
        <div className="flex items-start gap-4">
          <Avatar name={user.displayName ?? user.username} src={user.avatarUrl} size="lg" />
          <div className="flex-1 min-w-0">
            <div className="flex items-center gap-2 mb-0.5">
              <h1 className="text-base font-semibold text-white truncate">
                {user.displayName ?? user.username}
              </h1>
              {user.verified && (
                <svg className="w-4 h-4 text-brand-950/70 flex-shrink-0" fill="currentColor" viewBox="0 0 20 20">
                  <path fillRule="evenodd" d="M6.267 3.455a3.066 3.066 0 001.745-.723 3.066 3.066 0 013.976 0 3.066 3.066 0 001.745.723 3.066 3.066 0 012.812 2.812c.051.643.304 1.254.723 1.745a3.066 3.066 0 010 3.976 3.066 3.066 0 00-.723 1.745 3.066 3.066 0 01-2.812 2.812 3.066 3.066 0 00-1.745.723 3.066 3.066 0 01-3.976 0 3.066 3.066 0 00-1.745-.723 3.066 3.066 0 01-2.812-2.812 3.066 3.066 0 00-.723-1.745 3.066 3.066 0 010-3.976 3.066 3.066 0 00.723-1.745 3.066 3.066 0 012.812-2.812zm7.44 5.252a1 1 0 00-1.414-1.414L9 10.586 7.707 9.293a1 1 0 00-1.414 1.414l2 2a1 1 0 001.414 0l4-4z" clipRule="evenodd" />
                </svg>
              )}
            </div>
            <p className="text-brand-950/70 text-xs">@{user.username}</p>
            {user.city && <p className="text-brand-950/70 text-xs mt-0.5">{user.city}</p>}
          </div>
        </div>

        {/* Stats */}
        <div className="flex gap-5 mt-4 pt-4 border-t border-brand-950/20">
          <div>
            <p className="text-white font-semibold text-lg leading-none">{user.totalReviews}</p>
            <p className="text-brand-950/70 text-xs mt-0.5">Reviews</p>
          </div>
          <div>
            <p className="text-white font-semibold text-lg leading-none">{user.helpfulVotes}</p>
            <p className="text-brand-950/70 text-xs mt-0.5">Helpful votes</p>
          </div>
          <div>
            <p className="text-white font-semibold text-lg leading-none capitalize">{role?.toLowerCase()}</p>
            <p className="text-brand-950/70 text-xs mt-0.5">Role</p>
          </div>
        </div>
      </div>

      {/* Tabs */}
      <div className="flex gap-1 bg-gray-900 rounded-xl p-1 mb-4">
        {(['reviews', 'places', 'settings'] as Tab[]).map(t => (
          <button key={t} onClick={() => setTab(t)}
            className={`flex-1 py-2 text-sm font-medium rounded-lg capitalize transition-colors ${
              tab === t ? 'bg-gray-800 text-gray-100 shadow-sm shadow-black/20' : 'text-gray-500 hover:text-gray-300'
            }`}>
            {t}
          </button>
        ))}
      </div>

      {/* Tab content */}
      {tab === 'reviews' && (
        reviewsPage?.content.length === 0 ? (
          <EmptyState title="No reviews yet" description="Share your safety experience at places you've visited" />
        ) : (
          <div className="flex flex-col gap-3">
            {reviewsPage?.content.map(r => <ReviewCard key={r.id} rating={r} />)}
          </div>
        )
      )}

      {tab === 'places' && (
        placesPage?.content.length === 0 ? (
          <EmptyState
            title="No places added yet"
            description="Help others by adding places that aren't on the map yet"
            action={<Link to="/add-place"><Button size="sm">Add a place</Button></Link>}
          />
        ) : (
          <div className="flex flex-col gap-2">
            {placesPage?.content.map(p => <PlaceCard key={p.id} place={p} />)}
          </div>
        )
      )}

      {tab === 'settings' && (
        <div className="bg-gray-900 rounded-xl border border-gray-800 overflow-hidden">
          <div className="divide-y divide-gray-800">
            <Link to="/settings/account" className="flex items-center justify-between px-4 py-3 hover:bg-gray-800 transition-colors">
              <span className="text-sm text-gray-300">Account settings</span>
              <svg className="w-4 h-4 text-gray-500" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
                <path strokeLinecap="round" strokeLinejoin="round" d="M9 5l7 7-7 7" />
              </svg>
            </Link>
            <Link to="/settings/security" className="flex items-center justify-between px-4 py-3 hover:bg-gray-800 transition-colors">
              <span className="text-sm text-gray-300">Security & password</span>
              <svg className="w-4 h-4 text-gray-500" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
                <path strokeLinecap="round" strokeLinejoin="round" d="M9 5l7 7-7 7" />
              </svg>
            </Link>
            <div className="px-4 py-3">
              <Button variant="danger" size="sm" onClick={handleLogout}>Sign out</Button>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}
