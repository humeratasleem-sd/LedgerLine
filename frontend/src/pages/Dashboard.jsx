import { useEffect, useMemo, useState } from 'react'
import { ArrowUpRight, CircleHelp, FileClock, Landmark, ScanLine } from 'lucide-react'
import { Link } from 'react-router-dom'
import { apiClient } from '../services/api.js'
import { formatPaise, groupBalancesByCurrency } from '../utils/money.js'
import { formatDateTime, getApiError } from '../utils/transactions.js'

function Dashboard() {
  const [accounts, setAccounts] = useState([])
  const [transactions, setTransactions] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [activityError, setActivityError] = useState('')

  useEffect(() => {
    let active = true
    async function loadOverview() {
      try {
        const { data } = await apiClient.get('/api/accounts')
        if (!Array.isArray(data)) throw new Error('The accounts response was not a list.')
        if (!active) return
        setAccounts(data)
        const results = await Promise.allSettled(
          data.map((account) => apiClient.get(`/api/accounts/${encodeURIComponent(account.id)}/transactions?limit=50`)),
        )
        if (!active) return
        const failed = results.some((result) => result.status === 'rejected')
        const records = results.flatMap((result) => (
          result.status === 'fulfilled' && Array.isArray(result.value.data) ? result.value.data : []
        ))
        records.sort((left, right) => new Date(right.createdAt).getTime() - new Date(left.createdAt).getTime())
        setTransactions(records.slice(0, 8))
        if (failed) setActivityError('Some account activity could not be loaded.')
      } catch (requestError) {
        if (active) setError(getApiError(requestError, 'Authorized account data could not be loaded.'))
      } finally {
        if (active) setLoading(false)
      }
    }
    loadOverview()
    return () => { active = false }
  }, [])

  const balanceTotals = useMemo(() => groupBalancesByCurrency(accounts), [accounts])
  const balanceValue = loading
    ? 'Loading…'
    : error
      ? 'Unavailable'
      : balanceTotals.size === 1
        ? formatPaise([...balanceTotals.values()][0].toString(), [...balanceTotals.keys()][0])
        : balanceTotals.size > 1
          ? 'Multiple currencies'
          : '—'
  const cards = [
    { label: 'Authorized balances', value: balanceValue, detail: error || `${accounts.length} authorized account${accounts.length === 1 ? '' : 's'}`, icon: Landmark },
    { label: 'Recent records', value: loading ? 'Loading…' : String(transactions.length), detail: activityError || 'Latest returned activity across your accounts', icon: FileClock },
    { label: 'Accounts in scope', value: loading ? 'Loading…' : String(accounts.length), detail: 'Only accounts returned for your profile appear here.', icon: ArrowUpRight },
  ]

  return (
    <div className="dashboard-page">
      <section className="page-intro" aria-labelledby="overview-title">
        <div className="section-eyebrow"><span className="eyebrow-line" />YOUR FINANCIAL OVERVIEW</div>
        <div className="intro-row">
          <div>
            <h1 id="overview-title">A clearer view<br className="title-break" /> of your money.</h1>
            <p className="intro-description">Only account balances and transaction records returned for your access appear in this workspace.</p>
          </div>
          <div className="preview-stamp">
            <ScanLine size={16} aria-hidden="true" />
            <span>CONNECTED DATA<br /><strong>{loading ? 'CHECKING ACCESS' : error ? 'DATA UNAVAILABLE' : accounts.length ? 'AUTHORIZED DATA' : 'NO ACCOUNTS'}</strong></span>
          </div>
        </div>
      </section>

      <section className="overview-section" aria-label="Account overview">
        <div className="section-heading">
          <div><span className="section-caption">AT A GLANCE</span><h2>Overview</h2></div>
          <span className="data-note" role={error ? 'alert' : 'status'}>
            <CircleHelp size={15} aria-hidden="true" />
            {error ? 'Account data unavailable' : loading ? 'Loading accounts' : `${accounts.length} authorized account${accounts.length === 1 ? '' : 's'}`}
          </span>
        </div>
        {error && <div className="auth-error" role="alert">{error}</div>}
        <div className="overview-grid">
          {cards.map(({ label, detail, icon: Icon, value }) => (
            <article className="overview-card" key={label}>
              <div className="overview-card-top"><span className="metric-label">{label}</span><Icon size={17} strokeWidth={1.7} aria-hidden="true" /></div>
              <div className="metric-value">{value}</div>
              <p>{detail}</p>
            </article>
          ))}
        </div>
      </section>

      <section className="activity-section" aria-labelledby="activity-title">
        <div className="section-heading activity-heading">
          <div><span className="section-caption">THE LEDGER</span><h2 id="activity-title">Recent activity</h2></div>
          <Link className="text-link" to="/statements">View statements</Link>
        </div>
        {loading && <p className="account-message" role="status">Loading authorized activity…</p>}
        {!loading && error && <div className="account-empty"><h2>Activity unavailable</h2><p>Account access could not be confirmed, so no ledger records are shown.</p></div>}
        {!loading && !error && activityError && <p className="auth-error" role="alert">{activityError}</p>}
        {!loading && !error && accounts.length === 0 && (
          <div className="account-empty"><h2>No accounts assigned</h2><p>No activity can be shown until an account is assigned to your profile.</p></div>
        )}
        {!loading && !error && accounts.length > 0 && (
          <TransactionTableForOverview transactions={transactions} accounts={accounts} />
        )}
      </section>
      <footer className="dashboard-footer"><span>LedgerLine workspace</span><span>Financial details are supplied by connected services only.</span></footer>
    </div>
  )
}

function TransactionTableForOverview({ transactions, accounts }) {
  if (!transactions.length) {
    return <div className="account-empty"><h2>No recent activity</h2><p>No transaction records were returned for your authorized accounts.</p></div>
  }
  return (
    <div className="compact-activity-list">
      {transactions.map((transaction) => {
        const account = accounts.find((item) => String(item.id) === String(transaction.sourceAccountId || transaction.destinationAccountId))
        let amount = 'Unavailable'
        try { amount = formatPaise(transaction.amountPaise, transaction.currency) } catch { /* Keep malformed records visible. */ }
        return (
          <article className="compact-activity-row" key={transaction.id}>
            <div className="activity-date"><span>{formatDateTime(transaction.createdAt)}</span><strong>{transaction.transactionType || 'Transaction'}</strong></div>
            <div className="activity-account">{account?.accountNumber || 'Authorized account'}<span className={`status-tag status-${String(transaction.status).toLowerCase()}`}>{transaction.status || 'Unknown'}</span></div>
            <strong className="activity-amount">{amount}</strong>
          </article>
        )
      })}
    </div>
  )
}

export default Dashboard
