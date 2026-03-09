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
const IconUsers = () => (
    <svg className="nav-icon" viewBox="0 0 20 20" fill="currentColor">
        <path d="M9 6a3 3 0 11-6 0 3 3 0 016 0zM17 6a3 3 0 11-6 0 3 3 0 016 0zM12.93 17c.046-.327.07-.66.07-1a6.97 6.97 0 00-1.5-4.33A5 5 0 0119 16v1h-6.07zM6 11a5 5 0 015 5v1H1v-1a5 5 0 015-5z" />
    </svg>
)
const IconRobot = () => (
    <svg className="nav-icon" viewBox="0 0 20 20" fill="currentColor">
        <path fillRule="evenodd" d="M10 2a1 1 0 011 1v1.323l3.954 1.582 1.599-.8a1 1 0 01.894 1.79l-1.233.616 1.738 5.42a1 1 0 01-.285 1.05A3.989 3.989 0 0115 14a3.989 3.989 0 01-2.667-1.019 1 1 0 01-.285-1.05l1.715-5.349L11 5.677V17h2a1 1 0 110 2H7a1 1 0 110-2h2V5.677L6.237 7.582l1.715 5.349a1 1 0 01-.285 1.05A3.989 3.989 0 015 15a3.989 3.989 0 01-2.667-1.019 1 1 0 01-.285-1.05l1.738-5.42-1.233-.616a1 1 0 01.894-1.79l1.599.8L9 4.323V3a1 1 0 011-1z" clipRule="evenodd" />
    </svg>
)
const IconDocument = () => (
    <svg className="nav-icon" viewBox="0 0 20 20" fill="currentColor">
        <path fillRule="evenodd" d="M4 4a2 2 0 012-2h4.586A2 2 0 0112 2.586L15.414 6A2 2 0 0116 7.414V16a2 2 0 01-2 2H6a2 2 0 01-2-2V4zm2 6a1 1 0 011-1h6a1 1 0 110 2H7a1 1 0 01-1-1zm1 3a1 1 0 100 2h6a1 1 0 100-2H7z" clipRule="evenodd" />
    </svg>
)
const IconPlug = () => (
    <svg className="nav-icon" viewBox="0 0 20 20" fill="currentColor">
        <path d="M6 2.75a.75.75 0 011.5 0V6h5V2.75a.75.75 0 011.5 0V6h.75a.75.75 0 010 1.5H14v1.75A4.75 4.75 0 019.75 14v2.25H12a.75.75 0 010 1.5H8a.75.75 0 010-1.5h.25V14A4.75 4.75 0 014 9.25V7.5h-.75a.75.75 0 010-1.5H4V2.75a.75.75 0 011.5 0V6H6V2.75z" />
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
                            <NavLink to="/admin/users">
                                <IconUsers />
                                사용자 관리
                            </NavLink>
                            <NavLink to="/admin/agents">
                                <IconRobot />
                                에이전트 관리
                            </NavLink>
                            <NavLink to="/admin/audit">
                                <IconList />
                                감사 로그
                            </NavLink>
                            <NavLink to="/admin/report">
                                <IconDocument />
                                감사 리포트
                            </NavLink>
                            <NavLink to="/admin/mcp">
                                <IconPlug />
                                MCP 콘솔
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
