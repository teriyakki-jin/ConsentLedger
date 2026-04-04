import { useState } from 'react'
import {
    analyzeAnomalies,
    generateDemoScenario,
    type AnomalyFinding,
    type AnomalyReport,
} from '../../api/audit'
import Layout from '../../components/Layout'
import { Spinner } from '../../components/Spinner'

const SEVERITY_COLOR: Record<string, string> = {
    CRITICAL: '#f85149',
    HIGH: '#e3b341',
    MEDIUM: '#d29922',
    LOW: '#3fb950',
}

const PATTERN_LABEL: Record<string, string> = {
    ACCOUNT_TAKEOVER: '계정 탈취',
    DATA_EXFILTRATION: '데이터 유출',
    PRIVILEGE_ABUSE: '권한 남용',
    ABNORMAL_HOURS: '비정상 시간대',
}

const PATTERN_ICON: Record<string, string> = {
    ACCOUNT_TAKEOVER: '🔓',
    DATA_EXFILTRATION: '📤',
    PRIVILEGE_ABUSE: '⚠️',
    ABNORMAL_HOURS: '🌙',
}

function FindingCard({ finding }: { finding: AnomalyFinding }) {
    const color = SEVERITY_COLOR[finding.severity] ?? '#8b949e'
    return (
        <div className="finding-card" style={{ borderLeft: `3px solid ${color}` }}>
            <div className="finding-header">
                <span className="finding-icon">
                    {PATTERN_ICON[finding.patternType] ?? '🔍'}
                </span>
                <div className="finding-title">
                    <strong>{PATTERN_LABEL[finding.patternType] ?? finding.patternType}</strong>
                    <span className="finding-severity" style={{ color }}>
                        {finding.severity}
                    </span>
                </div>
            </div>
            <p className="finding-desc">{finding.description}</p>
            <div className="finding-meta">
                <span>행위자: <code>{finding.affectedActor?.slice(0, 12)}…</code></span>
                <span>증거 로그: {finding.evidenceLogIds?.slice(0, 5).join(', ')}{(finding.evidenceLogIds?.length ?? 0) > 5 ? '…' : ''}</span>
            </div>
            <div className="finding-recommend">
                💡 {finding.recommendation}
            </div>
        </div>
    )
}

