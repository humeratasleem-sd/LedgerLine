import { useCallback, useEffect, useMemo, useState } from 'react'
import { apiClient, setAccessToken } from '../services/api.js'
import { AuthContext } from './authContext.js'

export function AuthProvider({ children }) {
  const [user, setUser] = useState(null)
  const [forbiddenNotice, setForbiddenNotice] = useState('')
  const clearForbiddenNotice = useCallback(() => setForbiddenNotice(''), [])
  const login = useCallback(async (email, password) => {
    const { data } = await apiClient.post('/api/auth/login', { email, password })
    setAccessToken(data.accessToken)
    setUser(data.user)
    setForbiddenNotice('')
    return data.user
  }, [])
  const register = useCallback(async (fullName, email, password) => {
    const { data } = await apiClient.post('/api/auth/register', { fullName, email, password })
    setAccessToken(data.accessToken)
    setUser(data.user)
    setForbiddenNotice('')
    return data.user
  }, [])
  const logout = useCallback(() => {
    setAccessToken(null)
    setUser(null)
    setForbiddenNotice('')
  }, [])

  useEffect(() => {
    const clearSession = () => {
      setAccessToken(null)
      setUser(null)
    }
    const showForbiddenNotice = () => {
      setForbiddenNotice('You do not have permission to access that resource.')
    }
    window.addEventListener('ledgerline:unauthorized', clearSession)
    window.addEventListener('ledgerline:forbidden', showForbiddenNotice)
    return () => {
      window.removeEventListener('ledgerline:unauthorized', clearSession)
      window.removeEventListener('ledgerline:forbidden', showForbiddenNotice)
    }
  }, [])

  const value = useMemo(() => ({
    user,
    forbiddenNotice,
    clearForbiddenNotice,
    login,
    register,
    logout,
    updateUser: setUser,
  }), [user, forbiddenNotice, clearForbiddenNotice, login, register, logout])

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}
