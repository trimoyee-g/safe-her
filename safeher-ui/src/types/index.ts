// ── Auth ──────────────────────────────────────────────────────────────────────

export interface AuthResponse {
  userId: string
  username: string
  email: string
  role: 'USER' | 'ADMIN' | 'MODERATOR'
  accessToken: string
  refreshToken: string
  accessTokenExpiresIn: number
  refreshTokenExpiresIn: number
  issuedAt: string
}

export interface RegisterRequest {
  username: string
  email: string
  password: string
  displayName?: string
  city?: string
  country?: string
}

export interface LoginRequest {
  identifier: string
  password: string
  deviceInfo?: string
}

export interface RefreshTokenRequest {
  refreshToken: string
}

export interface ChangePasswordRequest {
  currentPassword: string
  newPassword: string
}

export interface ForgotPasswordRequest {
  email: string
}

export interface ResetPasswordRequest {
  token: string
  newPassword: string
}

// ── User ──────────────────────────────────────────────────────────────────────

export type Role = 'USER' | 'ADMIN' | 'MODERATOR'
export type Gender = 'MALE' | 'FEMALE' | 'NON_BINARY' | 'PREFER_NOT_TO_SAY'

export interface UserResponse {
  id: string
  authUserId: string
  username: string
  email: string
  displayName?: string
  bio?: string
  avatarUrl?: string
  phoneNumber?: string
  gender?: Gender
  dateOfBirth?: string
  city?: string
  country?: string
  latitude?: number
  longitude?: number
  role: Role
  anonymous: boolean
  active: boolean
  verified: boolean
  totalReviews: number
  helpfulVotes: number
  createdAt: string
  updatedAt: string
  lastSeenAt?: string
}

export interface PublicUserResponse {
  id: string
  username: string
  displayName?: string
  avatarUrl?: string
  bio?: string
  city?: string
  country?: string
  verified: boolean
  totalReviews: number
  helpfulVotes: number
}

export interface UpdateUserRequest {
  displayName?: string
  bio?: string
  phoneNumber?: string
  gender?: Gender
  dateOfBirth?: string
  city?: string
  country?: string
  latitude?: number
  longitude?: number
  anonymous?: boolean
}

// ── Place ─────────────────────────────────────────────────────────────────────

export type PlaceCategory =
  | 'RESTAURANT' | 'CAFE' | 'BAR' | 'PARK' | 'TRANSIT_STATION'
  | 'HOSPITAL' | 'PHARMACY' | 'HOTEL' | 'SHOPPING_MALL' | 'MARKET'
  | 'BANK' | 'ATM' | 'SCHOOL' | 'COLLEGE' | 'GYM' | 'SALON'
  | 'RELIGIOUS_SITE' | 'GOVERNMENT_OFFICE' | 'POLICE_STATION'
  | 'ENTERTAINMENT' | 'SPORTS_VENUE' | 'STREET' | 'NEIGHBORHOOD' | 'OTHER'

export type PlaceSource = 'GOOGLE' | 'OSM' | 'USER'

export interface PlaceResponse {
  id: string
  externalId?: string
  source: PlaceSource
  name: string
  description?: string
  category: PlaceCategory
  subCategory?: string
  address?: string
  city?: string
  state?: string
  country?: string
  postalCode?: string
  latitude: number
  longitude: number
  phoneNumber?: string
  website?: string
  googleMapsUrl?: string
  openingHours?: Record<string, string>
  photos?: string[]
  amenities?: string[]
  safetyScore: number
  totalRatings: number
  active: boolean
  verified: boolean
  createdBy: string
  createdAt: string
  updatedAt: string
  distanceMeters?: number
}

export interface PlaceSummaryResponse {
  id: string
  name: string
  category: PlaceCategory
  address?: string
  city?: string
  country?: string
  latitude: number
  longitude: number
  safetyScore: number
  totalRatings: number
  verified: boolean
  avatarPhoto?: string
  distanceMeters?: number
}

export interface CreatePlaceRequest {
  name: string
  description?: string
  category: PlaceCategory
  subCategory?: string
  address?: string
  city?: string
  state?: string
  country?: string
  postalCode?: string
  latitude: number
  longitude: number
  phoneNumber?: string
  website?: string
  openingHours?: Record<string, string>
  photos?: string[]
  amenities?: string[]
}

export interface GeoSearchParams {
  latitude: number
  longitude: number
  radiusMeters?: number
  category?: PlaceCategory
  page?: number
  size?: number
}

export interface KeywordSearchParams {
  query: string
  category?: PlaceCategory
  city?: string
  country?: string
  page?: number
  size?: number
}

// ── Rating ────────────────────────────────────────────────────────────────────

export type RatingSortBy = 'NEWEST' | 'OLDEST' | 'HIGHEST_SCORE' | 'LOWEST_SCORE' | 'MOST_HELPFUL'

export interface RatingResponse {
  id: string
  placeId: string
  userId?: string
  authorDisplayName?: string
  authorAvatarUrl?: string
  score: number
  title?: string
  body?: string
  photos?: string[]
  tags?: string[]
  anonymous: boolean
  helpfulCount: number
  markedHelpfulByMe?: boolean
  reportedCount?: number
  createdAt: string
  updatedAt: string
}

export interface PlaceRatingSummary {
  placeId: string
  averageScore: number
  totalRatings: number
  scoreDistribution: Record<number, number>
}

export interface CreateRatingRequest {
  placeId: string
  score: number
  title?: string
  body?: string
  photos?: string[]
  tags?: string[]
  anonymous?: boolean
}

export interface UpdateRatingRequest {
  score?: number
  title?: string
  body?: string
  photos?: string[]
  tags?: string[]
  anonymous?: boolean
}

export interface RatingSearchParams {
  query: string
  placeId?: string
  minScore?: number
  maxScore?: number
  page?: number
  size?: number
}

// ── Shared ────────────────────────────────────────────────────────────────────

export interface ApiResponse<T> {
  success: boolean
  message?: string
  data: T
  timestamp: string
}

export interface PagedResponse<T> {
  content: T[]
  page: number
  size: number
  totalElements: number
  totalPages: number
  last: boolean
}

// ── Rating search params (used by ratingsApi.search) ─────────────────────────
export interface RatingSearchParams {
  query: string
  placeId?: string
  minScore?: number
  maxScore?: number
  page?: number
  size?: number
}

// ── AI ────────────────────────────────────────────────────────────────────────

export interface ChatMessage {
  role: 'user' | 'assistant'
  content: string
}

export interface ChatRequest {
  message: string
  placeIds?: string[]
  history?: ChatMessage[]
}

export interface ChatResponse {
  message: string
  suggestedPlaceIds?: string[]
}

export interface ReviewAssistRequest {
  placeId: string
  score: number
  selectedTags?: string[]
  partialBody?: string
}
