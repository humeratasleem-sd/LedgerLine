import { FileText } from 'lucide-react'
import UnavailablePage from '../components/UnavailablePage.jsx'

function Statements() {
  return (
    <UnavailablePage
      eyebrow="ACCOUNT RECORDS"
      title="Statements"
      description="Statements will be available when a real account data integration is in place."
      icon={FileText}
    />
  )
}

export default Statements
