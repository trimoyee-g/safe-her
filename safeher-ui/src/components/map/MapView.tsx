import { useEffect, useRef, useCallback } from 'react'
import L from 'leaflet'
import type { PlaceSummaryResponse } from '@/types'
import { scoreColor } from '@/utils'

// Fix Leaflet default marker icons
delete (L.Icon.Default.prototype as unknown as Record<string, unknown>)._getIconUrl
L.Icon.Default.mergeOptions({
  iconRetinaUrl: 'https://unpkg.com/leaflet@1.9.4/dist/images/marker-icon-2x.png',
  iconUrl: 'https://unpkg.com/leaflet@1.9.4/dist/images/marker-icon.png',
  shadowUrl: 'https://unpkg.com/leaflet@1.9.4/dist/images/marker-shadow.png',
})

interface MapViewProps {
  places: PlaceSummaryResponse[]
  center?: [number, number]
  zoom?: number
  onBoundsChange?: (lat: number, lng: number, radiusMeters: number) => void
  onPlaceClick?: (place: PlaceSummaryResponse) => void
  selectedPlaceId?: string
  height?: string
}

function getScoreDot(score: number): string {
  let color = '#9CA3AF'
  if (score === 0) color = '#9CA3AF'
  else if (score >= 4.0) color = '#1D9E75'
  else if (score >= 2.5) color = '#F59E0B'
  else color = '#DC2626'

  return `
    <div style="
      width: 36px; height: 36px;
      background: ${color};
      border: 2.5px solid white;
      border-radius: 50%;
      display: flex; align-items: center; justify-content: center;
      font-size: 11px; font-weight: 600; color: white;
      box-shadow: 0 2px 6px rgba(0,0,0,0.25);
      cursor: pointer;
    ">${score > 0 ? score.toFixed(1) : '—'}</div>
  `
}

export function MapView({
  places,
  center = [12.9716, 77.5946],
  zoom = 14,
  onBoundsChange,
  onPlaceClick,
  selectedPlaceId,
  height = '100%',
}: MapViewProps) {
  const mapRef = useRef<L.Map | null>(null)
  const mapElRef = useRef<HTMLDivElement>(null)
  const markersRef = useRef<L.Marker[]>([])
  const userMarkerRef = useRef<L.CircleMarker | null>(null)

  // Init map
  useEffect(() => {
    if (!mapElRef.current || mapRef.current) return

    const map = L.map(mapElRef.current, { center, zoom, zoomControl: false })
    L.control.zoom({ position: 'bottomright' }).addTo(map)

    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
      attribution: '© OpenStreetMap contributors',
      maxZoom: 19,
    }).addTo(map)

    mapRef.current = map

    // Bounds change → trigger geo-search
    const handleMoveEnd = () => {
      if (!onBoundsChange) return
      const c = map.getCenter()
      const bounds = map.getBounds()
      const ne = bounds.getNorthEast()
      const sw = bounds.getSouthWest()
      const radiusMeters = Math.min(
        map.distance(c, ne),
        25_000
      )
      onBoundsChange(c.lat, c.lng, radiusMeters)
    }

    map.on('moveend', handleMoveEnd)
    setTimeout(handleMoveEnd, 100)

    return () => {
      map.off('moveend', handleMoveEnd)
      map.remove()
      mapRef.current = null
    }
  }, [])

  // Update markers when places change
  useEffect(() => {
    const map = mapRef.current
    if (!map) return

    markersRef.current.forEach(m => m.remove())
    markersRef.current = []

    places.forEach(place => {
      const icon = L.divIcon({
        html: getScoreDot(place.safetyScore),
        className: '',
        iconSize: [36, 36],
        iconAnchor: [18, 18],
        popupAnchor: [0, -20],
      })

      const marker = L.marker([place.latitude, place.longitude], { icon })
      marker.bindPopup(`
        <div style="font-family: Inter, sans-serif; min-width: 160px;">
          <p style="font-weight: 600; font-size: 13px; margin: 0 0 2px;">${place.name}</p>
          <p style="font-size: 11px; color: #6B7280; margin: 0 0 6px;">${place.totalRatings} review${place.totalRatings !== 1 ? 's' : ''}</p>
          <a href="/place/${place.id}" style="font-size: 12px; color: #1D9E75; font-weight: 500;">View details →</a>
        </div>
      `, { offset: [0, -8] })

      if (onPlaceClick) {
        marker.on('click', () => onPlaceClick(place))
      }

      marker.addTo(map)
      markersRef.current.push(marker)

      if (selectedPlaceId === place.id) {
        marker.openPopup()
      }
    })
  }, [places, selectedPlaceId, onPlaceClick])

  // Show user location
  const showUserLocation = useCallback(() => {
    const map = mapRef.current
    if (!map) return
    navigator.geolocation.getCurrentPosition((pos) => {
      const { latitude: lat, longitude: lng } = pos.coords
      if (userMarkerRef.current) userMarkerRef.current.remove()
      userMarkerRef.current = L.circleMarker([lat, lng], {
        radius: 8,
        fillColor: '#1D9E75',
        color: 'white',
        weight: 2.5,
        fillOpacity: 1,
      }).addTo(map)
      map.flyTo([lat, lng], 15, { duration: 1 })
    })
  }, [])

  return (
    <div className="relative" style={{ height }}>
      <div ref={mapElRef} className="w-full h-full rounded-inherit" />
      {/* User location button */}
      <button
        onClick={showUserLocation}
        className="absolute bottom-16 right-3 z-[1000] bg-white border border-gray-200 rounded-lg p-2 shadow-sm hover:bg-gray-50 transition-colors"
        title="My location"
        aria-label="Center map on my location"
      >
        <svg className="w-5 h-5 text-brand-600" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
          <path strokeLinecap="round" strokeLinejoin="round"
            d="M17.657 16.657L13.414 20.9a1.998 1.998 0 01-2.827 0l-4.244-4.243a8 8 0 1111.314 0z" />
          <path strokeLinecap="round" strokeLinejoin="round" d="M15 11a3 3 0 11-6 0 3 3 0 016 0z" />
        </svg>
      </button>
    </div>
  )
}
