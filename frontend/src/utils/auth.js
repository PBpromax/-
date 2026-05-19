const TOKEN_KEY = 'campushub_token'
const USER_ID_KEY = 'campushub_user_id'
const USER_ROLE_KEY = 'campushub_user_role'

export function getToken() {
  return localStorage.getItem(TOKEN_KEY)
}

export function setToken(token) {
  localStorage.setItem(TOKEN_KEY, token)
}

export function clearToken() {
  localStorage.removeItem(TOKEN_KEY)
  localStorage.removeItem(USER_ID_KEY)
  localStorage.removeItem(USER_ROLE_KEY)
}

export function getUserId() {
  const id = localStorage.getItem(USER_ID_KEY)
  return id ? Number(id) : null
}

export function setUserId(userId) {
  localStorage.setItem(USER_ID_KEY, String(userId))
}

export function getUserRole() {
  const role = localStorage.getItem(USER_ROLE_KEY)
  return role === null ? null : Number(role)
}

export function setUserRole(role) {
  localStorage.setItem(USER_ROLE_KEY, String(role))
}

export function isAdmin() {
  return getUserRole() === 1
}

export function isLoggedIn() {
  return !!getToken()
}
