<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { communityApi } from '../api/community'
import { resolveAssetUrl } from '../api/client'
import { useAuthStore } from '../stores/auth'
import type { Comment, Post } from '../types'
import { formatTime } from '../utils/format'
import { markdownToHtml } from '../utils/markdown'
import AvatarBadge from '../components/AvatarBadge.vue'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const post = ref<Post>()
const comments = ref<Comment[]>([])
const commentText = ref('')
const loading = ref(true)
const submitting = ref(false)
const error = ref('')
const postId = Number(route.params.id)
const renderedContent = computed(() => markdownToHtml(post.value?.content ?? ''))

async function load() {
  loading.value = true
  try {
    const [postResult, commentResult] = await Promise.all([
      communityApi.post(postId),
      communityApi.comments(postId),
    ])
    post.value = postResult
    comments.value = commentResult
  } catch (reason) {
    error.value = reason instanceof Error ? reason.message : '内容加载失败'
  } finally {
    loading.value = false
  }
}

async function like() {
  if (!auth.isAuthenticated) {
    await router.push({ name: 'login', query: { redirect: route.fullPath } })
    return
  }
  if (!post.value) return
  try {
    const result = await communityApi.toggleLike(post.value.id)
    post.value.likedByMe = result.liked
    post.value.likeCount = result.likeCount
  } catch (reason) {
    error.value = reason instanceof Error ? reason.message : '点赞失败'
  }
}

async function submitComment() {
  if (!auth.isAuthenticated) {
    await router.push({ name: 'login', query: { redirect: route.fullPath } })
    return
  }
  if (!commentText.value.trim() || !post.value) return
  submitting.value = true
  try {
    const created = await communityApi.createComment(post.value.id, commentText.value.trim())
    comments.value.push(created)
    post.value.commentCount += 1
    commentText.value = ''
  } catch (reason) {
    error.value = reason instanceof Error ? reason.message : '评论失败'
  } finally {
    submitting.value = false
  }
}

async function reportPost() {
  if (!auth.isAuthenticated) {
    await router.push({ name: 'login', query: { redirect: route.fullPath } })
    return
  }
  if (!post.value) return
  const reason = window.prompt('请简要说明举报原因（最多 500 字）')?.trim()
  if (!reason) return
  try {
    await communityApi.reportPost(post.value.id, reason.slice(0, 500))
    window.alert('举报已提交，管理员会尽快处理。')
  } catch (reason) {
    error.value = reason instanceof Error ? reason.message : '举报提交失败'
  }
}

onMounted(load)
</script>

<template>
  <div class="detail-page page-container">
    <RouterLink class="back-link" to="/">← 返回交流广场</RouterLink>
    <div v-if="loading" class="state-card surface-card"><span>正在打开帖子…</span></div>
    <div v-else-if="error || !post" class="state-card surface-card"><h3>{{ error || '帖子不存在' }}</h3><RouterLink class="button button--primary" to="/">回到首页</RouterLink></div>
    <template v-else>
      <article class="article-card surface-card">
        <div class="article-meta">
          <AvatarBadge :name="post.author.nickname" :url="post.author.avatarUrl" size="lg" />
          <div><b>{{ post.author.nickname }}</b><span>@{{ post.author.username }} · {{ formatTime(post.createdAt) }}</span></div>
          <span v-if="post.category" class="category-pill" :style="{ '--category-color': post.category.color }">{{ post.category.name }}</span>
        </div>
        <h1>{{ post.title }}</h1>
        <div v-if="post.imageUrls?.length" class="article-gallery" :class="`article-gallery--${Math.min(post.imageUrls.length, 3)}`">
          <a v-for="imageUrl in post.imageUrls" :key="imageUrl" :href="resolveAssetUrl(imageUrl)" target="_blank" rel="noreferrer">
            <img :src="resolveAssetUrl(imageUrl)" alt="帖子配图" loading="lazy" />
          </a>
        </div>
        <div class="article-content markdown-body" v-html="renderedContent"></div>
        <div class="article-actions">
          <button class="button" :class="post.likedByMe ? 'button--liked' : 'button--soft'" @click="like">{{ post.likedByMe ? '♥ 已点赞' : '♡ 点赞' }} · {{ post.likeCount }}</button>
          <span>{{ post.commentCount }} 条回应</span>
          <button class="article-report" type="button" @click="reportPost">举报</button>
        </div>
      </article>

      <section class="comments-card surface-card">
        <div class="comments-heading"><div><span class="section-label">CONVERSATION</span><h2>继续交流</h2></div><span>{{ comments.length }} 条评论</span></div>
        <form class="comment-form" @submit.prevent="submitComment">
          <AvatarBadge :name="auth.user?.nickname || '访客'" :url="auth.user?.avatarUrl" />
          <div><textarea v-model="commentText" maxlength="1000" rows="4" :placeholder="auth.isAuthenticated ? '友善地写下你的回应…' : '登录后参与讨论'" :disabled="!auth.isAuthenticated"></textarea><button class="button button--primary button--small" :disabled="submitting || !commentText.trim()">{{ submitting ? '发送中…' : '发表评论' }}</button></div>
        </form>
        <div v-if="comments.length" class="comment-list">
          <article v-for="comment in comments" :key="comment.id">
            <AvatarBadge :name="comment.author.nickname" :url="comment.author.avatarUrl" />
            <div><header><b>{{ comment.author.nickname }}</b><span>{{ formatTime(comment.createdAt) }}</span></header><p>{{ comment.content }}</p></div>
          </article>
        </div>
        <div v-else class="comments-empty">还没有评论，留下第一条友善回应吧。</div>
      </section>
    </template>
  </div>
</template>
