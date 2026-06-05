<template>
  <section class="notify-page">
    <article v-if="mode === 'list'" class="campus-card notify-shell">
      <header class="notify-header">
        <div>
          <h1>消息通知</h1>
          <nav class="notify-tabs" aria-label="消息分类">
            <button
              v-for="tab in tabs"
              :key="tab.key"
              type="button"
              :class="{ active: activeTab === tab.key }"
              @click="activeTab = tab.key"
            >
              {{ tab.label }}
            </button>
          </nav>
        </div>
        <span class="notify-avatar" aria-hidden="true">👨🏻</span>
      </header>

      <div class="notify-toolbar">
        <label class="notify-search">
          <span aria-hidden="true">⌕</span>
          <input v-model.trim="keyword" type="search" :placeholder="searchPlaceholder" />
        </label>
        <button type="button" @click="readAll">全部已读</button>
        <button type="button" @click="clearRead">清除已读</button>
      </div>

      <p v-if="message" class="message">{{ message }}</p>

      <div class="notify-content">
        <div class="notify-list">
          <article
            v-for="item in filteredItems"
            :key="item.id"
            :class="['notify-item', item.category, { unread: !item.read }]"
            tabindex="0"
            @click="openItem(item)"
            @keydown.enter.prevent="openItem(item)"
            @keydown.space.prevent="openItem(item)"
          >
            <span class="notify-icon" aria-hidden="true">{{ item.icon }}</span>
            <div class="notify-item-main">
              <div class="notify-title-row">
                <h2>{{ item.title }}</h2>
              </div>
              <p>{{ item.preview }}</p>
              <div v-if="item.category === 'order'" class="notify-badges">
                <span v-if="item.orderId">订单号：{{ item.orderId }}</span>
                <span>{{ eventLabel(item.eventType) }}</span>
              </div>
            </div>
            <div class="notify-item-meta">
              <time>{{ formatTime(item.createdAt) }}</time>
              <span v-if="!item.read" class="unread-dot" aria-label="未读"></span>
            </div>
          </article>
        </div>

      </div>
    </article>

    <article v-else-if="selectedItem" class="campus-card notify-detail-card">
      <header class="notify-detail-header">
        <button class="back-button" type="button" aria-label="返回消息列表" @click="backToList">‹</button>
        <h1>{{ selectedItem.category === 'order' ? '订单详情' : '通知详情' }}</h1>
        <span class="notify-avatar" aria-hidden="true">👨🏻</span>
      </header>

      <section class="notify-detail-body">
        <div class="notify-detail-title">
          <span :class="['notify-icon', selectedItem.category]" aria-hidden="true">{{ selectedItem.icon }}</span>
          <div>
            <h2>{{ selectedItem.title }}</h2>
            <p>{{ formatTime(selectedItem.createdAt) }} · {{ sourceLabel(selectedItem.category) }}</p>
          </div>
        </div>

        <template v-if="selectedItem.category === 'order'">
          <div class="order-notify-summary">
            <h3>{{ selectedItem.orderTitle }}</h3>
            <span :class="['status-pill', selectedItem.orderStatusClass]">{{ selectedItem.orderStatus }}</span>
            <p v-if="selectedItem.orderId">订单号：{{ selectedItem.orderId }}</p>
          </div>
          <p class="notify-detail-text">{{ displayContent(selectedItem.content) }}</p>
          <button class="primary notify-detail-action" type="button" @click="goOrderDetail(selectedItem)">
            {{ selectedItem.orderId ? '查看订单详情' : '查看相关详情' }}
          </button>
        </template>

        <template v-else>
          <p class="notify-detail-text">{{ displayContent(selectedItem.content) }}</p>
          <button class="primary notify-detail-action" type="button" @click="$router.push('/requirements')">
            去需求大厅体验
          </button>
        </template>
      </section>
    </article>
  </section>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import {
  clearReadNotifications,
  getUnreadCount,
  listNotifications,
  markAllNotificationsRead,
  markNotificationRead
} from '../api/notifications'
import { listOrders } from '../api/orders'

const router = useRouter()
const notifications = ref([])
const unreadCount = ref(0)
const activeTab = ref('all')
const keyword = ref('')
const message = ref('')
const mode = ref('list')
const selectedItem = ref(null)

const tabs = [
  { key: 'all', label: '全部' },
  { key: 'system', label: '系统通知' },
  { key: 'order', label: '订单消息' }
]

