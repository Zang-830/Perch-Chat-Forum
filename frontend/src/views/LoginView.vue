<script setup lang="ts">
import { ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'

const auth = useAuthStore()
const router = useRouter()
const route = useRoute()
const username = ref('')
const password = ref('')
const loading = ref(false)
const error = ref('')

async function submit() {
  error.value = ''
  loading.value = true
  try {
    await auth.login(username.value, password.value)
    const redirect = typeof route.query.redirect === 'string' ? route.query.redirect : '/'
    await router.push(redirect)
  } catch (reason) {
    error.value = reason instanceof Error ? reason.message : '登录失败'
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="auth-page page-container">
    <section class="auth-story">
      <span class="eyebrow eyebrow--light"><i></i> WELCOME BACK</span>
      <h1>欢迎回来，<br />校园故事还在继续。</h1>
      <p>分享新鲜事、寻找同伴，也给每一个认真表达的人送上一份回应。</p>
      <div class="auth-story__quote"><span>“</span><p>在这里，我第一次发现原来隔壁学院也有人和我喜欢同一支乐队。</p></div>
    </section>
    <section class="auth-card surface-card">
      <div class="auth-card__heading"><span>登录社区</span><h2>继续你的校园旅程</h2><p>还没有账号？<RouterLink to="/register">立即加入</RouterLink></p></div>
      <form @submit.prevent="submit">
        <label>用户名<input v-model.trim="username" autocomplete="username" required placeholder="请输入用户名" /></label>
        <label>密码<input v-model="password" type="password" autocomplete="current-password" required placeholder="请输入密码" /></label>
        <p v-if="error" class="form-error">{{ error }}</p>
        <button class="button button--primary button--block" type="submit" :disabled="loading">
          {{ loading ? '正在登录…' : '登录' }}
        </button>
      </form>
      <p class="form-note">登录即表示你同意遵守社区公约，共同维护友善真实的交流环境。</p>
    </section>
  </div>
</template>

