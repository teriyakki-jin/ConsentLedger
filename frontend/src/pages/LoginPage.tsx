import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { login } from '../api/auth'
import { useAuthStore } from '../store/auth.store'
import { jwtDecode } from '../utils/jwt'

export default function LoginPage() {
    const navigate = useNavigate()
    const setAuth = useAuthStore((s) => s.setAuth)
    const [email, setEmail] = useState('')
    const [password, setPassword] = useState('')
    const [loading, setLoading] = useState(false)
    const [error, setError] = useState<string | null>(null)

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault()
        setLoading(true)
        setError(null)
        try {
            const res = await login({ email, password })
            const payload = jwtDecode(res.accessToken)
            const role = payload?.role ?? 'USER'
            setAuth(res.accessToken, email, role as 'USER' | 'ADMIN')
            navigate('/dashboard')
        } catch (err: any) {
            setError(err?.response?.data?.message ?? '로그인에 실패했습니다.')
        } finally {
            setLoading(false)
        }
    }

    return (
        <div className="login-page">
            <div className="login-box">
                <div className="login-brand">
                    <div className="brand-name">ConsentLedger</div>
                    <div className="brand-tagline">데이터 전송 동의 관리 플랫폼</div>
                </div>
                <form className="login-form" onSubmit={handleSubmit}>
                    {error && <div className="alert alert-error">⚠ {error}</div>}
                    <div className="form-group">
                        <label className="form-label">이메일</label>
                        <input
                            id="email"
                            type="email"
                            className="form-input"
                            placeholder="user@example.com"
                            value={email}
                            onChange={(e) => setEmail(e.target.value)}
                            required
                            autoComplete="email"
                        />
                    </div>
                    <div className="form-group">
                        <label className="form-label">비밀번호</label>
                        <input
                            id="password"
                            type="password"
                            className="form-input"
                            placeholder="••••••••"
                            value={password}
                            onChange={(e) => setPassword(e.target.value)}
                            required
                            autoComplete="current-password"
                        />
                    </div>
                    <button id="login-btn" type="submit" className="btn btn-primary" disabled={loading}>
                        {loading ? '로그인 중...' : '로그인'}
                    </button>
                </form>
            </div>
        </div>
    )
}
