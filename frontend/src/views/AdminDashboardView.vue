<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { adminApi } from '../api/community'
import type { AdminPost, AdminReport, AdminUser, DashboardStats } from '../types'
import { formatTime } from '../utils/format'

type Section = 'overview' | 'posts' | 'users' | 'reports'

const activeSection = ref<Section>('overview')
const stats = ref<DashboardStats>()
const users = ref<AdminUser[]>([])
const posts = ref<AdminPost[]>([])
const reports = ref<AdminReport[]>([])
const usersTotal = ref(0)
const postsTotal = ref(0)
const reportsTotal = ref(0)
const loading = ref(true)
const error = ref('')
const notice = ref('')
const keyword = ref('')
const userStatus = ref('')
const postStatus = ref('')
const reportStatus = ref('PENDING')

const recentPosts = computed(() => posts.value.slice(0, 5))
const pendingReports = computed(() => reports.value.filter((item) => item.status === 'PENDING').slice(0, 4))

async function loadDashboard() {
  loading.value = true
  error.value = ''
  try {
    const [statsResult, usersResult, postsResult, reportsResult] = await Promise.all([
      adminApi.dashboard(),
      adminApi.users({ page: 1, size: 20, keyword: keyword.value, status: userStatus.value }),
      adminApi.posts({ page: 1, size: 20, keyword: keyword.value, status: postStatus.value }),
      adminApi.reports({ page: 1, size: 20, status: reportStatus.value }),
    ])
    stats.value = statsResult
    users.value = usersResult.items
    usersTotal.value = usersResult.total
    posts.value = postsResult.items
    postsTotal.value = postsResult.total
    reports.value = reportsResult.items
    reportsTotal.value = reportsResult.total
  } catch (reason) {
    error.value = reason instanceof Error ? reason.message : '管理数据加载失败'
  } finally {
    loading.value = false
  }
}

async function refreshUsers() {
  const result = await adminApi.users({ page: 1, size: 20, keyword: keyword.value, status: userStatus.value })
  users.value = result.items
  usersTotal.value = result.total
}

async function refreshPosts() {
  const result = await adminApi.posts({ page: 1, size: 20, keyword: keyword.value, status: postStatus.value })
  posts.value = result.items
  postsTotal.value = result.total
}

async function refreshReports() {
  const result = await adminApi.reports({ page: 1, size: 20, status: reportStatus.value })
  reports.value = result.items
  reportsTotal.value = result.total
}

function flash(message: string) {
  notice.value = message
  window.setTimeout(() => { notice.value = '' }, 2200)
}

async function toggleUser(user: AdminUser) {
  const next = user.status === 'ACTIVE' ? 'BANNED' : 'ACTIVE'
  const label = next === 'BANNED' ? '封禁' : '恢复'
  if (!window.confirm(`确认${label}用户“${user.nickname}”吗？`)) return
  try {
    await adminApi.updateUserStatus(user.id, next)
    user.status = next
    if (stats.value) {
      stats.value.activeUsers += next === 'ACTIVE' ? 1 : -1
      stats.value.bannedUsers += next === 'BANNED' ? 1 : -1
    }
    flash(`已${label}该用户`)
  } catch (reason) {
    error.value = reason instanceof Error ? reason.message : '操作失败'
  }
}

async function togglePost(post: AdminPost) {
  const next = post.status === 'PUBLISHED' ? 'HIDDEN' : 'PUBLISHED'
  const label = next === 'HIDDEN' ? '隐藏' : '恢复'
  if (!window.confirm(`确认${label}帖子“${post.title}”吗？`)) return
  try {
    await adminApi.updatePostStatus(post.id, next)
    post.status = next
    if (stats.value) {
      stats.value.publishedPosts += next === 'PUBLISHED' ? 1 : -1
      stats.value.hiddenPosts += next === 'HIDDEN' ? 1 : -1
    }
    flash(`帖子已${label}`)
  } catch (reason) {
    error.value = reason instanceof Error ? reason.message : '操作失败'
  }
}

async function resolveReport(report: AdminReport, result: 'RESOLVED' | 'REJECTED') {
  const note = window.prompt(result === 'RESOLVED' ? '处理备注（可选）' : '驳回原因（可选）') ?? ''
  try {
    await adminApi.resolveReport(report.id, result, note)
    report.status = result
    report.handleNote = note
    if (stats.value && stats.value.pendingReports > 0) stats.value.pendingReports -= 1
    flash(result === 'RESOLVED' ? '举报已处理' : '举报已驳回')
  } catch (reason) {
    error.value = reason instanceof Error ? reason.message : '操作失败'
  }
}

