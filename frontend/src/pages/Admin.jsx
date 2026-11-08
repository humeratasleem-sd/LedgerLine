import { useCallback, useEffect, useState } from 'react'
import { BadgeCheck, CircleDollarSign, RefreshCw, Users } from 'lucide-react'
import { apiClient } from '../services/api.js'
import { useAuth } from '../auth/useAuth.js'
import { formatPaise } from '../utils/money.js'
import { createIdempotencyKey, getApiError, parseRupeesToPaise } from '../utils/transactions.js'

function Admin() {
  const { user: signedInUser } = useAuth()
  const [accounts, setAccounts] = useState([])
  const [users, setUsers] = useState([])
  const [accountsError, setAccountsError] = useState('')
  const [usersError, setUsersError] = useState('')
  const [loading, setLoading] = useState(true)
  const [working, setWorking] = useState('')
  const [notice, setNotice] = useState('')
  const [error, setError] = useState('')
  const [accountForm, setAccountForm] = useState({ accountNumber: '', currency: 'INR', ownerUserId: '' })
  const [accountFormBusy, setAccountFormBusy] = useState(false)
  const [posting, setPosting] = useState({
    kind: 'deposit',
    accountId: '',
    amount: '',
    externalReference: '',
    attested: false,
  })
  const [postingBusy, setPostingBusy] = useState(false)
  const [postingResult, setPostingResult] = useState(null)
  const [userEdits, setUserEdits] = useState({})

  const loadData = useCallback(async () => {
    const [accountResult, userResult] = await Promise.allSettled([
      apiClient.get('/api/admin/accounts'),
      apiClient.get('/api/admin/users'),
    ])
    if (accountResult.status === 'fulfilled' && Array.isArray(accountResult.value.data)) {
      setAccounts(accountResult.value.data)
    } else {
      setAccountsError(accountResult.status === 'rejected'
        ? getApiError(accountResult.reason, 'Administrative accounts could not be loaded.')
        : 'The administrative accounts response was not a list.')
    }
    if (userResult.status === 'fulfilled' && Array.isArray(userResult.value.data)) {
      setUsers(userResult.value.data)
    } else {
      setUsersError(userResult.status === 'rejected'
        ? getApiError(userResult.reason, 'Users could not be loaded.')
        : 'The users response was not a list.')
    }
    setLoading(false)
  }, [])

  useEffect(() => { Promise.resolve().then(loadData) }, [loadData])

  const activeUsers = users.filter((item) => item.accountStatus === 'ACTIVE')
  const activeAccounts = accounts.filter((item) => item.status === 'ACTIVE' && item.currency === 'INR')

  async function createAccount(event) {
    event.preventDefault()
    setError('')
    setNotice('')
    setAccountFormBusy(true)
    try {
      await apiClient.post('/api/admin/accounts', {
        accountNumber: accountForm.accountNumber.trim(),
        currency: accountForm.currency.trim().toUpperCase(),
        ownerUserId: Number(accountForm.ownerUserId),
      })
      setNotice('Account created. The new record and balance are shown only after they are returned by the service.')
      setAccountForm({ accountNumber: '', currency: 'INR', ownerUserId: '' })
      await loadData()
    } catch (requestError) {
      setError(getApiError(requestError, 'Account could not be created.'))
    } finally {
      setAccountFormBusy(false)
    }
  }

  async function updateUser(item) {
    const edit = userEdits[item.id]
    if (!edit) return
    const status = edit.accountStatus ?? item.accountStatus
    const role = edit.role ?? item.role
    setWorking(String(item.id))
    setError('')
    setNotice('')
    try {
      if (status !== item.accountStatus) {
        await apiClient.patch(`/api/admin/users/${encodeURIComponent(item.id)}/status`, { accountStatus: status })
      }
      if (role !== item.role) {
        await apiClient.patch(`/api/admin/users/${encodeURIComponent(item.id)}/role`, { role })
      }
      setNotice(`Profile updated for ${item.fullName}.`)
      await loadData()
    } catch (requestError) {
      setError(getApiError(requestError, 'User profile could not be updated.'))
    } finally {
      setWorking('')
    }
  }

  async function postOperationalEntry(event) {
    event.preventDefault()
    setError('')
    setNotice('')
    setPostingResult(null)
    if (!posting.attested) {
      setError('Confirm the external settlement attestation before posting.')
      return
    }
    let amountPaise
    try {
      amountPaise = parseRupeesToPaise(posting.amount)
    } catch (validationError) {
      setError(validationError.message)
      return
    }
    if (!posting.accountId) {
      setError('Select an active account returned by the administration service.')
      return
    }
    const externalReference = posting.externalReference.trim()
    if (!/^[A-Za-z0-9][A-Za-z0-9._:/-]{0,127}$/.test(externalReference)) {
      setError('Enter an external reference of 1–128 characters, starting with a letter or number and using only letters, numbers, ., _, :, /, or -.')
      return
    }
    setPostingBusy(true)
    try {
      const isDeposit = posting.kind === 'deposit'
      const path = isDeposit ? '/api/admin/transactions/deposits' : '/api/admin/transactions/withdrawals'
      const accountField = isDeposit ? 'destinationAccountId' : 'sourceAccountId'
      const { data } = await apiClient.post(path, {
        [accountField]: Number(posting.accountId),
        amountPaise,
        externalReference,
        externalSettlementVerified: true,
      }, {
        headers: { 'Idempotency-Key': createIdempotencyKey() },
      })
      setPostingResult({ kind: 'success', transaction: data })
      setNotice(`${isDeposit ? 'Deposit' : 'Withdrawal'} posted as an admin-attested external settlement.`)
      await loadData()
    } catch (requestError) {
      if (requestError.response?.status === 422) {
        setPostingResult({ kind: 'refused', transaction: requestError.response.data })
        setError(getApiError(requestError, 'The operation was refused and recorded by the service.'))
      } else {
        setError(getApiError(requestError, 'The operational posting could not be completed. Verify the ledger before making another intentional submission.'))
      }
    } finally {
      setPostingBusy(false)
    }
  }

  const accountsForOptions = activeAccounts

  return (
    <section className="admin-page" aria-labelledby="admin-title">
      <div className="section-eyebrow"><span className="eyebrow-line" />ADMINISTRATIVE WORKSPACE</div>
      <div className="admin-page-heading">
        <div><h1 id="admin-title">Administration</h1><p>Manage users and account access; operational postings require an explicit settlement attestation.</p></div>
        <button className="quiet-button" type="button" onClick={() => { setLoading(true); setAccountsError(''); setUsersError(''); loadData() }} disabled={loading}><RefreshCw size={14} />Refresh</button>
      </div>

      {error && <p className="auth-error" role="alert">{error}</p>}
      {notice && <p className="success-message" role="status">{notice}</p>}
      {loading && <p className="account-message" role="status">Loading administrative records…</p>}

      <section className="admin-section" aria-labelledby="users-title">
        <div className="admin-section-heading"><div><span className="section-caption">ACCESS CONTROL</span><h2 id="users-title">Users</h2></div><Users size={19} aria-hidden="true" /></div>
        {usersError && <p className="auth-error" role="alert">{usersError}</p>}
        {!loading && !usersError && users.length === 0 && <div className="account-empty"><h2>No user records</h2><p>No user profiles were returned by the administration service.</p></div>}
        {!usersError && users.length > 0 && (
          <div className="admin-user-list">
            {users.map((item) => {
              const edit = userEdits[item.id] || {}
              const status = edit.accountStatus ?? item.accountStatus
              const role = edit.role ?? item.role
              const changed = status !== item.accountStatus || role !== item.role
              const busy = working === String(item.id)
              return (
                <article className="admin-user-row" key={item.id}>
                  <div className="admin-user-identity"><strong>{item.fullName}</strong><span>{item.email}</span><small>{item.id === signedInUser.id ? 'You' : `User ${item.id}`}</small></div>
                  <label className="field-label">Status
                    <select value={status} disabled={busy} onChange={(event) => setUserEdits((old) => ({ ...old, [item.id]: { ...old[item.id], accountStatus: event.target.value } }))}>
                      <option value="ACTIVE">Active</option><option value="DISABLED">Disabled</option>
                    </select>
                  </label>
                  <label className="field-label">Role
                    <select value={role} disabled={busy} onChange={(event) => setUserEdits((old) => ({ ...old, [item.id]: { ...old[item.id], role: event.target.value } }))}>
                      <option value="ACCOUNT_HOLDER">Account holder</option><option value="ADMIN">Admin</option><option value="AUDITOR">Auditor</option>
                    </select>
                  </label>
                  <button className="secondary-button" type="button" disabled={busy || !changed || (item.id === signedInUser.id && status !== item.accountStatus)} onClick={() => updateUser(item)}>
                    {busy ? 'Saving…' : 'Save'}
                  </button>
                </article>
              )
            })}
          </div>
        )}
      </section>

      <section className="admin-section" aria-labelledby="create-account-title">
        <div className="admin-section-heading"><div><span className="section-caption">ACCOUNT PROVISIONING</span><h2 id="create-account-title">Create an account</h2></div><BadgeCheck size={19} aria-hidden="true" /></div>
        <p className="section-description">Creation requests are sent to the service; balances and resulting account state are never assumed in the interface.</p>
        <form className="admin-form" onSubmit={createAccount}>
          <label className="field-label">Account number
            <input required maxLength={34} pattern="[A-Za-z0-9-]+" value={accountForm.accountNumber} onChange={(event) => setAccountForm((old) => ({ ...old, accountNumber: event.target.value }))} />
          </label>
          <label className="field-label">Currency
            <input required minLength={3} maxLength={3} pattern="[A-Za-z]{3}" value={accountForm.currency} onChange={(event) => setAccountForm((old) => ({ ...old, currency: event.target.value.toUpperCase() }))} />
          </label>
          <label className="field-label">Owner
            <select required value={accountForm.ownerUserId} onChange={(event) => setAccountForm((old) => ({ ...old, ownerUserId: event.target.value }))}>
              <option value="">Select an active user</option>
              {activeUsers.map((item) => <option key={item.id} value={item.id}>{item.fullName} · {item.email}</option>)}
            </select>
          </label>
          <button className="primary-button" type="submit" disabled={accountFormBusy || !activeUsers.length}>{accountFormBusy ? 'Creating…' : 'Create account'}</button>
        </form>
        {!usersError && !loading && !activeUsers.length && <p className="form-hint">No active user is available to assign as owner.</p>}
      </section>

      <section className="admin-section settlement-section" aria-labelledby="settlement-title">
        <div className="admin-section-heading"><div><span className="section-caption">OPERATIONAL POSTING</span><h2 id="settlement-title">Deposit or withdrawal</h2></div><CircleDollarSign size={19} aria-hidden="true" /></div>
        <div className="settlement-attestation">
          <strong>Admin-attested external settlement only</strong>
          <p>This is a manual ledger posting for receipt or payout settlement verified outside LedgerLine. It does not initiate, receive, or process a payment through a bank, card network, or other payment rail.</p>
        </div>
        {accountsError && <p className="auth-error" role="alert">{accountsError}</p>}
        <form className="admin-form operational-form" onSubmit={postOperationalEntry}>
          <label className="field-label">Posting type
            <select value={posting.kind} disabled={postingBusy} onChange={(event) => setPosting((old) => ({ ...old, kind: event.target.value, accountId: '' }))}>
              <option value="deposit">Deposit</option><option value="withdrawal">Withdrawal</option>
            </select>
          </label>
          <label className="field-label">{posting.kind === 'deposit' ? 'Destination account' : 'Source account'}
            <select required value={posting.accountId} disabled={postingBusy || !accountsForOptions.length} onChange={(event) => setPosting((old) => ({ ...old, accountId: event.target.value }))}>
              <option value="">Select an active account</option>
              {accountsForOptions.map((item) => <option key={item.id} value={item.id}>{item.accountNumber} · {item.currency}</option>)}
            </select>
          </label>
          <label className="field-label">Amount (INR)
            <input required inputMode="decimal" autoComplete="off" placeholder="0.00" value={posting.amount} disabled={postingBusy} onChange={(event) => setPosting((old) => ({ ...old, amount: event.target.value }))} />
            <span className="form-hint">Enter rupees with up to two decimal places. Stored as integer paise.</span>
          </label>
          <label className="field-label">External reference
            <input
              required
              minLength={1}
              maxLength={128}
              pattern="[A-Za-z0-9][A-Za-z0-9._:/-]{0,127}"
              autoComplete="off"
              value={posting.externalReference}
              disabled={postingBusy}
              onChange={(event) => setPosting((old) => ({ ...old, externalReference: event.target.value }))}
              aria-describedby="external-reference-guidance"
            />
            <span className="form-hint" id="external-reference-guidance">Required. 1–128 characters: letters, numbers, dot, underscore, colon, slash, or hyphen.</span>
          </label>
          <label className="attestation-check">
            <input type="checkbox" checked={posting.attested} disabled={postingBusy} onChange={(event) => setPosting((old) => ({ ...old, attested: event.target.checked }))} />
            <span>I verified the external receipt or payout and authorize this manual ledger posting. This does not process a payment.</span>
          </label>
          <button className="primary-button" type="submit" disabled={postingBusy || !posting.attested || !accountsForOptions.length}>
            {postingBusy ? 'Posting…' : posting.kind === 'deposit' ? 'Record deposit' : 'Record withdrawal'}
          </button>
        </form>
        {!accountsError && !loading && !activeAccounts.length && <p className="form-hint">No active account is available for operational posting.</p>}
        {postingResult && <PostingResult result={postingResult} />}
      </section>
    </section>
  )
}

function PostingResult({ result }) {
  const transaction = result.transaction
  let amount = ''
  try {
    if (transaction?.amountPaise != null) amount = formatPaise(transaction.amountPaise, transaction.currency)
  } catch {
    amount = ''
  }
  return (
    <div className={result.kind === 'success' ? 'success-message posting-result' : 'auth-error posting-result'} role={result.kind === 'success' ? 'status' : 'alert'}>
      <strong>{result.kind === 'success' ? 'Service accepted the posting' : 'Service refused the posting'}</strong>
      {transaction?.status && <span>Status: {transaction.status}</span>}
      {amount && <span>Amount: {amount}</span>}
      {transaction?.failureCode && <span>Reason: {transaction.failureCode}</span>}
    </div>
  )
}

export default Admin
