<script setup lang="ts">
import { useAuthStore } from '../stores/auth'
import AvatarBadge from '../components/AvatarBadge.vue'

const auth = useAuthStore()
</script>

<template>
  <div v-if="auth.user" class="profile-page page-container">
    <section class="profile-hero">
      <AvatarBadge :name="auth.user.nickname" :url="auth.user.avatarUrl" size="lg" />
      <div><span class="eyebrow eyebrow--light"><i></i> COMMUNITY MEMBER</span><h1>{{ auth.user.nickname }}</h1><p>@{{ auth.user.username }} · {{ auth.user.bio || '在校园里认真生活，也认真分享。' }}</p></div>
    </section>
    <section class="profile-grid">
      <div class="surface-card profile-card"><span class="section-label">账号信息</span><dl><div><dt>用户名</dt><dd>{{ auth.user.username }}</dd></div><div><dt>社区昵称</dt><dd>{{ auth.user.nickname }}</dd></div><div><dt>身份</dt><dd>{{ auth.user.role === 'ADMIN' ? '管理员' : '社区成员' }}</dd></div></dl></div>
      <div class="surface-card profile-card"><span class="section-label">我的空间</span><h2>更多个人能力正在路上</h2><p>后续将逐步加入我的帖子、收藏、消息通知和二手交易记录。</p><RouterLink class="button button--primary" to="/publish">发布新动态</RouterLink></div>
    </section>
  </div>
</template>

