import { useEffect, useState } from 'react'
import { listConsents } from '../api/consents'
import { listTransfers } from '../api/transfers'
import type { ConsentResponse } from '../api/consents'
import type { TransferResponse } from '../api/transfers'
import Layout from '../components/Layout'
import { Spinner } from '../components/Spinner'
import { useNavigate } from 'react-router-dom'

export default function DashboardPage() {
    const navigate = useNavigate()
    const [consents, setConsents] = useState<ConsentResponse[]>([])
    const [transfers, setTransfers] = useState<TransferResponse[]>([])
    const [loading, setLoading] = useState(true)

    useEffect(() => {
        Promise.all([listConsents(), listTransfers()])
            .then(([c, t]) => { setConsents(c); setTransfers(t) })
            .finally(() => setLoading(false))
    }, [])

    const active = consents.filter((c) => c.status === 'ACTIVE').length
    const revoked = consents.filter((c) => c.status === 'REVOKED').length
    const expired = consents.filter((c) => c.status === 'EXPIRED').length
    const pending = transfers.filter((t) => t.status === 'REQUESTED').length
    const completed = transfers.filter((t) => t.status === 'COMPLETED').length

    const recentConsents = [...consents]
        .sort((a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime())
        .slice(0, 5)

    return (
        <Layout title="대시보드">
            {loading ? <Spinner /> : (
                <>
                    <div className="stat-grid">
                        <div className="stat-card">
                            <div className="stat-label">활성 동의</div>
                            <div className="stat-value" style={{ color: 'var(--green)' }}>{active}</div>
                            <div className="stat-sub">현재 유효한 동의</div>
                        </div>
                        <div className="stat-card">
                            <div className="stat-label">총 동의</div>
                            <div className="stat-value">{consents.length}</div>
                            <div className="stat-sub">철회 {revoked} · 만료 {expired}</div>
                        </div>
                        <div className="stat-card">
                            <div className="stat-label">전송 요청</div>
                            <div className="stat-value">{transfers.length}</div>
                            <div className="stat-sub">대기 {pending} · 완료 {completed}</div>
                        </div>
                        <div className="stat-card">
                            <div className="stat-label">완료된 전송</div>
                            <div className="stat-value" style={{ color: 'var(--accent-cyan)' }}>{completed}</div>
                            <div className="stat-sub">성공적으로 처리됨</div>
                        </div>
                    </div>

                    <div className="page-title-bar mb-16">
                        <h2>최근 동의</h2>
                        <button className="btn btn-secondary btn-sm" onClick={() => navigate('/consents')}>
                            전체 보기
                        </button>
                    </div>

                    <div className="table-wrap">
                        <table>
                            <thead>
                                <tr>
                                    <th>데이터 보유자</th>
                                    <th>스코프</th>
                                    <th>상태</th>
                                    <th>생성일</th>
                                </tr>
                            </thead>
                            <tbody>
                                {recentConsents.length === 0 ? (
                                    <tr><td colSpan={4} style={{ textAlign: 'center', color: 'var(--text-muted)', padding: '32px' }}>동의 없음</td></tr>
                                ) : recentConsents.map((c) => (
                                    <tr key={c.id} style={{ cursor: 'pointer' }} onClick={() => navigate(`/consents/${c.id}`)}>
                                        <td>{c.dataHolderName}</td>
                                        <td>
                                            <div className="tag-list">
                                                {c.scopes.map((s) => <span key={s} className="tag">{s}</span>)}
                                            </div>
                                        </td>
                                        <td>
                                            <span className={`badge badge-${c.status.toLowerCase()}`}>
                                                {c.status === 'ACTIVE' ? '활성' : c.status === 'REVOKED' ? '철회됨' : '만료됨'}
                                            </span>
                                        </td>
                                        <td className="text-secondary">{new Date(c.createdAt).toLocaleDateString('ko-KR')}</td>
                                    </tr>
                                ))}
                            </tbody>
                        </table>
                    </div>
                </>
            )}
        </Layout>
    )
}
