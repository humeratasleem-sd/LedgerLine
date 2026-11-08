import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom'
import { AuthProvider } from './auth/AuthContext.jsx'
import { ProtectedRoute, PublicOnlyRoute, RoleRoute } from './auth/ProtectedRoute.jsx'
import AppShell from './components/AppShell.jsx'
import Accounts from './pages/Accounts.jsx'
import Admin from './pages/Admin.jsx'
import Audit from './pages/Audit.jsx'
import Dashboard from './pages/Dashboard.jsx'
import Login from './pages/Login.jsx'
import Profile from './pages/Profile.jsx'
import Register from './pages/Register.jsx'
import Statements from './pages/Statements.jsx'
import Transfers from './pages/Transfers.jsx'

function App() {
  return (
    <AuthProvider>
      <BrowserRouter>
        <Routes>
          <Route path="login" element={<PublicOnlyRoute><Login /></PublicOnlyRoute>} />
          <Route path="register" element={<PublicOnlyRoute><Register /></PublicOnlyRoute>} />
          <Route element={<ProtectedRoute />}>
            <Route element={<AppShell />}>
              <Route index element={<Dashboard />} />
              <Route path="accounts" element={<Accounts />} />
              <Route element={<RoleRoute roles={['ADMIN']} />}>
                <Route path="admin" element={<Admin />} />
              </Route>
              <Route path="statements" element={<Statements />} />
              <Route path="profile" element={<Profile />} />
              <Route element={<RoleRoute roles={['ACCOUNT_HOLDER']} />}>
                <Route path="transfers" element={<Transfers />} />
              </Route>
              <Route element={<RoleRoute roles={['ADMIN', 'AUDITOR']} />}>
                <Route path="audit" element={<Audit />} />
              </Route>
            </Route>
          </Route>
          <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
      </BrowserRouter>
    </AuthProvider>
  )
}

export default App
