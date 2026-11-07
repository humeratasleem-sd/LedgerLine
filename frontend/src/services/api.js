import axios from 'axios'

let accessToken = null

export const apiClient = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080',
  timeout: 5000,
  headers: {
    Accept: 'application/json',
  },
})

export function setAccessToken(token) {
  accessToken = token
}

apiClient.interceptors.request.use((config) => {
  if (accessToken) {
    config.headers.Authorization = `Bearer ${accessToken}`
  }
  return config
})

apiClient.interceptors.response.use(
  (response) => response,
  (error) => {
    const status = error.response?.status
    if (status === 401) {
      setAccessToken(null)
      window.dispatchEvent(new CustomEvent('ledgerline:unauthorized'))
    } else if (status === 403) {
      window.dispatchEvent(new CustomEvent('ledgerline:forbidden'))
    }
    return Promise.reject(error)
  },
)
