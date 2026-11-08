import { useEffect, useState } from 'react'
import { BadgeCheck } from 'lucide-react'
import { apiClient } from '../services/api.js'

function Audit() {
  const [events, setEvents] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  useEffect(() => {
    let active = true
    apiClient.get('/api/audit/events?limit=50')
      .then(({ data }) => {
        if (active) setEvents(data)
      })
      .catch((requestError) => {
        if (active) setError(requestError.response?.data?.detail || 'Audit events could not be loaded.')
      })
      .finally(() => {
        if (active) setLoading(false)
      })
    return () => {
      active = false
    }
  }, [])

  return (
    <section className="coming-page" aria-labelledby="audit-title">
      <div className="section-eyebrow"><span className="eyebrow-line" />RECORD HISTORY</div>
      <div className="audit-heading">
        <div>
          <h1 id="audit-title">Audit trail</h1>
          <p>Administrative changes are recorded for authorized read-only review.</p>
        </div>
        <p className="read-only-note">Read-only view · available to administrators and auditors.</p>
        <span className="coming-icon" aria-hidden="true"><BadgeCheck size={22} strokeWidth={1.7} /></span>
      </div>
      {loading && <p className="account-message" role="status">Loading audit events…</p>}
      {error && <p className="auth-error account-error" role="alert">{error}</p>}
      {!loading && !error && events.length === 0 && (
        <div className="account-empty">
          <h2>No audit events recorded</h2>
          <p>Authorized audit records will appear here when administrative changes occur.</p>
        </div>
      )}
      {!error && events.length > 0 && (
        <div className="audit-list">
          {events.map((event) => (
            <article className="audit-event" key={event.id}>
              <div className="audit-event-primary">
                <strong>{event.action}</strong>
                <span>{event.entityType}{event.entityId ? ` · ${event.entityId}` : ''}</span>
              </div>
              <div className="audit-event-meta">
                <span>Actor {event.actorUserId ?? 'system'}</span>
                <time dateTime={event.createdAt}>{new Date(event.createdAt).toLocaleString()}</time>
              </div>
            </article>
          ))}
        </div>
      )}
    </section>
  )
}

export default Audit
