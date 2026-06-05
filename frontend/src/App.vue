<template>
  <div :class="['app-shell', { 'auth-shell': isAuthRoute, 'hub-shell': isHubRoute }]">
    <aside v-if="!isAuthRoute && !isHubRoute" class="sidebar">
      <div class="brand">Campus<span>Hub</span></div>

      <template v-if="loggedIn">
        <RouterLink to="/requirements"><span class="nav-icon">⌘</span>需求大厅</RouterLink>
        <RouterLink to="/requirements/publish"><span class="nav-icon">➤</span>发布需求</RouterLink>
        <RouterLink to="/orders"><span class="nav-icon">▣</span>我的订单</RouterLink>
        <RouterLink to="/profile"><span class="nav-icon">♙</span>个人资料</RouterLink>
        <RouterLink to="/notifications"><span class="nav-icon">♢</span>消息通知</RouterLink>
        <RouterLink v-if="adminVisible" to="/admin">管理后台</RouterLink>
        <a class="sidebar-exit" href="#" @click.prevent="showLogoutConfirm = true"><span class="nav-icon">↪</span>退出</a>
      </template>
      <template v-else>
        <RouterLink to="/login">登录</RouterLink>
        <RouterLink to="/register">注册</RouterLink>
      </template>
    </aside>

    <main class="content">
      <RouterView />
    </main>

    <div
      v-if="showLogoutConfirm"
      class="modal-backdrop"
      role="presentation"
      @click.self="showLogoutConfirm = false"
    >
      <section class="confirm-modal" role="dialog" aria-modal="true" aria-labelledby="logout-title">
        <h2 id="logout-title">是否确定退出当前账号</h2>
        <p>退出后需要重新登录才能继续使用 CampusHub。</p>
        <div class="modal-actions">
          <button type="button" @click="showLogoutConfirm = false">取消</button>
          <button class="primary" type="button" @click="handleLogout">确定退出</button>
        </div>
      </section>
    </div>
  </div>
</template>

<script setup>
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { isLoggedIn, isAdmin, clearToken } from './utils/auth'

const router = useRouter()
const route = useRoute()
const showLogoutConfirm = ref(false)

const isAuthRoute = computed(() => route.path === '/login' || route.path === '/register')
const isHubRoute = computed(() => route.path === '/home')

const loggedIn = computed(() => {
  // localStorage itself is not reactive, so use route changes to refresh the menu after login/logout.
  route.fullPath
  return isLoggedIn()
})

const adminVisible = computed(() => {
  route.fullPath
  return isAdmin()
})

function handleLogout() {
  showLogoutConfirm.value = false
  clearToken()
  router.push('/login')
}
</script>
