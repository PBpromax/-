<template>
  <section class="detail-art-page">
    <article v-if="order" class="campus-card order-detail-shell">
      <header class="order-detail-top">
        <button class="back-button" type="button" aria-label="返回订单列表" @click="$router.push('/orders')">‹</button>
        <div class="order-heading">
          <span class="task-icon" aria-hidden="true">▧</span>
          <div>
            <h1>{{ order.reqTitle || '订单详情' }}</h1>
            <p>订单号：{{ order.orderId }} · {{ formatTime(order.createdAt) }}</p>
          </div>
          <span :class="['status-pill', statusClass(order.status)]">{{ statusLabel(order.status) }}</span>
        </div>
        <div class="task-price">
          <span>酬金</span>
          <strong>￥{{ Number(order.amount).toFixed(2) }}</strong>
        </div>
      </header>

      <p v-if="message" :class="messageType">{{ message }}</p>

      <div class="order-detail-grid">
        <section class="order-left">
          <div class="order-people">
            <div class="person-card">
              <span class="person-avatar">{{ initials(order.publisherName) }}</span>
              <div>
                <small>发布者</small>
                <h2>{{ order.publisherName || '匿名用户' }}</h2>
              </div>
            </div>
            <div class="person-card">
              <span class="person-avatar receiver">{{ initials(order.receiverName) }}</span>
              <div>
                <small>接单者</small>
                <h2>{{ order.receiverName || '暂未接单' }}</h2>
              </div>
            </div>
          </div>

          <section class="task-section">
            <h2>订单描述</h2>
            <p>{{ order.reqDescription || '暂无订单描述。' }}</p>
          </section>

          <section class="task-section">
            <h2>订单进度</h2>
            <div class="order-timeline">
              <div v-for="step in timeline" :key="step.key" :class="['timeline-step', { active: step.active, done: step.done }]">
                <span></span>
                <div>
                  <strong>{{ step.label }}</strong>
                  <p>{{ step.caption }}</p>
                </div>
              </div>
            </div>
          </section>
        </section>

        <aside class="order-actions-panel">
          <h2>订单操作</h2>
          <button class="primary" :disabled="order.status !== 'IN_PROGRESS'" @click="updateStatus('SUBMIT')">
            提交验收
          </button>
          <button class="primary" :disabled="order.status !== 'TO_CONFIRM'" @click="updateStatus('CONFIRM')">
            确认完成
          </button>
          <button :disabled="order.status === 'COMPLETED' || order.status === 'CANCELED'" @click="updateStatus('CANCEL')">
            取消订单
          </button>

          <section v-if="order.status === 'COMPLETED'" class="evaluation-panel inline-evaluation-panel">
            <div class="evaluation-heading">
              <div>
                <span class="detail-label">订单评价</span>
                <h2>评价{{ evaluationTargetName }}</h2>
                <p>完成订单后，双方都可以各评价一次。</p>
              </div>
              <div class="evaluation-score-preview">{{ evaluationForm.star }} 星</div>
            </div>

            <div class="rating-control" aria-label="选择评分">
              <button
                v-for="star in 5"
                :key="star"
                type="button"
                :class="{ active: evaluationForm.star >= star }"
                :aria-label="`${star} 星`"
                :disabled="order.currentUserEvaluated"
                @click="evaluationForm.star = star"
              >
                ★
              </button>
            </div>

            <label class="evaluation-content">
              评价内容
              <textarea
                v-model.trim="evaluationForm.content"
                maxlength="500"
                :disabled="order.currentUserEvaluated"
                placeholder="写下这次合作体验，例如沟通是否顺畅、完成质量如何。"
              />
            </label>

            <div class="evaluation-actions">
              <span>信用分变化：{{ creditPreview }}</span>
              <button class="primary" :disabled="submittingEvaluation || order.currentUserEvaluated" @click="handleEvaluationSubmit">
                {{ order.currentUserEvaluated ? '已评价' : (submittingEvaluation ? '提交中...' : '提交评价') }}
              </button>
            </div>
          </section>
        </aside>
      </div>
    </article>

    <article v-if="!order" class="campus-card order-detail-shell">
      <p v-if="message" :class="messageType">{{ message }}</p>
      <p v-else>订单详情加载中...</p>
    </article>
  </section>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute } from 'vue-router'
