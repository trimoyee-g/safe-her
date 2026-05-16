import { apiClient } from './axios'
import type { ApiResponse, UserResponse, PublicUserResponse, UpdateUserRequest, PagedResponse } from '@/types'

const BASE = '/v1/users'

export const usersApi = {
  getMe: async (): Promise<UserResponse> => {
    const res = await apiClient.get<ApiResponse<UserResponse>>(`${BASE}/me`)
    return res.data.data
  },

  getById: async (id: string): Promise<UserResponse> => {
    const res = await apiClient.get<ApiResponse<UserResponse>>(`${BASE}/${id}`)
    return res.data.data
  },

  getPublicProfile: async (id: string): Promise<PublicUserResponse> => {
    const res = await apiClient.get<ApiResponse<PublicUserResponse>>(`${BASE}/public/${id}`)
    return res.data.data
  },

  updateMe: async (id: string, data: UpdateUserRequest): Promise<UserResponse> => {
    const res = await apiClient.put<ApiResponse<UserResponse>>(`${BASE}/${id}`, data)
    return res.data.data
  },

  updateAvatar: async (id: string, avatarUrl: string): Promise<UserResponse> => {
    const res = await apiClient.patch<ApiResponse<UserResponse>>(`${BASE}/${id}/avatar`, { avatarUrl })
    return res.data.data
  },

  searchUsers: async (q: string, page = 0, size = 20): Promise<PagedResponse<PublicUserResponse>> => {
    const res = await apiClient.get<ApiResponse<PagedResponse<PublicUserResponse>>>(
      `${BASE}/search`, { params: { q, page, size } }
    )
    return res.data.data
  },

  deleteAccount: async (id: string): Promise<void> => {
    await apiClient.delete(`${BASE}/${id}`)
  },
}
