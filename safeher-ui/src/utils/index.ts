import type { PlaceCategory } from '@/types'

// ── Safety score helpers ──────────────────────────────────────────────────────

export function scoreColor(score: number): string {
  if (score === 0) return 'bg-gray-400 text-white'
  if (score >= 4.0) return 'bg-brand-400 text-white'
  if (score >= 2.5) return 'bg-amber-500 text-white'
  return 'bg-red-600 text-white'
}

export function scoreBorderColor(score: number): string {
  if (score === 0) return 'border-gray-300 text-gray-500'
  if (score >= 4.0) return 'border-brand-400 text-brand-600'
  if (score >= 2.5) return 'border-amber-400 text-amber-600'
  return 'border-red-400 text-red-600'
}

export function scoreLabel(score: number): string {
  if (score === 0) return 'No ratings yet'
  if (score >= 4.5) return 'Very safe'
  if (score >= 4.0) return 'Safe'
  if (score >= 3.0) return 'Generally safe'
  if (score >= 2.0) return 'Use caution'
  return 'Not safe'
}

export function scoreTextColor(score: number): string {
  if (score === 0) return 'text-gray-400'
  if (score >= 4.0) return 'text-brand-600'
  if (score >= 2.5) return 'text-amber-600'
  return 'text-red-600'
}

// ── Category helpers ──────────────────────────────────────────────────────────

export const CATEGORY_LABELS: Record<PlaceCategory, string> = {
  RESTAURANT: 'Restaurant',
  CAFE: 'Café',
  BAR: 'Bar',
  PARK: 'Park',
  TRANSIT_STATION: 'Transit station',
  HOSPITAL: 'Hospital',
  PHARMACY: 'Pharmacy',
  HOTEL: 'Hotel',
  SHOPPING_MALL: 'Shopping mall',
  MARKET: 'Market',
  BANK: 'Bank',
  ATM: 'ATM',
  SCHOOL: 'School',
  COLLEGE: 'College',
  GYM: 'Gym',
  SALON: 'Salon',
  RELIGIOUS_SITE: 'Religious site',
  GOVERNMENT_OFFICE: 'Government office',
  POLICE_STATION: 'Police station',
  ENTERTAINMENT: 'Entertainment',
  SPORTS_VENUE: 'Sports venue',
  STREET: 'Street',
  NEIGHBORHOOD: 'Neighborhood',
  OTHER: 'Other',
}

export const CATEGORY_ICONS: Record<PlaceCategory, string> = {
  RESTAURANT: '🍽️',
  CAFE: '☕',
  BAR: '🍺',
  PARK: '🌳',
  TRANSIT_STATION: '🚉',
  HOSPITAL: '🏥',
  PHARMACY: '💊',
  HOTEL: '🏨',
  SHOPPING_MALL: '🛍️',
  MARKET: '🏪',
  BANK: '🏦',
  ATM: '💳',
  SCHOOL: '🏫',
  COLLEGE: '🎓',
  GYM: '🏋️',
  SALON: '💇',
  RELIGIOUS_SITE: '🕌',
  GOVERNMENT_OFFICE: '🏛️',
  POLICE_STATION: '👮',
  ENTERTAINMENT: '🎭',
  SPORTS_VENUE: '🏟️',
  STREET: '🛣️',
  NEIGHBORHOOD: '🏘️',
  OTHER: '📍',
}

// ── Safety tags ───────────────────────────────────────────────────────────────

export const POSITIVE_TAGS = [
  'well-lit', 'cctv', 'busy area', 'police nearby',
  'good transport links', 'security guards', 'safe at night',
  'family friendly', 'open spaces',
]

export const NEGATIVE_TAGS = [
  'poor lighting', 'isolated', 'felt unsafe at night',
  'harassment', 'pickpocketing', 'no cctv', 'deserted',
  'bad neighbourhood',
]

export const ALL_TAGS = [...POSITIVE_TAGS, ...NEGATIVE_TAGS]

export function isPositiveTag(tag: string): boolean {
  return POSITIVE_TAGS.includes(tag)
}

// ── Formatting ─────────────────────────────────────────────────────────────────

export function formatDistance(meters: number): string {
  if (meters < 1000) return `${Math.round(meters)}m`
  return `${(meters / 1000).toFixed(1)}km`
}

export function formatScore(score: number): string {
  if (score === 0) return '—'
  return score.toFixed(1)
}

export function formatCount(n: number): string {
  if (n >= 1000) return `${(n / 1000).toFixed(1)}k`
  return n.toString()
}
