import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom'
import { useAuthStore } from './store/auth.store'
import LoginPage from './pages/LoginPage'
import DashboardPage from './pages/DashboardPage'
import ConsentsPage from './pages/ConsentsPage'
import ConsentDetailPage from './pages/ConsentDetailPage'
import TransfersPage from './pages/TransfersPage'
import TransferDetailPage from './pages/TransferDetailPage'
import AuditPage from './pages/admin/AuditPage'

function ProtectedRoute({ children, adminOnly = false }: { children: React.ReactNode; adminOnly?: boolean }) {
  const { token, role } = useAuthStore()
  if (!token) return <Navigate to="/login" replace />
  if (adminOnly && role !== 'ADMIN') return <Navigate to="/dashboard" replace />
  return <>{children}</>
}

export default function App() {
  const { token } = useAuthStore()

  return (
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

        <Route path="*" element={<Navigate to={token ? '/dashboard' : '/login'} replace />} />
      </Routes>
    </BrowserRouter>
  )
}
