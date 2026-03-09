import { useEffect, useState } from 'react'
import { getMcpStatus, invokeMcpTool, type McpInvokeResponse, type McpServerStatus } from '../../api/admin'
import Layout from '../../components/Layout'
import { EmptyState, Spinner } from '../../components/Spinner'

const sampleCurl = (sseEndpoint: string) => `curl -N http://localhost:8080${sseEndpoint} \\
  -H "Authorization: Bearer <ADMIN_JWT>"`

const TOOL_EXAMPLES: Record<string, string> = {
    analyzeAnomalies: '{\n  "days": 7\n}',
    getAuditLogs: '{\n  "page": 0,\n  "action": "TRANSFER_EXECUTED"\n}',
    getConsentsByUser: '{\n  "userId": "a0000000-0000-0000-0000-000000000002"\n}',
    getTransferRequests: '{}',
    listUsers: '{}',
    verifyAuditChain: '{}',
}

export default function McpPage() {
    const [status, setStatus] = useState<McpServerStatus | null>(null)
    const [loading, setLoading] = useState(true)
    const [error, setError] = useState<string | null>(null)
    const [selectedTool, setSelectedTool] = useState('verifyAuditChain')
    const [paramsText, setParamsText] = useState(TOOL_EXAMPLES.verifyAuditChain)
    const [invoking, setInvoking] = useState(false)
    const [invokeError, setInvokeError] = useState<string | null>(null)
    const [invokeResult, setInvokeResult] = useState<McpInvokeResponse | null>(null)

    useEffect(() => {
        getMcpStatus()
            .then((result) => {
                setStatus(result)
                const defaultTool = result.tools.some((tool) => tool.name === 'verifyAuditChain')
                    ? 'verifyAuditChain'
                    : result.tools[0]?.name ?? 'verifyAuditChain'
                setSelectedTool(defaultTool)
                setParamsText(TOOL_EXAMPLES[defaultTool] ?? '{}')
            })
            .catch(() => setError('MCP 상태를 불러오지 못했습니다. 관리자 권한과 서버 로그를 확인하세요.'))
            .finally(() => setLoading(false))
    }, [])

    const handleToolChange = (toolName: string) => {
        setSelectedTool(toolName)
        setParamsText(TOOL_EXAMPLES[toolName] ?? '{}')
        setInvokeError(null)
    }

    const handleInvoke = async () => {
        setInvoking(true)
        setInvokeError(null)
        try {
            const parsedParams = paramsText.trim() ? JSON.parse(paramsText) : {}
            const result = await invokeMcpTool(selectedTool, parsedParams)
            setInvokeResult(result)
        } catch (err: any) {
            const apiMessage = err?.response?.data?.message
            const rawError = err instanceof SyntaxError ? '파라미터 JSON 형식이 올바르지 않습니다.' : null
            setInvokeError(rawError ?? apiMessage ?? 'MCP 툴 실행에 실패했습니다.')
        } finally {
            setInvoking(false)
        }
    }

    return (
        <Layout title="MCP 콘솔">
            {loading ? <Spinner /> : error || !status ? (
                <EmptyState message={error ?? 'MCP 상태를 불러올 수 없습니다.'} />
            ) : (
                <div className="mcp-page">
                    <div className="stat-grid">
                        <div className="stat-card">
                            <div className="stat-label">서버 이름</div>
                            <div className="stat-value mcp-stat-value">{status.serverName}</div>
                            <div className="stat-sub">Spring AI MCP 서버 식별자</div>
                        </div>
                        <div className="stat-card">
                            <div className="stat-label">전송 방식</div>
                            <div className="stat-value mcp-stat-value">{status.transport}</div>
                            <div className="stat-sub">현재 서버 transport 타입</div>
                        </div>
                        <div className="stat-card">
                            <div className="stat-label">등록 툴</div>
                            <div className="stat-value">{status.registeredTools}</div>
                            <div className="stat-sub">관리자 권한 필요: {status.adminProtected ? '예' : '아니오'}</div>
                        </div>
                    </div>

                    <div className="mcp-grid">
                        <section className="card mcp-panel">
                            <div className="mcp-panel-header">
                                <h2>연결 정보</h2>
                                <span className="badge badge-approved">MCP READY</span>
                            </div>
                            <div className="mcp-kv-list">
                                <div className="mcp-kv-row">
                                    <span>SSE Endpoint</span>
                                    <code>{status.sseEndpoint}</code>
                                </div>
                                <div className="mcp-kv-row">
                                    <span>Message Endpoint</span>
                                    <code>{status.messageEndpointTemplate}</code>
                                </div>
                                <div className="mcp-kv-row">
                                    <span>Version</span>
                                    <code>{status.serverVersion}</code>
                                </div>
                            </div>
                            <div className="mcp-note">
                                <strong>브라우저 주소창으로는 확인이 어렵습니다.</strong>
                                <p>`/sse`로 세션을 연 뒤, 내려온 `sessionId`를 사용해 `/mcp/message`로 JSON-RPC를 보내야 합니다.</p>
                            </div>
                        </section>

                        <section className="card mcp-panel">
                            <div className="mcp-panel-header">
                                <h2>빠른 확인 절차</h2>
                            </div>
                            <ol className="mcp-steps">
                                <li>관리자 계정으로 로그인해 JWT를 발급받습니다.</li>
                                <li><code>{status.sseEndpoint}</code>에 Bearer 토큰을 넣어 연결합니다.</li>
                                <li>SSE 응답의 <code>event:endpoint</code> 값을 확인합니다.</li>
                                <li>그 주소로 <code>initialize</code>, <code>tools/list</code>, <code>tools/call</code>을 보냅니다.</li>
                            </ol>
                            <pre className="mcp-code-block">{sampleCurl(status.sseEndpoint)}</pre>
                        </section>
                    </div>

                    <section className="card mcp-panel">
                        <div className="mcp-panel-header">
                            <h2>등록된 툴</h2>
                            <span className="text-secondary">{status.tools.length} tools</span>
                        </div>
                        <div className="mcp-tool-grid">
                            {status.tools.map((tool) => (
                                <article key={tool.name} className="mcp-tool-card">
                                    <div className="mcp-tool-meta">
                                        <code>{tool.name}</code>
                                        <span>{tool.sourceClass}</span>
                                    </div>
                                    <p>{tool.description}</p>
                                </article>
                            ))}
                        </div>
                    </section>

                    <section className="card mcp-panel">
                        <div className="mcp-panel-header">
                            <h2>툴 실행 콘솔</h2>
                            <span className="text-secondary">관리자 전용 수동 테스트</span>
                        </div>

                        <div className="mcp-console-controls">
                            <label className="mcp-label">
                                <span>Tool</span>
                                <select
                                    className="form-input"
                                    value={selectedTool}
                                    onChange={(e) => handleToolChange(e.target.value)}
                                >
                                    {status.tools.map((tool) => (
                                        <option key={tool.name} value={tool.name}>
                                            {tool.name}
                                        </option>
                                    ))}
                                </select>
                            </label>

                            <label className="mcp-label">
                                <span>Params JSON</span>
                                <textarea
                                    className="form-input mcp-textarea"
                                    value={paramsText}
                                    onChange={(e) => setParamsText(e.target.value)}
                                    spellCheck={false}
                                />
                            </label>

                            <div className="mcp-console-actions">
                                <button
                                    className="btn btn-primary"
                                    onClick={handleInvoke}
                                    disabled={invoking}
                                >
                                    {invoking ? '실행 중...' : '툴 실행'}
                                </button>
                                <button
                                    className="btn btn-secondary"
                                    onClick={() => setParamsText(TOOL_EXAMPLES[selectedTool] ?? '{}')}
                                    disabled={invoking}
                                >
                                    예제 로드
                                </button>
                            </div>
                        </div>

                        {invokeError && (
                            <div className="mcp-error-box">{invokeError}</div>
                        )}

                        {invokeResult && (
                            <div className="mcp-result-box">
                                <div className="mcp-result-meta">
                                    <span>{invokeResult.toolName}</span>
                                    <span>{new Date(invokeResult.executedAt).toLocaleString('ko-KR')}</span>
                                </div>
                                <pre className="mcp-code-block">{invokeResult.output}</pre>
                            </div>
                        )}
                    </section>
                </div>
            )}
        </Layout>
    )
}
