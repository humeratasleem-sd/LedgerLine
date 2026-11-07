export function formatPaise(balancePaise, currency) {
  const amount = BigInt(balancePaise)
  const rupees = amount / 100n
  const paise = (amount % 100n).toString().padStart(2, '0')
  const formattedRupees = new Intl.NumberFormat('en-IN').format(rupees)
  return `${currency} ${formattedRupees}.${paise}`
}

export function groupBalancesByCurrency(accounts) {
  return accounts.reduce((totals, account) => {
    const balance = BigInt(account.balancePaise)
    totals.set(account.currency, (totals.get(account.currency) || 0n) + balance)
    return totals
  }, new Map())
}
