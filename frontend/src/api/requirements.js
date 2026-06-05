import { request } from './http'

export function listRequirements(params) {
  if (Array.isArray(params.type)) {
    const types = params.type
    return Promise.all(types.map((type) => listRequirements({ ...params, type }))).then((pages) => {
      const merged = new Map()
      pages.forEach((page) => {
        ;(page.list || []).forEach((item) => merged.set(item.reqId, item))
      })
      return {
        ...(pages[0] || {}),
        total: pages.reduce((sum, page) => sum + Number(page.total || 0), 0),
        list: [...merged.values()].sort((a, b) => String(b.createdAt || '').localeCompare(String(a.createdAt || '')))
      }
    })
  }
  const search = new URLSearchParams()
  Object.entries(params).forEach(([key, value]) => {
    if (value !== undefined && value !== null && value !== '') {
      search.append(key, value)
    }
  })
  return request(`/v1/requirements?${search.toString()}`)
}

export function recommendRequirements(params = {}) {
  const search = new URLSearchParams()
  Object.entries(params).forEach(([key, value]) => {
    if (value !== undefined && value !== null && value !== '') {
      search.append(key, value)
    }
  })
  return request(`/v1/requirements/recommendations?${search.toString()}`)
}

export function getRequirement(reqId) {
  return request(`/v1/requirements/${reqId}`)
}

export function createRequirement(data) {
  return request('/v1/requirements', {
    method: 'POST',
    body: JSON.stringify(data)
  })
}

export function acceptOrder(reqId) {
  return request('/v1/orders', {
    method: 'POST',
    body: JSON.stringify({ reqId })
  })
}
