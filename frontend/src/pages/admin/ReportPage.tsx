import { useState } from 'react'
import { downloadAuditReport } from '../../api/admin'
import Layout from '../../components/Layout'

export default function ReportPage() {
    const [from, setFrom] = useState('')
    const [to, setTo] = useState('')
    const [loading, setLoading] = useState(false)
    const [error, setError] = useState<string | null>(null)

    const handleDownload = async () => {
        if (!from || !to) {
            setError('시작일과 종료일을 모두 입력해주세요.')
            return
        }
        if (from > to) {
            setError('시작일은 종료일 이전이어야 합니다.')
            return
        }
        setError(null)
        setLoading(true)
        try {
            const blob = await downloadAuditReport(
                new Date(from).toISOString(),
                new Date(to + 'T23:59:59').toISOString(),
            )
            const url = URL.createObjectURL(blob)
            const a = document.createElement('a')
            a.href = url
            a.download = `audit-report-${from}-${to}.pdf`
            a.click()
            URL.revokeObjectURL(url)
        } catch {
            setError('리포트 생성에 실패했습니다. 날짜 범위를 확인해주세요.')
        } finally {
            setLoading(false)
        }
    }

    return (
        <Layout title="감사 리포트">
            <div className="card" style={{ maxWidth: '480px' }}>
                <h2 style={{ marginBottom: '20px', fontSize: '1rem', fontWeight: 600 }}>PDF 리포트 다운로드</h2>

                <div style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>
                    <div>
                        <label className="form-label">시작일</label>
                        <input
                            type="date"
                            className="form-input"
                            value={from}
                            onChange={(e) => setFrom(e.target.value)}
                        />
                    </div>
                    <div>
                        <label className="form-label">종료일</label>
                        <input
                            type="date"
                            className="form-input"
                            value={to}
                            onChange={(e) => setTo(e.target.value)}
                        />
                    </div>

                    {error && (
                        <p style={{ color: 'var(--danger)', fontSize: '0.85rem' }}>{error}</p>
                    )}

                    <button
                        className="btn btn-primary"
                        onClick={handleDownload}
                        disabled={loading}
                    >
                        {loading ? '생성 중...' : '📄 PDF 다운로드'}
                    </button>
                </div>
            </div>
        </Layout>
    )
}
