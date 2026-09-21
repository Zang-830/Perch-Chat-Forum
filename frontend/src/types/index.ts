export interface ApiResponse<T> {
  code: number
  message: string
  data: T
}

export interface PageResult<T> {
  items: T[]
  total: number
  page: number
  size: number
}

export interface User {
  id: number
  username: string
  nickname: string
  avatarUrl?: string
  bio?: string
  role: string
}

export interface Category {
  id: number
  code: string
  name: string
  description: string
  color: string
}

export interface Post {
  id: number
  title: string
  summary: string
  content?: string
  likeCount: number
  commentCount: number
  createdAt: string
  author: User
  category?: Category
  imageUrls: string[]
  likedByMe: boolean
}

export interface Comment {
  id: number
  content: string
  createdAt: string
  author: User
}

export interface AuthPayload {
  token: string
  user: User
}

export interface UploadResult {
  id: number
  url: string
  originalName: string
  contentType: string
  size: number
}

export interface DashboardStats {
  totalUsers: number
  activeUsers: number
  bannedUsers: number
  totalPosts: number
  publishedPosts: number
  hiddenPosts: number
  totalComments: number
  totalImages: number
  pendingReports: number
}

export interface AdminUser {
  id: number
  username: string
  nickname: string
  role: string
  status: string
  createdAt: string
}

export interface AdminPost {
  id: number
  title: string
  authorName: string
  categoryName: string
  status: string
  likeCount: number
  commentCount: number
  imageCount: number
  createdAt: string
}

export interface AdminReport {
  id: number
  targetType: string
  targetId: number
  targetTitle?: string
  reporterName: string
  reason: string
  status: string
  handleNote?: string
  createdAt: string
  handledAt?: string
}
