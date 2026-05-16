import { apiClient } from './axios'
import type {
  ApiResponse, AuthResponse, RegisterRequest, LoginRequest,
  ChangePasswordRequest, ForgotPasswordRequest, ResetPasswordRequest,
} from '@/types'

const BASE = '/v1/auth'

export const authApi = {
  register: async (data: RegisterRequest): Promise<AuthResponse> => {
    const res = await apiClient.post<ApiResponse<AuthResponse>>(`${BASE}/register`, data)
    return res.data.data
  },

  login: async (data: LoginRequest): Promise<AuthResponse> => {
    const res = await apiClient.post<ApiResponse<AuthResponse>>(`${BASE}/login`, data)
    return res.data.data
  },

  logout: async (refreshToken?: string): Promise<void> => {
    await apiClient.post(`${BASE}/logout`, refreshToken ? { refreshToken } : {})
  },

  logoutAll: async (): Promise<void> => {
    await apiClient.post(`${BASE}/logout-all`)
  },

  changePassword: async (data: ChangePasswordRequest): Promise<void> => {
    await apiClient.post(`${BASE}/change-password`, data)
  },

  forgotPassword: async (data: ForgotPasswordRequest): Promise<void> => {
    await apiClient.post(`${BASE}/forgot-password`, data)
  },

  resetPassword: async (data: ResetPasswordRequest): Promise<void> => {
    await apiClient.post(`${BASE}/reset-password`, data)
  },

  validate: async (): Promise<boolean> => {
    try {
      await apiClient.get(`${BASE}/validate`)
      return true
    } catch {
      return false
    }
  },
}
