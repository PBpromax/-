<template>
  <section class="login-visual-page">
    <div class="login-artboard">
      <div class="login-brand">Campus<span>Hub</span></div>
      <div class="login-panel register-panel">
        <header class="login-panel-header">
          <h1>创建账号</h1>
        </header>

        <form class="form-grid login-grid register-grid" @submit.prevent="handleRegister">
          <label class="login-input-row">
            <input v-model="form.username" maxlength="64" placeholder="请输入用户名" required autofocus />
          </label>
          <label class="login-input-row">
            <input v-model="form.password" type="password" maxlength="64" placeholder="请输入密码" required />
          </label>
          <label class="login-input-row">
            <input v-model="form.studentId" maxlength="32" placeholder="请输入学号（选填）" />
          </label>
          <label class="login-input-row">
            <input v-model="form.campus" maxlength="64" placeholder="请输入校区（选填）" />
          </label>
          <div class="wide">
            <button class="primary" type="submit" :disabled="loading">
              {{ loading ? '注册中...' : '注册' }}
            </button>
          </div>
        </form>

        <div class="auth-footer">
          <p class="auth-switch">
            已有账号？<RouterLink to="/login">去登录</RouterLink>
          </p>
        </div>
      </div>
    </div>

    <Transition name="login-error-float">
      <div
        v-if="message"
        class="login-error-backdrop"
        role="presentation"
        @click.self="message = ''"
      >
        <section class="login-error-modal" role="alertdialog" aria-modal="true" aria-labelledby="register-error-title">
          <h2 id="register-error-title">注册失败</h2>
          <p>{{ message }}</p>
          <button class="primary" type="button" @click="message = ''">我知道了</button>
        </section>
      </div>
    </Transition>
  </section>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { register } from '../api/auth'

const router = useRouter()
const loading = ref(false)
const message = ref('')
const form = reactive({
  username: '',
  password: '',
  studentId: '',
  campus: ''
})

async function handleRegister() {
  loading.value = true
  message.value = ''
  try {
    await register(form)
    router.push('/login')
  } catch (error) {
    message.value = error.message
  } finally {
    loading.value = false
  }
}
</script>
