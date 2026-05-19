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
  </section>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { changeOrderStatus, getOrderDetail } from '../api/orders'

const route = useRoute()
const order = ref(null)
const message = ref('')
const messageType = ref('message')

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

onMounted(loadOrder)
</script>
