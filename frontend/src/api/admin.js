import { request } from './http'

export function listAdminUsers() {
  return request('/api/v1/admin/users')
}

export function listAdminRequirements() {
  return request('/api/v1/admin/requirements')
}

export function listAdminOrders() {
  return request('/api/v1/admin/orders')
}

export function cancelAdminRequirement(reqId) {
  return request(`/api/v1/admin/requirements/${reqId}/cancel`, {
    method: 'PUT'
  })
}

export function updateAdminUser(userId, data) {
  return request(`/api/v1/admin/users/${userId}`, {
    method: 'PUT',
    body: JSON.stringify(data)
  })
}

export function deleteAdminUser(userId) {
  return request(`/api/v1/admin/users/${userId}`, {
    method: 'DELETE'
  })
}
