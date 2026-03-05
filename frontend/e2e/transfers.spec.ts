import { test, expect } from '@playwright/test'
import { loginAs, USER, DATA_HOLDER_ID } from './helpers'

test.describe('전송 요청', () => {
    test.beforeEach(async ({ page }) => {
        await loginAs(page, USER)
    })

    test('전송 요청 목록 페이지 렌더링', async ({ page }) => {
        await page.goto('/transfers')
        await expect(page.locator('h1')).toContainText('전송 요청')
        await expect(page.locator('#btn-create-transfer')).toBeVisible()
    })

    test('전송 요청 생성', async ({ page }) => {
        // 먼저 동의 생성
        await page.goto('/consents')
        await page.click('#btn-create-consent')
        await page.fill('#input-dataholder-id', DATA_HOLDER_ID)
        await page.fill('#input-scopes', 'READ')
        await page.click('#btn-submit-consent')
        await expect(page.locator('.modal')).not.toBeVisible()

        // 동의 ID 획득
        const detailBtn = page.locator('button:has-text("상세 보기")').first()
        await detailBtn.click()
        const url = page.url()
        const consentId = url.split('/consents/')[1]

        // 전송 요청 생성
        await page.goto('/transfers')
        await page.click('#btn-create-transfer')
        await expect(page.locator('.modal')).toBeVisible()

        await page.fill('#input-consent-id', consentId)
        await page.fill('#input-method', 'API_PUSH')
        await page.click('#btn-submit-transfer')

        await expect(page.locator('.modal')).not.toBeVisible()
        await expect(page.locator('table')).toBeVisible()
    })

    test('전송 요청 상세 페이지 이동', async ({ page }) => {
        await page.goto('/transfers')
        const firstDetailBtn = page.locator('button:has-text("상세 보기")').first()
        const count = await firstDetailBtn.count()
        if (count === 0) {
            test.skip()
            return
        }
        await firstDetailBtn.click()
        await expect(page).toHaveURL(/\/transfers\/[\w-]+/)
        await expect(page.locator('h1')).toContainText('전송 요청 상세')
    })

    test('전송 요청 승인', async ({ page }) => {
        await page.goto('/transfers')
        const approveBtn = page.locator('button:has-text("승인")').first()
        const count = await approveBtn.count()
        if (count === 0) {
            test.skip()
            return
        }
        await approveBtn.click()
        await expect(page.locator('.badge-approved')).toBeVisible()
    })
})
