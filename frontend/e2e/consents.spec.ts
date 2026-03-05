import { test, expect } from '@playwright/test'
import { loginAs, USER, DATA_HOLDER_ID } from './helpers'

test.describe('동의 관리', () => {
    test.beforeEach(async ({ page }) => {
        await loginAs(page, USER)
        await page.goto('/consents')
    })

    test('동의 목록 페이지 렌더링', async ({ page }) => {
        await expect(page.locator('h1')).toContainText('동의 관리')
        await expect(page.locator('#btn-create-consent')).toBeVisible()
    })

    test('새 동의 생성', async ({ page }) => {
        await page.click('#btn-create-consent')
        await expect(page.locator('.modal')).toBeVisible()

        await page.fill('#input-dataholder-id', DATA_HOLDER_ID)
        await page.fill('#input-scopes', 'READ, WRITE')
        await page.click('#btn-submit-consent')

        await expect(page.locator('.modal')).not.toBeVisible()
        await expect(page.locator('table')).toBeVisible()
    })

    test('동의 상세 페이지 이동', async ({ page }) => {
        const firstDetailBtn = page.locator('button:has-text("상세 보기")').first()
        await expect(firstDetailBtn).toBeVisible()
        await firstDetailBtn.click()
        await expect(page).toHaveURL(/\/consents\/[\w-]+/)
        await expect(page.locator('h1')).toContainText('동의 상세')
    })

    test('동의 철회', async ({ page }) => {
        await page.click('#btn-create-consent')
        await page.fill('#input-dataholder-id', DATA_HOLDER_ID)
        await page.fill('#input-scopes', 'READ')
        await page.click('#btn-submit-consent')
        await expect(page.locator('.modal')).not.toBeVisible()

        // 방금 생성한 동의의 상세 페이지로 이동
        const firstDetailBtn = page.locator('button:has-text("상세 보기")').first()
        await firstDetailBtn.click()
        await expect(page).toHaveURL(/\/consents\/[\w-]+/)

        const revokeBtn = page.locator('button:has-text("동의 철회")')
        await expect(revokeBtn).toBeVisible()
        await revokeBtn.click()

        await expect(page.locator('.badge-revoked')).toBeVisible()
    })

    test('동의 생성 모달 취소', async ({ page }) => {
        await page.click('#btn-create-consent')
        await expect(page.locator('.modal')).toBeVisible()
        await page.click('button:has-text("취소")')
        await expect(page.locator('.modal')).not.toBeVisible()
    })
})
