import client from './client'

export interface UserSummary {
    id: string
    email: string
    role: string
    createdAt: string
}

export interface AgentSummary {
    id: string
    name: string
    status: 'ACTIVE' | 'SUSPENDED' | 'REVOKED'
    clientId: string
    lastRotatedAt: string | null
    createdAt: string
}

export const getUsers = () =>
    client.get<{ data: UserSummary[] }>('/admin/users').then((r) => r.data.data)

export const getAgents = () =>
    client.get<{ data: AgentSummary[] }>('/admin/agents').then((r) => r.data.data)

const UUID_RE = /^[\w-]+$/

export const updateAgentStatus = (id: string, status: 'ACTIVE' | 'SUSPENDED' | 'REVOKED') => {
    if (!UUID_RE.test(id)) throw new Error('Invalid agent ID')
    return client.patch<{ data: AgentSummary }>(`/admin/agents/${id}/status`, { status }).then((r) => r.data.data)
}

export const downloadAuditReport = (from: string, to: string) =>
    client.get('/admin/reports/audit', {
        params: { from, to },
        responseType: 'blob',
    }).then((r) => r.data as Blob)
