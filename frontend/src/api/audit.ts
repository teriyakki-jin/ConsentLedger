import client from './client'

export interface AuditLogResponse {
    id: number
    ts: string
    action: string
    objectType: string
    objectId: string
    actorUserId: string | null
    actorAgentId: string | null
    outcome: string
    payloadHash: string
    prevHash: string
    hash: string
}

export interface AuditVerifyResponse {
    valid: boolean
    totalChecked: number
    firstBrokenId: number | null
}

export interface AuditListParams {
    action?: string
    objectType?: string
    from?: string
    to?: string
    page?: number
    size?: number
}

export interface PagedAuditResponse {
    data: AuditLogResponse[]
    meta: { total: number; page: number; size: number }
}

export const getAuditLogs = (params: AuditListParams = {}) =>
    client.get<PagedAuditResponse>('/admin/audit-logs', { params }).then((r) => r.data)

export const verifyAuditChain = () =>
    client.get<{ data: AuditVerifyResponse }>('/admin/audit-logs/verify').then((r) => r.data.data)