export default function AnomalyPage() {
    const [report, setReport] = useState<AnomalyReport | null>(null)
    const [days, setDays] = useState(7)
    const [analyzing, setAnalyzing] = useState(false)
    const [generating, setGenerating] = useState(false)
    const [demoMsg, setDemoMsg] = useState<string | null>(null)
    const [error, setError] = useState<string | null>(null)

    const handleAnalyze = async () => {
        setAnalyzing(true)
        setError(null)
        try {
            const result = await analyzeAnomalies(days)
            setReport(result)
        } catch {
            setError('분석 실패. 서버 로그를 확인하거나 OPENAI_API_KEY 설정을 확인하세요.')
        } finally {
            setAnalyzing(false)
        }
    }

    const handleGenerateDemo = async () => {
        setGenerating(true)
        setDemoMsg(null)
        try {
            const result = await generateDemoScenario()
            setDemoMsg(`✅ ${result.message} (${result.generatedLogs}건 생성)`)
        } catch {
            setDemoMsg('❌ 시나리오 생성 실패')
        } finally {
            setGenerating(false)
        }
    }

    const criticalCount = report?.findings.filter((f) => f.severity === 'CRITICAL').length ?? 0
    const highCount = report?.findings.filter((f) => f.severity === 'HIGH').length ?? 0

    return (
        <Layout title="AI 이상 탐지">
            {/* 상단 컨트롤 */}
            <div className="toolbar mb-24">
                <div className="toolbar-left" style={{ gap: '12px', flexWrap: 'wrap' }}>
                    <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                        <label style={{ color: 'var(--text-secondary)', fontSize: '0.85rem' }}>분석 기간</label>
                        <select
                            className="form-input"
                            style={{ width: '120px' }}
                            value={days}
                            onChange={(e) => setDays(Number(e.target.value))}
                        >
                            {[1, 3, 7, 14, 30].map((d) => (
                                <option key={d} value={d}>{d}일</option>
                            ))}
                        </select>
                    </div>
                    <button className="btn btn-primary" onClick={handleAnalyze} disabled={analyzing}>
                        {analyzing ? '🤖 분석 중…' : '🤖 AI 분석 실행'}
                    </button>
                </div>
                <div className="toolbar-right">
                    <button className="btn btn-secondary" onClick={handleGenerateDemo} disabled={generating}>
                        {generating ? '생성 중…' : '⚡ 데모 시나리오 생성'}
                    </button>
                </div>
            </div>

            {demoMsg && (
                <div className="demo-msg-banner">{demoMsg}</div>
            )}

            {error && (
                <div className="verify-result verify-fail" style={{ marginBottom: '16px' }}>
                    <span style={{ fontSize: '1.4rem' }}>❌</span>
                    <p>{error}</p>
                </div>
            )}

            {analyzing && (
                <div style={{ textAlign: 'center', padding: '60px 0' }}>
                    <Spinner />
                    <p style={{ color: 'var(--text-secondary)', marginTop: '16px' }}>
                        Claude가 감사 로그를 분석하고 있습니다…
                    </p>
                </div>
            )}

            {!analyzing && report && (
                <>
                    {/* 요약 카드 */}
                    <div className="stat-grid mb-24">
                        <div className="stat-card">
                            <div className="stat-label">분석 로그</div>
                            <div className="stat-value">{report.totalLogsAnalyzed}</div>
                            <div className="stat-sub">최근 {report.periodDays}일</div>
                        </div>
                        <div className="stat-card">
                            <div className="stat-label">탐지된 이상</div>
                            <div className="stat-value" style={{ color: report.findings.length > 0 ? 'var(--red)' : 'var(--green)' }}>
                                {report.findings.length}
                            </div>
                            <div className="stat-sub">패턴 발견</div>
                        </div>
                        <div className="stat-card">
                            <div className="stat-label">CRITICAL / HIGH</div>
                            <div className="stat-value" style={{ color: criticalCount > 0 ? 'var(--red)' : highCount > 0 ? 'var(--yellow)' : 'var(--green)' }}>
                                {criticalCount} / {highCount}
                            </div>
                            <div className="stat-sub">즉시 대응 필요</div>
                        </div>
                        <div className="stat-card">
                            <div className="stat-label">분석 상태</div>
                            <div className="stat-value" style={{ fontSize: '1rem' }}>
                                {report.analysisStatus === 'SUCCESS' ? '✅ 완료' :
                                    report.analysisStatus === 'NO_LOGS' ? '📭 로그 없음' : '⚠️ 실패'}
                            </div>
                            <div className="stat-sub">{new Date(report.analyzedAt).toLocaleString('ko-KR')}</div>
                        </div>
                    </div>

                    {/* 리스크 요약 */}
                    {report.riskSummary && (
                        <div className="card mb-24" style={{ padding: '16px 20px' }}>
                            <div style={{ fontSize: '0.78rem', color: 'var(--text-secondary)', marginBottom: '6px' }}>AI 리스크 요약</div>
                            <p style={{ fontSize: '0.92rem', lineHeight: 1.6 }}>{report.riskSummary}</p>
                        </div>
                    )}

                    {/* 탐지 결과 */}
                    {report.findings.length === 0 ? (
                        <div className="verify-result verify-ok">
                            <span style={{ fontSize: '1.5rem' }}>✅</span>
                            <p>이상 패턴이 탐지되지 않았습니다.</p>
                        </div>
                    ) : (
                        <div className="findings-grid">
                            {report.findings.map((f, i) => (
                                <FindingCard key={i} finding={f} />
                            ))}
                        </div>
                    )}
                </>
            )}

            {!analyzing && !report && !error && (
                <div style={{ textAlign: 'center', padding: '80px 0', color: 'var(--text-secondary)' }}>
                    <div style={{ fontSize: '3rem', marginBottom: '16px' }}>🤖</div>
                    <p style={{ fontSize: '1rem' }}>AI 분석 실행 버튼을 눌러 감사 로그의 이상 패턴을 탐지하세요.</p>
                    <p style={{ fontSize: '0.85rem', marginTop: '8px' }}>
                        먼저 <strong>데모 시나리오 생성</strong>으로 의심 로그를 만들어보세요.
                    </p>
                </div>
            )}
        </Layout>
    )
}
