<template>
  <section>
    <header class="page-header">
      <div>
        <h1>需求大厅</h1>
        <p>浏览、搜索和筛选校内互助需求。</p>
      </div>
      <button @click="loadRequirements">刷新</button>
    </header>

    <div class="toolbar">
      <input v-model="filters.keyword" placeholder="搜索标题或描述" @keyup.enter="applyFilters" />
      <select v-model="filters.type">
        <option value="">全部分类</option>
        <option value="EXPRESS">快递跑腿</option>
        <option value="TUTORING">学习求助</option>
        <option value="SECOND_HAND">二手交易</option>
        <option value="LOST_FOUND">失物招领</option>
        <option value="TEAM_UP">组队招募</option>
      </select>
      <select v-model="filters.status">
        <option value="">全部状态</option>
        <option value="PENDING">待接单</option>
        <option value="ACCEPTED">已接单</option>
        <option value="COMPLETED">已完成</option>
        <option value="CANCELED">已取消</option>
      </select>
      <button class="primary" @click="applyFilters">筛选</button>
      <button @click="loadRecommendations">智能推荐</button>
    </div>

    <p v-if="message" class="message">{{ message }}</p>

    <div class="list">
      <RouterLink
        v-for="item in requirements"
        :key="item.reqId"
        class="requirement-row"
        :to="`/requirements/${item.reqId}`"
      >
        <div>
          <h2>{{ item.title }}</h2>
          <p>{{ item.publisherName || '匿名用户' }} · {{ typeLabel(item.type) }} · {{ formatTime(item.createdAt) }}</p>
        </div>
        <div class="row-meta">
          <strong>{{ Number(item.budget).toFixed(2) }}</strong>
          <span :class="['status', item.status.toLowerCase()]">{{ statusLabel(item.status) }}</span>
        </div>
      </RouterLink>
    </div>

    <div class="pagination">
      <button :disabled="filters.page <= 1" @click="changePage(filters.page - 1)">上一页</button>
      <span>第 {{ filters.page }} 页 / 共 {{ total }} 条</span>
      <button :disabled="filters.page * filters.pageSize >= total" @click="changePage(filters.page + 1)">下一页</button>
    </div>
  </section>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { listRequirements, recommendRequirements } from '../api/requirements'

const requirements = ref([])
const total = ref(0)
const message = ref('')
const filters = reactive({
  keyword: '',
  type: '',
  status: '',
  page: 1,
  pageSize: 10
})

function activeRequirements(list) {
  return list.filter((item) => item.status !== 'CANCELED')
}

function formatTime(value) {
  return value ? value.replace('T', ' ').slice(0, 16) : ''
}

function statusLabel(status) {
  return {
    PENDING: '待接单',
    ACCEPTED: '已接单',
    COMPLETED: '已完成',
    CANCELED: '已取消'
  }[status] || status
}

function typeLabel(type) {
  return {
    EXPRESS: '快递跑腿',
    STUDY: '学习求助',
    SECOND_HAND: '二手交易',
    LOST_FOUND: '失物招领',
    TUTORING: '学业辅导',
    MATERIAL: '资料共享',
    TEAM_UP: '组队招募',
    CARPOOL: '拼车出行',
    QA: '问答求助',
    OTHER: '其他'
  }[type] || type
}

async function loadRequirements() {
  message.value = ''
  try {
    const data = await listRequirements(filters)
    requirements.value = activeRequirements(data.list)
    total.value = requirements.value.length
    if (requirements.value.length === 0) {
      message.value = '暂无符合条件的需求'
    }
  } catch (error) {
    message.value = error.message
  }
}

function applyFilters() {
  filters.page = 1
  loadRequirements()
}

function changePage(page) {
  filters.page = page
  loadRequirements()
}

async function loadRecommendations() {
  message.value = ''
  try {
    const data = await recommendRequirements({ page: 1, pageSize: filters.pageSize })
    requirements.value = activeRequirements(data.list)
    total.value = requirements.value.length
    filters.page = data.page
    if (requirements.value.length === 0) {
      message.value = '暂无可推荐需求'
    }
  } catch (error) {
    message.value = error.message
  }
}

onMounted(loadRequirements)
</script>
