import { useCallback, useEffect, useState } from 'react'
import { Landmark, RefreshCw } from 'lucide-react'
import { Link } from 'react-router-dom'
import { apiClient } from '../services/api.js'
import { useAuth } from '../auth/useAuth.js'
import { formatPaise } from '../utils/money.js'
import { getApiError } from '../utils/transactions.js'

function Accounts() {
  const { user } = useAuth()
  const isAdmin = user.role === 'ADMIN'
  const [accounts, setAccounts] = useState([])
  const [users, setUsers] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [workingId, setWorkingId] = useState('')
  const [notice, setNotice] = useState('')
  const [formState, setFormState] = useState({})

  const loadData = useCallback(async () => {
    try {
      if (isAdmin) {
        const [accountResponse, userResponse] = await Promise.all([
          apiClient.get('/api/admin/accounts'),
          apiClient.get('/api/admin/users'),
        ])
        if (!Array.isArray(accountResponse.data) || !Array.isArray(userResponse.data)) {
          throw new Error('The administration response was not a list.')
        }
        setAccounts(accountResponse.data)
        setUsers(userResponse.data)
      } else {
        const { data } = await apiClient.get('/api/accounts')
        if (!Array.isArray(data)) throw new Error('The accounts response was not a list.')
        setAccounts(data)
      }
    } catch (requestError) {
      setError(getApiError(requestError, 'Accounts could not be loaded from LedgerLine.'))
    } finally {
      setLoading(false)
    }
  }, [isAdmin])

  useEffect(() => { Promise.resolve().then(loadData) }, [loadData])

  async function updateAccountStatus(accountId, status) {
    setWorkingId(String(accountId))
    setNotice('')
    setError('')
    try {
      await apiClient.patch(`/api/admin/accounts/${encodeURIComponent(accountId)}/status`, { status })
      setNotice(`Account status updated to ${status}.`)
      await loadData()
    } catch (requestError) {
      setError(getApiError(requestError, 'Account status could not be updated.'))
    } finally {
      setWorkingId('')
    }
  }

  async function updatePermission(accountId) {
    const form = formState[accountId]
    if (!form?.userId || !form?.permission) {
      setError('Select a user and permission before saving.')
      return
    }
    setWorkingId(String(accountId))
    setNotice('')
    setError('')
    try {
      await apiClient.put(`/api/admin/accounts/${encodeURIComponent(accountId)}/permissions`, {
        userId: Number(form.userId),
        permission: form.permission,
      })
      setNotice('Account permission updated.')
      await loadData()
    } catch (requestError) {
      setError(getApiError(requestError, 'The permission could not be updated.'))
    } finally {
      setWorkingId('')
    }
  }

  return (
    <section className="coming-page" aria-labelledby="accounts-title">
      <div className="section-eyebrow"><span className="eyebrow-line" />ACCOUNT DIRECTORY</div>
      <div className="account-page-heading">
        <div><h1 id="accounts-title">Accounts</h1><p>{isAdmin ? 'Review account records and manage lifecycle and access.' : 'Only accounts assigned to your profile are shown.'}</p></div>
        <span className="coming-icon" aria-hidden="true"><Landmark size={22} strokeWidth={1.7} /></span>
      </div>
      {isAdmin && <div className="admin-shortcut"><span>Account provisioning and cash posting</span><Link className="text-link" to="/admin">Open administration →</Link></div>}
      {loading && <p className="account-message" role="status">Loading {isAdmin ? 'administrative' : 'authorized'} accounts…</p>}
      {error && <p className="auth-error account-error" role="alert">{error}</p>}
      {notice && <p className="success-message" role="status">{notice}</p>}
      {!loading && !error && accounts.length === 0 && (
        <div className="account-empty"><h2>No accounts assigned</h2><p>{isAdmin ? 'There are no account records available from the administration service.' : 'There are no accounts authorized for your profile. LedgerLine does not display sample balances.'}</p></div>
      )}
      {!loading && !error && accounts.length > 0 && (
        <div className="account-grid">
          {accounts.map((account) => {
            const form = formState[account.id] || {}
            const busy = workingId === String(account.id)
            return (
              <article className="account-card" key={account.id}>
                <div className="account-card-heading"><span>{account.currency} ACCOUNT</span><span className="account-status">{account.status}</span></div>
                <h2>{account.accountNumber}</h2>
                <dl>
                  <div><dt>Ledger balance</dt><dd>{formatPaise(account.balancePaise, account.currency)}</dd></div>
                  <div><dt>Created</dt><dd>{new Date(account.createdAt).toLocaleDateString()}</dd></div>
                  {!isAdmin && account.permission && <div><dt>Your permission</dt><dd>{account.permission}</dd></div>}
                </dl>
                {isAdmin && (
                  <div className="account-management">
                    <label className="field-label" htmlFor={`account-status-${account.id}`}>Lifecycle status</label>
                    <div className="inline-form">
                      <select id={`account-status-${account.id}`} value={form.status ?? account.status} disabled={busy} onChange={(event) => setFormState((old) => ({ ...old, [account.id]: { ...old[account.id], status: event.target.value } }))}>
                        {[...new Set(['ACTIVE', 'FROZEN', 'CLOSED', account.status])].map((status) => <option key={status} value={status}>{status}</option>)}
                      </select>
                      <button className="secondary-button" type="button" disabled={busy || (form.status ?? account.status) === account.status} onClick={() => updateAccountStatus(account.id, form.status ?? account.status)}>
                        {busy ? 'Saving…' : 'Update'}
                      </button>
                    </div>
                    <label className="field-label" htmlFor={`permission-user-${account.id}`}>Assign account access</label>
                    <div className="inline-form permission-form">
                      <select id={`permission-user-${account.id}`} value={form.userId || ''} disabled={busy} onChange={(event) => setFormState((old) => ({ ...old, [account.id]: { ...old[account.id], userId: event.target.value } }))}>
                        <option value="">Choose user</option>
                        {users.filter((item) => item.accountStatus === 'ACTIVE').map((item) => <option key={item.id} value={item.id}>{item.fullName} · {item.email}</option>)}
                      </select>
                      <select aria-label={`Permission for ${account.accountNumber}`} value={form.permission || 'VIEW'} disabled={busy} onChange={(event) => setFormState((old) => ({ ...old, [account.id]: { ...old[account.id], permission: event.target.value } }))}>
                        <option value="VIEW">View</option><option value="TRANSFER">Transfer</option><option value="OWNER">Owner</option>
                      </select>
                      <button className="secondary-button" type="button" disabled={busy || !form.userId} onClick={() => updatePermission(account.id)}>{busy ? 'Saving…' : 'Assign'}</button>
                    </div>
                  </div>
                )}
              </article>
            )
          })}
        </div>
      )}
      {!loading && isAdmin && <button className="quiet-button refresh-button" type="button" onClick={() => { setLoading(true); setError(''); loadData() }} disabled={loading}><RefreshCw size={14} />Refresh records</button>}
    </section>
  )
}

export default Accounts
