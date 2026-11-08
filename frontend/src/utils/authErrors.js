export function getAuthErrorMessage(error, action) {
  const verb = action === 'register' ? 'create your account' : 'sign in'
  const status = error.response?.status
  const detail = error.response?.data?.detail

  if (!error.response) {
    return `LedgerLine could not be reached, so we could not ${verb}. Check your connection and try again.`
  }
  if (status === 401) {
    return 'Email or password could not be verified. Check your details and try again.'
  }
  if (status >= 500) {
    return `The LedgerLine ${action === 'register' ? 'registration' : 'sign-in'} service is temporarily unavailable. Please try again shortly.`
  }
  if (typeof detail === 'string' && detail.trim()) return detail
  return action === 'register'
    ? 'Your account could not be created. Review your details and try again.'
    : 'Unable to sign in. Review your details and try again.'
}
