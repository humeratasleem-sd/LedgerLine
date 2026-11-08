import { formatPaise } from '../utils/money.js'
import { formatDateTime } from '../utils/transactions.js'

function TransactionTable({ transactions, accounts, emptyMessage = 'No transactions found for this account.' }) {
  const accountNumbers = new Map(accounts.map((account) => [String(account.id), account.accountNumber]))

  if (!transactions.length) {
    return <div className="account-empty"><h2>No activity yet</h2><p>{emptyMessage}</p></div>
  }

  return (
    <div className="table-scroll">
      <table className="data-table">
        <thead>
          <tr>
            <th scope="col">Date</th>
            <th scope="col">Activity</th>
            <th scope="col">Accounts</th>
            <th scope="col">Status</th>
            <th scope="col" className="numeric-cell">Amount</th>
          </tr>
        </thead>
        <tbody>
          {transactions.map((transaction) => {
            const from = accountNumbers.get(String(transaction.sourceAccountId))
            const to = accountNumbers.get(String(transaction.destinationAccountId))
            let amount = 'Unavailable'
            try {
              amount = formatPaise(transaction.amountPaise, transaction.currency)
            } catch {
              // Keep a malformed API record visible without taking down the ledger.
            }
            return (
              <tr key={transaction.id}>
                <td><time dateTime={transaction.createdAt}>{formatDateTime(transaction.createdAt)}</time></td>
                <td>
                  <strong>{transaction.transactionType || 'Transaction'}</strong>
                  {transaction.failureCode && <span className="table-subtext">Reason: {transaction.failureCode}</span>}
                </td>
                <td>
                  <span>{from ? `From ${from}` : transaction.sourceAccountId ? 'From another account' : '—'}</span>
                  {transaction.destinationAccountId && (
                    <span className="table-subtext">{to ? `To ${to}` : 'To another account'}</span>
                  )}
                </td>
                <td><span className={`status-tag status-${String(transaction.status).toLowerCase()}`}>{transaction.status || 'Unknown'}</span></td>
                <td className="numeric-cell">{amount}</td>
              </tr>
            )
          })}
        </tbody>
      </table>
    </div>
  )
}

export default TransactionTable
