import { useEffect, useState } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import { getConsent, revokeConsent, type ConsentResponse } from '../api/consents'
import Layout from '../components/Layout'
import StatusBadge from '../components/StatusBadge'
import { Spinner } from '../components/Spinner'
import { formatDate } from '../utils/jwt'

export default function ConsentDetailPage() {
    const { id } = useParams<{ id: string }>()
    const navigate = useNavigate()
    const [consent, setConsent] = useState<ConsentResponse | null>(null)
    const [loading, setLoading] = useState(true)
    const [revoking, setRevoking] = useState(false)
    const [error, setError] = useState<string | null>(null)

    useEffect(() => {
        if (!id) return
        getConsent(id).then(setConsent).finally(() => setLoading(false))
    }, [id])

    const handleRevoke = async () => {
        if (!consent || !confirm('동의를 철회하시겠습니까?')) return
        setRevoking(true)
        setError(null)
        try {
            const updated = await revokeConsent(consent.id)
            setConsent(updated)
        } catch (err: any) {
            setError(err?.response?.data?.message ?? '철회에 실패했습니다.')
        } finally {
            setRevoking(false)
        }
    }

    return (
        <Layout title="동의 상세">
            {loading ? <Spinner /> : !consent ? (
                <div className="alert alert-error">동의를 찾을 수 없습니다.</div>
            ) : (
                <>
                    <div className="toolbar mb-24">
                        <button className="btn btn-secondary btn-sm" onClick={() => navigate('/consents')}>
                            ← 목록으로
                        </button>
                        <div className="action-bar">
                            {consent.status === 'ACTIVE' && (
                                <button
                                    id="btn-revoke"
                                    className="btn btn-danger"
                                    onClick={handleRevoke}
                                    disabled={revoking}
                                >
                                    {revoking ? '철회 중...' : '철회하기'}
                                </button>
                            )}
                        </div>
                    </div>

                    {error && <div className="alert alert-error mb-16">⚠ {error}</div>}

                    <div className="card mb-24">
                        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '20px' }}>
                            <h2 style={{ fontSize: '1.1rem', fontWeight: 700 }}>{consent.dataHolderName}</h2>
                            <StatusBadge status={consent.status} />
                        </div>

                        <div className="detail-grid">
                            <div className="detail-field">
                                <label>동의 ID</label>
                                <span className="font-mono" style={{ fontSize: '0.8rem' }}>{consent.id}</span>
                            </div>
                            <div className="detail-field">
                                <label>데이터 보유자 ID</label>
                                <span className="font-mono" style={{ fontSize: '0.8rem' }}>{consent.dataHolderId}</span>
                            </div>
                            <div className="detail-field">
                                <label>사용자 ID</label>
                                <span className="font-mono" style={{ fontSize: '0.8rem' }}>{consent.userId}</span>
                            </div>
                            <div className="detail-field">
                                <label>대리인 ID</label>
                                <span className="text-secondary">{consent.agentId ?? '-'}</span>
                            </div>
                            <div className="detail-field">
                                <label>만료 일시</label>
                                <span className="text-secondary">{formatDate(consent.expiresAt)}</span>
                            </div>
                            <div className="detail-field">
                                <label>생성일</label>
                                <span className="text-secondary">{formatDate(consent.createdAt)}</span>
                            </div>
                            <div className="detail-field">
                                <label>수정일</label>
                                <span className="text-secondary">{formatDate(consent.updatedAt)}</span>
                            </div>
                        </div>

                        <div className="detail-field">
                            <label>스코프</label>
                            <div className="tag-list" style={{ marginTop: '6px' }}>
                                {consent.scopes.map((s) => <span key={s} className="tag">{s}</span>)}
                            </div>
                        </div>
                    </div>
                </>
            )}
        </Layout>
    )
}
