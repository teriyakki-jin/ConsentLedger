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
    totalLogs: number
    verifiedCount: number
    firstBrokenLogId: number | null
    reason: string | null
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

export interface AnomalyFinding {
    patternType: string
    severity: 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL'
    description: string
    affectedActor: string
    evidenceLogIds: number[]
    recommendation: string
}

export interface AnomalyReport {
    analyzedAt: string
    periodDays: number
    totalLogsAnalyzed: number
    findings: AnomalyFinding[]
    riskSummary: string
    hasCriticalFindings: boolean
    analysisStatus: 'SUCCESS' | 'ANALYSIS_FAILED' | 'NO_LOGS'
}

export interface TamperResult {
    tamperedLogId: number
    chainValid: boolean
    totalChecked: number
    firstBrokenId: number
    message: string
}

export interface TamperStatus {
    totalLogs: number
    tamperedLogs: number
    isTampered: boolean
}

export const getAuditLogs = (params: AuditListParams = {}) =>
    client.get<PagedAuditResponse>('/admin/audit-logs', { params }).then((r) => r.data)

export const verifyAuditChain = () =>
    client.get<{ data: AuditVerifyResponse }>('/admin/audit-logs/verify').then((r) => r.data.data)

export const analyzeAnomalies = (days = 7) =>
    client.get<{ data: AnomalyReport }>('/admin/audit-logs/analyze', { params: { days } }).then((r) => r.data.data)

export const generateDemoScenario = () =>
    client.post<{ data: { generatedLogs: number; message: string } }>('/admin/demo/anomaly-scenario').then((r) => r.data.data)

export const tamperLog = (logId: number) =>
    client.post<{ data: TamperResult }>(`/admin/demo/tamper/${logId}`).then((r) => r.data.data)

export const getTamperStatus = () =>
    client.get<{ data: TamperStatus }>('/admin/demo/tamper/status').then((r) => r.data.data)
