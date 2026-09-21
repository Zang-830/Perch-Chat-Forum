<script setup lang="ts">
import { RouterLink } from 'vue-router'
import type { Post } from '../types'
import { formatTime } from '../utils/format'
import AvatarBadge from './AvatarBadge.vue'
import { resolveAssetUrl } from '../api/client'

defineProps<{ post: Post }>()
defineEmits<{ like: [post: Post] }>()
</script>

<template>
  <article class="post-card surface-card">
    <div class="post-card__top">
      <AvatarBadge :name="post.author.nickname" :url="post.author.avatarUrl" />
      <div class="post-card__author">
        <strong>{{ post.author.nickname }}</strong>
        <span>@{{ post.author.username }} · {{ formatTime(post.createdAt) }}</span>
      </div>
      <span
        v-if="post.category"
        class="category-pill"
        :style="{ '--category-color': post.category.color }"
      >
        {{ post.category.name }}
      </span>
    </div>

    <RouterLink class="post-card__content" :to="`/posts/${post.id}`">
      <h2>{{ post.title }}</h2>
      <p>{{ post.summary }}</p>
    </RouterLink>

    <RouterLink
      v-if="post.imageUrls?.length"
      class="post-card__gallery"
      :class="`post-card__gallery--${Math.min(post.imageUrls.length, 3)}`"
      :to="`/posts/${post.id}`"
      :aria-label="`查看帖子中的 ${post.imageUrls.length} 张图片`"
    >
      <span v-for="(imageUrl, index) in post.imageUrls.slice(0, 3)" :key="imageUrl">
        <img :src="resolveAssetUrl(imageUrl)" alt="" loading="lazy" />
        <i v-if="index === 2 && post.imageUrls.length > 3">+{{ post.imageUrls.length - 3 }}</i>
      </span>
    </RouterLink>

    <div class="post-card__actions">
      <button class="action-button" :class="{ active: post.likedByMe }" type="button" @click="$emit('like', post)">
        <span aria-hidden="true">{{ post.likedByMe ? '♥' : '♡' }}</span>
        {{ post.likeCount }}
        <span class="sr-only">点赞</span>
      </button>
      <RouterLink class="action-button" :to="`/posts/${post.id}`">
        <span aria-hidden="true">◌</span> {{ post.commentCount }} <span class="sr-only">条评论</span>
      </RouterLink>
      <span class="post-card__read">阅读全文 <span aria-hidden="true">→</span></span>
    </div>
  </article>
</template>