import { changeOrderStatus, getOrderDetail, submitEvaluation } from '../api/orders'
import { getUserId } from '../utils/auth'

const route = useRoute()
const order = ref(null)
const message = ref('')
const messageType = ref('message')
const submittingEvaluation = ref(false)
const evaluationForm = reactive({
  star: 5,
  content: ''
})

const currentUserId = computed(() => getUserId())
const evaluationTargetName = computed(() => {
  if (!order.value) return '对方'
  if (currentUserId.value === order.value.publisherId) {
    return order.value.receiverName || '接单者'
  }
  if (currentUserId.value === order.value.receiverId) {
    return order.value.publisherName || '发布者'
  }
  return '对方'
})
const creditPreview = computed(() => ({
  5: '+2',
  4: '+1',
  3: '不变',
  2: '-1',
  1: '-2'
}[evaluationForm.star]))
const timeline = computed(() => {
  const status = order.value?.status
  const orderIndex = ['CREATED', 'IN_PROGRESS', 'TO_CONFIRM', 'COMPLETED'].indexOf(status)
  return [
    { key: 'created', label: '订单已发布', caption: `发布者：${order.value?.publisherName || '匿名用户'}`, done: true, active: status === 'CREATED' },
    { key: 'accepted', label: '订单已接取', caption: `接单者：${order.value?.receiverName || '接单者'}`, done: orderIndex >= 1 || ['IN_PROGRESS', 'TO_CONFIRM', 'COMPLETED'].includes(status), active: status === 'IN_PROGRESS' },
    { key: 'submit', label: '等待验收', caption: '接单者提交后由发布者确认', done: ['TO_CONFIRM', 'COMPLETED'].includes(status), active: status === 'TO_CONFIRM' },
    { key: 'done', label: '订单已完成', caption: '双方可进行评价', done: status === 'COMPLETED', active: status === 'COMPLETED' }
  ]
})

function formatTime(value) {
  return value ? value.replace('T', ' ').slice(0, 16) : ''
}

function statusLabel(status) {
  return {
    CREATED: '已创建',
    IN_PROGRESS: '进行中',
    TO_CONFIRM: '待验收',
    COMPLETED: '已完成',
    CANCELED: '已取消'
  }[status] || '未知状态'
}

function statusClass(status) {
  return {
    CREATED: 'pending',
    IN_PROGRESS: 'active',
    TO_CONFIRM: 'pending',
    COMPLETED: 'done',
    CANCELED: 'cancel'
  }[status] || 'pending'
}

function initials(name) {
  return (name || '用').slice(0, 1)
}

async function loadOrder() {
  try {
    order.value = await getOrderDetail(route.params.orderId)
  } catch (error) {
    messageType.value = 'message error'
    message.value = error.message
  }
}

async function updateStatus(event) {
  message.value = ''
  try {
    order.value = await changeOrderStatus(route.params.orderId, event)
    messageType.value = 'message success'
    message.value = '订单状态已更新'
  } catch (error) {
    messageType.value = 'message error'
    message.value = error.message
  }
}

async function handleEvaluationSubmit() {
  message.value = ''
  submittingEvaluation.value = true
  try {
    await submitEvaluation(route.params.orderId, {
      star: evaluationForm.star,
      content: evaluationForm.content
    })
    messageType.value = 'message success'
    message.value = '评价提交成功'
    evaluationForm.content = ''
    order.value.currentUserEvaluated = true
  } catch (error) {
    messageType.value = 'message error'
    message.value = error.message
  } finally {
    submittingEvaluation.value = false
  }
}

onMounted(loadOrder)
</script>
