import { createRouter, createWebHashHistory, createWebHistory } from 'vue-router'
import HomeView from '../views/HomeView.vue'

const router = createRouter({
  history: import.meta.env.VITE_ROUTER_MODE === 'hash'
    ? createWebHashHistory(import.meta.env.BASE_URL)
    : createWebHistory(import.meta.env.BASE_URL),
  routes: [
    { path: '/', name: 'home', component: HomeView },
    { path: '/login', name: 'login', component: () => import('../views/LoginView.vue'), meta: { guest: true } },
    { path: '/register', name: 'register', component: () => import('../views/RegisterView.vue'), meta: { guest: true } },
    { path: '/publish', name: 'publish', component: () => import('../views/PublishView.vue'), meta: { requiresAuth: true } },
    { path: '/posts/:id', name: 'post-detail', component: () => import('../views/PostDetailView.vue') },
    { path: '/profile', name: 'profile', component: () => import('../views/ProfileView.vue'), meta: { requiresAuth: true } },
    { path: '/admin', name: 'admin', component: () => import('../views/AdminDashboardView.vue'), meta: { requiresAuth: true, requiresAdmin: true } },
    { path: '/:pathMatch(.*)*', redirect: '/' },
  ],
  scrollBehavior: () => ({ top: 0, behavior: 'smooth' }),
})

router.beforeEach((to) => {
  const authenticated = Boolean(localStorage.getItem('campus_token'))
  let role = ''
  try {
    role = JSON.parse(localStorage.getItem('campus_user') || '{}').role || ''
  } catch {
    role = ''
  }
  if (to.meta.requiresAuth && !authenticated) {
    return { name: 'login', query: { redirect: to.fullPath } }
  }
  if (to.meta.guest && authenticated) {
    return { name: 'home' }
  }
  if (to.meta.requiresAdmin && role !== 'ADMIN') {
    return { name: 'home' }
  }
})

export default router
