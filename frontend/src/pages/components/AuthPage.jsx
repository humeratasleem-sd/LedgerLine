import { WalletCards } from 'lucide-react'
import { Link } from 'react-router-dom'

function AuthPage({ eyebrow, title, description, children }) {
  return (
    <main className="auth-screen">
      <div className={`auth-layout${eyebrow === 'CREATE YOUR ACCOUNT' ? ' auth-layout-register' : ''}`}>
        <aside className="auth-editorial" aria-label="About LedgerLine">
          <Link className="brand auth-brand" to="/" aria-label="LedgerLine home">
            <span className="brand-mark" aria-hidden="true"><WalletCards size={21} strokeWidth={1.8} /></span>
            <span className="brand-name">Ledger<span>Line</span></span>
          </Link>
          <div className="auth-editorial-copy">
            <span className="auth-editorial-kicker">A CLEARER KIND OF LEDGER</span>
            <p>Every record<br />in its place.</p>
            <div className="auth-ruled-motif" aria-hidden="true">
              <span />
              <span />
              <span />
              <span />
            </div>
            <span className="auth-editorial-note">Your authorized account data, clearly presented. No sample balances. No imagined activity.</span>
          </div>
          <div className="auth-editorial-footer"><span>LEDGERLINE WORKSPACE</span><span>PRIVATE BY DESIGN</span></div>
        </aside>
        <section className="auth-panel" aria-labelledby="auth-title">
          <div className="section-eyebrow"><span className="eyebrow-line" />{eyebrow}</div>
          <h1 id="auth-title">{title}</h1>
          <p className="auth-description">{description}</p>
          {children}
          <p className="auth-security-note">Access tokens remain in memory and are cleared when you sign out or close this page.</p>
        </section>
      </div>
    </main>
  )
}

export default AuthPage
