import { useEffect, useState, useCallback } from 'react'
import {
    getAuditLogs,
    verifyAuditChain,
    tamperLog,
    type AuditLogResponse,
    type AuditVerifyResponse,
    type TamperResult,
} from '../../api/audit'
import Layout from '../../components/Layout'
import { Spinner, EmptyState } from '../../components/Spinner'
import { formatDate } from '../../utils/jwt'

export default function AuditPage() {
    const [logs, setLogs] = useState<AuditLogResponse[]>([])
    const [meta, setMeta] = useState({ total: 0, page: 0, size: 50 })
    const [loading, setLoading] = useState(true)
    const [verifying, setVerifying] = useState(false)
    const [verify, setVerify] = useState<AuditVerifyResponse | null>(null)
    const [tampering, setTampering] = useState(false)
    const [tamperResult, setTamperResult] = useState<TamperResult | null>(null)
    const [selectedLogId, setSelectedLogId] = useState<number | null>(null)

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
        setTamperResult(null)
        try {
            const result = await verifyAuditChain()
            setVerify(result)
        } finally {
            setVerifying(false)
        }
    }

    const handleTamper = async () => {
        if (!selectedLogId) return
        setTampering(true)
        setVerify(null)
        setTamperResult(null)
        try {
            const result = await tamperLog(selectedLogId)
            setTamperResult(result)
            load()
        } finally {
            setTampering(false)
        }
    }

    const totalPages = Math.ceil(meta.total / meta.size)
    const brokenId = tamperResult?.firstBrokenId ?? verify?.firstBrokenLogId

    const rowStyle = (log: AuditLogResponse): React.CSSProperties => {
        if (!brokenId || brokenId < 0) return {}
        if (log.id === brokenId) return { background: 'rgba(248,81,73,0.18)', outline: '1px solid var(--red)' }
        if (log.id > brokenId) return { background: 'rgba(248,81,73,0.06)', opacity: 0.6 }
        return {}
    }

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
                <div className="toolbar-right" style={{ gap: '8px' }}>
                    <button
                        className="btn btn-secondary"
                        onClick={handleVerify}
                        disabled={verifying || tampering}
                    >
                        {verifying ? '검증 중…' : '🔒 무결성 검증'}
                    </button>
                    <button
                        className="btn btn-danger"
                        onClick={handleTamper}
                        disabled={!selectedLogId || tampering || verifying}
                        title={selectedLogId ? `ID ${selectedLogId} 변조 시뮬레이션` : '테이블에서 로그를 선택하세요'}
                    >
                        {tampering ? '변조 중…' : selectedLogId ? `⚠️ ID ${selectedLogId} 변조` : '⚠️ 변조 (선택 필요)'}
                    </button>
                </div>
            </div>

            {verify && (
                <div className={`verify-result ${verify.valid ? 'verify-ok' : 'verify-fail'}`}>
                    <span style={{ fontSize: '1.5rem' }}>{verify.valid ? '✅' : '❌'}</span>
                    <div>
                        <p>
                            {verify.valid
                                ? '해시 체인 무결성이 확인되었습니다.'
                                : `무결성 오류 — ID ${verify.firstBrokenLogId}에서 변조 감지 (${verify.reason})`}
                        </p>
                        <span style={{ fontSize: '0.78rem', color: 'var(--text-secondary)' }}>
                            총 {verify.verifiedCount}건 검증 완료
                        </span>
                    </div>
                </div>
            )}

            {tamperResult && (
                <div className={`verify-result ${tamperResult.chainValid ? 'verify-ok' : 'verify-fail'}`}>
                    <span style={{ fontSize: '1.5rem' }}>{tamperResult.chainValid ? '✅' : '🔴'}</span>
                    <div>
                        <p>{tamperResult.message}</p>
                        <span style={{ fontSize: '0.78rem', color: 'var(--text-secondary)' }}>
                            변조된 로그 ID: {tamperResult.tamperedLogId} · 검증: {tamperResult.totalChecked}건
                        </span>
                    </div>
                </div>
            )}

            <p style={{ fontSize: '0.8rem', color: 'var(--text-secondary)', marginBottom: '12px' }}>
                💡 행을 클릭하여 선택 후 <strong>변조 버튼</strong>을 누르면 해시 체인이 깨지는 것을 확인할 수 있습니다.
            </p>

            {loading ? <Spinner /> : logs.length === 0 ? (
                <EmptyState message="감사 로그가 없습니다." />
            ) : (
                <>
                    <div className="table-wrap">
                        <table>
                            <thead>
                                <tr>
                                    <th style={{ width: '36px' }}></th>
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
                                    <tr
                                        key={log.id}
                                        style={{ cursor: 'pointer', ...rowStyle(log) }}
                                        onClick={() => setSelectedLogId(log.id === selectedLogId ? null : log.id)}
                                    >
                                        <td style={{ textAlign: 'center' }}>
                                            {log.id === selectedLogId
                                                ? <span style={{ color: 'var(--accent-cyan)' }}>◉</span>
                                                : log.id === brokenId
                                                    ? <span>💥</span>
                                                    : ''}
                                        </td>
                                        <td className="td-mono">{log.id}</td>
                                        <td className="text-secondary" style={{ fontSize: '0.78rem', whiteSpace: 'nowrap' }}>
                                            {formatDate(log.ts)}
                                        </td>
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
                                            {log.actorUserId
                                                ? log.actorUserId.slice(0, 8) + '…'
                                                : log.actorAgentId
                                                    ? '🤖 ' + log.actorAgentId.slice(0, 6) + '…'
                                                    : '-'}
                                        </td>
                                        <td>
                                            <span
                                                className="hash-chip"
                                                title={log.hash}
                                                style={log.id === brokenId ? { color: 'var(--red)', borderColor: 'var(--red)' } : {}}
                                            >
                                                {log.id === brokenId ? '💥 ' : ''}{log.hash}
                                            </span>
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
