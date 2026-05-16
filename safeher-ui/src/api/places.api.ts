import { apiClient } from './axios'
import type {
  ApiResponse, PagedResponse, PlaceResponse, PlaceSummaryResponse,
  CreatePlaceRequest, GeoSearchParams, KeywordSearchParams,
} from '@/types'

const BASE = '/v1/places'

export const placesApi = {
  getById: async (id: string): Promise<PlaceResponse> => {
    const res = await apiClient.get<ApiResponse<PlaceResponse>>(`${BASE}/${id}`)
    return res.data.data
  },

  list: async (page = 0, size = 20): Promise<PagedResponse<PlaceSummaryResponse>> => {
    const res = await apiClient.get<ApiResponse<PagedResponse<PlaceSummaryResponse>>>(BASE, {
      params: { page, size },
    })
    return res.data.data
  },

  listByCategory: async (
    category: string, page = 0, size = 20
  ): Promise<PagedResponse<PlaceSummaryResponse>> => {
    const res = await apiClient.get<ApiResponse<PagedResponse<PlaceSummaryResponse>>>(
      `${BASE}/category/${category}`, { params: { page, size } }
    )
    return res.data.data
  },

  topRated: async (): Promise<PlaceSummaryResponse[]> => {
    const res = await apiClient.get<ApiResponse<PlaceSummaryResponse[]>>(`${BASE}/top-rated`)
    return res.data.data
  },

  geoSearch: async (params: GeoSearchParams): Promise<PagedResponse<PlaceSummaryResponse>> => {
    const res = await apiClient.get<ApiResponse<PagedResponse<PlaceSummaryResponse>>>(
      `${BASE}/geo-search`, { params }
    )
    return res.data.data
  },

  keywordSearch: async (params: KeywordSearchParams): Promise<PagedResponse<PlaceSummaryResponse>> => {
    const res = await apiClient.get<ApiResponse<PagedResponse<PlaceSummaryResponse>>>(
      `${BASE}/search`, { params }
    )
    return res.data.data
  },

  myPlaces: async (page = 0, size = 20): Promise<PagedResponse<PlaceSummaryResponse>> => {
    const res = await apiClient.get<ApiResponse<PagedResponse<PlaceSummaryResponse>>>(
      `${BASE}/my-places`, { params: { page, size } }
    )
    return res.data.data
  },

  create: async (data: CreatePlaceRequest): Promise<PlaceResponse> => {
    const res = await apiClient.post<ApiResponse<PlaceResponse>>(BASE, data)
    return res.data.data
  },

  update: async (id: string, data: Partial<CreatePlaceRequest>): Promise<PlaceResponse> => {
    const res = await apiClient.put<ApiResponse<PlaceResponse>>(`${BASE}/${id}`, data)
    return res.data.data
  },

  delete: async (id: string): Promise<void> => {
    await apiClient.delete(`${BASE}/${id}`)
  },

  report: async (id: string): Promise<void> => {
    await apiClient.post(`${BASE}/${id}/report`)
  },
}
