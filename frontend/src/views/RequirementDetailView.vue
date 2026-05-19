<template>
  <section>
    <header class="page-header">
      <div>
        <h1>{{ requirement?.title || '需求详情' }}</h1>
        <p v-if="requirement">{{ requirement.publisherName || '匿名用户' }} · {{ formatTime(requirement.createdAt) }}</p>
      </div>
      <RouterLink class="button" to="/requirements">返回列表</RouterLink>
    </header>

    <p v-if="message" :class="messageType">{{ message }}</p>

    <article v-if="requirement" class="detail-panel requirement-detail-panel">
      <div class="requirement-detail-head">
        <div class="requirement-detail-title">
          <span class="detail-label">需求标题</span>
          <h2>{{ requirement.title }}</h2>
        </div>
        <div class="requirement-detail-price">
          <span>预算</span>
          <strong>￥{{ Number(requirement.budget).toFixed(2) }}</strong>
        </div>
      </div>

      <p class="description requirement-detail-description">{{ requirement.description }}</p>

      <div class="requirement-detail-footer">
        <div class="detail-meta">
          <span>分类：{{ typeLabel(requirement.type) }}</span>
          <span :class="['status', requirement.status.toLowerCase()]">{{ statusLabel(requirement.status) }}</span>
          <span>{{ requirement.publisherName || '匿名用户' }}</span>
        </div>

        <button
          class="primary detail-accept-button"
          :disabled="!requirement.acceptable || accepting"
          @click="handleAccept"
        >
          {{ accepting ? '接单中...' : (requirement.acceptable ? '接单' : '当前不可接单') }}
        </button>
      </div>
    </article>
  </section>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { getRequirement, acceptOrder } from '../api/requirements'

const route = useRoute()
const requirement = ref(null)
const message = ref('')
const messageType = ref('message')
const accepting = ref(false)

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
    TUTORING: '学业辅导',
    MATERIAL: '资料共享',
    TEAM_UP: '组队招募',
    CARPOOL: '拼车出行',
    QA: '问答求助',
    OTHER: '其他'
  }[type] || type
}

async function loadRequirement() {
  try {
    requirement.value = await getRequirement(route.params.reqId)
  } catch (error) {
    message.value = error.message
  }
}

async function handleAccept() {
  if (!requirement.value) return
  accepting.value = true
  message.value = ''
  try {
    const data = await acceptOrder(requirement.value.reqId)
    messageType.value = 'message success'
    message.value = `接单成功！订单编号：${data.orderId}`
    requirement.value.acceptable = false
    requirement.value.status = 'ACCEPTED'
  } catch (error) {
    messageType.value = 'message error'
    message.value = error.message
  } finally {
    accepting.value = false
  }
}

onMounted(loadRequirement)
</script>
