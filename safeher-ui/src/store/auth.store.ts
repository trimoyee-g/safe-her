import { create } from 'zustand'
import { persist } from 'zustand/middleware'
import type { AuthResponse, Role } from '@/types'

interface AuthState {
  isAuthenticated: boolean
  userId: string | null
  authUserId: string | null
  username: string | null
  email: string | null
  role: Role | null
  accessToken: string | null
  refreshToken: string | null

  login: (auth: AuthResponse) => void
  logout: () => void
  updateTokens: (accessToken: string, refreshToken: string) => void
  isAdmin: () => boolean
}

export const useAuthStore = create<AuthState>()(
  persist(
    (set, get) => ({
      isAuthenticated: false,
      userId: null,
      authUserId: null,
      username: null,
      email: null,
      role: null,
      accessToken: null,
      refreshToken: null,

      login: (auth: AuthResponse) => {
        localStorage.setItem('accessToken', auth.accessToken)
        localStorage.setItem('refreshToken', auth.refreshToken)
        set({
          isAuthenticated: true,
          userId: auth.userId,
          authUserId: auth.userId,
          username: auth.username,
          email: auth.email,
          role: auth.role,
          accessToken: auth.accessToken,
          refreshToken: auth.refreshToken,
        })
      },

      logout: () => {
        localStorage.removeItem('accessToken')
        localStorage.removeItem('refreshToken')
        set({
          isAuthenticated: false,
          userId: null,
          authUserId: null,
          username: null,
          email: null,
          role: null,
          accessToken: null,
          refreshToken: null,
        })
      },

      updateTokens: (accessToken: string, refreshToken: string) => {
        localStorage.setItem('accessToken', accessToken)
        localStorage.setItem('refreshToken', refreshToken)
        set({ accessToken, refreshToken })
      },

      isAdmin: () => {
        const { role } = get()
        return role === 'ADMIN' || role === 'MODERATOR'
      },
    }),
    {
      name: 'safeher-auth',
      partialize: (state) => ({
        isAuthenticated: state.isAuthenticated,
        userId: state.userId,
        username: state.username,
        email: state.email,
        role: state.role,
        accessToken: state.accessToken,
        refreshToken: state.refreshToken,
      }),
    }
  )
)
