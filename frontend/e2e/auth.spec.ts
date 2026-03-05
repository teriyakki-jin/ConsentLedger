import { test, expect } from '@playwright/test'
import { loginAs, logout, USER, ADMIN } from './helpers'

test.describe('인증', () => {
    test('잘못된 비밀번호로 로그인 실패', async ({ page }) => {
        await page.goto('/login')
        await page.fill('#email', USER.email)
        await page.fill('#password', 'wrongpassword')
        await page.click('#login-btn')
        await expect(page.locator('.alert-error')).toBeVisible()
        await expect(page).toHaveURL(/\/login/)
    })

    test('일반 사용자 로그인 → 대시보드 이동', async ({ page }) => {
        await loginAs(page, USER)
        await expect(page).toHaveURL(/\/dashboard/)
        await expect(page.locator('.logo-text')).toContainText('ConsentLedger')
    })

    test('일반 사용자는 관리자 페이지 접근 불가', async ({ page }) => {
        await loginAs(page, USER)
        await page.goto('/admin/users')
        await expect(page).toHaveURL(/\/dashboard/)
    })

    test('관리자 로그인 → 관리자 메뉴 표시', async ({ page }) => {
        await loginAs(page, ADMIN)
        await expect(page.locator('text=사용자 관리')).toBeVisible()
        await expect(page.locator('text=에이전트 관리')).toBeVisible()
        await expect(page.locator('text=감사 로그')).toBeVisible()
        await expect(page.locator('text=감사 리포트')).toBeVisible()
    })

    test('로그아웃 → 로그인 페이지로 이동', async ({ page }) => {
        await loginAs(page, USER)
        await logout(page)
        await expect(page).toHaveURL(/\/login/)
    })

    test('로그아웃 후 보호 경로 접근 시 로그인 페이지로 리다이렉트', async ({ page }) => {
        await loginAs(page, USER)
        await logout(page)
        await page.goto('/consents')
        await expect(page).toHaveURL(/\/login/)
    })
})
