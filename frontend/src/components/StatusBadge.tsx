type Status =
    | 'ACTIVE' | 'REVOKED' | 'EXPIRED'
    | 'REQUESTED' | 'APPROVED' | 'DENIED' | 'COMPLETED' | 'FAILED'

const labelMap: Record<Status, string> = {
    ACTIVE: '활성',
    REVOKED: '철회됨',
    EXPIRED: '만료됨',
    REQUESTED: '요청됨',
    APPROVED: '승인됨',
    DENIED: '거부됨',
    COMPLETED: '완료됨',
    FAILED: '실패',
}

export default function StatusBadge({ status }: { status: Status }) {
    const cls = `badge badge-${status.toLowerCase()}`
    return <span className={cls}>{labelMap[status] ?? status}</span>
}
