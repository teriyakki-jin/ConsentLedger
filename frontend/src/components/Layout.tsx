import { NavLink, useNavigate } from 'react-router-dom'
import { useAuthStore } from '../store/auth.store'

const IconHome = () => (
    <svg className="nav-icon" viewBox="0 0 20 20" fill="currentColor">
        <path d="M10.707 2.293a1 1 0 00-1.414 0l-7 7A1 1 0 003 11h1v6a1 1 0 001 1h4v-4h2v4h4a1 1 0 001-1v-6h1a1 1 0 00.707-1.707l-7-7z" />
    </svg>
)
const IconShield = () => (
    <svg className="nav-icon" viewBox="0 0 20 20" fill="currentColor">
        <path fillRule="evenodd" d="M10 1a9 9 0 100 18A9 9 0 0010 1zm-.875 5.5a.875.875 0 111.75 0v4.25a.875.875 0 01-1.75 0V6.5zm.875 7.5a1 1 0 100-2 1 1 0 000 2z" clipRule="evenodd" />
    </svg>
)
const IconArrow = () => (
    <svg className="nav-icon" viewBox="0 0 20 20" fill="currentColor">
        <path fillRule="evenodd" d="M3 10a.75.75 0 01.75-.75h10.638L10.23 5.29a.75.75 0 111.04-1.08l5.5 5.25a.75.75 0 010 1.08l-5.5 5.25a.75.75 0 11-1.04-1.08l4.158-3.96H3.75A.75.75 0 013 10z" clipRule="evenodd" />
    </svg>
)
const IconList = () => (
    <svg className="nav-icon" viewBox="0 0 20 20" fill="currentColor">
        <path fillRule="evenodd" d="M2 4.75A.75.75 0 012.75 4h14.5a.75.75 0 010 1.5H2.75A.75.75 0 012 4.75zM2 10a.75.75 0 01.75-.75h14.5a.75.75 0 010 1.5H2.75A.75.75 0 012 10zm0 5.25a.75.75 0 01.75-.75h14.5a.75.75 0 010 1.5H2.75a.75.75 0 01-.75-.75z" clipRule="evenodd" />
    </svg>
)

export default function Layout({ children, title }: { children: React.ReactNode; title: string }) {
    const { email, role, logout } = useAuthStore()
    const navigate = useNavigate()

    const handleLogout = () => {
        logout()
        navigate('/login')
    }

    return (
        <div className="app-layout">
            <aside className="sidebar">
                <div className="sidebar-logo">
                    <div className="logo-text">ConsentLedger</div>
                    <div className="logo-sub">데이터 전송 동의 관리 플랫폼</div>
                </div>

                <nav className="sidebar-nav">
                    <span className="sidebar-section">메인</span>
                    <NavLink to="/dashboard" end>
                        <IconHome />
                        대시보드
                    </NavLink>

                    <span className="sidebar-section">관리</span>
                    <NavLink to="/consents">
                        <IconShield />
                        동의 관리
                    </NavLink>
                    <NavLink to="/transfers">
                        <IconArrow />
                        전송 요청
                    </NavLink>

                    {role === 'ADMIN' && (
                        <>
                            <span className="sidebar-section">관리자</span>
                            <NavLink to="/admin/audit">
                                <IconList />
                                감사 로그
                            </NavLink>
                        </>
                    )}
                </nav>

                <div className="sidebar-footer">
                    <div className="sidebar-user">
                        <div className="avatar">{email?.charAt(0).toUpperCase() ?? 'U'}</div>
                        <div className="user-info">
                            <div className="user-email">{email}</div>
                            <div className="user-role">{role}</div>
                        </div>
                        <button className="logout-btn" title="로그아웃" onClick={handleLogout}>✕</button>
                    </div>
                </div>
            </aside>

            <div className="main-content">
                <header className="page-header">
                    <h1>{title}</h1>
                </header>
                <main className="page-body">{children}</main>
            </div>
        </div>
    )
}
