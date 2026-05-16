import { useNavigate } from 'react-router-dom'
import { useForm, Controller } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { placesApi } from '@/api/places.api'
import { queryKeys } from '@/lib/queryClient'
import { Input, Textarea, Select, Button } from '@/components/ui'
import type { PlaceCategory } from '@/types'
import { CATEGORY_LABELS } from '@/utils'
import { useState } from 'react'

const schema = z.object({
  name:        z.string().min(2, 'Name is required').max(255),
  category:    z.string().min(1, 'Category is required'),
  description: z.string().max(2000).optional(),
  address:     z.string().max(500).optional(),
  city:        z.string().max(100).optional(),
  country:     z.string().max(100).optional(),
  latitude:    z.number({ invalid_type_error: 'Valid latitude required' }).min(-90).max(90),
  longitude:   z.number({ invalid_type_error: 'Valid longitude required' }).min(-180).max(180),
  website:     z.string().url('Must be a valid URL').optional().or(z.literal('')),
})
type FormData = z.infer<typeof schema>

const CATEGORY_OPTIONS = [
  { value: '', label: 'Select category...' },
  ...Object.entries(CATEGORY_LABELS).map(([v, l]) => ({ value: v, label: l })),
]

export function AddPlacePage() {
  const navigate = useNavigate()
  const queryClient = useQueryClient()
  const [geoLoading, setGeoLoading] = useState(false)

  const { register, handleSubmit, setValue, formState: { errors } } = useForm<FormData>({
    resolver: zodResolver(schema),
  })

  const { mutate, isPending, error } = useMutation({
    mutationFn: (data: FormData) => placesApi.create({
      name: data.name,
      category: data.category as PlaceCategory,
      description: data.description,
      address: data.address,
      city: data.city,
      country: data.country,
      latitude: data.latitude,
      longitude: data.longitude,
      website: data.website || undefined,
    }),
    onSuccess: (place) => {
      queryClient.invalidateQueries({ queryKey: queryKeys.places.all })
      navigate(`/place/${place.id}`)
    },
  })

  const useMyLocation = () => {
    setGeoLoading(true)
    navigator.geolocation.getCurrentPosition(
      (pos) => {
        setValue('latitude', parseFloat(pos.coords.latitude.toFixed(6)))
        setValue('longitude', parseFloat(pos.coords.longitude.toFixed(6)))
        setGeoLoading(false)
      },
      () => setGeoLoading(false)
    )
  }

  const errMsg = (error as { response?: { data?: { message?: string } } })?.response?.data?.message

  return (
    <div className="max-w-xl mx-auto px-4 py-6">
      <div className="flex items-center gap-3 mb-6">
        <button onClick={() => navigate(-1)} className="text-gray-400 hover:text-gray-600 transition-colors">
          <svg className="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
            <path strokeLinecap="round" strokeLinejoin="round" d="M15 19l-7-7 7-7" />
          </svg>
        </button>
        <div>
          <h1 className="text-base font-semibold text-gray-900">Add a place</h1>
          <p className="text-sm text-gray-500">Help others discover safe places</p>
        </div>
      </div>

      <form onSubmit={handleSubmit(d => mutate(d))} className="flex flex-col gap-4">
        <div className="bg-white rounded-xl border border-gray-100 p-4 flex flex-col gap-4">
          <Input label="Place name" placeholder="e.g. Gariahat Market" {...register('name')} error={errors.name?.message} />
          <Select label="Category" options={CATEGORY_OPTIONS} {...register('category')} error={errors.category?.message} />
          <Textarea label="Description (optional)" placeholder="What is this place? What makes it notable?"
            rows={3} {...register('description')} error={errors.description?.message} />
        </div>

        <div className="bg-white rounded-xl border border-gray-100 p-4 flex flex-col gap-4">
          <h2 className="text-sm font-medium text-gray-700">Location</h2>
          <div className="grid grid-cols-2 gap-3">
            <Input
              label="Latitude"
              type="number"
              step="0.000001"
              placeholder="22.5726"
              {...register('latitude', { valueAsNumber: true })}
              error={errors.latitude?.message}
            />
            <Input
              label="Longitude"
              type="number"
              step="0.000001"
              placeholder="88.3639"
              {...register('longitude', { valueAsNumber: true })}
              error={errors.longitude?.message}
            />
          </div>
          <Button type="button" variant="secondary" size="sm" loading={geoLoading} onClick={useMyLocation}>
            Use my current location
          </Button>
          <Input label="Address" placeholder="Street address" {...register('address')} />
          <div className="grid grid-cols-2 gap-3">
            <Input label="City" placeholder="Kolkata" {...register('city')} />
            <Input label="Country" placeholder="India" {...register('country')} />
          </div>
        </div>

        <div className="bg-white rounded-xl border border-gray-100 p-4">
          <Input label="Website (optional)" type="url" placeholder="https://..."
            {...register('website')} error={errors.website?.message} />
        </div>

        {errMsg && <p className="text-sm text-red-600 text-center">{errMsg}</p>}

        <Button type="submit" loading={isPending} fullWidth size="lg">
          Add place
        </Button>
      </form>
    </div>
  )
}
