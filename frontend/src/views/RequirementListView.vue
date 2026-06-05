<template>
  <section class="campus-page requirements-page">
    <header class="campus-page-header">
      <h1>需求大厅</h1>
    </header>

    <div class="campus-toolbar">
      <select v-model="filters.type" @change="applyFilters">
        <option value="">全部分类</option>
        <option v-for="item in categories" :key="item.type" :value="item.type">{{ item.label }}</option>
      </select>
      <input v-model="filters.keyword" placeholder="搜索标题或描述" @keyup.enter="applyFilters" />
      <button class="campus-green-btn" @click="applyFilters">搜索</button>
    </div>

    <p v-if="message" class="message">{{ message }}</p>

    <div class="requirements-layout">
      <article class="glass-panel demand-panel">
        <div class="panel-title">
          <h2>可接单需求</h2>
          <span>{{ requirements.length }}</span>
        </div>

        <div class="demand-scroll">
          <RouterLink
            v-for="item in requirements"
            :key="item.reqId"
            class="demand-bubble"
            :to="`/requirements/${item.reqId}`"
            :style="{ '--type-color': categoryMeta(item.type).color }"
          >
            <span class="type-orb">
              <img v-if="categoryMeta(item.type).iconUrl" :src="categoryMeta(item.type).iconUrl" :alt="categoryMeta(item.type).label" />
              <b v-else>{{ categoryMeta(item.type).icon }}</b>
            </span>
            <div class="bubble-main">
              <h3>{{ item.title }}</h3>
              <p>{{ item.publisherName || '匿名用户' }} · {{ categoryMeta(item.type).label }}</p>
              <small>{{ item.description || '暂无描述' }}</small>
            </div>
            <div class="bubble-side">
              <strong>{{ money(item.budget) }}</strong>
              <span :class="['status', item.status.toLowerCase()]">{{ statusLabel(item.status) }}</span>
            </div>
          </RouterLink>

          <div v-if="requirements.length === 0" class="campus-empty">暂无符合条件的需求</div>
        </div>
        <button v-if="requirements.length > 0" class="load-more" :disabled="filters.page * filters.pageSize >= total" @click="changePage(filters.page + 1)">
          查看更多需求⌄
        </button>
      </article>

      <aside class="side-stack">
        <article class="glass-panel mini-orders">
          <div class="panel-title compact">
            <h2>我的派单</h2>
            <RouterLink to="/orders">更多 ›</RouterLink>
          </div>
          <div class="mini-scroll">
            <RouterLink
              v-for="item in publishedPreview"
              :key="`p-${item.reqId}-${item.orderId || ''}`"
              class="mini-order"
              :to="item.orderId ? `/orders/${item.orderId}` : `/requirements/${item.reqId}`"
            >
              <span>{{ item.reqTitle }}</span>
              <strong>{{ money(item.amount) }}</strong>
              <small>{{ statusLabel(item.status) }}</small>
            </RouterLink>
            <div v-if="publishedPreview.length === 0" class="mini-empty">暂无派单</div>
          </div>
        </article>

        <article class="glass-panel mini-orders">
          <div class="panel-title compact">
            <h2>我的接单</h2>
            <RouterLink to="/orders">更多 ›</RouterLink>
          </div>
          <div class="mini-scroll">
            <RouterLink
              v-for="item in receivedPreview"
              :key="`r-${item.reqId}-${item.orderId || ''}`"
              class="mini-order"
              :to="item.orderId ? `/orders/${item.orderId}` : `/requirements/${item.reqId}`"
            >
              <span>{{ item.reqTitle }}</span>
              <strong>{{ money(item.amount) }}</strong>
              <small>{{ statusLabel(item.status) }}</small>
            </RouterLink>
            <div v-if="receivedPreview.length === 0" class="mini-empty">暂无接单</div>
          </div>
        </article>
      </aside>
    </div>
  </section>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { listRequirements } from '../api/requirements'
import { listOrders } from '../api/orders'
import { categoryMeta, requirementCategories } from '../utils/categories'

const categories = requirementCategories

const requirements = ref([])
const publishedPreview = ref([])
const receivedPreview = ref([])
const total = ref(0)
const message = ref('')
const filters = reactive({
  keyword: '',
  type: '',
  status: '',
  page: 1,
  pageSize: 5
})

function activeRequirements(list) {
  return list.filter((item) => item.status !== 'CANCELED')
}

function formatTime(value) {
  return value ? value.replace('T', ' ').slice(0, 16) : ''
}

function money(value) {
  return Number(value || 0).toFixed(2)
}

function statusLabel(status) {
  return {
    PENDING: '待接单',
    ACCEPTED: '已接单',
    IN_PROGRESS: '进行中',
    TO_CONFIRM: '待确认',
    COMPLETED: '已完成',
    CANCELED: '已取消'
  }[status] || status
}

async function loadRequirements() {
  message.value = ''
  try {
    const data = await listRequirements(expandedTypeFilters())
    requirements.value = activeRequirements(data.list)
    total.value = data.total ?? requirements.value.length
  } catch (error) {
    message.value = error.message
  }
}

async function loadOrderPreview() {
  try {
    const [published, received] = await Promise.all([
      listOrders({ tab: 'published', page: 1, pageSize: 3 }),
      listOrders({ tab: 'received', page: 1, pageSize: 2 })
    ])
    publishedPreview.value = published.list || []
    receivedPreview.value = received.list || []
  } catch {
    publishedPreview.value = []
    receivedPreview.value = []
  }
}

function applyFilters() {
  filters.page = 1
  loadRequirements()
}

function expandedTypeFilters() {
  const meta = categoryMeta(filters.type)
  if (!filters.type || !meta.aliases?.length) {
    return filters
  }
  return {
    ...filters,
    type: [meta.type, ...meta.aliases]
  }
}

function changePage(page) {
  filters.page = page
  loadRequirements()
}

onMounted(() => {
  loadRequirements()
  loadOrderPreview()
})
</script>
