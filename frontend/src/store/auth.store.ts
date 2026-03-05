import { create } from 'zustand'
import { persist } from 'zustand/middleware'

interface AuthState {
    token: string | null
    email: string | null
    role: 'USER' | 'ADMIN' | null
    setAuth: (token: string, email: string, role: 'USER' | 'ADMIN') => void
    logout: () => void
}

export const useAuthStore = create<AuthState>()(
    persist(
        (set) => ({
            token: null,
            email: null,
            role: null,
            setAuth: (token, email, role) => set({ token, email, role }),
            logout: () => set({ token: null, email: null, role: null }),
        }),
        { name: 'cl-auth' }
    )
)
