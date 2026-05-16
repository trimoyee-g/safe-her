import { apiClient } from './axios'
import type {
  ApiResponse, PagedResponse, RatingResponse, PlaceRatingSummary,
  CreateRatingRequest, UpdateRatingRequest, RatingSortBy, RatingSearchParams,
} from '@/types'

const BASE = '/v1/ratings'

export const ratingsApi = {
  getById: async (id: string): Promise<RatingResponse> => {
    const res = await apiClient.get<ApiResponse<RatingResponse>>(`${BASE}/${id}`)
    return res.data.data
  },

  getByPlace: async (
    placeId: string,
    sortBy: RatingSortBy = 'NEWEST',
    page = 0,
    size = 20
  ): Promise<PagedResponse<RatingResponse>> => {
    const res = await apiClient.get<ApiResponse<PagedResponse<RatingResponse>>>(
      `${BASE}/place/${placeId}`, { params: { sortBy, page, size } }
    )
    return res.data.data
  },

  getSummary: async (placeId: string): Promise<PlaceRatingSummary> => {
    const res = await apiClient.get<ApiResponse<PlaceRatingSummary>>(
      `${BASE}/place/${placeId}/summary`
    )
    return res.data.data
  },

  getByUser: async (
    userId: string, page = 0, size = 20
  ): Promise<PagedResponse<RatingResponse>> => {
    const res = await apiClient.get<ApiResponse<PagedResponse<RatingResponse>>>(
      `${BASE}/user/${userId}`, { params: { page, size } }
    )
    return res.data.data
  },

  myReviews: async (page = 0, size = 20): Promise<PagedResponse<RatingResponse>> => {
    const res = await apiClient.get<ApiResponse<PagedResponse<RatingResponse>>>(
      `${BASE}/my-reviews`, { params: { page, size } }
    )
    return res.data.data
  },

  search: async (params: RatingSearchParams): Promise<PagedResponse<RatingResponse>> => {
    const res = await apiClient.get<ApiResponse<PagedResponse<RatingResponse>>>(
      `${BASE}/search`, { params }
    )
    return res.data.data
  },

  create: async (data: CreateRatingRequest): Promise<RatingResponse> => {
    const res = await apiClient.post<ApiResponse<RatingResponse>>(BASE, data)
    return res.data.data
  },

  update: async (id: string, data: UpdateRatingRequest): Promise<RatingResponse> => {
    const res = await apiClient.put<ApiResponse<RatingResponse>>(`${BASE}/${id}`, data)
    return res.data.data
  },

  delete: async (id: string): Promise<void> => {
    await apiClient.delete(`${BASE}/${id}`)
  },

  markHelpful: async (id: string): Promise<void> => {
    await apiClient.post(`${BASE}/${id}/helpful`)
  },

  report: async (id: string): Promise<void> => {
    await apiClient.post(`${BASE}/${id}/report`)
  },
}
