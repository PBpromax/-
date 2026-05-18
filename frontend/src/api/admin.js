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
