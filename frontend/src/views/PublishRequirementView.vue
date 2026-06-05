<template>
  <section class="campus-page publish-page">
    <header class="campus-page-header">
      <h1>发布需求</h1>
    </header>

    <p v-if="message" :class="messageType">{{ message }}</p>

    <div class="publish-layout">
      <form class="glass-panel publish-form" @submit.prevent="handlePublish">
        <section class="publish-step">
          <h2><span>1</span>填写基础信息</h2>
          <div class="publish-basic-grid">
            <label class="field-block">
              标题
              <input v-model="form.title" maxlength="50" placeholder="起个简洁明确的标题" required />
              <small>{{ form.title.length }}/50</small>
            </label>

            <div class="field-block category-field">
              选择需求分类
              <div class="category-grid">
                <button
                  v-for="item in categories"
                  :key="item.type"
                  type="button"
                  :class="['category-card', { selected: form.type === item.type }]"
                  :style="{ '--type-color': item.color }"
                  @click="form.type = item.type"
                >
                  <span class="category-icon-wrap">
                    <img v-if="item.iconUrl" :src="item.iconUrl" :alt="item.label" />
                    <b v-else>{{ item.icon }}</b>
                  </span>
                  <span class="category-card-label">{{ item.label }}</span>
                </button>
              </div>
            </div>
          </div>
        </section>

        <section class="publish-step">
          <h2><span>2</span>设置任务报酬</h2>
          <div class="reward-row">
            <label class="field-block">
              预算（元）
              <input v-model.trim="form.budget" type="text" inputmode="decimal" placeholder="￥ 0.00" required />
              <small>0 元表示无偿互助</small>
            </label>
            <div class="reward-tip">
              <span>💰</span>
              合理的报酬能让你的需求更快被同学看到。
            </div>
          </div>
        </section>

        <section class="publish-step">
          <h2><span>3</span>详细描述需求</h2>
          <label class="field-block">
            <textarea
              v-model="form.description"
              maxlength="500"
              rows="5"
              placeholder="请详细说明需求内容、地点、时间、联系方式或注意事项等信息"
              required
            ></textarea>
            <small>{{ form.description.length }}/500</small>
          </label>
        </section>

        <div class="publish-actions">
          <button type="button" @click="$router.push('/requirements')">取消</button>
          <button class="campus-green-btn" type="submit" :disabled="loading">
            {{ loading ? '发布中...' : '立即发布' }}
          </button>
        </div>
      </form>

      <aside class="glass-panel demand-preview">
        <h2>需求预览</h2>
        <p>这是你的需求在大厅中的展示效果</p>
        <article class="preview-card" :style="{ '--type-color': selectedCategory.color }">
          <div class="preview-top">
            <span class="type-orb">
              <img v-if="selectedCategory.iconUrl" :src="selectedCategory.iconUrl" :alt="selectedCategory.label" />
              <b v-else>{{ selectedCategory.icon }}</b>
            </span>
            <div>
              <small>{{ previewName }}</small>
              <strong>待接单</strong>
            </div>
          </div>
          <h3>{{ form.title || '标题' }}</h3>
          <span class="preview-type">{{ selectedCategory.label }}</span>
          <div class="preview-budget">￥ {{ money(form.budget) }}</div>
          <div class="preview-desc">
            <b>描述</b>
            <p v-if="form.description.trim()">{{ form.description }}</p>
          </div>
        </article>
      </aside>
    </div>
  </section>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { createRequirement } from '../api/requirements'
import { getProfile } from '../api/profile'
import { requirementCategories } from '../utils/categories'

const router = useRouter()
const loading = ref(false)
const message = ref('')
const messageType = ref('message')
const profile = ref(null)

const categories = requirementCategories

const form = reactive({
  title: '',
  description: '',
  budget: null,
  type: 'EXPRESS'
})

const selectedCategory = computed(() => categories.find((item) => item.type === form.type) || categories[0])
const previewName = computed(() => profile.value?.nickname || profile.value?.username || '用户名')

function money(value) {
  return Number(value || 0).toFixed(2)
}

function validate() {
  if (!form.title.trim()) {
    message.value = '请填写标题'
    messageType.value = 'message error'
    return false
  }
  if (form.budget === null || form.budget === '') {
    message.value = '请填写预算金额'
    messageType.value = 'message error'
    return false
  }
  if (Number(form.budget) < 0) {
    message.value = '预算金额必须大于等于 0'
    messageType.value = 'message error'
    return false
  }
  if (!form.type) {
    message.value = '请选择需求分类'
    messageType.value = 'message error'
    return false
  }
  return true
}

async function handlePublish() {
  message.value = ''
  if (!validate()) return

  loading.value = true
  try {
    const data = await createRequirement({
      title: form.title.trim(),
      description: form.description.trim(),
      budget: Number(form.budget),
      type: form.type
    })
    messageType.value = 'message success'
    message.value = `发布成功！需求编号：${data.reqId}`
    setTimeout(() => router.push(`/requirements/${data.reqId}`), 900)
  } catch (error) {
    messageType.value = 'message error'
    message.value = error.message
  } finally {
    loading.value = false
  }
}

async function loadProfileForPreview() {
  try {
    profile.value = await getProfile()
  } catch {
    profile.value = null
  }
}

onMounted(loadProfileForPreview)
</script>
