<template>
  <section class="detail-art-page">
    <article v-if="requirement" class="campus-card task-detail-shell">
      <header class="task-detail-top">
        <button class="back-button" type="button" aria-label="返回需求大厅" @click="$router.push('/requirements')">‹</button>
        <div class="task-title-block">
          <span class="task-icon" aria-hidden="true">
            <img v-if="typeMeta(requirement.type).iconUrl" :src="typeMeta(requirement.type).iconUrl" :alt="typeMeta(requirement.type).label" />
            <b v-else>{{ typeMeta(requirement.type).icon }}</b>
          </span>
          <div>
            <h1>{{ requirement.title }}</h1>
            <p>发布于 {{ formatTime(requirement.createdAt) }}</p>
          </div>
          <span :class="['status-pill', statusClass(requirement.status)]">{{ statusLabel(requirement.status) }}</span>
        </div>
        <div class="task-price">
          <span>预算</span>
          <strong>￥{{ Number(requirement.budget).toFixed(2) }}</strong>
        </div>
      </header>

      <p v-if="message" :class="messageType">{{ message }}</p>

      <div class="task-detail-grid">
        <section class="task-detail-main">
          <div class="task-info-card task-meta-line">
            <span>分类：{{ typeLabel(requirement.type) }}</span>
            <span>发布者：{{ requirement.publisherName || '匿名用户' }}</span>
          </div>

          <div class="task-description-row">
            <div class="task-info-card task-section">
              <h2>需求描述</h2>
              <p>{{ requirement.description || '发布者暂未填写详细描述。' }}</p>
            </div>

            <div class="task-info-card task-contact-card">
              <h2>联系方式</h2>
              <div :class="['task-contact-lines', { masked: !contactVisible }]">
                <span><b>微信</b><em>{{ requirement.publisherWechat || '暂未填写' }}</em></span>
                <span><b>QQ</b><em>{{ requirement.publisherQq || '暂未填写' }}</em></span>
                <span><b>电话</b><em>{{ requirement.publisherPhone || '暂未填写' }}</em></span>
              </div>
              <div v-if="!contactVisible" class="contact-mask">接单后可见</div>
            </div>
          </div>

          <div class="task-info-card task-section">
            <h2>协作提示</h2>
            <p>接单后请及时与发布者沟通地点、时间和验收方式，完成后可在订单详情中提交验收与评价。</p>
          </div>
        </section>

        <aside class="task-side-card">
          <h2>接单信息</h2>
          <div class="task-side-row">
            <span>当前状态</span>
            <strong>{{ statusLabel(requirement.status) }}</strong>
          </div>
          <button
            class="primary task-accept-button"
            :disabled="!requirement.acceptable || accepting"
            @click="handleAccept"
          >
            {{ accepting ? '接单中...' : (requirement.acceptable ? '可接单' : '不可接单') }}
          </button>
        </aside>
      </div>
    </article>

    <article v-else class="campus-card task-detail-shell">
      <p v-if="message" :class="messageType">{{ message }}</p>
      <p v-else>需求详情加载中...</p>
    </article>
  </section>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { getRequirement, acceptOrder } from '../api/requirements'
import { categoryMeta } from '../utils/categories'

const route = useRoute()
const requirement = ref(null)
const message = ref('')
const messageType = ref('message')
const accepting = ref(false)
const acceptedHere = ref(false)
const contactVisible = computed(() => acceptedHere.value)

function formatTime(value) {
  return value ? value.replace('T', ' ').slice(0, 16) : ''
}

function statusLabel(status) {
  return {
    PENDING: '待接单',
    ACCEPTED: '已接单',
    IN_PROGRESS: '进行中',
    TO_CONFIRM: '待确认',
    COMPLETED: '已完成',
    CANCELED: '已取消'
  }[status] || '未知状态'
}

function statusClass(status) {
  return {
    PENDING: 'pending',
    ACCEPTED: 'active',
    IN_PROGRESS: 'active',
    TO_CONFIRM: 'pending',
    COMPLETED: 'done',
    CANCELED: 'cancel'
  }[status] || 'pending'
}

function typeLabel(type) {
  return categoryMeta(type).label
}

function typeMeta(type) {
  return categoryMeta(type)
}

async function loadRequirement() {
  try {
    requirement.value = await getRequirement(route.params.reqId)
  } catch (error) {
    messageType.value = 'message error'
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
    acceptedHere.value = true
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
