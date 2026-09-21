import { request } from './client'
import type {
  AdminPost,
  AdminReport,
  AdminUser,
  AuthPayload,
  Category,
  Comment,
  DashboardStats,
  PageResult,
  Post,
  UploadResult,
  User,
} from '../types'

export const authApi = {
  login: (payload: { username: string; password: string }) =>
    request<AuthPayload>('/api/auth/login', { method: 'POST', body: JSON.stringify(payload) }),
  register: (payload: { username: string; nickname: string; password: string }) =>
    request<AuthPayload>('/api/auth/register', { method: 'POST', body: JSON.stringify(payload) }),
  me: () => request<User>('/api/auth/me'),
}

export const communityApi = {
  categories: () => request<Category[]>('/api/categories'),
  posts: (params: { page?: number; size?: number; keyword?: string; categoryId?: number }) => {
    const query = new URLSearchParams()
    Object.entries(params).forEach(([key, value]) => {
      if (value !== undefined && value !== '') query.set(key, String(value))
    })
    return request<PageResult<Post>>(`/api/posts?${query}`)
  },
  post: (id: number) => request<Post>(`/api/posts/${id}`),
  createPost: (payload: { categoryId: number; title: string; content: string; imageUrls: string[] }) =>
    request<Post>('/api/posts', { method: 'POST', body: JSON.stringify(payload) }),
  uploadImage: (file: File) => {
    const formData = new FormData()
    formData.append('file', file)
    return request<UploadResult>('/api/files/images', { method: 'POST', body: formData })
  },
  toggleLike: (id: number) =>
    request<{ liked: boolean; likeCount: number }>(`/api/posts/${id}/like`, { method: 'POST' }),
  comments: (postId: number) => request<Comment[]>(`/api/posts/${postId}/comments`),
  createComment: (postId: number, content: string) =>
    request<Comment>(`/api/posts/${postId}/comments`, {
      method: 'POST',
      body: JSON.stringify({ content }),
    }),
  reportPost: (postId: number, reason: string) =>
    request<void>('/api/reports', {
      method: 'POST',
      body: JSON.stringify({ targetType: 'POST', targetId: postId, reason }),
    }),
}

export const adminApi = {
  dashboard: () => request<DashboardStats>('/api/admin/dashboard'),
  users: (params: { page?: number; size?: number; keyword?: string; status?: string } = {}) => {
    const query = new URLSearchParams()
    Object.entries(params).forEach(([key, value]) => value !== undefined && value !== '' && query.set(key, String(value)))
    return request<PageResult<AdminUser>>(`/api/admin/users?${query}`)
  },
  updateUserStatus: (id: number, status: string) =>
    request<void>(`/api/admin/users/${id}/status`, { method: 'PUT', body: JSON.stringify({ status }) }),
  posts: (params: { page?: number; size?: number; keyword?: string; status?: string } = {}) => {
    const query = new URLSearchParams()
    Object.entries(params).forEach(([key, value]) => value !== undefined && value !== '' && query.set(key, String(value)))
    return request<PageResult<AdminPost>>(`/api/admin/posts?${query}`)
  },
  updatePostStatus: (id: number, status: string) =>
    request<void>(`/api/admin/posts/${id}/status`, { method: 'PUT', body: JSON.stringify({ status }) }),
  reports: (params: { page?: number; size?: number; status?: string } = {}) => {
    const query = new URLSearchParams()
    Object.entries(params).forEach(([key, value]) => value !== undefined && value !== '' && query.set(key, String(value)))
    return request<PageResult<AdminReport>>(`/api/admin/reports?${query}`)
  },
  resolveReport: (id: number, status: 'RESOLVED' | 'REJECTED', note = '') =>
    request<void>(`/api/admin/reports/${id}`, { method: 'PUT', body: JSON.stringify({ status, note }) }),
}
