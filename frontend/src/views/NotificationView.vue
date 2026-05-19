<template>
  <section>
    <header class="page-header">
      <div>
        <h1>消息通知</h1>
        <p>{{ unreadCount }} 条未读通知</p>
      </div>
      <div class="header-actions">
        <label class="inline-check">
          <input v-model="unreadOnly" type="checkbox" @change="loadNotifications" />
          只看未读
        </label>
        <button class="primary" @click="readAll">全部已读</button>
        <button @click="clearRead">清除已读</button>
      </div>
    </header>

    <p v-if="message" class="message">{{ message }}</p>

    <div class="list">
      <article
        v-for="item in notifications"
        :key="item.notificationId"
        :class="['notification-row', { unread: !item.read, expanded: expandedIds.has(item.notificationId) }]"
        tabindex="0"
        @click="toggleNotification(item.notificationId)"
        @keydown.enter.prevent="toggleNotification(item.notificationId)"
        @keydown.space.prevent="toggleNotification(item.notificationId)"
      >
        <div class="notification-main">
          <h2>{{ item.title }}</h2>
          <p>{{ item.content }}</p>
        </div>
        <small class="notification-meta">{{ item.eventType }} · {{ formatTime(item.createdAt) }}</small>
        <button
          v-if="!item.read"
          class="notification-action"
          @click.stop="readOne(item.notificationId)"
        >
          标为已读
        </button>
        <div class="notification-detail">
          <p>{{ item.content }}</p>
        </div>
      </article>
    </div>
  </section>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import {
  clearReadNotifications,
  getUnreadCount,
  listNotifications,
  markAllNotificationsRead,
  markNotificationRead
} from '../api/notifications'

const notifications = ref([])
const unreadCount = ref(0)
const unreadOnly = ref(false)
const message = ref('')
const expandedIds = ref(new Set())

function formatTime(value) {
  return value ? value.replace('T', ' ').slice(0, 16) : ''
}

async function refreshUnreadCount() {
  const data = await getUnreadCount()
  unreadCount.value = data.unreadCount
}

async function loadNotifications() {
  message.value = ''
  try {
    notifications.value = await listNotifications(unreadOnly.value)
    const visibleIds = new Set(notifications.value.map((item) => item.notificationId))
    expandedIds.value = new Set([...expandedIds.value].filter((id) => visibleIds.has(id)))
    await refreshUnreadCount()
    if (notifications.value.length === 0) {
      message.value = '暂无通知'
    }
  } catch (error) {
    message.value = error.message
  }
}

function toggleNotification(notificationId) {
  const next = new Set(expandedIds.value)
  if (next.has(notificationId)) {
    next.delete(notificationId)
  } else {
    next.add(notificationId)
  }
  expandedIds.value = next
}

async function readOne(notificationId) {
  await markNotificationRead(notificationId)
  await loadNotifications()
}

async function readAll() {
  await markAllNotificationsRead()
  await loadNotifications()
}

async function clearRead() {
  await clearReadNotifications()
  await loadNotifications()
}

onMounted(loadNotifications)
</script>
