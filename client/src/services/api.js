import axios from 'axios'

const API_BASE_URL = import.meta.env.VITE_API_URL || 'http://localhost:8080'

const api = axios.create({
  baseURL: API_BASE_URL,
  timeout: 30000,
  headers: {
    'Content-Type': 'application/json'
  }
})

api.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('token')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => {
    return Promise.reject(error)
  }
)

api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      localStorage.removeItem('token')
      localStorage.removeItem('user')
      window.location.href = '/login'
    }
    return Promise.reject(error)
  }
)

export const authApi = {
  login: (credentials) => api.post('/api/auth/login', credentials),
  register: (userData) => api.post('/api/auth/register', userData),
  getCurrentUser: () => api.get('/api/auth/me')
}

export const documentApi = {
  getBySpace: (spaceId) => api.get(`/api/documents/space/${spaceId}`),
  getById: (id) => api.get(`/api/documents/${id}`),
  create: (data) => api.post('/api/documents', data),
  update: (id, data) => api.put(`/api/documents/${id}`, data),
  autoSave: (id, data) => api.post(`/api/documents/${id}/auto-save`, data),
  restoreDraft: (id) => api.post(`/api/documents/${id}/restore-draft`),
  moveToTrash: (id) => api.post(`/api/documents/${id}/trash`),
  restoreFromTrash: (id) => api.post(`/api/documents/${id}/restore`),
  deletePermanently: (id) => api.delete(`/api/documents/${id}`),
  getTrash: (spaceId) => api.get(`/api/documents/space/${spaceId}/trash`),
  rollback: (id, version) => api.post(`/api/documents/${id}/rollback?version=${version}`),
  search: (spaceId, keyword) => api.get(`/api/documents/search?spaceId=${spaceId}&keyword=${encodeURIComponent(keyword)}`),
  addTag: (documentId, tagId) => api.post(`/api/documents/${documentId}/tags/${tagId}`),
  removeTag: (documentId, tagId) => api.delete(`/api/documents/${documentId}/tags/${tagId}`)
}

export const versionApi = {
  getByDocument: (documentId) => api.get(`/api/documents/${documentId}/versions`),
  getByVersion: (documentId, version) => api.get(`/api/documents/${documentId}/versions/${version}`)
}

export const spaceApi = {
  getAll: () => api.get('/api/spaces'),
  getById: (id) => api.get(`/api/spaces/${id}`),
  create: (data) => api.post('/api/spaces', data),
  update: (id, data) => api.put(`/api/spaces/${id}`, data),
  delete: (id) => api.delete(`/api/spaces/${id}`),
  getMembers: (spaceId) => api.get(`/api/spaces/${spaceId}/members`),
  addMember: (spaceId, data) => api.post(`/api/spaces/${spaceId}/members`, data),
  removeMember: (spaceId, userId) => api.delete(`/api/spaces/${spaceId}/members/${userId}`),
  updateMemberRole: (spaceId, userId, role) => api.put(`/api/spaces/${spaceId}/members/${userId}?role=${role}`)
}

export const tagApi = {
  getBySpace: (spaceId) => api.get(`/api/tags/space/${spaceId}`),
  create: (spaceId, data) => api.post(`/api/tags?spaceId=${spaceId}`, data),
  update: (id, data) => api.put(`/api/tags/${id}`, data),
  delete: (id) => api.delete(`/api/tags/${id}`)
}

export const commentApi = {
  getByDocument: (documentId) => api.get(`/api/comments/document/${documentId}`),
  create: (data) => api.post('/api/comments', data),
  update: (id, data) => api.put(`/api/comments/${id}`, data),
  delete: (id) => api.delete(`/api/comments/${id}`)
}

export const shareApi = {
  getByDocument: (documentId) => api.get(`/api/shares/document/${documentId}`),
  create: (data) => api.post('/api/shares', data),
  toggle: (id) => api.put(`/api/shares/${id}/toggle`),
  delete: (id) => api.delete(`/api/shares/${id}`),
  access: (data) => api.post('/api/shares/access', data)
}

export default api
