import { useEffect, useState } from 'react'
import { getAgents, updateAgentStatus, type AgentSummary } from '../../api/admin'
import Layout from '../../components/Layout'
import { Spinner, EmptyState } from '../../components/Spinner'
import { formatDate } from '../../utils/jwt'

const STATUS_OPTIONS: AgentSummary['status'][] = ['ACTIVE', 'SUSPENDED', 'REVOKED']
const STATUS_LABEL: Record<AgentSummary['status'], string> = {
    ACTIVE: '활성',
    SUSPENDED: '정지',
    REVOKED: '해지',
}
const STATUS_BADGE: Record<AgentSummary['status'], string> = {
    ACTIVE: 'badge-active',
    SUSPENDED: 'badge-revoked',
    REVOKED: 'badge-failed',
}

const isValidStatus = (s: string): s is AgentSummary['status'] =>
    STATUS_OPTIONS.includes(s as AgentSummary['status'])

export default function AgentsPage() {
    const [agents, setAgents] = useState<AgentSummary[]>([])
    const [loading, setLoading] = useState(true)
    const [updating, setUpdating] = useState<string | null>(null)
    const [fetchError, setFetchError] = useState<string | null>(null)
    const [updateError, setUpdateError] = useState<string | null>(null)

    useEffect(() => {
        getAgents()
            .then(setAgents)
            .catch(() => setFetchError('에이전트 목록을 불러오는데 실패했습니다.'))
            .finally(() => setLoading(false))
    }, [])

    const handleStatusChange = async (agent: AgentSummary, status: AgentSummary['status']) => {
        if (agent.status === status) return
        setUpdating(agent.id)
        setUpdateError(null)
        try {
            const updated = await updateAgentStatus(agent.id, status)
            setAgents((prev) => prev.map((a) => (a.id === updated.id ? updated : a)))
        } catch {
            setUpdateError('에이전트 상태 변경에 실패했습니다.')
        } finally {
            setUpdating(null)
        }
    }

    return (
        <Layout title="에이전트 관리">
            {updateError && (
                <div style={{ marginBottom: '16px', color: 'var(--danger)', fontSize: '0.85rem' }}>{updateError}</div>
            )}
            {loading ? <Spinner /> : fetchError ? (
                <EmptyState message={fetchError} />
            ) : agents.length === 0 ? (
                <EmptyState message="등록된 에이전트가 없습니다." />
            ) : (
                <div className="table-wrap">
                    <table>
                        <thead>
                            <tr>
                                <th>ID</th>
                                <th>이름</th>
                                <th>Client ID</th>
                                <th>상태</th>
                                <th>마지막 키 변경</th>
                                <th>등록일</th>
                                <th>상태 변경</th>
                            </tr>
                        </thead>
                        <tbody>
                            {agents.map((agent) => (
                                <tr key={agent.id}>
                                    <td className="td-mono">{agent.id.slice(0, 8)}…</td>
                                    <td style={{ fontWeight: 600 }}>{agent.name}</td>
                                    <td className="td-mono">{agent.clientId}</td>
                                    <td>
                                        <span className={`badge ${STATUS_BADGE[agent.status]}`}>
                                            {STATUS_LABEL[agent.status]}
                                        </span>
                                    </td>
                                    <td className="text-secondary" style={{ fontSize: '0.78rem', whiteSpace: 'nowrap' }}>
                                        {agent.lastRotatedAt ? formatDate(agent.lastRotatedAt) : '-'}
                                    </td>
                                    <td className="text-secondary" style={{ fontSize: '0.78rem', whiteSpace: 'nowrap' }}>
                                        {formatDate(agent.createdAt)}
                                    </td>
                                    <td>
                                        <select
                                            className="form-input"
                                            style={{ padding: '4px 8px', fontSize: '0.82rem' }}
                                            value={agent.status}
                                            disabled={updating === agent.id}
                                            onChange={(e) => { if (isValidStatus(e.target.value)) handleStatusChange(agent, e.target.value) }}
                                        >
                                            {STATUS_OPTIONS.map((s) => (
                                                <option key={s} value={s}>{STATUS_LABEL[s]}</option>
                                            ))}
                                        </select>
                                    </td>
                                </tr>
                            ))}
                        </tbody>
                    </table>
                </div>
            )}
        </Layout>
    )
}
