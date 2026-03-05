import client from './client'

export interface LoginRequest {
    email: string
    password: string
}

export interface LoginResponse {
    accessToken: string
    tokenType: string
    expiresIn: number
}

export const login = (data: LoginRequest) =>
    client.post<{ data: LoginResponse }>('/auth/login', data).then((r) => r.data.data)
