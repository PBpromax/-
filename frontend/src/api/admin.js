import { request } from './http'

export function listAdminUsers() {
  return request('/v1/admin/users')
}

export function listAdminRequirements() {
  return request('/v1/admin/requirements')
}

export function listAdminOrders() {
  return request('/v1/admin/orders')
}

export function cancelAdminRequirement(reqId) {
  return request(`/v1/admin/requirements/${reqId}/cancel`, {
    method: 'PUT'
  })
}

export function updateAdminUser(userId, data) {
  return request(`/v1/admin/users/${userId}`, {
    method: 'PUT',
    body: JSON.stringify(data)
  })
}

export function deleteAdminUser(userId) {
  return request(`/v1/admin/users/${userId}`, {
    method: 'DELETE'
  })
}
