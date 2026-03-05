/** Very minimal JWT payload decoder (no verification – for display only) */
export function jwtDecode(token: string): Record<string, any> | null {
    try {
        const base64Url = token.split('.')[1]
        const base64 = base64Url.replace(/-/g, '+').replace(/_/g, '/')
        return JSON.parse(atob(base64))
    } catch {
        return null
    }
}

export function formatDate(iso: string | null | undefined): string {
    if (!iso) return '-'
    return new Date(iso).toLocaleString('ko-KR', { hour12: false })
}

export function shortId(id: string | null | undefined): string {
    if (!id) return '-'
    return id.slice(0, 8) + '…'
}
