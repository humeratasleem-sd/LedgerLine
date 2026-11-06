import { ArrowLeftRight } from 'lucide-react'
import UnavailablePage from '../components/UnavailablePage.jsx'

function Transfers() {
  return (
    <UnavailablePage
      eyebrow="PAYMENT ACTIVITY"
      title="Transfers"
      description="Transfers are not available in this preview. No payment forms or financial actions are enabled."
      icon={ArrowLeftRight}
    />
  )
}

export default Transfers
