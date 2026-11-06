import { Landmark } from 'lucide-react'
import UnavailablePage from '../components/UnavailablePage.jsx'

function Accounts() {
  return (
    <UnavailablePage
      eyebrow="ACCOUNT DIRECTORY"
      title="Accounts"
      description="Account information is not connected yet. No balances or account details are shown in this preview."
      icon={Landmark}
    />
  )
}

export default Accounts
