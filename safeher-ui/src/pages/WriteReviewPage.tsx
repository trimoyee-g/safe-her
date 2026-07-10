import { useState } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { useForm, Controller } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { placesApi } from '@/api/places.api'
import { ratingsApi } from '@/api/ratings.api'
import { queryKeys } from '@/lib/queryClient'
import { StarPicker } from '@/components/rating/StarPicker'
import { Button, Input, Textarea, Toggle, Tag, Spinner } from '@/components/ui'
import { ALL_TAGS, isPositiveTag } from '@/utils'
import type { CreateRatingRequest } from '@/types'

const schema = z.object({
  score:     z.number().min(1, 'Please select a score').max(5),
  title:     z.string().max(150).optional(),
  body:      z.string().max(2000).optional(),
  anonymous: z.boolean(),
})
type FormData = z.infer<typeof schema>

export function WriteReviewPage() {
  const { id: placeId } = useParams<{ id: string }>()
  const navigate = useNavigate()
  const queryClient = useQueryClient()
  const [selectedTags, setSelectedTags] = useState<string[]>([])

  const { data: place, isLoading: placeLoading } = useQuery({
    queryKey: queryKeys.places.detail(placeId!),
    queryFn: () => placesApi.getById(placeId!),
    enabled: !!placeId,
  })

  const { control, register, handleSubmit, watch, formState: { errors } } = useForm<FormData>({
    resolver: zodResolver(schema),
    defaultValues: { score: 0, anonymous: false },
  })

  const bodyLength = watch('body')?.length ?? 0

  const { mutate, isPending, error } = useMutation({
    mutationFn: (data: CreateRatingRequest) => ratingsApi.create(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: queryKeys.ratings.byPlace(placeId!, 'NEWEST', 0) })
      queryClient.invalidateQueries({ queryKey: queryKeys.ratings.summary(placeId!) })
      queryClient.invalidateQueries({ queryKey: queryKeys.places.detail(placeId!) })
      navigate(`/place/${placeId}`)
    },
  })

  const onSubmit = (data: FormData) => {
    mutate({
      placeId: placeId!,
      score: data.score,
      title: data.title || undefined,
      body: data.body || undefined,
      tags: selectedTags,
      anonymous: data.anonymous,
    })
  }

  const toggleTag = (tag: string) => {
    setSelectedTags(prev =>
      prev.includes(tag) ? prev.filter(t => t !== tag) : [...prev, tag]
    )
  }

  if (placeLoading) {
    return (
      <div className="flex justify-center py-20">
        <Spinner size="lg" className="text-brand-400" />
      </div>
    )
  }

  return (
    <div className="max-w-xl mx-auto px-4 py-6">
      {/* Header */}
      <div className="flex items-center gap-3 mb-6">
        <button onClick={() => navigate(-1)} className="text-gray-500 hover:text-gray-300 transition-colors">
          <svg className="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
            <path strokeLinecap="round" strokeLinejoin="round" d="M15 19l-7-7 7-7" />
          </svg>
        </button>
        <div>
          <h1 className="text-base font-semibold text-gray-100">Write a review</h1>
          {place && <p className="text-sm text-gray-400">{place.name}</p>}
        </div>
      </div>

      <form onSubmit={handleSubmit(onSubmit)} className="flex flex-col gap-5">
        {/* Score */}
        <div className="bg-gray-900 rounded-xl border border-gray-800 p-5">
          <p className="text-sm font-medium text-gray-300 text-center mb-4">How safe did you feel here?</p>
          <Controller
            name="score"
            control={control}
            render={({ field }) => (
              <StarPicker
                value={field.value}
                onChange={field.onChange}
                error={errors.score?.message}
              />
            )}
          />
        </div>

        {/* Tags */}
        <div className="bg-gray-900 rounded-xl border border-gray-800 p-4">
          <p className="text-sm font-medium text-gray-300 mb-1">What describes this place?</p>
          <p className="text-xs text-gray-500 mb-3">Select all that apply</p>
          <div className="flex flex-wrap gap-2">
            {ALL_TAGS.map(tag => (
              <Tag
                key={tag}
                label={tag}
                positive={isPositiveTag(tag)}
                selected={selectedTags.includes(tag)}
                onClick={() => toggleTag(tag)}
              />
            ))}
          </div>
        </div>

        {/* Review text */}
        <div className="bg-gray-900 rounded-xl border border-gray-800 p-4 flex flex-col gap-3">
          <Input
            label="Title (optional)"
            placeholder="Summarise your experience..."
            {...register('title')}
            error={errors.title?.message}
          />
          <div>
            <Textarea
              label="Share your experience (optional)"
              placeholder="What was it like? Help others feel safer..."
              rows={4}
              {...register('body')}
              error={errors.body?.message}
            />
            <p className="text-right text-xs text-gray-500 mt-1">{bodyLength} / 2000</p>
          </div>
        </div>

        {/* Anonymous toggle */}
        <div className="bg-gray-900 rounded-xl border border-gray-800 p-4">
          <Controller
            name="anonymous"
            control={control}
            render={({ field }) => (
              <Toggle
                checked={field.value}
                onChange={field.onChange}
                label="Post anonymously"
                description="Your name won't appear on this review"
              />
            )}
          />
          {watch('anonymous') && (
            <div className="mt-3 bg-brand-400/10 rounded-lg px-3 py-2">
              <p className="text-xs text-brand-200">
                Your identity is stored securely and only used to prevent duplicate reviews.
                It will never be shown publicly.
              </p>
            </div>
          )}
        </div>

        {error && (
          <p className="text-sm text-red-400 text-center">
            {(error as { response?: { data?: { message?: string } } })?.response?.data?.message ?? 'Failed to post review. Please try again.'}
          </p>
        )}

        <Button type="submit" loading={isPending} fullWidth size="lg">
          Post review
        </Button>
      </form>
    </div>
  )
}
