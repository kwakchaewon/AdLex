import axios from 'axios'
import type { CheckRequest, CheckResponse } from '@/types/check'

const checkClient = axios.create({
  baseURL: '/v1',
  timeout: 30000,
  headers: { 'Content-Type': 'application/json' },
})

export function runCheck(apiKey: string, req: CheckRequest): Promise<CheckResponse> {
  return checkClient
    .post<CheckResponse>('/check', req, {
      headers: { 'X-API-Key': apiKey },
    })
    .then((r) => r.data)
}
