import client from './client'

export type TransferStatus = 'REQUESTED' | 'APPROVED' | 'DENIED' | 'COMPLETED' | 'FAILED'

export interface TransferResponse {
    id: string
    consentId: string
    method: string
    status: TransferStatus
    requesterUserId: string | null
    requesterAgentId: string | null
    approvedByUserId: string | null
    idempotencyKey: string
    createdAt: string
    updatedAt: string
}

export interface TransferCreateRequest {
    consentId: string
    method: string
    idempotencyKey: string
}

export const createTransfer = (data: TransferCreateRequest) =>
    client.post<{ data: TransferResponse }>('/transfer-requests', data).then((r) => r.data.data)

export const listTransfers = () =>
    client.get<{ data: TransferResponse[] }>('/transfer-requests').then((r) => r.data.data)

export const approveTransfer = (id: string) =>
    client.post<{ data: TransferResponse }>(`/transfer-requests/${id}/approve`).then((r) => r.data.data)

export const executeTransfer = (id: string) =>
    client.post<{ data: TransferResponse }>(`/transfer-requests/${id}/execute`).then((r) => r.data.data)
