<template>
  <section class="function-hall">
    <div class="hall-brand">Campus<span>Hub</span></div>
    <svg class="hall-path" viewBox="0 0 1000 620" aria-hidden="true">
      <path d="M245 405 C310 482 400 430 400 342 C495 278 565 250 650 303 C720 342 735 270 795 235" />
      <circle cx="400" cy="342" r="8" />
      <circle cx="560" cy="276" r="8" />
      <circle cx="690" cy="315" r="8" />
    </svg>

    <button
      v-for="item in actions"
      :key="item.path"
      :class="['hall-action', item.className, { leaving: transition.activePath === item.path }]"
      type="button"
      :style="{ '--accent': item.color }"
      @click="goWithTransition(item, $event)"
    >
      <span class="hall-action-icon">{{ item.icon }}</span>
      <strong>{{ item.label }}</strong>
    </button>

    <button class="hall-logout" type="button" @click="handleLogout">
      <span>↪</span>
      退出
    </button>

    <div
      v-if="logoutConfirm"
      class="modal-backdrop"
      role="presentation"
      @click.self="logoutConfirm = false"
    >
      <section class="confirm-modal" role="dialog" aria-modal="true" aria-labelledby="hall-logout-title">
        <h2 id="hall-logout-title">是否确定退出当前账号</h2>
        <p>退出后需要重新登录才能继续使用 CampusHub。</p>
        <div class="modal-actions">
          <button type="button" @click="logoutConfirm = false">取消</button>
          <button class="primary" type="button" @click="confirmLogout">确定退出</button>
        </div>
      </section>
    </div>

    <div
      v-if="transition.visible"
      class="hall-transition"
      :style="{
        '--x': `${transition.x}px`,
        '--y': `${transition.y}px`,
        '--color': transition.color
      }"
      aria-hidden="true"
    >
      <span class="hall-transition-ripple"></span>
      <span class="hall-transition-wash"></span>
    </div>
  </section>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { clearToken } from '../utils/auth'

const router = useRouter()
const logoutConfirm = ref(false)

const actions = [
  { label: '发布需求', path: '/requirements/publish', icon: '➤', color: '#f59e0b', className: 'publish' },
  { label: '需求大厅', path: '/requirements', icon: '🤝', color: '#22c55e', className: 'requirements' },
  { label: '我的订单', path: '/orders', icon: '▣', color: '#3b82f6', className: 'orders' },
  { label: '个人资料', path: '/profile', icon: '♙', color: '#22b8c7', className: 'profile' },
  { label: '消息通知', path: '/notifications', icon: '♢', color: '#8b5cf6', className: 'notifications' }
]

const transition = reactive({
  visible: false,
  activePath: '',
  x: 0,
  y: 0,
  color: '#22c55e'
})

function goWithTransition(item, event) {
  if (transition.visible) return
  const rect = event.currentTarget.getBoundingClientRect()
  transition.x = rect.left + rect.width / 2
  transition.y = rect.top + rect.height / 2
  transition.color = item.color
  transition.activePath = item.path
  transition.visible = true
  window.setTimeout(() => {
    router.push(item.path)
    window.setTimeout(() => {
      transition.visible = false
      transition.activePath = ''
    }, 180)
  }, 620)
}

function handleLogout() {
  logoutConfirm.value = true
}

function confirmLogout() {
  clearToken()
  router.push('/login')
}
</script>
