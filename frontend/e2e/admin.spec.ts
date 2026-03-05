import { test, expect } from '@playwright/test'
import { loginAs, ADMIN } from './helpers'

test.describe('관리자 페이지', () => {
    test.beforeEach(async ({ page }) => {
        await loginAs(page, ADMIN)
    })

    test('사용자 목록 조회', async ({ page }) => {
        await page.goto('/admin/users')
        await expect(page.locator('h1')).toContainText('사용자 관리')
        await expect(page.locator('table')).toBeVisible()
        // admin, user 계정이 최소 1건 이상
        await expect(page.locator('tbody tr').first()).toBeVisible()
    })

    test('에이전트 목록 조회', async ({ page }) => {
        await page.goto('/admin/agents')
        await expect(page.locator('h1')).toContainText('에이전트 관리')
        await expect(page.locator('table')).toBeVisible()
    })

    test('에이전트 상태 변경 (ACTIVE → SUSPENDED → ACTIVE)', async ({ page }) => {
        await page.goto('/admin/agents')
        await expect(page.locator('table')).toBeVisible()

        const firstSelect = page.locator('select').first()
        await expect(firstSelect).toBeVisible()

        // SUSPENDED로 변경
        await firstSelect.selectOption('SUSPENDED')
        await expect(page.locator('.badge-revoked').first()).toBeVisible()

        // ACTIVE로 복원
        await firstSelect.selectOption('ACTIVE')
        await expect(page.locator('.badge-active').first()).toBeVisible()
    })

    test('감사 로그 목록 조회', async ({ page }) => {
        await page.goto('/admin/audit')
        await expect(page.locator('h1')).toContainText('감사 로그')
        await expect(page.locator('table')).toBeVisible()
    })

    test('감사 로그 해시 체인 무결성 검증', async ({ page }) => {
        await page.goto('/admin/audit')
        await page.click('#btn-verify')
        await expect(page.locator('.verify-result')).toBeVisible()
        await expect(page.locator('.verify-ok')).toBeVisible()
    })

    test('감사 리포트 페이지 렌더링', async ({ page }) => {
        await page.goto('/admin/report')
        await expect(page.locator('h1')).toContainText('감사 리포트')
        await expect(page.locator('input[type="date"]')).toHaveCount(2)
        await expect(page.locator('button:has-text("PDF 다운로드")')).toBeVisible()
    })

    test('감사 리포트 날짜 역방향 검증', async ({ page }) => {
        await page.goto('/admin/report')
        await page.fill('input[type="date"]:first-of-type', '2025-12-31')
        await page.fill('input[type="date"]:last-of-type', '2025-01-01')
        await page.click('button:has-text("PDF 다운로드")')
        await expect(page.locator('text=시작일은 종료일 이전이어야 합니다.')).toBeVisible()
    })

    test('감사 리포트 PDF 다운로드', async ({ page }) => {
        await page.goto('/admin/report')

        const today = new Date().toISOString().slice(0, 10)
        const monthAgo = new Date(Date.now() - 30 * 24 * 60 * 60 * 1000).toISOString().slice(0, 10)

        await page.fill('input[type="date"]:first-of-type', monthAgo)
        await page.fill('input[type="date"]:last-of-type', today)

        const [download] = await Promise.all([
            page.waitForEvent('download'),
            page.click('button:has-text("PDF 다운로드")'),
        ])
        expect(download.suggestedFilename()).toMatch(/audit-report.*\.pdf/)
    })
})
