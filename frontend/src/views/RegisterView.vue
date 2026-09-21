<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'

const auth = useAuthStore()
const router = useRouter()
const username = ref('')
const nickname = ref('')
const password = ref('')
const confirmPassword = ref('')
const loading = ref(false)
const error = ref('')

async function submit() {
  error.value = ''
  if (password.value !== confirmPassword.value) {
    error.value = '两次输入的密码不一致'
    return
  }
  loading.value = true
  try {
    await auth.register(username.value, nickname.value, password.value)
    await router.push('/')
  } catch (reason) {
    error.value = reason instanceof Error ? reason.message : '注册失败'
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="auth-page auth-page--register page-container">
    <section class="auth-story">
      <span class="eyebrow eyebrow--light"><i></i> JOIN THE CAMPUS</span>
      <h1>从一句你好，<br />开始连接整座校园。</h1>
      <p>创建你的社区身份，在安全、轻松的空间里分享观点、交换经验、认识伙伴。</p>
      <div class="auth-benefits"><span>✓ 真实校园话题</span><span>✓ 友善互动氛围</span><span>✓ 持续扩展服务</span></div>
    </section>
    <section class="auth-card surface-card">
      <div class="auth-card__heading"><span>创建账号</span><h2>加入栖语校园社区</h2><p>已经有账号？<RouterLink to="/login">直接登录</RouterLink></p></div>
      <form @submit.prevent="submit">
        <label>用户名<input v-model.trim="username" minlength="4" maxlength="20" pattern="[a-zA-Z0-9_]+" autocomplete="username" required placeholder="4-20 位字母、数字或下划线" /></label>
        <label>社区昵称<input v-model.trim="nickname" maxlength="20" required placeholder="大家会怎样称呼你" /></label>
        <label>密码<input v-model="password" type="password" minlength="8" maxlength="40" autocomplete="new-password" required placeholder="至少 8 位" /></label>
        <label>确认密码<input v-model="confirmPassword" type="password" autocomplete="new-password" required placeholder="再次输入密码" /></label>
        <p v-if="error" class="form-error">{{ error }}</p>
        <button class="button button--primary button--block" type="submit" :disabled="loading">
          {{ loading ? '正在创建…' : '创建账号' }}
        </button>
      </form>
    </section>
  </div>
</template>

