import { apiClient } from './axios'
import type { ApiResponse, ChatRequest, ChatResponse, ReviewAssistRequest } from '@/types'

const BASE = '/v1/ai'

export const aiApi = {
  chat: async (data: ChatRequest): Promise<ChatResponse> => {
    const res = await apiClient.post<ApiResponse<ChatResponse>>(`${BASE}/chat`, data)
    return res.data.data
  },

  reviewAssist: async (data: ReviewAssistRequest): Promise<string> => {
    const res = await apiClient.post<ApiResponse<string>>(`${BASE}/review-assist`, data)
    return res.data.data
  },

  generateSummary: async (placeId: string): Promise<string> => {
    const res = await apiClient.post<ApiResponse<string>>(`${BASE}/places/${placeId}/summary`)
    return res.data.data
  },
}
