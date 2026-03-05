import { Page } from '@playwright/test'

export const ADMIN = { email: 'admin@consentledger.com', password: 'admin1234' }
export const USER = { email: 'user@consentledger.com', password: 'user1234' }
export const DATA_HOLDER_ID = 'c0000000-0000-0000-0000-000000000001'

export async function loginAs(page: Page, creds: { email: string; password: string }) {
    await page.goto('/login')
    await page.fill('#email', creds.email)
    await page.fill('#password', creds.password)
    await page.click('#login-btn')
    await page.waitForURL('**/dashboard')
}

export async function logout(page: Page) {
    await page.click('.logout-btn')
    await page.waitForURL('**/login')
}
