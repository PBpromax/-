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
          <div class="admin-row-head">
            <div class="admin-user-summary">
              <span>{{ user.username }} / {{ user.nickname || '未设置昵称' }}</span>
              <small>{{ user.role ? '管理员' : '普通用户' }}</small>
              <small>信用分 {{ user.credit_score }}</small>
            </div>
            <div class="admin-actions">
              <button @click="startEditUser(user)">编辑</button>
              <button :disabled="user.username === 'admin_test'" @click="deleteUser(user)">删除</button>
            </div>
          </div>
          <form v-if="editingUserId === user.user_id" class="admin-user-form" @submit.prevent="saveUser(user.user_id)">
            <input v-model.trim="userForm.nickname" placeholder="昵称" />
            <input v-model.trim="userForm.studentId" placeholder="学号" />
            <input v-model.trim="userForm.campus" placeholder="校区" />
            <input v-model.trim="userForm.college" placeholder="学院" />
            <input v-model.trim="userForm.major" placeholder="专业" />
            <input v-model.trim="userForm.grade" placeholder="年级" />
            <input v-model.number="userForm.creditScore" max="100" min="0" placeholder="信用分" type="number" />
            <label class="inline-check">
              <input v-model="userForm.contactVisible" type="checkbox" />
              展示联系方式
            </label>
            <textarea v-model.trim="userForm.bio" placeholder="自我介绍" />
            <div class="admin-form-actions">
              <button class="primary" type="submit">保存</button>
              <button type="button" @click="cancelEditUser">取消</button>
            </div>
          </form>
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
  deleteAdminUser,
  listAdminOrders,
  listAdminRequirements,
  listAdminUsers,
  updateAdminUser
} from '../api/admin'

const users = ref([])
const requirements = ref([])
const orders = ref([])
const message = ref('')
const messageType = ref('message')
const editingUserId = ref(null)
const userForm = ref({
  nickname: '',
  studentId: '',
  campus: '',
  college: '',
  major: '',
  grade: '',
  bio: '',
  contactVisible: false,
  creditScore: 100
})

async function loadAll() {
  message.value = ''
  try {
    const [userData, requirementData, orderData] = await Promise.all([
      listAdminUsers(),
      listAdminRequirements(),
      listAdminOrders()
    ])
    users.value = userData
    requirements.value = requirementData.filter((req) => req.status !== 'CANCELED')
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

function startEditUser(user) {
  editingUserId.value = user.user_id
  userForm.value = {
    nickname: user.nickname || '',
    studentId: user.student_id || '',
    campus: user.campus || '',
    college: user.college || '',
    major: user.major || '',
    grade: user.grade || '',
    bio: user.bio || '',
    contactVisible: Boolean(user.contact_visible),
    creditScore: Math.min(100, Math.max(0, Number(user.credit_score || 0)))
  }
}

function cancelEditUser() {
  editingUserId.value = null
}

async function saveUser(userId) {
  try {
    await updateAdminUser(userId, {
      ...userForm.value,
      creditScore: Math.min(100, Math.max(0, Number(userForm.value.creditScore || 0)))
    })
    messageType.value = 'message success'
    message.value = '用户信息已更新'
    editingUserId.value = null
    await loadAll()
  } catch (error) {
    messageType.value = 'message error'
    message.value = error.message
  }
}

async function deleteUser(user) {
  if (!window.confirm(`确定删除用户 ${user.username} 吗？相关需求、订单、评价和通知也会被清理。`)) {
    return
  }
  try {
    await deleteAdminUser(user.user_id)
    messageType.value = 'message success'
    message.value = '用户已删除'
    await loadAll()
  } catch (error) {
    messageType.value = 'message error'
    message.value = error.message
  }
}

onMounted(loadAll)
</script>
