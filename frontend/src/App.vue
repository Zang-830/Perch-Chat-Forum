<script setup lang="ts">
import { computed } from 'vue'
import { RouterLink, RouterView, useRoute, useRouter } from 'vue-router'
import { useAuthStore } from './stores/auth'
import AvatarBadge from './components/AvatarBadge.vue'

const auth = useAuthStore()
const router = useRouter()
const route = useRoute()
const isAdminRoute = computed(() => route.name === 'admin')

function logout() {
  auth.logout()
  router.push('/')
}
</script>

<template>
  <div class="app-shell">
    <header v-if="!isAdminRoute" class="topbar">
      <div class="topbar__inner">
        <RouterLink class="brand" to="/" aria-label="栖语校园社区首页">
          <span class="brand__mark" aria-hidden="true">
            <span></span><span></span><span></span>
          </span>
          <span>栖语</span>
          <small>校园社区</small>
        </RouterLink>

        <nav class="main-nav" aria-label="主导航">
          <RouterLink to="/">社区广场</RouterLink>
          <a href="#future-modules" title="即将上线">校园服务</a>
        </nav>

        <div class="topbar__actions">
          <RouterLink v-if="auth.isAuthenticated" class="button button--primary button--small" to="/publish">
            <span aria-hidden="true">＋</span> 发布
          </RouterLink>
          <template v-if="auth.isAuthenticated && auth.user">
            <RouterLink v-if="auth.user.role === 'ADMIN'" class="admin-entry" to="/admin">管理后台</RouterLink>
            <RouterLink class="user-chip" to="/profile">
              <AvatarBadge :name="auth.user.nickname" :url="auth.user.avatarUrl" size="sm" />
              <span>{{ auth.user.nickname }}</span>
            </RouterLink>
            <button class="text-button" type="button" @click="logout">退出</button>
          </template>
          <template v-else>
            <RouterLink class="text-button" to="/login">登录</RouterLink>
            <RouterLink class="button button--primary button--small" to="/register">加入社区</RouterLink>
          </template>
        </div>
      </div>
    </header>

    <main>
      <RouterView v-slot="{ Component }">
        <Transition name="page" mode="out-in">
          <component :is="Component" />
        </Transition>
      </RouterView>
    </main>

    <footer v-if="!isAdminRoute" class="site-footer">
      <span>栖语校园社区 · 让每一种校园声音都有回应</span>
      <span>© 2026 Campus Community</span>
    </footer>
  </div>
</template>
