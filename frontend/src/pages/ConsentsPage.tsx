import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { listConsents, createConsent, type ConsentResponse } from '../api/consents'
import { listDataHolders, type DataHolderSummary } from '../api/dataholders'
import Layout from '../components/Layout'
import StatusBadge from '../components/StatusBadge'
import { Spinner, EmptyState } from '../components/Spinner'
import { formatDate } from '../utils/jwt'

const DEFAULT_EXPIRY_TIME = '23:59'

function toDateInputValue(date: Date) {
    return date.toISOString().slice(0, 10)
}

function buildExpiryIso(date: string, time: string) {
    if (!date) return undefined
    const localDateTime = new Date(`${date}T${time || DEFAULT_EXPIRY_TIME}:00`)
    if (Number.isNaN(localDateTime.getTime())) return undefined
    return localDateTime.toISOString()
}

export default function ConsentsPage() {
    const navigate = useNavigate()
    const [consents, setConsents] = useState<ConsentResponse[]>([])
    const [dataHolders, setDataHolders] = useState<DataHolderSummary[]>([])
    const [loading, setLoading] = useState(true)
    const [showModal, setShowModal] = useState(false)

    // Form state
    const [dataHolderId, setDataHolderId] = useState('')
    const [scopesInput, setScopesInput] = useState('')
    const [expiryDate, setExpiryDate] = useState('')
    const [expiryTime, setExpiryTime] = useState(DEFAULT_EXPIRY_TIME)
    const [submitting, setSubmitting] = useState(false)
    const [formError, setFormError] = useState<string | null>(null)

    const load = () => {
        setLoading(true)
        Promise.all([listConsents(), listDataHolders()])
            .then(([consentResult, dataHolderResult]) => {
                setConsents(consentResult)
                setDataHolders(dataHolderResult)
            })
            .finally(() => setLoading(false))
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
                expiresAt: buildExpiryIso(expiryDate, expiryTime),
            })
            setShowModal(false)
            setDataHolderId('')
            setScopesInput('')
            setExpiryDate('')
            setExpiryTime(DEFAULT_EXPIRY_TIME)
            load()
        } catch (err: any) {
            setFormError(err?.response?.data?.message ?? '생성에 실패했습니다.')
        } finally {
            setSubmitting(false)
        }
    }

    const applyExpiryPreset = (daysToAdd: number) => {
        const target = new Date()
        target.setDate(target.getDate() + daysToAdd)
        setExpiryDate(toDateInputValue(target))
        setExpiryTime(DEFAULT_EXPIRY_TIME)
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
                                <label className="form-label">데이터 보유자 <span style={{ color: 'var(--red)' }}>*</span></label>
                                <select
                                    id="input-dataholder-id"
                                    className="form-input"
                                    value={dataHolderId}
                                    onChange={(e) => setDataHolderId(e.target.value)}
                                    required
                                >
                                    <option value="">선택하세요</option>
                                    {dataHolders.map((holder) => (
                                        <option key={holder.id} value={holder.id}>
                                            {holder.name} ({holder.institutionCode}) · {holder.supportedMethods.join(', ')}
                                        </option>
                                    ))}
                                </select>
                                <span className="form-hint">이름으로 선택하면 내부적으로 올바른 UUID가 전송됩니다.</span>
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
                                <div style={{ display: 'flex', gap: '8px', flexWrap: 'wrap', marginBottom: '8px' }}>
                                    <button type="button" className="btn btn-secondary btn-sm" onClick={() => applyExpiryPreset(7)}>+7일</button>
                                    <button type="button" className="btn btn-secondary btn-sm" onClick={() => applyExpiryPreset(30)}>+30일</button>
                                    <button type="button" className="btn btn-secondary btn-sm" onClick={() => applyExpiryPreset(90)}>+90일</button>
                                    <button
                                        type="button"
                                        className="btn btn-secondary btn-sm"
                                        onClick={() => {
                                            setExpiryDate('')
                                            setExpiryTime(DEFAULT_EXPIRY_TIME)
                                        }}
                                    >
                                        만료 없음
                                    </button>
                                </div>
                                <div style={{ display: 'grid', gridTemplateColumns: '1fr 140px', gap: '10px' }}>
                                    <input
                                        id="input-expires-date"
                                        type="date"
                                        className="form-input"
                                        value={expiryDate}
                                        min={toDateInputValue(new Date())}
                                        onChange={(e) => setExpiryDate(e.target.value)}
                                    />
                                    <input
                                        id="input-expires-time"
                                        type="time"
                                        className="form-input"
                                        value={expiryTime}
                                        onChange={(e) => setExpiryTime(e.target.value || DEFAULT_EXPIRY_TIME)}
                                        disabled={!expiryDate}
                                    />
                                </div>
                                <span className="form-hint">날짜만 고르면 시간은 기본적으로 23:59가 적용됩니다.</span>
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
