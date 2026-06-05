import { clearToken, getToken } from '../utils/auth'

const API_BASE = import.meta.env.VITE_API_BASE_URL || '/api'

function buildUrl(path) {
  if (!API_BASE) {
    return path
  }
  const base = API_BASE.replace(/\/$/, '')
  const normalizedPath = path.startsWith('/') ? path : `/${path}`
  if (base === '/api' && normalizedPath.startsWith('/api/')) {
    return normalizedPath
  }
  return `${base}${normalizedPath}`
}

export async function request(path, options = {}) {
  const token = getToken()
  const headers = {
    'Content-Type': 'application/json',
    ...(token ? { Authorization: `Bearer ${token}` } : {}),
    ...(options.headers || {})
  }

  const response = await fetch(buildUrl(path), {
    ...options,
    headers
  })

  const text = await response.text()
  let payload
  try {
    payload = text ? JSON.parse(text) : null
  } catch (error) {
    throw new Error(
      `请求没有到达后端，请刷新页面或检查服务是否正常运行（状态 ${response.status}，类型 ${response.headers.get('content-type') || '未知'}，内容 ${text.slice(0, 120) || '空'}）`
    )
  }
  if (!payload) {
    throw new Error(response.ok ? '服务暂时没有返回内容' : '服务请求失败，请稍后重试')
  }
  if (response.status === 401 || payload.code === 401) {
    clearToken()
    if (window.location.pathname !== '/login') {
      window.location.href = '/login'
    }
    throw new Error('登录已失效，请重新登录')
  }
  if (payload.code !== 200) {
    throw new Error(payload.message || '请求失败')
  }
  return payload.data
}
