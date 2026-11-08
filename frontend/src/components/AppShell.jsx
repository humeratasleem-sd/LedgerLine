import {
  Activity,
  BriefcaseBusiness,
  ArrowLeftRight,
  BadgeCheck,
  FileText,
  LayoutDashboard,
  Landmark,
  LogOut,
  WalletCards,
} from 'lucide-react'
import { NavLink, Outlet, useLocation, useNavigate } from 'react-router-dom'
import { useAuth } from '../auth/useAuth.js'
import HealthStatus from './HealthStatus.jsx'

const navigation = [
  { label: 'Overview', path: '/', icon: LayoutDashboard, roles: ['ADMIN', 'ACCOUNT_HOLDER', 'AUDITOR'] },
  { label: 'Accounts', path: '/accounts', icon: Landmark, roles: ['ADMIN', 'ACCOUNT_HOLDER', 'AUDITOR'] },
  { label: 'Transfers', path: '/transfers', icon: ArrowLeftRight, roles: ['ACCOUNT_HOLDER'] },
  { label: 'Statements', path: '/statements', icon: FileText, roles: ['ADMIN', 'ACCOUNT_HOLDER', 'AUDITOR'] },
  { label: 'Audit trail', path: '/audit', icon: BadgeCheck, roles: ['ADMIN', 'AUDITOR'] },
  { label: 'Admin', path: '/admin', icon: BriefcaseBusiness, roles: ['ADMIN'] },
]

const pageNames = {
  '/': 'Overview',
  '/accounts': 'Accounts',
  '/transfers': 'Transfers',
  '/statements': 'Statements',
  '/audit': 'Audit trail',
  '/admin': 'Administration',
  '/profile': 'My profile',
}

function AppShell() {
  const { pathname } = useLocation()
  const navigate = useNavigate()
  const { user, logout, forbiddenNotice, clearForbiddenNotice } = useAuth()
  const pageName = pageNames[pathname] || 'Overview'
  const visibleNavigation = navigation.filter(({ roles }) => roles.includes(user.role))
  const initials = user.fullName.split(/\s+/).slice(0, 2).map((part) => part[0]).join('').toUpperCase()

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
          {visibleNavigation.map(({ label, path, icon: Icon }) => (
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
            <NavLink className="profile-placeholder" to="/profile" aria-label="View your profile">
              <span className="profile-avatar" aria-hidden="true">{initials}</span>
              <span className="profile-label">{user.fullName}</span>
            </NavLink>
            <button
              className="logout-button"
              type="button"
              onClick={() => {
                logout()
                navigate('/login', { replace: true })
              }}
            >
              <LogOut size={15} aria-hidden="true" />
              <span>Sign out</span>
            </button>
          </div>
        </header>

        <main className="main-content" id="main-content">
          {forbiddenNotice && (
            <div className="forbidden-notice" role="alert">
              <span>{forbiddenNotice}</span>
              <button type="button" onClick={clearForbiddenNotice} aria-label="Dismiss access message">Dismiss</button>
            </div>
          )}
          <Outlet />
        </main>
      </div>

      <nav
        className="mobile-nav"
        aria-label="Mobile navigation"
        style={{ gridTemplateColumns: `repeat(${visibleNavigation.length}, minmax(0, 1fr))` }}
      >
        {visibleNavigation.map(({ label, path, icon: Icon }) => (
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
