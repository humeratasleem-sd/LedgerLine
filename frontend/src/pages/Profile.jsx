import { useEffect, useState } from 'react'
import { apiClient } from '../services/api.js'
import { useAuth } from '../auth/useAuth.js'

function Profile() {
  const { user, updateUser } = useAuth()
  const [error, setError] = useState('')

  useEffect(() => {
    let active = true
    apiClient.get('/api/auth/me')
      .then(({ data }) => {
        if (active) updateUser(data)
      })
      .catch(() => {
        if (active) setError('Your profile could not be refreshed. Sign in again if your session has expired.')
      })
    return () => {
      active = false
    }
  }, [updateUser])

  if (!user) return null

  return (
    <section className="coming-page" aria-labelledby="profile-title">
      <div className="section-eyebrow"><span className="eyebrow-line" />YOUR PROFILE</div>
      <div className="profile-card">
        <h1 id="profile-title">Account details</h1>
        {error && <p className="auth-error" role="alert">{error}</p>}
        <dl className="profile-details">
          <div><dt>Full name</dt><dd>{user.fullName}</dd></div>
          <div><dt>Email</dt><dd>{user.email}</dd></div>
          <div><dt>Role</dt><dd>{user.role}</dd></div>
          <div><dt>Account status</dt><dd>{user.accountStatus}</dd></div>
          <div><dt>Member since</dt><dd>{new Date(user.createdAt).toLocaleDateString()}</dd></div>
        </dl>
      </div>
    </section>
  )
}

export default Profile
