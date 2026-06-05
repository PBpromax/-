<template>
  <section class="campus-page orders-page">
    <header class="campus-page-header order-head">
      <h1>我的订单</h1>
    </header>

    <div class="order-tabs">
      <button :class="{ active: tab === 'received' }" @click="switchTab('received')">我接取的</button>
      <button :class="{ active: tab === 'published' }" @click="switchTab('published')">我发布的</button>
    </div>

    <section class="glass-panel order-board">
      <div class="order-filter-row">
        <div class="order-status-tabs">
          <button :class="{ active: statusFilter === '' }" @click="statusFilter = ''">全部</button>
          <button :class="{ active: statusFilter === 'PENDING' }" @click="statusFilter = 'PENDING'">待接单</button>
          <button :class="{ active: statusFilter === 'IN_PROGRESS' }" @click="statusFilter = 'IN_PROGRESS'">进行中</button>
          <button :class="{ active: statusFilter === 'COMPLETED' }" @click="statusFilter = 'COMPLETED'">已完成</button>
        </div>
        <input v-model="keyword" placeholder="搜索订单标题、需求内容、发布者" />
      </div>

      <p v-if="message" class="message">{{ message }}</p>

      <div v-if="filteredOrders.length === 0" class="order-empty">
        <div class="empty-illustration">▤</div>
        <p>暂无相关订单</p>
      </div>

      <RouterLink
        v-for="item in filteredOrders"
        :key="item.reqId + (item.orderId || '')"
        class="order-card"
        :to="item.orderId ? `/orders/${item.orderId}` : `/requirements/${item.reqId}`"
      >
        <span class="type-orb">{{ iconFor(item.reqTitle) }}</span>
        <div class="order-card-main">
          <h2>{{ item.reqTitle }}</h2>
          <p>{{ item.description || (tab === 'received' ? '查看需求详情并完成订单' : '管理我发布的需求订单') }}</p>
          <small>
            <template v-if="tab === 'received'">发布者：{{ item.publisherName || '同学' }}</template>
            <template v-else>接单者：{{ item.receiverName || '暂无接单' }}</template>
            · 订单号：{{ item.orderId || item.reqId }} · 发布时间：{{ formatTime(item.createdAt) }}
          </small>
        </div>
        <div class="order-card-actions">
          <strong v-if="tab === 'published'">￥ {{ money(item.amount) }}</strong>
          <span :class="['status', statusClass(item.status)]">{{ statusLabel(item.status) }}</span>
          <div class="order-buttons">
            <button v-if="tab === 'published' && isRunning(item.status)" type="button" @click.prevent>联系接取者</button>
            <button v-if="tab === 'received' && isRunning(item.status)" type="button" @click.prevent>联系发布者</button>
            <button v-if="isRunning(item.status)" class="campus-green-btn" type="button" @click.prevent>{{ tab === 'received' ? '完成订单' : '确认完成' }}</button>
            <button v-if="item.status === 'COMPLETED'" type="button" @click.prevent>查看评价</button>
            <button class="more-btn" type="button" @click.prevent>•••</button>
          </div>
        </div>
      </RouterLink>

      <div v-if="filteredOrders.length > 0" class="order-bottom">没有更多订单了</div>
    </section>
  </section>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { listOrders } from '../api/orders'

const tab = ref('received')
const orders = ref([])
const total = ref(0)
const page = ref(1)
const pageSize = ref(20)
const message = ref('')
const keyword = ref('')
const statusFilter = ref('')

const filteredOrders = computed(() => {
  return orders.value.filter((item) => {
    const text = `${item.reqTitle || ''} ${item.description || ''} ${item.publisherName || ''} ${item.receiverName || ''}`
    const matchesKeyword = !keyword.value || text.toLowerCase().includes(keyword.value.toLowerCase())
    const normalized = normalizeStatus(item.status)
    const matchesStatus = !statusFilter.value || normalized === statusFilter.value
    return matchesKeyword && matchesStatus
  })
})

function formatTime(value) {
  return value ? value.replace('T', ' ').slice(0, 16) : ''
}

function money(value) {
  return Number(value || 0).toFixed(2)
}

function normalizeStatus(s) {
  if (s === 'ACCEPTED' || s === 'TO_CONFIRM') return 'IN_PROGRESS'
  return s
}

function statusClass(s) {
  return normalizeStatus(s).toLowerCase()
}

function statusLabel(s) {
  const labels = {
    PENDING: '待接单',
    IN_PROGRESS: '进行中',
    TO_CONFIRM: '待确认',
    COMPLETED: '已完成',
    CANCELED: '已取消',
    ACCEPTED: '进行中'
  }
  return labels[s] || s
}

function isRunning(s) {
  return ['ACCEPTED', 'IN_PROGRESS', 'TO_CONFIRM'].includes(s)
}

function iconFor(title = '') {
  if (title.includes('快递')) return '🛵'
  if (title.includes('资料') || title.includes('打印')) return '🏃'
  if (title.includes('答疑') || title.includes('数学')) return '📖'
  if (title.includes('二手')) return '📁'
  return '▣'
}

async function loadOrders() {
  message.value = ''
  try {
    const data = await listOrders({ tab: tab.value, page: page.value, pageSize: pageSize.value })
    orders.value = data.list || []
    total.value = data.total || 0
  } catch (error) {
    message.value = error.message
  }
}

function switchTab(t) {
  tab.value = t
  page.value = 1
  statusFilter.value = ''
  keyword.value = ''
  loadOrders()
}

onMounted(loadOrders)
</script>
