import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom'
import ErrorBoundary from './components/ErrorBoundary'
import { useAuthStore } from './store/auth.store'
import { jwtDecode } from './utils/jwt'
import LoginPage from './pages/LoginPage'
import DashboardPage from './pages/DashboardPage'
import ConsentsPage from './pages/ConsentsPage'
import ConsentDetailPage from './pages/ConsentDetailPage'
import TransfersPage from './pages/TransfersPage'
import TransferDetailPage from './pages/TransferDetailPage'
import AuditPage from './pages/admin/AuditPage'
import UsersPage from './pages/admin/UsersPage'
import AgentsPage from './pages/admin/AgentsPage'
import ReportPage from './pages/admin/ReportPage'
import McpPage from './pages/admin/McpPage'

function ProtectedRoute({ children, adminOnly = false }: { children: React.ReactNode; adminOnly?: boolean }) {
  const { token, role, logout } = useAuthStore()
  if (!token) return <Navigate to="/login" replace />

  const payload = jwtDecode(token)
  if (!payload || (typeof payload.exp === 'number' && payload.exp * 1000 < Date.now())) {
    logout()
    return <Navigate to="/login" replace />
  }

  if (adminOnly && role !== 'ADMIN') return <Navigate to="/dashboard" replace />
  return <>{children}</>
}

export default function App() {
  const { token } = useAuthStore()

  return (
    <ErrorBoundary>
    <BrowserRouter>
      <Routes>
        <Route path="/login" element={token ? <Navigate to="/dashboard" replace /> : <LoginPage />} />

        <Route path="/dashboard" element={
          <ProtectedRoute><DashboardPage /></ProtectedRoute>
        } />

        <Route path="/consents" element={
          <ProtectedRoute><ConsentsPage /></ProtectedRoute>
        } />
        <Route path="/consents/:id" element={
          <ProtectedRoute><ConsentDetailPage /></ProtectedRoute>
        } />

        <Route path="/transfers" element={
          <ProtectedRoute><TransfersPage /></ProtectedRoute>
        } />
        <Route path="/transfers/:id" element={
          <ProtectedRoute><TransferDetailPage /></ProtectedRoute>
        } />

        <Route path="/admin/audit" element={
          <ProtectedRoute adminOnly><AuditPage /></ProtectedRoute>
        } />
        <Route path="/admin/users" element={
          <ProtectedRoute adminOnly><UsersPage /></ProtectedRoute>
        } />
        <Route path="/admin/agents" element={
          <ProtectedRoute adminOnly><AgentsPage /></ProtectedRoute>
        } />
        <Route path="/admin/report" element={
          <ProtectedRoute adminOnly><ReportPage /></ProtectedRoute>
        } />
        <Route path="/admin/mcp" element={
          <ProtectedRoute adminOnly><McpPage /></ProtectedRoute>
        } />

        <Route path="*" element={<Navigate to={token ? '/dashboard' : '/login'} replace />} />
      </Routes>
    </BrowserRouter>
    </ErrorBoundary>
  )
}
