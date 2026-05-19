<template>
  <section>
    <header class="page-header">
      <div>
        <h1>订单详情</h1>
        <p v-if="order">{{ formatTime(order.createdAt) }}</p>
      </div>
      <RouterLink class="button" to="/orders">返回列表</RouterLink>
    </header>

    <p v-if="message" :class="messageType">{{ message }}</p>

    <article v-if="order" class="detail-panel">
      <div class="detail-meta">
        <span>订单号：{{ order.orderId }}</span>
        <span>状态：{{ order.status }}</span>
        <span>金额：{{ Number(order.amount).toFixed(2) }}</span>
      </div>
      <div class="detail-meta">
        <span>需求：{{ order.reqTitle }}</span>
        <span>发布者：{{ order.publisherName }}</span>
        <span>接单者：{{ order.receiverName }}</span>
      </div>
      <p class="description">{{ order.reqDescription }}</p>
      <div class="toolbar">
        <button class="primary" :disabled="order.status !== 'IN_PROGRESS'" @click="updateStatus('SUBMIT')">
          提交验收
        </button>
        <button class="primary" :disabled="order.status !== 'TO_CONFIRM'" @click="updateStatus('CONFIRM')">
          确认完成
        </button>
        <button :disabled="order.status === 'COMPLETED' || order.status === 'CANCELED'" @click="updateStatus('CANCEL')">
          取消订单
        </button>
      </div>
    </article>

    <article v-if="order?.status === 'COMPLETED'" class="detail-panel evaluation-panel">
      <div class="evaluation-heading">
        <div>
          <span class="detail-label">订单评价</span>
          <h2>评价{{ evaluationTargetName }}</h2>
          <p>完成订单后，双方都可以各评价一次，评分会同步影响对方信用分。</p>
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
          placeholder="写下这次合作体验，例如沟通是否顺畅、完成质量如何。"
        />
      </label>

      <div class="evaluation-actions">
        <span>信用分变化：{{ creditPreview }}</span>
        <button class="primary" :disabled="submittingEvaluation" @click="handleEvaluationSubmit">
          {{ submittingEvaluation ? '提交中...' : '提交评价' }}
        </button>
      </div>
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
const creditPreview = computed(() => {
  return {
    5: '+2',
    4: '+1',
    3: '不变',
    2: '-1',
    1: '-2'
  }[evaluationForm.star]
})

function formatTime(value) {
  return value ? value.replace('T', ' ').slice(0, 16) : ''
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
  } catch (error) {
    messageType.value = 'message error'
    message.value = error.message
  } finally {
    submittingEvaluation.value = false
  }
}

onMounted(loadOrder)
</script>
