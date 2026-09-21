import type { ApiResponse } from '../types'

const API_BASE = (import.meta.env.VITE_API_BASE_URL ?? '').replace(/\/$/, '')

export function resolveAssetUrl(url?: string) {
  if (!url || /^(?:[a-z]+:|\/\/|blob:|data:)/i.test(url)) return url
  return `${API_BASE}${url.startsWith('/') ? '' : '/'}${url}`
}

export class ApiError extends Error {
  constructor(
    message: string,
    public readonly status: number,
    public readonly code?: number,
  ) {
    super(message)
  }
}

export async function request<T>(path: string, options: RequestInit = {}): Promise<T> {
  const token = localStorage.getItem('campus_token')
  const headers = new Headers(options.headers)
  if (options.body && !(options.body instanceof FormData) && !headers.has('Content-Type')) {
    headers.set('Content-Type', 'application/json')
  }
  if (token) {
    headers.set('Authorization', `Bearer ${token}`)
  }

  const response = await fetch(`${API_BASE}${path}`, { ...options, headers })
  let body: ApiResponse<T>
  try {
    body = (await response.json()) as ApiResponse<T>
  } catch {
    throw new ApiError('服务响应异常，请稍后重试', response.status)
  }
  if (!response.ok || body.code !== 0) {
    if (response.status === 401) {
      localStorage.removeItem('campus_token')
      localStorage.removeItem('campus_user')
    }
    throw new ApiError(body.message || '请求失败', response.status, body.code)
  }
  return body.data
}
