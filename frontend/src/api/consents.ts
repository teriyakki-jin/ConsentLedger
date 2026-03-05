import client from './client'

export type ConsentStatus = 'ACTIVE' | 'REVOKED' | 'EXPIRED'

export interface ConsentResponse {
    id: string
    userId: string
    agentId: string | null
    dataHolderId: string
    dataHolderName: string
    scopes: string[]
    status: ConsentStatus
    expiresAt: string | null
    createdAt: string
    updatedAt: string
}

export interface ConsentCreateRequest {
    dataHolderId: string
    agentId?: string
    scopes: string[]
    expiresAt?: string
}

export const createConsent = (data: ConsentCreateRequest) =>
    client.post<{ data: ConsentResponse }>('/consents', data).then((r) => r.data.data)

export const listConsents = () =>
    client.get<{ data: ConsentResponse[] }>('/consents').then((r) => r.data.data)

export const getConsent = (id: string) =>
    client.get<{ data: ConsentResponse }>(`/consents/${id}`).then((r) => r.data.data)

export const revokeConsent = (id: string) =>
    client.post<{ data: ConsentResponse }>(`/consents/${id}/revoke`).then((r) => r.data.data)
