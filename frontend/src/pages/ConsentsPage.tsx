import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { listConsents, createConsent, type ConsentResponse } from '../api/consents'
import Layout from '../components/Layout'
import StatusBadge from '../components/StatusBadge'
import { Spinner, EmptyState } from '../components/Spinner'
import { formatDate } from '../utils/jwt'

export default function ConsentsPage() {
    const navigate = useNavigate()
    const [consents, setConsents] = useState<ConsentResponse[]>([])
    const [loading, setLoading] = useState(true)
    const [showModal, setShowModal] = useState(false)

    // Form state
    const [dataHolderId, setDataHolderId] = useState('')
    const [scopesInput, setScopesInput] = useState('')
    const [expiresAt, setExpiresAt] = useState('')
    const [submitting, setSubmitting] = useState(false)
    const [formError, setFormError] = useState<string | null>(null)

    const load = () => {
        setLoading(true)
        listConsents().then(setConsents).finally(() => setLoading(false))
    }

    useEffect(() => { load() }, [])

    const handleCreate = async (e: React.FormEvent) => {
        e.preventDefault()
        setSubmitting(true)
        setFormError(null)
        try {
            const scopes = scopesInput.split(',').map((s) => s.trim()).filter(Boolean)
            await createConsent({
                dataHolderId,
                scopes,
                expiresAt: expiresAt || undefined,
            })
            setShowModal(false)
            setDataHolderId('')
            setScopesInput('')
            setExpiresAt('')
            load()
        } catch (err: any) {
            setFormError(err?.response?.data?.message ?? '생성에 실패했습니다.')
        } finally {
            setSubmitting(false)
        }
    }

    return (
        <Layout title="동의 관리">
            <div className="toolbar">
                <div className="toolbar-left">
                    <h2 style={{ fontSize: '1.1rem', fontWeight: 700 }}>동의 목록</h2>
                    <span className="text-muted" style={{ fontSize: '0.8rem' }}>총 {consents.length}건</span>
                </div>
                <div className="toolbar-right">
                    <button id="btn-create-consent" className="btn btn-primary" onClick={() => setShowModal(true)}>
                        + 새 동의 생성
                    </button>
                </div>
            </div>

            {loading ? <Spinner /> : (
                consents.length === 0 ? (
                    <EmptyState message="동의가 없습니다. 새 동의를 생성해 보세요." />
                ) : (
                    <div className="table-wrap">
                        <table>
                            <thead>
                                <tr>
                                    <th>데이터 보유자</th>
                                    <th>스코프</th>
                                    <th>상태</th>
                                    <th>만료일</th>
                                    <th>생성일</th>
                                    <th></th>
                                </tr>
                            </thead>
                            <tbody>
                                {consents.map((c) => (
                                    <tr key={c.id}>
                                        <td>{c.dataHolderName}</td>
                                        <td>
                                            <div className="tag-list">
                                                {c.scopes.map((s) => <span key={s} className="tag">{s}</span>)}
                                            </div>
                                        </td>
                                        <td><StatusBadge status={c.status} /></td>
                                        <td className="text-secondary">{formatDate(c.expiresAt)}</td>
                                        <td className="text-secondary">{formatDate(c.createdAt)}</td>
                                        <td>
                                            <button
                                                className="btn btn-secondary btn-sm"
                                                onClick={() => navigate(`/consents/${c.id}`)}
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
                        <h2>새 동의 생성</h2>
                        <form onSubmit={handleCreate} style={{ display: 'flex', flexDirection: 'column', gap: '14px' }}>
                            {formError && <div className="alert alert-error">⚠ {formError}</div>}
                            <div className="form-group">
                                <label className="form-label">데이터 보유자 ID <span style={{ color: 'var(--red)' }}>*</span></label>
                                <input
                                    id="input-dataholder-id"
                                    className="form-input"
                                    placeholder="UUID"
                                    value={dataHolderId}
                                    onChange={(e) => setDataHolderId(e.target.value)}
                                    required
                                />
                            </div>
                            <div className="form-group">
                                <label className="form-label">스코프 <span style={{ color: 'var(--red)' }}>*</span></label>
                                <input
                                    id="input-scopes"
                                    className="form-input"
                                    placeholder="READ, WRITE (쉼표 구분)"
                                    value={scopesInput}
                                    onChange={(e) => setScopesInput(e.target.value)}
                                    required
                                />
                                <span className="form-hint">예: READ, WRITE, DELETE</span>
                            </div>
                            <div className="form-group">
                                <label className="form-label">만료 일시 (선택)</label>
                                <input
                                    id="input-expires"
                                    type="datetime-local"
                                    className="form-input"
                                    value={expiresAt}
                                    onChange={(e) => setExpiresAt(e.target.value ? new Date(e.target.value).toISOString() : '')}
                                />
                            </div>
                            <div className="modal-actions">
                                <button type="button" className="btn btn-secondary" onClick={() => setShowModal(false)}>
                                    취소
                                </button>
                                <button id="btn-submit-consent" type="submit" className="btn btn-primary" disabled={submitting}>
                                    {submitting ? '생성 중...' : '생성하기'}
                                </button>
                            </div>
                        </form>
                    </div>
                </div>
            )}
        </Layout>
    )
}
