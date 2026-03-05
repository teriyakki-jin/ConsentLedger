import { useEffect, useState } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import {
    approveTransfer,
    executeTransfer,
    type TransferResponse,
} from '../api/transfers'
import client from '../api/client'
import Layout from '../components/Layout'
import StatusBadge from '../components/StatusBadge'
import { Spinner } from '../components/Spinner'
import { formatDate } from '../utils/jwt'

export default function TransferDetailPage() {
    const { id } = useParams<{ id: string }>()
    const navigate = useNavigate()
    const [transfer, setTransfer] = useState<TransferResponse | null>(null)
    const [loading, setLoading] = useState(true)
    const [error, setError] = useState<string | null>(null)
    const [processing, setProcessing] = useState(false)

    useEffect(() => {
        if (!id) return
        client.get<{ data: TransferResponse }>(`/transfer-requests/${id}`)
            .then((r) => setTransfer(r.data.data))
            .catch(() => setError('전송 요청을 찾을 수 없습니다.'))
            .finally(() => setLoading(false))
    }, [id])

    const handleAction = async (action: 'approve' | 'execute') => {
        if (!transfer) return
        setProcessing(true)
        setError(null)
        try {
            const updated = action === 'approve'
                ? await approveTransfer(transfer.id)
                : await executeTransfer(transfer.id)
            setTransfer(updated)
        } catch (err: any) {
            setError(err?.response?.data?.message ?? `${action} 처리에 실패했습니다.`)
        } finally {
            setProcessing(false)
        }
    }

    return (
        <Layout title="전송 요청 상세">
            {loading ? <Spinner /> : error ? (
                <div className="alert alert-error">{error}</div>
            ) : !transfer ? null : (
                <>
                    <div className="toolbar mb-24">
                        <button className="btn btn-secondary btn-sm" onClick={() => navigate('/transfers')}>
                            ← 목록으로
                        </button>
                        <div className="action-bar">
                            {transfer.status === 'REQUESTED' && (
                                <button
                                    id="btn-approve"
                                    className="btn btn-success"
                                    onClick={() => handleAction('approve')}
                                    disabled={processing}
                                >
                                    {processing ? '처리 중...' : '✓ 승인하기'}
                                </button>
                            )}
                            {transfer.status === 'APPROVED' && (
                                <button
                                    id="btn-execute"
                                    className="btn btn-primary"
                                    onClick={() => handleAction('execute')}
                                    disabled={processing}
                                >
                                    {processing ? '처리 중...' : '▶ 전송 실행'}
                                </button>
                            )}
                        </div>
                    </div>

                    {error && <div className="alert alert-error mb-16">⚠ {error}</div>}

                    <div className="card">
                        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '20px' }}>
                            <h2 style={{ fontSize: '1.1rem', fontWeight: 700 }}>전송 요청 정보</h2>
                            <StatusBadge status={transfer.status} />
                        </div>

                        <div className="detail-grid">
                            <div className="detail-field">
                                <label>전송 요청 ID</label>
                                <span className="font-mono" style={{ fontSize: '0.8rem' }}>{transfer.id}</span>
                            </div>
                            <div className="detail-field">
                                <label>동의 ID</label>
                                <span className="font-mono" style={{ fontSize: '0.8rem' }}>{transfer.consentId}</span>
                            </div>
                            <div className="detail-field">
                                <label>전송 방식</label>
                                <span>{transfer.method}</span>
                            </div>
                            <div className="detail-field">
                                <label>요청자 사용자 ID</label>
                                <span className="text-secondary">{transfer.requesterUserId ?? '-'}</span>
                            </div>
                            <div className="detail-field">
                                <label>요청자 에이전트 ID</label>
                                <span className="text-secondary">{transfer.requesterAgentId ?? '-'}</span>
                            </div>
                            <div className="detail-field">
                                <label>승인자 ID</label>
                                <span className="text-secondary">{transfer.approvedByUserId ?? '-'}</span>
                            </div>
                            <div className="detail-field">
                                <label>멱등성 키</label>
                                <span className="font-mono" style={{ fontSize: '0.78rem' }}>{transfer.idempotencyKey}</span>
                            </div>
                            <div className="detail-field">
                                <label>생성일</label>
                                <span className="text-secondary">{formatDate(transfer.createdAt)}</span>
                            </div>
                            <div className="detail-field">
                                <label>수정일</label>
                                <span className="text-secondary">{formatDate(transfer.updatedAt)}</span>
                            </div>
                        </div>
                    </div>
                </>
            )}
        </Layout>
    )
}
