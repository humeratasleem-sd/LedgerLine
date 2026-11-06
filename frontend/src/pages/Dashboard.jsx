import { ArrowUpRight, CircleHelp, FileClock, Landmark, ScanLine } from 'lucide-react'

const overviewItems = [
  {
    label: 'Available balance',
    detail: 'Connect an account to see a verified balance.',
    icon: Landmark,
  },
  {
    label: 'Money in',
    detail: 'Incoming activity will appear when available.',
    icon: ArrowUpRight,
  },
  {
    label: 'Money out',
    detail: 'Outgoing activity will appear when available.',
    icon: FileClock,
  },
]

function Dashboard() {
  return (
    <div className="dashboard-page">
      <section className="page-intro" aria-labelledby="overview-title">
        <div className="section-eyebrow"><span className="eyebrow-line" />YOUR FINANCIAL OVERVIEW</div>
        <div className="intro-row">
          <div>
            <h1 id="overview-title">A clearer view<br className="title-break" /> of your money.</h1>
            <p className="intro-description">
              A considered home for your financial picture. Your overview will take shape once an account is connected.
            </p>
          </div>
          <div className="preview-stamp">
            <ScanLine size={16} aria-hidden="true" />
            <span>PREVIEW<br /><strong>NO ACCOUNT DATA</strong></span>
          </div>
        </div>
      </section>

      <section className="overview-section" aria-label="Account overview">
        <div className="section-heading">
          <div>
            <span className="section-caption">AT A GLANCE</span>
            <h2>Overview</h2>
          </div>
          <span className="data-note"><CircleHelp size={15} aria-hidden="true" /> No connected accounts</span>
        </div>
        <div className="overview-grid">
          {overviewItems.map(({ label, detail, icon: Icon }, index) => (
            <article className={`overview-card overview-card-${index + 1}`} key={label}>
              <div className="overview-card-top">
                <span className="metric-label">{label}</span>
                <Icon size={17} strokeWidth={1.7} aria-hidden="true" />
              </div>
              <div className="metric-value" aria-label="No data available">—</div>
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
              <p>There are no transactions to display. LedgerLine never fills this space with sample financial data.</p>
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
