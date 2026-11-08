import { useEffect, useState } from 'react'
import { FileText } from 'lucide-react'
import { apiClient } from '../services/api.js'
import TransactionTable from '../components/TransactionTable.jsx'
import { getApiError } from '../utils/transactions.js'

function Statements() {
  const [accounts, setAccounts] = useState([])
  const [accountId, setAccountId] = useState('')
  const [transactions, setTransactions] = useState([])
  const [accountsLoading, setAccountsLoading] = useState(true)
  const [historyLoading, setHistoryLoading] = useState(false)
  const [accountsError, setAccountsError] = useState('')
  const [historyError, setHistoryError] = useState('')

  useEffect(() => {
    let active = true
    apiClient.get('/api/accounts')
      .then(({ data }) => {
        if (!Array.isArray(data)) throw new Error('The accounts response was not a list.')
        if (!active) return
        setAccounts(data)
        if (data.length) {
          setAccountId(String(data[0].id))
          setHistoryLoading(true)
        }
      })
      .catch((requestError) => {
        if (active) setAccountsError(getApiError(requestError, 'Authorized accounts could not be loaded.'))
      })
      .finally(() => { if (active) setAccountsLoading(false) })
    return () => { active = false }
  }, [])

  useEffect(() => {
    if (!accountId || !accounts.some((account) => String(account.id) === accountId)) {
      return undefined
    }
    let active = true
    apiClient.get(`/api/accounts/${encodeURIComponent(accountId)}/transactions?limit=50`)
      .then(({ data }) => {
        if (!Array.isArray(data)) throw new Error('The statement response was not a list.')
        if (active) setTransactions(data)
      })
      .catch((requestError) => {
        if (active) setHistoryError(getApiError(requestError, 'Transaction history could not be loaded for this account.'))
      })
      .finally(() => { if (active) setHistoryLoading(false) })
    return () => { active = false }
  }, [accountId, accounts])

  return (
    <section className="coming-page transaction-page" aria-labelledby="statements-title">
      <div className="section-eyebrow"><span className="eyebrow-line" />ACCOUNT RECORDS</div>
      <div className="account-page-heading">
        <div><h1 id="statements-title">Statements</h1><p>Review transaction records, including refused attempts, for an authorized account.</p></div>
        <span className="coming-icon" aria-hidden="true"><FileText size={22} strokeWidth={1.7} /></span>
      </div>
      {accountsLoading && <p className="account-message" role="status">Loading authorized accounts…</p>}
      {accountsError && <p className="auth-error account-error" role="alert">{accountsError}</p>}
      {!accountsLoading && !accountsError && accounts.length === 0 && <div className="account-empty"><h2>No accounts assigned</h2><p>Statements are available only for accounts returned as authorized for your profile.</p></div>}
      {!accountsLoading && !accountsError && accounts.length > 0 && (
        <>
          <div className="statement-selector">
            <label className="field-label" htmlFor="statement-account">Account</label>
            <select id="statement-account" value={accountId} onChange={(event) => { setHistoryLoading(true); setHistoryError(''); setAccountId(event.target.value) }}>
              {accounts.map((account) => <option key={account.id} value={account.id}>{account.accountNumber} · {account.currency} · {account.permission || 'Authorized'}</option>)}
            </select>
          </div>
          {historyError && <p className="auth-error account-error" role="alert">{historyError}</p>}
          {historyLoading && <p className="account-message" role="status">Loading account statement…</p>}
          {!historyLoading && !historyError && (
            <TransactionTable transactions={transactions} accounts={accounts} emptyMessage="No transaction records, including refused attempts, were returned for this account." />
          )}
        </>
      )}
    </section>
  )
}

export default Statements
