import { useEffect, useState } from 'react'
import { apiClient } from '../services/api.js'

function HealthStatus() {
  const [status, setStatus] = useState('loading')

  useEffect(() => {
    let active = true

    apiClient.get('/actuator/health')
      .then(() => {
        if (active) setStatus('reachable')
      })
      .catch(() => {
        if (active) setStatus('unavailable')
      })

    return () => {
      active = false
    }
  }, [])

  const labels = {
    loading: 'Checking service',
    reachable: 'Service reachable',
    unavailable: 'Service unavailable',
  }

  return (
    <div
      className={`health-status health-${status}`}
      role="status"
      aria-live="polite"
      aria-label={`Backend ${labels[status].toLowerCase()}`}
    >
      <span className="health-dot" aria-hidden="true" />
      <span>{labels[status]}</span>
    </div>
  )
}

export default HealthStatus
