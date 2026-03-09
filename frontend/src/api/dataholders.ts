import client from './client'

export interface DataHolderSummary {
    id: string
    institutionCode: string
    name: string
    supportedMethods: string[]
}

export const listDataHolders = () =>
    client.get<{ data: DataHolderSummary[] }>('/data-holders').then((r) => r.data.data)
