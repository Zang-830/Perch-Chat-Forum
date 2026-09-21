<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { communityApi } from '../api/community'
import { useAuthStore } from '../stores/auth'
import type { Category, Post } from '../types'
import PostCard from '../components/PostCard.vue'

const router = useRouter()
const auth = useAuthStore()
const categories = ref<Category[]>([])
const posts = ref<Post[]>([])
const total = ref(0)
const page = ref(1)
const loading = ref(true)
const error = ref('')
const keyword = ref('')
const selectedCategory = ref<number | undefined>()

async function loadPosts(append = false) {
  loading.value = true
  error.value = ''
  try {
    const result = await communityApi.posts({
      page: page.value,
      size: 8,
      keyword: keyword.value.trim(),
      categoryId: selectedCategory.value,
    })
    posts.value = append ? [...posts.value, ...result.items] : result.items
    total.value = result.total
  } catch (reason) {
    error.value = reason instanceof Error ? reason.message : '加载失败，请稍后重试'
  } finally {
    loading.value = false
  }
}

async function loadInitial() {
  try {
    categories.value = await communityApi.categories()
  } catch (reason) {
    error.value = reason instanceof Error ? reason.message : '分类加载失败'
  }
  await loadPosts()
}

function selectCategory(id?: number) {
  selectedCategory.value = id
  page.value = 1
  loadPosts()
}

function search() {
  page.value = 1
  loadPosts()
}

async function loadMore() {
  page.value += 1
  await loadPosts(true)
}

async function toggleLike(post: Post) {
  if (!auth.isAuthenticated) {
    router.push({ name: 'login', query: { redirect: router.currentRoute.value.fullPath } })
    return
  }
  try {
    const result = await communityApi.toggleLike(post.id)
    post.likedByMe = result.liked
    post.likeCount = result.likeCount
  } catch (reason) {
    error.value = reason instanceof Error ? reason.message : '操作失败'
  }
}

onMounted(loadInitial)
</script>

<template>
  <div class="home-page">
    <section class="hero page-container">
      <div class="hero__copy">
        <span class="eyebrow"><i></i> 你的校园，此刻正在发生</span>
        <h1>分享日常，发现同频的<br /><em>校园故事。</em></h1>
        <p>从学习互助到活动组队，在友善、真实的社区里，让每一次表达都遇见回应。</p>
        <form class="hero-search" role="search" @submit.prevent="search">
          <span aria-hidden="true">⌕</span>
          <input v-model="keyword" aria-label="搜索帖子" placeholder="搜索课程、活动或校园话题…" />
          <button class="button button--dark" type="submit">搜索</button>
        </form>
      </div>
      <div class="hero__visual" aria-hidden="true">
        <div class="orbit orbit--one"></div>
        <div class="orbit orbit--two"></div>
        <div class="floating-note floating-note--one"><span>📚</span><b>期末复习搭子</b><small>12 人正在讨论</small></div>
        <div class="floating-note floating-note--two"><span>🏸</span><b>周末羽毛球</b><small>还差 2 位同学</small></div>
        <div class="hero__center"><span>Hi!</span><strong>今天也在<br />认真生活</strong></div>
      </div>
    </section>

    <section class="community-layout page-container">
      <aside class="category-panel surface-card">
        <div class="section-label">浏览频道</div>
        <button :class="{ active: selectedCategory === undefined }" @click="selectCategory()">
          <span class="category-icon">✦</span><span><b>全部动态</b><small>看看校园新鲜事</small></span>
        </button>
        <button
          v-for="category in categories"
          :key="category.id"
          :class="{ active: selectedCategory === category.id }"
          @click="selectCategory(category.id)"
        >
          <span class="category-icon" :style="{ background: `${category.color}18`, color: category.color }">●</span>
          <span><b>{{ category.name }}</b><small>{{ category.description }}</small></span>
        </button>
        <div class="category-panel__tip">
          <span>◈</span>
          <div><b>友善社区公约</b><p>真诚表达，尊重差异，一起维护舒适的交流空间。</p></div>
        </div>
      </aside>

      <div class="feed-column">
        <div class="feed-heading">
          <div>
            <span class="section-label">COMMUNITY FEED</span>
            <h2>{{ selectedCategory ? '频道动态' : '社区广场' }}</h2>
          </div>
          <RouterLink class="button button--soft" to="/publish">写下此刻</RouterLink>
        </div>

        <div v-if="loading && posts.length === 0" class="skeleton-stack" aria-label="正在加载">
          <div v-for="item in 3" :key="item" class="skeleton-card surface-card"><i></i><span></span><span></span></div>
        </div>
        <div v-else-if="error && posts.length === 0" class="state-card surface-card">
          <span>暂时走神了</span><h3>{{ error }}</h3><button class="button button--primary" @click="loadPosts()">重新加载</button>
        </div>
        <div v-else-if="posts.length === 0" class="state-card surface-card">
          <span>这里还很安静</span><h3>成为第一个发帖的人吧</h3><RouterLink class="button button--primary" to="/publish">发布第一条动态</RouterLink>
        </div>
        <TransitionGroup v-else name="list" tag="div" class="post-list">
          <PostCard v-for="post in posts" :key="post.id" :post="post" @like="toggleLike" />
        </TransitionGroup>
        <button v-if="posts.length < total" class="load-more" :disabled="loading" @click="loadMore">
          {{ loading ? '正在加载…' : '加载更多' }}
        </button>
      </div>

      <aside class="right-rail">
        <div class="rail-card surface-card">
          <div class="rail-card__title"><span>校园热议</span><small>本周</small></div>
          <ol class="trend-list">
            <li><i>01</i><div><b>新学期选课经验</b><small>学习交流 · 热度上升</small></div></li>
            <li><i>02</i><div><b>社团招新情报站</b><small>校园生活 · 128 讨论</small></div></li>
            <li><i>03</i><div><b>食堂隐藏菜单</b><small>校园生活 · 96 讨论</small></div></li>
          </ol>
        </div>

        <div id="future-modules" class="future-card">
          <span class="eyebrow eyebrow--light"><i></i> NEXT MODULE</span>
          <h3>二手市场<br />即将接入</h3>
          <p>统一账号与信用体系，让校园闲置流转更简单。</p>
          <div class="future-card__tags"><span>闲置发布</span><span>校内面交</span><span>收藏沟通</span></div>
        </div>
      </aside>
    </section>
  </div>
</template>