onMounted(loadDashboard)
</script>

<template>
  <div class="admin-page">
    <aside class="admin-sidebar">
      <RouterLink class="admin-brand" to="/">
        <span class="brand__mark" aria-hidden="true"><span></span><span></span><span></span></span>
        <span><b>栖语管理台</b><small>Campus Console</small></span>
      </RouterLink>
      <nav aria-label="管理导航">
        <button :class="{ active: activeSection === 'overview' }" @click="activeSection = 'overview'"><i>⌂</i><span>数据概览</span></button>
        <button :class="{ active: activeSection === 'posts' }" @click="activeSection = 'posts'"><i>▤</i><span>内容管理</span><em>{{ postsTotal }}</em></button>
        <button :class="{ active: activeSection === 'users' }" @click="activeSection = 'users'"><i>♙</i><span>用户管理</span></button>
        <button :class="{ active: activeSection === 'reports' }" @click="activeSection = 'reports'"><i>!</i><span>举报中心</span><em v-if="stats?.pendingReports">{{ stats.pendingReports }}</em></button>
      </nav>
      <div class="admin-sidebar__foot"><span>系统运行正常</span><small>开发环境 · v0.2.0</small><RouterLink to="/">← 返回社区</RouterLink></div>
    </aside>

    <main class="admin-main">
      <header class="admin-header">
        <div><span class="section-label">ADMIN CONSOLE</span><h1>{{ activeSection === 'overview' ? '社区运营概览' : activeSection === 'posts' ? '内容管理' : activeSection === 'users' ? '用户管理' : '举报中心' }}</h1></div>
        <div class="admin-header__actions"><span class="admin-live"><i></i>实时数据</span><button class="admin-icon-button" title="刷新" @click="loadDashboard">↻</button></div>
      </header>

      <div v-if="notice" class="admin-toast">✓ {{ notice }}</div>
      <div v-if="error" class="admin-alert"><span>{{ error }}</span><button @click="error = ''">×</button></div>
      <div v-if="loading" class="admin-loading"><i></i><span>正在整理社区数据…</span></div>

      <template v-else-if="activeSection === 'overview' && stats">
        <section class="admin-stats">
          <article><span class="admin-stat-icon purple">♙</span><div><small>社区用户</small><strong>{{ stats.totalUsers.toLocaleString() }}</strong><em>{{ stats.activeUsers }} 位状态正常</em></div></article>
          <article><span class="admin-stat-icon cyan">▤</span><div><small>累计帖子</small><strong>{{ stats.totalPosts.toLocaleString() }}</strong><em>{{ stats.publishedPosts }} 篇公开展示</em></div></article>
          <article><span class="admin-stat-icon amber">◫</span><div><small>上传图片</small><strong>{{ stats.totalImages.toLocaleString() }}</strong><em>本地安全存储</em></div></article>
          <article><span class="admin-stat-icon rose">!</span><div><small>待处理举报</small><strong>{{ stats.pendingReports }}</strong><em>{{ stats.hiddenPosts }} 篇内容已隐藏</em></div></article>
        </section>

        <section class="admin-overview-grid">
          <div class="admin-panel admin-panel--activity">
            <div class="admin-panel__heading"><div><span class="section-label">ACTIVITY</span><h2>近期待审核内容</h2></div><button @click="activeSection = 'posts'">查看全部 →</button></div>
            <div class="admin-activity-list">
              <article v-for="post in recentPosts" :key="post.id">
                <span class="admin-content-icon">{{ post.imageCount ? '▧' : 'T' }}</span>
                <div><b>{{ post.title }}</b><small>{{ post.authorName }} · {{ post.categoryName }} · {{ formatTime(post.createdAt) }}</small></div>
                <span class="status-badge" :class="post.status.toLowerCase()">{{ post.status === 'PUBLISHED' ? '展示中' : '已隐藏' }}</span>
              </article>
            </div>
          </div>

          <div class="admin-panel">
            <div class="admin-panel__heading"><div><span class="section-label">REPORTS</span><h2>待处理举报</h2></div><button @click="activeSection = 'reports'">进入中心 →</button></div>
            <div v-if="pendingReports.length" class="admin-report-mini">
              <article v-for="report in pendingReports" :key="report.id"><span>!</span><div><b>{{ report.targetTitle }}</b><p>{{ report.reason }}</p><small>{{ report.reporterName }} · {{ formatTime(report.createdAt) }}</small></div></article>
            </div>
            <div v-else class="admin-empty">目前没有待处理举报</div>
          </div>
        </section>
      </template>

      <template v-else-if="activeSection === 'posts'">
        <section class="admin-toolbar"><div class="admin-search"><span>⌕</span><input v-model="keyword" placeholder="搜索帖子标题" @keyup.enter="refreshPosts" /></div><select v-model="postStatus" @change="refreshPosts"><option value="">全部状态</option><option value="PUBLISHED">展示中</option><option value="HIDDEN">已隐藏</option></select><button class="button button--primary button--small" @click="refreshPosts">筛选</button><span class="admin-total">共 {{ postsTotal }} 条</span></section>
        <section class="admin-table-card"><div class="admin-table admin-table--posts"><header><span>内容</span><span>发布者 / 分类</span><span>互动</span><span>状态</span><span>操作</span></header><article v-for="post in posts" :key="post.id"><div><b>{{ post.title }}</b><small>#{{ post.id }} · {{ formatTime(post.createdAt) }} · {{ post.imageCount }} 张图片</small></div><div><b>{{ post.authorName }}</b><small>{{ post.categoryName }}</small></div><div><span>♥ {{ post.likeCount }}</span><span>◌ {{ post.commentCount }}</span></div><div><span class="status-badge" :class="post.status.toLowerCase()">{{ post.status === 'PUBLISHED' ? '展示中' : '已隐藏' }}</span></div><div><RouterLink :to="`/posts/${post.id}`" target="_blank">查看</RouterLink><button :class="{ danger: post.status === 'PUBLISHED' }" @click="togglePost(post)">{{ post.status === 'PUBLISHED' ? '隐藏' : '恢复' }}</button></div></article></div></section>
      </template>

      <template v-else-if="activeSection === 'users'">
        <section class="admin-toolbar"><div class="admin-search"><span>⌕</span><input v-model="keyword" placeholder="搜索用户名或昵称" @keyup.enter="refreshUsers" /></div><select v-model="userStatus" @change="refreshUsers"><option value="">全部状态</option><option value="ACTIVE">正常</option><option value="BANNED">已封禁</option></select><button class="button button--primary button--small" @click="refreshUsers">筛选</button><span class="admin-total">共 {{ usersTotal }} 人</span></section>
        <section class="admin-table-card"><div class="admin-table admin-table--users"><header><span>用户</span><span>用户名</span><span>角色</span><span>加入时间</span><span>状态 / 操作</span></header><article v-for="user in users" :key="user.id"><div><span class="admin-user-avatar">{{ user.nickname.slice(0, 1) }}</span><b>{{ user.nickname }}</b></div><div><span>@{{ user.username }}</span></div><div><span>{{ user.role === 'ADMIN' ? '管理员' : '社区成员' }}</span></div><div><span>{{ formatTime(user.createdAt) }}</span></div><div><span class="status-badge" :class="user.status.toLowerCase()">{{ user.status === 'ACTIVE' ? '正常' : '已封禁' }}</span><button v-if="user.role !== 'ADMIN'" :class="{ danger: user.status === 'ACTIVE' }" @click="toggleUser(user)">{{ user.status === 'ACTIVE' ? '封禁' : '恢复' }}</button></div></article></div></section>
      </template>

      <template v-else>
        <section class="admin-toolbar"><select v-model="reportStatus" @change="refreshReports"><option value="">全部状态</option><option value="PENDING">待处理</option><option value="RESOLVED">已处理</option><option value="REJECTED">已驳回</option></select><span class="admin-total">共 {{ reportsTotal }} 条</span></section>
        <section class="admin-report-list"><article v-for="report in reports" :key="report.id"><div class="admin-report-list__icon">!</div><div class="admin-report-list__body"><div><span class="status-badge" :class="report.status.toLowerCase()">{{ report.status === 'PENDING' ? '待处理' : report.status === 'RESOLVED' ? '已处理' : '已驳回' }}</span><small>{{ formatTime(report.createdAt) }}</small></div><h3>{{ report.targetTitle || `${report.targetType} #${report.targetId}` }}</h3><p>{{ report.reason }}</p><span>举报人：{{ report.reporterName }}</span><em v-if="report.handleNote">处理备注：{{ report.handleNote }}</em></div><div v-if="report.status === 'PENDING'" class="admin-report-list__actions"><button class="button button--soft button--small" @click="resolveReport(report, 'REJECTED')">驳回</button><button class="button button--primary button--small" @click="resolveReport(report, 'RESOLVED')">标记已处理</button></div></article><div v-if="!reports.length" class="admin-empty">当前筛选条件下没有举报记录</div></section>
      </template>
    </main>
  </div>
</template>
