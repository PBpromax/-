<template>
  <section>
    <header class="page-header">
      <div>
        <h1>管理后台</h1>
        <p>查看用户、需求和订单，并下架违规或过期需求。</p>
      </div>
      <button class="primary" @click="loadAll">刷新</button>
    </header>

    <p v-if="message" :class="messageType">{{ message }}</p>

    <div class="admin-grid">
      <section class="detail-panel">
        <h2>用户</h2>
        <div v-for="user in users" :key="user.user_id" class="admin-row">
          <span>{{ user.username }} / {{ user.nickname || '未设置昵称' }}</span>
          <small>信用分 {{ user.credit_score }} · role {{ user.role }}</small>
        </div>
      </section>

      <section class="detail-panel">
        <h2>需求</h2>
        <div v-for="req in requirements" :key="req.req_id" class="admin-row">
          <span>{{ req.title }}</span>
          <small>{{ req.type }} · {{ req.status }} · {{ req.publisher_name }}</small>
          <button :disabled="req.status === 'CANCELED'" @click="cancelRequirement(req.req_id)">
            下架
          </button>
        </div>
      </section>

      <section class="detail-panel">
        <h2>订单</h2>
        <div v-for="order in orders" :key="order.order_id" class="admin-row">
          <span>{{ order.req_title }}</span>
          <small>{{ order.status }} · {{ Number(order.amount).toFixed(2) }}</small>
        </div>
      </section>
    </div>
  </section>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import {
  cancelAdminRequirement,
  listAdminOrders,
  listAdminRequirements,
  listAdminUsers
} from '../api/admin'

const users = ref([])
const requirements = ref([])
const orders = ref([])
const message = ref('')
const messageType = ref('message')

async function loadAll() {
  message.value = ''
  try {
    const [userData, requirementData, orderData] = await Promise.all([
      listAdminUsers(),
      listAdminRequirements(),
      listAdminOrders()
    ])
    users.value = userData
    requirements.value = requirementData
    orders.value = orderData
  } catch (error) {
    messageType.value = 'message error'
    message.value = error.message
  }
}

async function cancelRequirement(reqId) {
  try {
    await cancelAdminRequirement(reqId)
    messageType.value = 'message success'
    message.value = '需求已下架'
    await loadAll()
  } catch (error) {
    messageType.value = 'message error'
    message.value = error.message
  }
}

onMounted(loadAll)
</script>
