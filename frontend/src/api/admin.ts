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

export interface McpToolSummary {
    name: string
    description: string
    sourceClass: string
}

export interface McpServerStatus {
    serverName: string
    serverVersion: string
    transport: string
    sseEndpoint: string
    messageEndpointTemplate: string
    adminProtected: boolean
    registeredTools: number
    tools: McpToolSummary[]
}

export interface McpInvokeResponse {
    toolName: string
    executedAt: string
    output: string
}

export const getUsers = () =>
    client.get<{ data: UserSummary[] }>('/admin/users').then((r) => r.data.data)

export const getAgents = () =>
    client.get<{ data: AgentSummary[] }>('/admin/agents').then((r) => r.data.data)

export const getMcpStatus = () =>
    client.get<{ data: McpServerStatus }>('/admin/mcp').then((r) => r.data.data)

export const invokeMcpTool = (toolName: string, params?: Record<string, unknown>) =>
    client.post<{ data: McpInvokeResponse }>('/admin/mcp/invoke', {
        toolName,
        params: params ?? {},
    }).then((r) => r.data.data)

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
