import { BadgeCheck } from 'lucide-react'
import UnavailablePage from '../components/UnavailablePage.jsx'

function Audit() {
  return (
    <UnavailablePage
      eyebrow="RECORD HISTORY"
      title="Audit trail"
      description="There is no audit data connected to this preview. No permissions or security states are implied."
      icon={BadgeCheck}
    />
  )
}

export default Audit
