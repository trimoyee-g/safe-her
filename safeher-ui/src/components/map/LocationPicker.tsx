import { useEffect, useRef } from 'react'
import L from 'leaflet'

interface LocationPickerProps {
  latitude?: number
  longitude?: number
  onPick: (lat: number, lng: number) => void
  height?: string
}

const DEFAULT_CENTER: [number, number] = [12.9716, 77.5946]

export function LocationPicker({ latitude, longitude, onPick, height = '240px' }: LocationPickerProps) {
  const mapElRef = useRef<HTMLDivElement>(null)
  const mapRef = useRef<L.Map | null>(null)
  const markerRef = useRef<L.Marker | null>(null)
  const onPickRef = useRef(onPick)
  onPickRef.current = onPick

  // Init map once
  useEffect(() => {
    if (!mapElRef.current || mapRef.current) return

    const hasInitial = latitude !== undefined && longitude !== undefined
    const map = L.map(mapElRef.current, {
      center: hasInitial ? [latitude!, longitude!] : DEFAULT_CENTER,
      zoom: hasInitial ? 15 : 12,
    })

    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
      attribution: '© OpenStreetMap contributors',
      maxZoom: 19,
    }).addTo(map)

    const placeMarker = (lat: number, lng: number) => {
      if (markerRef.current) {
        markerRef.current.setLatLng([lat, lng])
      } else {
        markerRef.current = L.marker([lat, lng], { draggable: true }).addTo(map)
        markerRef.current.on('dragend', () => {
          const pos = markerRef.current!.getLatLng()
          onPickRef.current(pos.lat, pos.lng)
        })
      }
    }

    if (hasInitial) placeMarker(latitude!, longitude!)

    map.on('click', (e: L.LeafletMouseEvent) => {
      placeMarker(e.latlng.lat, e.latlng.lng)
      onPickRef.current(e.latlng.lat, e.latlng.lng)
    })

    mapRef.current = map

    return () => {
      map.remove()
      mapRef.current = null
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  return (
    <div>
      <div ref={mapElRef} className="w-full rounded-lg border border-gray-700 overflow-hidden" style={{ height }} />
      <p className="text-xs text-gray-500 mt-1.5">Click the map (or drag the pin) to set the exact location.</p>
    </div>
  )
}
