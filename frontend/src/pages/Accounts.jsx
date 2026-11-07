import { useEffect, useState } from 'react'
import { Landmark } from 'lucide-react'
import { apiClient } from '../services/api.js'
import { formatPaise } from '../utils/money.js'

function Accounts() {
  const [accounts, setAccounts] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  useEffect(() => {
    let active = true
    apiClient.get('/api/accounts')
      .then(({ data }) => {
        if (active) setAccounts(data)
      })
      .catch((requestError) => {
        if (active) {
          setError(requestError.response?.data?.detail || 'Accounts could not be loaded from LedgerLine.')
        }
      })
      .finally(() => {
        if (active) setLoading(false)
      })
    return () => {
      active = false
    }
  }, [])

  return (
    <section className="coming-page" aria-labelledby="accounts-title">
      <div className="section-eyebrow"><span className="eyebrow-line" />ACCOUNT DIRECTORY</div>
      <div className="account-page-heading">
        <div>
          <h1 id="accounts-title">Accounts</h1>
          <p>Only accounts assigned to your profile are shown.</p>
        </div>
        <span className="coming-icon" aria-hidden="true"><Landmark size={22} strokeWidth={1.7} /></span>
      </div>

      {loading && <p className="account-message" role="status">Loading authorized accounts…</p>}
      {error && <p className="auth-error account-error" role="alert">{error}</p>}
      {!loading && !error && accounts.length === 0 && (
        <div className="account-empty">
          <h2>No accounts assigned</h2>
          <p>There are no accounts authorized for your profile. LedgerLine does not display sample balances.</p>
        </div>
      )}
      {!error && accounts.length > 0 && (
        <div className="account-grid">
          {accounts.map((account) => (
            <article className="account-card" key={account.id}>
              <div className="account-card-heading">
                <span>{account.currency} ACCOUNT</span>
                <span className="account-status">{account.status}</span>
              </div>
              <h2>{account.accountNumber}</h2>
              <dl>
                <div>
                  <dt>Available balance</dt>
                  <dd>{formatPaise(account.balancePaise, account.currency)}</dd>
                </div>
                <div>
                  <dt>Created</dt>
                  <dd>{new Date(account.createdAt).toLocaleDateString()}</dd>
                </div>
              </dl>
            </article>
          ))}
        </div>
      )}
    </section>
  )
}

export default Accounts
