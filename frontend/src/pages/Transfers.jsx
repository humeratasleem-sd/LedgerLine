import { useCallback, useEffect, useRef, useState } from 'react'
import { ArrowLeftRight } from 'lucide-react'
import { apiClient } from '../services/api.js'
import TransactionTable from '../components/TransactionTable.jsx'
import { formatPaise } from '../utils/money.js'
import { getApiError, getIdempotencyAttempt, parseRupeesToPaise } from '../utils/transactions.js'

function Transfers() {
  const [accounts, setAccounts] = useState([])
  const [transactions, setTransactions] = useState([])
  const [sourceAccountId, setSourceAccountId] = useState('')
  const [destinationAccountId, setDestinationAccountId] = useState('')
  const [amount, setAmount] = useState('')
  const [loading, setLoading] = useState(true)
  const [historyLoading, setHistoryLoading] = useState(false)
  const [submitting, setSubmitting] = useState(false)
  const [error, setError] = useState('')
  const [historyError, setHistoryError] = useState('')
  const [success, setSuccess] = useState('')
  const idempotencyAttempt = useRef(null)

  const loadData = useCallback(async () => {
    try {
      const { data } = await apiClient.get('/api/accounts')
      if (!Array.isArray(data)) throw new Error('The accounts response was not a list.')
      setAccounts(data)
      setHistoryLoading(true)
      const results = await Promise.allSettled(
        data.map((account) => apiClient.get(`/api/accounts/${encodeURIComponent(account.id)}/transactions?limit=50`)),
      )
      const records = results.flatMap((result) => (
        result.status === 'fulfilled' && Array.isArray(result.value.data) ? result.value.data : []
      ))
      records.sort((left, right) => new Date(right.createdAt).getTime() - new Date(left.createdAt).getTime())
      setTransactions(records)
      setHistoryError(results.some((result) => result.status === 'rejected') ? 'Some account history could not be loaded.' : '')
    } catch (requestError) {
      setError(getApiError(requestError, 'Authorized account data could not be loaded.'))
    } finally {
      setLoading(false)
      setHistoryLoading(false)
    }
  }, [])

  useEffect(() => { Promise.resolve().then(loadData) }, [loadData])

  const availableAccounts = accounts.filter((account) => (
    account.currency === 'INR' && ['OWNER', 'TRANSFER'].includes(account.permission)
  ))
  const destinationAccounts = accounts.filter((account) => account.currency === 'INR')
  const sourceAccount = availableAccounts.find((account) => String(account.id) === sourceAccountId)

  async function handleSubmit(event) {
    event.preventDefault()
    setError('')
    setSuccess('')
    const source = availableAccounts.find((item) => String(item.id) === sourceAccountId)
    const destination = accounts.find((item) => String(item.id) === destinationAccountId)
    if (!source || !destination) {
      setError('Choose both accounts from the accounts returned for your profile.')
      return
    }
    if (String(source.id) === String(destination.id)) {
      setError('Source and destination accounts must be different.')
      return
    }
    let amountPaise
    try {
      amountPaise = parseRupeesToPaise(amount)
    } catch (validationError) {
      setError(validationError.message)
      return
    }
    const request = {
      sourceAccountId: Number(source.id),
      destinationAccountId: Number(destination.id),
      amountPaise,
    }
    setSubmitting(true)
    try {
      idempotencyAttempt.current = getIdempotencyAttempt(idempotencyAttempt.current, request)
      const { data } = await apiClient.post('/api/transactions/transfers', request, {
        headers: { 'Idempotency-Key': idempotencyAttempt.current.key },
      })
      idempotencyAttempt.current = null
      let amountLabel = 'the requested amount'
      try {
        if (data?.amountPaise != null) amountLabel = formatPaise(data.amountPaise, data.currency)
      } catch {
        amountLabel = 'the requested amount'
      }
      setSuccess(`Transfer accepted by LedgerLine for ${amountLabel}.`)
      setAmount('')
      setLoading(true)
      await loadData()
    } catch (requestError) {
      const status = requestError.response?.status
      if (status && status < 500) idempotencyAttempt.current = null
      const detail = getApiError(requestError, requestError.response?.status === 422
        ? 'The transfer was refused and recorded by the service.'
        : 'The transfer could not be completed. Check your account activity before trying again.')
      setError(requestError.response?.status === 422 ? `Transfer refused: ${detail}` : detail)
      if (requestError.response?.status === 422) {
        setLoading(true)
        await loadData()
      }
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <section className="coming-page transaction-page" aria-labelledby="transfers-title">
      <div className="section-eyebrow"><span className="eyebrow-line" />ACCOUNT-HOLDER ACTIVITY</div>
      <div className="account-page-heading">
        <div><h1 id="transfers-title">Transfers</h1><p>Move funds between accounts you are authorized to use.</p></div>
        <span className="coming-icon" aria-hidden="true"><ArrowLeftRight size={22} strokeWidth={1.7} /></span>
      </div>
      {loading && <p className="account-message" role="status">Loading authorized accounts…</p>}
      {error && <p className="auth-error account-error" role="alert">{error}</p>}
      {success && <p className="success-message" role="status">{success}</p>}
      {!loading && !error && !availableAccounts.length && (
        <div className="account-empty"><h2>No INR account in scope</h2><p>A source account must be returned by the authorized accounts service. Transfer permissions are checked again by the service when submitted.</p></div>
      )}
      {!error && accounts.length > 0 && (
        <form className="transaction-form" onSubmit={handleSubmit}>
          <label className="field-label">From account
            <select required value={sourceAccountId} disabled={loading || submitting || !availableAccounts.length} onChange={(event) => setSourceAccountId(event.target.value)}>
              <option value="">Select an authorized source</option>
              {availableAccounts.map((account) => <option key={account.id} value={account.id}>{account.accountNumber} · {account.currency}</option>)}
            </select>
            {sourceAccount && <span className="form-hint">Available balance: {formatPaise(sourceAccount.balancePaise, sourceAccount.currency)}</span>}
          </label>
          <label className="field-label">To account
            <select required value={destinationAccountId} disabled={loading || submitting} onChange={(event) => setDestinationAccountId(event.target.value)}>
              <option value="">Select a destination account</option>
              {destinationAccounts.map((account) => <option key={account.id} value={account.id} disabled={String(account.id) === sourceAccountId}>{account.accountNumber} · {account.currency}</option>)}
            </select>
          </label>
          <label className="field-label">Amount (INR)
            <input required inputMode="decimal" autoComplete="off" placeholder="0.00" value={amount} disabled={loading || submitting} onChange={(event) => setAmount(event.target.value)} />
            <span className="form-hint">Positive amount, up to two decimal places. Converted to integer paise without floating-point rounding.</span>
          </label>
          <p className="form-hint form-wide">Transfers are account-to-account ledger instructions. Only the API response confirms acceptance; a refusal will be shown separately.</p>
          <button className="primary-button form-wide" type="submit" disabled={loading || submitting || !sourceAccountId || !destinationAccountId || !amount}>
            {submitting ? 'Submitting transfer…' : 'Review and submit transfer'}
          </button>
        </form>
      )}

      <section className="activity-section" aria-labelledby="transfer-history-title">
        <div className="section-heading"><div><span className="section-caption">RECENT RECORDS</span><h2 id="transfer-history-title">Transfer history</h2></div></div>
        {historyLoading && <p className="account-message" role="status">Loading account history…</p>}
        {historyError && <p className="auth-error" role="alert">{historyError}</p>}
        {!loading && !historyLoading && <TransactionTable transactions={transactions} accounts={accounts} emptyMessage="Transaction history will appear here when returned by the ledger service." />}
      </section>
    </section>
  )
}

export default Transfers
