import { useEffect, useState } from 'react'
import { ArrowUpRight, CircleHelp, FileClock, Landmark, ScanLine } from 'lucide-react'
import { apiClient } from '../services/api.js'
import { formatPaise, groupBalancesByCurrency } from '../utils/money.js'

function Dashboard() {
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
        if (active) setError(requestError.response?.data?.detail || 'Account data could not be loaded.')
      })
      .finally(() => {
        if (active) setLoading(false)
      })
    return () => {
      active = false
    }
  }, [])

  const balanceTotals = groupBalancesByCurrency(accounts)
  const balanceValue = loading
    ? 'Loading…'
    : error
      ? 'Unavailable'
      : balanceTotals.size === 1
        ? formatPaise([...balanceTotals.values()][0].toString(), [...balanceTotals.keys()][0])
        : balanceTotals.size > 1
          ? 'Multiple currencies'
          : '—'
  const overviewItems = [
    {
      label: 'Available balance',
      value: balanceValue,
      detail: error
        ? error
        : accounts.length
          ? `Across ${accounts.length} authorized account${accounts.length === 1 ? '' : 's'}.`
          : 'No authorized accounts are connected.',
      icon: Landmark,
    },
    {
      label: 'Money in',
      value: '—',
      detail: 'Transaction activity is not connected yet.',
      icon: ArrowUpRight,
    },
    {
      label: 'Money out',
      value: '—',
      detail: 'Transaction activity is not connected yet.',
      icon: FileClock,
    },
  ]

  return (
    <div className="dashboard-page">
      <section className="page-intro" aria-labelledby="overview-title">
        <div className="section-eyebrow"><span className="eyebrow-line" />YOUR FINANCIAL OVERVIEW</div>
        <div className="intro-row">
          <div>
            <h1 id="overview-title">A clearer view<br className="title-break" /> of your money.</h1>
            <p className="intro-description">
              Authorized account balances appear here. Transaction activity will be available when ledger APIs are connected.
            </p>
          </div>
          <div className="preview-stamp">
            <ScanLine size={16} aria-hidden="true" />
            <span>PREVIEW<br /><strong>
              {loading ? 'CHECKING ACCESS' : error ? 'DATA UNAVAILABLE' : accounts.length ? 'AUTHORIZED DATA' : 'NO ACCOUNT DATA'}
            </strong></span>
          </div>
        </div>
      </section>

      <section className="overview-section" aria-label="Account overview">
        <div className="section-heading">
          <div>
            <span className="section-caption">AT A GLANCE</span>
            <h2>Overview</h2>
          </div>
          <span className="data-note" role={error ? 'alert' : undefined}>
            <CircleHelp size={15} aria-hidden="true" />
            {error ? 'Account data unavailable' : loading ? 'Loading accounts' : `${accounts.length} authorized account${accounts.length === 1 ? '' : 's'}`}
          </span>
        </div>
        <div className="overview-grid">
          {overviewItems.map(({ label, detail, icon: Icon, value }, index) => (
            <article className={`overview-card overview-card-${index + 1}`} key={label}>
              <div className="overview-card-top">
                <span className="metric-label">{label}</span>
                <Icon size={17} strokeWidth={1.7} aria-hidden="true" />
              </div>
              <div className="metric-value">{value}</div>
              <p>{detail}</p>
            </article>
          ))}
        </div>
      </section>

      <section className="activity-section" aria-labelledby="activity-title">
        <div className="section-heading activity-heading">
          <div>
            <span className="section-caption">THE LEDGER</span>
            <h2 id="activity-title">Recent activity</h2>
          </div>
          <span className="activity-period">LATEST</span>
        </div>

        <div className="empty-ledger">
          <div className="ledger-motif" aria-hidden="true">
            <div className="ledger-motif-head"><span>DATE</span><span>DESCRIPTION</span><span>AMOUNT</span></div>
            <div className="ledger-rule"><span /><i /><span /></div>
            <div className="ledger-rule"><span /><i /><span /></div>
            <div className="ledger-rule"><span /><i /><span /></div>
            <div className="ledger-rule"><span /><i /><span /></div>
          </div>
          <div className="empty-ledger-copy">
            <span className="empty-ledger-icon"><FileClock size={20} strokeWidth={1.7} aria-hidden="true" /></span>
            <div>
              <h3>Your ledger is ready for real activity.</h3>
              <p>Transaction records are not connected yet. LedgerLine never fills this space with sample financial data.</p>
            </div>
          </div>
          <div className="ledger-foot">
            <span>DATA INTEGRITY</span>
            <span><span className="integrity-dot" /> LIVE RECORDS ONLY</span>
          </div>
        </div>
      </section>
      <footer className="dashboard-footer">
        <span>LedgerLine preview</span>
        <span>Financial details appear only when supplied by a connected service.</span>
      </footer>
    </div>
  )
}

export default Dashboard
