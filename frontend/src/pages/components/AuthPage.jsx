import { WalletCards } from 'lucide-react'
import { Link } from 'react-router-dom'

function AuthPage({ eyebrow, title, description, children }) {
  return (
    <main className="auth-screen">
      <section className="auth-panel" aria-labelledby="auth-title">
        <Link className="brand auth-brand" to="/" aria-label="LedgerLine home">
          <span className="brand-mark" aria-hidden="true"><WalletCards size={21} strokeWidth={1.8} /></span>
          <span className="brand-name">Ledger<span>Line</span></span>
        </Link>
        <div className="section-eyebrow"><span className="eyebrow-line" />{eyebrow}</div>
        <h1 id="auth-title">{title}</h1>
        <p className="auth-description">{description}</p>
        {children}
        <p className="auth-security-note">Access tokens remain in memory and are cleared when you sign out or close this page.</p>
      </section>
    </main>
  )
}

export default AuthPage
