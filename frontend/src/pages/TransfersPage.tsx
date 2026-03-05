import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { listTransfers, createTransfer, type TransferResponse } from '../api/transfers'
import { listConsents, type ConsentResponse } from '../api/consents'
import Layout from '../components/Layout'
import StatusBadge from '../components/StatusBadge'
import { Spinner, EmptyState } from '../components/Spinner'
import { formatDate, shortId } from '../utils/jwt'

export default function TransfersPage() {
    const navigate = useNavigate()
    const [transfers, setTransfers] = useState<TransferResponse[]>([])
    const [consents, setConsents] = useState<ConsentResponse[]>([])
    const [loading, setLoading] = useState(true)
    const [showModal, setShowModal] = useState(false)

    const [consentId, setConsentId] = useState('')
    const [method, setMethod] = useState('')
    const [submitting, setSubmitting] = useState(false)
    const [formError, setFormError] = useState<string | null>(null)

    const load = () => {
        setLoading(true)
        Promise.all([listTransfers(), listConsents()])
            .then(([t, c]) => { setTransfers(t); setConsents(c) })
            .finally(() => setLoading(false))
    }

    useEffect(() => { load() }, [])

    const handleCreate = async (e: React.FormEvent) => {
        e.preventDefault()
        setSubmitting(true)
        setFormError(null)
        try {
            const idempotencyKey = crypto.randomUUID()
            await createTransfer({ consentId, method, idempotencyKey })
            setShowModal(false)
            setConsentId('')
            setMethod('')
            load()
        } catch (err: any) {
            setFormError(err?.response?.data?.message ?? '생성에 실패했습니다.')
        } finally {
            setSubmitting(false)
        }
    }

    const activeConsents = consents.filter((c) => c.status === 'ACTIVE')

    return (
        <Layout title="전송 요청">
            <div className="toolbar">
                <div className="toolbar-left">
                    <h2 style={{ fontSize: '1.1rem', fontWeight: 700 }}>전송 요청 목록</h2>
                    <span className="text-muted" style={{ fontSize: '0.8rem' }}>총 {transfers.length}건</span>
                </div>
                <div className="toolbar-right">
                    <button id="btn-create-transfer" className="btn btn-primary" onClick={() => setShowModal(true)}>
                        + 전송 요청 생성
                    </button>
                </div>
            </div>

            {loading ? <Spinner /> : (
                transfers.length === 0 ? (
                    <EmptyState message="전송 요청이 없습니다." />
                ) : (
                    <div className="table-wrap">
                        <table>
                            <thead>
                                <tr>
                                    <th>동의 ID</th>
                                    <th>전송 방식</th>
                                    <th>상태</th>
                                    <th>생성일</th>
                                    <th></th>
                                </tr>
                            </thead>
                            <tbody>
                                {transfers.map((t) => (
                                    <tr key={t.id}>
                                        <td className="td-mono">{shortId(t.consentId)}</td>
                                        <td>{t.method}</td>
                                        <td><StatusBadge status={t.status} /></td>
                                        <td className="text-secondary">{formatDate(t.createdAt)}</td>
                                        <td>
                                            <button
                                                className="btn btn-secondary btn-sm"
                                                onClick={() => navigate(`/transfers/${t.id}`)}
                                            >
                                                상세 보기
                                            </button>
                                        </td>
                                    </tr>
                                ))}
                            </tbody>
                        </table>
                    </div>
                )
            )}

            {showModal && (
                <div className="modal-backdrop" onClick={() => setShowModal(false)}>
                    <div className="modal" onClick={(e) => e.stopPropagation()}>
                        <h2>전송 요청 생성</h2>
                        <form onSubmit={handleCreate} style={{ display: 'flex', flexDirection: 'column', gap: '14px' }}>
                            {formError && <div className="alert alert-error">⚠ {formError}</div>}
                            <div className="form-group">
                                <label className="form-label">동의 선택 <span style={{ color: 'var(--red)' }}>*</span></label>
                                <select
                                    id="select-consent"
                                    className="form-select"
                                    value={consentId}
                                    onChange={(e) => setConsentId(e.target.value)}
                                    required
                                >
                                    <option value="">동의를 선택하세요</option>
                                    {activeConsents.map((c) => (
                                        <option key={c.id} value={c.id}>
                                            {c.dataHolderName} — {c.scopes.join(', ')}
                                        </option>
                                    ))}
                                </select>
                                {activeConsents.length === 0 && (
                                    <span className="form-hint" style={{ color: 'var(--yellow)' }}>활성 동의가 없습니다.</span>
                                )}
                            </div>
                            <div className="form-group">
                                <label className="form-label">전송 방식 <span style={{ color: 'var(--red)' }}>*</span></label>
                                <input
                                    id="input-method"
                                    className="form-input"
                                    placeholder="예: MYDATA_API, SFTP"
                                    value={method}
                                    onChange={(e) => setMethod(e.target.value)}
                                    required
                                />
                            </div>
                            <div className="modal-actions">
                                <button type="button" className="btn btn-secondary" onClick={() => setShowModal(false)}>
                                    취소
                                </button>
                                <button id="btn-submit-transfer" type="submit" className="btn btn-primary" disabled={submitting}>
                                    {submitting ? '생성 중...' : '요청하기'}
                                </button>
                            </div>
                        </form>
                    </div>
                </div>
            )}
        </Layout>
    )
}
