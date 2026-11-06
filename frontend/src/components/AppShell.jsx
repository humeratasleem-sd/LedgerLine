import {
  Activity,
  ArrowLeftRight,
  BadgeCheck,
  FileText,
  LayoutDashboard,
  Landmark,
  WalletCards,
} from 'lucide-react'
import { NavLink, Outlet, useLocation } from 'react-router-dom'
import HealthStatus from './HealthStatus.jsx'

const navigation = [
  { label: 'Overview', path: '/', icon: LayoutDashboard },
  { label: 'Accounts', path: '/accounts', icon: Landmark },
  { label: 'Transfers', path: '/transfers', icon: ArrowLeftRight },
  { label: 'Statements', path: '/statements', icon: FileText },
  { label: 'Audit trail', path: '/audit', icon: BadgeCheck },
]

const pageNames = {
  '/': 'Overview',
  '/accounts': 'Accounts',
  '/transfers': 'Transfers',
  '/statements': 'Statements',
  '/audit': 'Audit trail',
}

function AppShell() {
  const { pathname } = useLocation()
  const pageName = pageNames[pathname] || 'Overview'

  return (
    <div className="app-frame">
      <aside className="sidebar" aria-label="Primary">
        <NavLink className="brand" to="/" aria-label="LedgerLine home">
          <span className="brand-mark" aria-hidden="true">
            <WalletCards size={21} strokeWidth={1.8} />
          </span>
          <span className="brand-name">Ledger<span>Line</span></span>
        </NavLink>

        <div className="workspace-label">WORKSPACE</div>
        <nav className="side-nav" aria-label="Main navigation">
          {navigation.map(({ label, path, icon: Icon }) => (
            <NavLink
              key={path}
              to={path}
              end={path === '/'}
              className={({ isActive }) => `nav-link${isActive ? ' is-active' : ''}`}
            >
              <Icon size={18} strokeWidth={1.8} aria-hidden="true" />
              <span>{label}</span>
            </NavLink>
          ))}
        </nav>

        <div className="sidebar-foot">
          <div className="integrity-note">
            <span className="integrity-icon"><Activity size={16} aria-hidden="true" /></span>
            <div>
              <strong>Built for clarity</strong>
              <p>Only connected data belongs here.</p>
            </div>
          </div>
          <div className="sidebar-version">LEDGERLINE <span>·</span> PREVIEW</div>
        </div>
      </aside>

      <div className="workspace">
        <header className="topbar">
          <div className="topbar-context">
            <span className="context-kicker">WORKSPACE</span>
            <span className="context-divider" aria-hidden="true">/</span>
            <span className="context-current">{pageName}</span>
          </div>
          <div className="topbar-tools">
            <HealthStatus />
            <div className="profile-placeholder" aria-label="Preview workspace, no signed-in user">
              <span className="profile-avatar" aria-hidden="true">LL</span>
              <span className="profile-label">Preview workspace</span>
            </div>
          </div>
        </header>

        <main className="main-content" id="main-content">
          <Outlet />
        </main>
      </div>

      <nav className="mobile-nav" aria-label="Mobile navigation">
        {navigation.map(({ label, path, icon: Icon }) => (
          <NavLink
            key={path}
            to={path}
            end={path === '/'}
            aria-label={label}
            className={({ isActive }) => `mobile-nav-link${isActive ? ' is-active' : ''}`}
          >
            <Icon size={19} strokeWidth={1.8} aria-hidden="true" />
            <span>{label === 'Overview' ? 'Home' : label === 'Audit trail' ? 'Audit' : label}</span>
          </NavLink>
        ))}
      </nav>
    </div>
  )
}

export default AppShell
