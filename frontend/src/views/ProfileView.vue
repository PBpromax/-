<template>
  <section class="profile-page">
    <article class="campus-card profile-shell">
      <header class="profile-topbar">
        <div>
          <h1>个人资料</h1>
        </div>
        <button class="primary" type="button" :disabled="saving" @click="editing ? saveProfile() : (editing = true)">
          {{ editing ? (saving ? '保存中' : '保存修改') : '编辑资料' }}
        </button>
      </header>

      <p v-if="message" class="message">{{ message }}</p>

      <div class="profile-layout">
        <aside class="profile-identity-card">
          <div class="profile-avatar">{{ avatarText }}</div>
          <h2>{{ displayName }}</h2>
          <p>{{ form.college || '学院未填写' }}</p>
          <div class="profile-tags">
            <span>{{ form.grade || '年级未填写' }}</span>
            <span>{{ form.major || '专业未填写' }}</span>
          </div>
          <div class="profile-credit">
            <span>信用分</span>
            <strong>{{ cappedCredit }}</strong>
          </div>
          <div class="profile-mini-list">
            <span><b>用户名</b>{{ form.username || '-' }}</span>
            <span><b>学号</b>{{ form.studentId || '-' }}</span>
            <span><b>校区</b>{{ form.campus || '-' }}</span>
          </div>
        </aside>

        <div class="profile-main">
          <template v-if="!editing">
            <section class="profile-display-grid">
              <div class="profile-info-card profile-bio-card">
                <div class="section-title">
                  <span aria-hidden="true">☷</span>
                  <h3>个人简介</h3>
                </div>
                <p class="profile-bio">{{ form.bio || '还没有填写自我介绍。' }}</p>
              </div>

              <div class="profile-info-card profile-contact-card">
                <div class="section-title">
                  <span aria-hidden="true">☏</span>
                  <h3>联系方式展示</h3>
                </div>
                <div class="contact-lines">
                  <span><b>微信</b><em>{{ form.wechat || '' }}</em></span>
                  <span><b>QQ</b><em>{{ form.qq || '' }}</em></span>
                  <span><b>电话</b><em>{{ form.phone || '' }}</em></span>
                </div>
              </div>

              <div class="profile-info-card profile-campus-card">
                <div class="section-title">
                  <span aria-hidden="true">⌁</span>
                  <h3>校园信息</h3>
                </div>
                <div class="campus-lines">
                  <span><b>学院</b><em>{{ form.college || '' }}</em></span>
                  <span><b>专业</b><em>{{ form.major || '' }}</em></span>
                  <span><b>年级</b><em>{{ form.grade || '' }}</em></span>
                </div>
              </div>

              <div class="profile-info-card">
                <div class="section-title">
                  <span aria-hidden="true">★</span>
                  <h3>信用等级</h3>
                </div>
                <p>{{ creditLevel }}</p>
              </div>
              <div class="profile-info-card">
                <div class="section-title">
                  <span aria-hidden="true">✓</span>
                  <h3>认证状态</h3>
                </div>
                <p>已登录 CampusHub 账户</p>
              </div>
            </section>
          </template>

          <form v-else class="profile-edit-grid" @submit.prevent="saveProfile">
            <section class="profile-edit-card">
              <div class="section-title">
                <span aria-hidden="true">♙</span>
                <h3>基础信息</h3>
              </div>
              <label>
                用户名/昵称
                <input v-model="form.nickname" maxlength="64" :placeholder="form.username || '请输入显示名称'" />
              </label>
              <label>
                信用分
                <input :value="cappedCredit" disabled />
              </label>
            </section>

            <section class="profile-edit-card">
              <div class="section-title">
                <span aria-hidden="true">⌂</span>
                <h3>校园信息</h3>
              </div>
              <label>
                校区
                <input v-model="form.campus" maxlength="64" />
              </label>
              <label>
                学院
                <input v-model="form.college" maxlength="64" />
              </label>
              <label>
                专业
                <input v-model="form.major" maxlength="64" />
              </label>
              <label>
                年级
                <input v-model="form.grade" maxlength="32" />
              </label>
            </section>

            <section class="profile-edit-card">
              <div class="section-title">
                <span aria-hidden="true">☑</span>
                <h3>展示设置</h3>
              </div>
              <label>
                学号
                <input v-model="form.studentId" maxlength="32" />
              </label>
              <label class="checkbox-row">
                <input v-model="form.contactVisible" type="checkbox" />
                允许展示联系方式
              </label>
            </section>

            <section class="profile-edit-card profile-contact-edit-card">
              <div class="section-title">
                <span aria-hidden="true">☏</span>
                <h3>联系方式</h3>
              </div>
              <label>
                QQ
                <input v-model="form.qq" maxlength="32" />
              </label>
              <label>
                微信
                <input v-model="form.wechat" maxlength="64" />
              </label>
              <label>
                电话
                <input v-model="form.phone" maxlength="32" />
              </label>
            </section>

            <section class="profile-edit-card profile-edit-wide">
              <div class="section-title">
                <span aria-hidden="true">☰</span>
                <h3>个人简介</h3>
              </div>
              <textarea v-model="form.bio" maxlength="255" rows="4" placeholder="写下你擅长的事情、常在的校区或协作偏好。" />
            </section>

          </form>
        </div>
      </div>
    </article>
  </section>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { getProfile, updateProfile } from '../api/profile'

const saving = ref(false)
const editing = ref(false)
const message = ref('')
const form = reactive({
  username: '',
  nickname: '',
  studentId: '',
  qq: '',
  wechat: '',
  phone: '',
  campus: '',
  college: '',
  major: '',
  grade: '',
  bio: '',
  contactVisible: false,
  creditScore: 100
})
const snapshot = ref(null)

const displayName = computed(() => form.nickname || form.username || '未设置昵称')
const avatarText = computed(() => displayName.value.slice(0, 1).toUpperCase())
const cappedCredit = computed(() => Math.min(Number(form.creditScore || 0), 100))
const creditLevel = computed(() => {
  if (cappedCredit.value >= 95) return '优秀协作者'
  if (cappedCredit.value >= 85) return '可信协作者'
  if (cappedCredit.value >= 70) return '稳定协作者'
  return '需提升信用'
})

function fillForm(profile) {
  Object.assign(form, profile, {
    creditScore: Math.min(Number(profile.creditScore ?? 100), 100)
  })
  snapshot.value = JSON.parse(JSON.stringify(form))
}

async function loadProfile() {
  try {
    fillForm(await getProfile())
  } catch (error) {
    message.value = error.message
  }
}

async function saveProfile() {
  saving.value = true
  message.value = ''
  try {
    const saved = await updateProfile({
      nickname: form.nickname,
      studentId: form.studentId,
      qq: form.qq,
      wechat: form.wechat,
      phone: form.phone,
      campus: form.campus,
      college: form.college,
      major: form.major,
      grade: form.grade,
      bio: form.bio,
      contactVisible: form.contactVisible
    })
    fillForm(saved)
    editing.value = false
    message.value = '个人资料已更新'
  } catch (error) {
    message.value = error.message
  } finally {
    saving.value = false
  }
}

onMounted(loadProfile)
</script>
