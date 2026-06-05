<template>
  <section class="login-visual-page">
    <div class="login-artboard">
      <div class="login-brand">Campus<span>Hub</span></div>
      <div class="login-panel">
        <header class="login-panel-header">
          <h1>欢迎回来！</h1>
        </header>

        <form class="form-grid login-grid" @submit.prevent="handleLogin">
          <label class="login-input-row">
            <input
              v-model="form.username"
              maxlength="64"
              placeholder="请输入学号/邮箱/手机号"
              required
              autofocus
            />
          </label>
          <label class="login-input-row">
            <span class="password-field">
              <input
                v-model="form.password"
                :type="showPassword ? 'text' : 'password'"
                maxlength="64"
                placeholder="请输入密码"
                required
              />
              <button
                :class="['password-toggle', { visible: showPassword }]"
                type="button"
                :aria-label="showPassword ? '隐藏密码' : '显示密码'"
                @click="showPassword = !showPassword"
              ></button>
            </span>
          </label>
          <div class="login-options wide">
            <label class="inline-check">
              <input type="checkbox" />
              记住我
            </label>
          </div>
          <div class="wide">
            <button class="primary" type="submit" :disabled="loading">
              {{ loading ? '登录中...' : '登录' }}
            </button>
          </div>
        </form>

        <div class="auth-footer">
          <p class="auth-switch">
            还没有账号？<RouterLink to="/register">去注册</RouterLink>
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
        <section class="login-error-modal" role="alertdialog" aria-modal="true" aria-labelledby="login-error-title">
          <h2 id="login-error-title">登录失败</h2>
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
import { login } from '../api/auth'
import { setToken, setUserId, setUserRole } from '../utils/auth'

const router = useRouter()
const loading = ref(false)
const message = ref('')
const showPassword = ref(false)
const form = reactive({
  username: '',
  password: ''
})

async function handleLogin() {
  loading.value = true
  message.value = ''
  try {
    const data = await login(form)
    setToken(data.token)
    setUserId(data.userId)
    setUserRole(data.role)
    router.push('/home')
  } catch (error) {
    message.value = error.message
  } finally {
    loading.value = false
  }
}
</script>
