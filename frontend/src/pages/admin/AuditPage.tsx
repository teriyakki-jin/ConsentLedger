import { useEffect, useState, useCallback } from 'react'
import { getAuditLogs, verifyAuditChain, type AuditLogResponse, type AuditVerifyResponse } from '../../api/audit'
import Layout from '../../components/Layout'
import { Spinner, EmptyState } from '../../components/Spinner'
import { formatDate } from '../../utils/jwt'

export default function AuditPage() {
    const [logs, setLogs] = useState<AuditLogResponse[]>([])
    const [meta, setMeta] = useState({ total: 0, page: 0, size: 50 })
    const [loading, setLoading] = useState(true)
    const [verifying, setVerifying] = useState(false)
    const [verify, setVerify] = useState<AuditVerifyResponse | null>(null)

    // Filters
    const [action, setAction] = useState('')
    const [objectType, setObjectType] = useState('')
    const [page, setPage] = useState(0)

    const load = useCallback(() => {
        setLoading(true)
        getAuditLogs({ action: action || undefined, objectType: objectType || undefined, page, size: 50 })
            .then((r) => { setLogs(r.data); setMeta(r.meta) })
            .finally(() => setLoading(false))
    }, [action, objectType, page])

    useEffect(() => { load() }, [load])

    const handleVerify = async () => {
        setVerifying(true)
        try {
            const result = await verifyAuditChain()
            setVerify(result)
        } finally {
            setVerifying(false)
        }
    }

    const totalPages = Math.ceil(meta.total / meta.size)

    return (
        <Layout title="감사 로그">
            <div className="toolbar mb-24">
                <div className="toolbar-left">
                    <input
                        className="form-input"
                        style={{ width: '160px' }}
                        placeholder="액션 필터"
                        value={action}
                        onChange={(e) => { setAction(e.target.value); setPage(0) }}
                    />
                    <input
                        className="form-input"
                        style={{ width: '160px' }}
                        placeholder="객체 유형 필터"
                        value={objectType}
                        onChange={(e) => { setObjectType(e.target.value); setPage(0) }}
                    />
                    <button className="btn btn-secondary btn-sm" onClick={load}>검색</button>
                </div>
                <div className="toolbar-right">
                    <button
                        id="btn-verify"
                        className="btn btn-secondary"
                        onClick={handleVerify}
                        disabled={verifying}
                    >
                        {verifying ? '검증 중...' : '🔒 무결성 검증'}
                    </button>
                </div>
            </div>

            {verify && (
                <div className={`verify-result ${verify.valid ? 'verify-ok' : 'verify-fail'}`}>
                    <span style={{ fontSize: '1.5rem' }}>{verify.valid ? '✅' : '❌'}</span>
                    <div>
                        <p>{verify.valid ? '해시 체인 무결성이 확인되었습니다.' : `무결성 오류 발견 — ID: ${verify.firstBrokenId}`}</p>
                        <span style={{ fontSize: '0.78rem', color: 'var(--text-secondary)' }}>
                            총 {verify.totalChecked}건 검증 완료
                        </span>
                    </div>
                </div>
            )}

            {loading ? <Spinner /> : logs.length === 0 ? (
                <EmptyState message="감사 로그가 없습니다." />
            ) : (
                <>
                    <div className="table-wrap">
                        <table>
                            <thead>
                                <tr>
                                    <th>ID</th>
                                    <th>시각</th>
                                    <th>액션</th>
                                    <th>객체 유형</th>
                                    <th>결과</th>
                                    <th>행위자</th>
                                    <th>해시</th>
                                </tr>
                            </thead>
                            <tbody>
                                {logs.map((log) => (
                                    <tr key={log.id}>
                                        <td className="td-mono">{log.id}</td>
                                        <td className="text-secondary" style={{ fontSize: '0.78rem', whiteSpace: 'nowrap' }}>{formatDate(log.ts)}</td>
                                        <td>
                                            <span style={{ fontWeight: 600, fontSize: '0.82rem' }}>{log.action}</span>
                                        </td>
                                        <td className="text-secondary">{log.objectType}</td>
                                        <td>
                                            <span className={`badge ${log.outcome === 'SUCCESS' ? 'badge-completed' : 'badge-failed'}`}>
                                                {log.outcome}
                                            </span>
                                        </td>
                                        <td className="td-mono">
                                            {log.actorUserId ? log.actorUserId.slice(0, 8) + '…' : log.actorAgentId ? '🤖 ' + log.actorAgentId.slice(0, 6) + '…' : '-'}
                                        </td>
                                        <td>
                                            <span className="hash-chip" title={log.hash}>{log.hash}</span>
                                        </td>
                                    </tr>
                                ))}
                            </tbody>
                        </table>
                    </div>

                    <div className="pagination">
                        <button onClick={() => setPage(0)} disabled={page === 0}>«</button>
                        <button onClick={() => setPage((p) => Math.max(0, p - 1))} disabled={page === 0}>‹</button>
                        <span className="page-info">{page + 1} / {totalPages || 1}</span>
                        <button onClick={() => setPage((p) => p + 1)} disabled={page >= totalPages - 1}>›</button>
                        <button onClick={() => setPage(totalPages - 1)} disabled={page >= totalPages - 1}>»</button>
                    </div>
                </>
            )}
        </Layout>
    )
}
