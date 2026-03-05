import { useEffect, useState } from 'react'
import { getUsers, type UserSummary } from '../../api/admin'
import Layout from '../../components/Layout'
import { Spinner, EmptyState } from '../../components/Spinner'
import { formatDate } from '../../utils/jwt'

export default function UsersPage() {
    const [users, setUsers] = useState<UserSummary[]>([])
    const [loading, setLoading] = useState(true)
    const [error, setError] = useState<string | null>(null)

    useEffect(() => {
        getUsers()
            .then(setUsers)
            .catch(() => setError('사용자 목록을 불러오는데 실패했습니다.'))
            .finally(() => setLoading(false))
    }, [])

    return (
        <Layout title="사용자 관리">
            {loading ? <Spinner /> : error ? (
                <EmptyState message={error} />
            ) : users.length === 0 ? (
                <EmptyState message="사용자가 없습니다." />
            ) : (
                <div className="table-wrap">
                    <table>
                        <thead>
                            <tr>
                                <th>ID</th>
                                <th>이메일</th>
                                <th>역할</th>
                                <th>가입일</th>
                            </tr>
                        </thead>
                        <tbody>
                            {users.map((user) => (
                                <tr key={user.id}>
                                    <td className="td-mono">{user.id.slice(0, 8)}…</td>
                                    <td>{user.email}</td>
                                    <td>
                                        <span className={`badge ${user.role === 'ADMIN' ? 'badge-approved' : 'badge-active'}`}>
                                            {user.role}
                                        </span>
                                    </td>
                                    <td className="text-secondary" style={{ fontSize: '0.78rem', whiteSpace: 'nowrap' }}>
                                        {formatDate(user.createdAt)}
                                    </td>
                                </tr>
                            ))}
                        </tbody>
                    </table>
                </div>
            )}
        </Layout>
    )
}