const mappedNotifications = computed(() => notifications.value.map(mapNotification))
const filteredItems = computed(() => {
  const word = keyword.value.toLowerCase()
  return mappedNotifications.value.filter((item) => {
    const tabMatched = activeTab.value === 'all' || item.category === activeTab.value
    const wordMatched = !word || `${item.title} ${item.preview} ${item.content}`.toLowerCase().includes(word)
    return tabMatched && wordMatched
  })
})
const searchPlaceholder = computed(() => activeTab.value === 'order' ? '搜索订单或消息' : '搜索标题或概述')

function mapNotification(item) {
  const category = item.eventType?.startsWith('ORDER') || item.eventType?.startsWith('EVALUATION') ? 'order' : 'system'
  const orderId = item.content?.match(/订单(?:号)?\s*(\d+)/)?.[1] || ''
  const reqId = item.content?.match(/需求\s*(\d+)/)?.[1] || ''
  return {
    ...item,
    id: item.notificationId,
    category,
    icon: category === 'order' ? '▧' : '☊',
    preview: displayContent(item.content),
    orderId,
    reqId,
    orderTitle: item.title?.includes('评价') ? '订单评价' : item.title,
    orderStatus: statusText(item.content),
    orderStatusClass: statusClass(item.content)
  }
}

function statusText(content = '') {
  const normalized = normalizeStatusContent(content)
  if (normalized.includes('已完成')) return '已完成'
  if (normalized.includes('待确认')) return '待确认'
  if (normalized.includes('进行中')) return '进行中'
  if (normalized.includes('已接单')) return '已接单'
  if (normalized.includes('已取消')) return '已取消'
  if (normalized.includes('待接单')) return '待接单'
  if (content.includes('COMPLETED') || content.includes('完成')) return '已完成'
  if (content.includes('TO_CONFIRM')) return '待确认'
  if (content.includes('IN_PROGRESS')) return '进行中'
  if (content.includes('ACCEPTED') || content.includes('接单')) return '已接取'
  if (content.includes('CANCELED') || content.includes('取消') || content.includes('下架')) return '已取消'
  if (content.includes('PENDING') || content.includes('待')) return '待处理'
  return '待查看'
}

function normalizeStatusContent(content = '') {
  return content
    .replaceAll('TO_CONFIRM', '待确认')
    .replaceAll('IN_PROGRESS', '进行中')
    .replaceAll('COMPLETED', '已完成')
    .replaceAll('ACCEPTED', '已接单')
    .replaceAll('CANCELED', '已取消')
    .replaceAll('PENDING', '待接单')
}

function displayContent(content = '') {
  return normalizeStatusContent(content)
}

function statusClass(content = '') {
  if (content.includes('COMPLETED') || content.includes('完成')) return 'done'
  if (content.includes('CANCELED') || content.includes('取消') || content.includes('下架')) return 'cancel'
  return 'active'
}

function eventLabel(eventType) {
  return {
    ORDER_ACCEPTED: '订单已接取',
    ORDER_STATUS_CHANGED: '状态更新',
    EVALUATION_SUBMITTED: '收到评价',
    REQUIREMENT_PUBLISHED: '需求发布',
    ADMIN_REQUIREMENT_CANCELED: '管理员通知'
  }[eventType] || '系统消息'
}

function sourceLabel(category) {
  return category === 'order' ? '订单中心' : 'CampusHub 运营团队'
}

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
    notifications.value = await listNotifications(false)
    await refreshUnreadCount()
  } catch (error) {
    message.value = error.message
  }
}

async function openItem(item) {
  selectedItem.value = item
  mode.value = 'detail'
  if (!item.read) {
    await markNotificationRead(item.notificationId)
    item.read = true
  }
  await refreshUnreadCount()
}

function backToList() {
  mode.value = 'list'
  selectedItem.value = null
}

async function readAll() {
  await markAllNotificationsRead()
  await loadNotifications()
}

async function clearRead() {
  await clearReadNotifications()
  await loadNotifications()
}

async function goOrderDetail(item) {
  if (item.orderId) {
    router.push(`/orders/${item.orderId}`)
    return
  }
  if (!item.reqId) {
    router.push('/orders')
    return
  }
  try {
    const [received, published] = await Promise.all([
      listOrders({ tab: 'received', page: 1, pageSize: 50 }),
      listOrders({ tab: 'published', page: 1, pageSize: 50 })
    ])
    const related = [...(received.list || []), ...(published.list || [])]
      .find((order) => String(order.reqId) === String(item.reqId) && order.orderId)
    if (related?.orderId) {
      router.push(`/orders/${related.orderId}`)
      return
    }
  } catch (error) {
    message.value = error.message
  }
  router.push(`/requirements/${item.reqId}`)
}

onMounted(loadNotifications)
</script>
