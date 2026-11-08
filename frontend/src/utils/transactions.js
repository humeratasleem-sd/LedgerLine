export function parseRupeesToPaise(value) {
  const normalized = value.trim()
  if (!/^(?:0|[1-9]\d*)(?:\.\d{1,2})?$/.test(normalized)) {
    throw new Error('Enter a positive amount with up to two decimal places.')
  }
  const [rupees, fraction = ''] = normalized.split('.')
  const paise = BigInt(rupees) * 100n + BigInt(fraction.padEnd(2, '0') || '0')
  if (paise <= 0n) throw new Error('Amount must be greater than zero.')
  if (paise > BigInt(Number.MAX_SAFE_INTEGER)) {
    throw new Error('Amount exceeds the supported precise entry limit.')
  }
  return Number(paise)
}

export function createIdempotencyKey() {
  if (globalThis.crypto?.randomUUID) return globalThis.crypto.randomUUID()
  if (!globalThis.crypto?.getRandomValues) {
    throw new Error('A secure browser context is required to submit this transaction.')
  }
  const bytes = globalThis.crypto.getRandomValues(new Uint8Array(16))
  bytes[6] = (bytes[6] & 0x0f) | 0x40
  bytes[8] = (bytes[8] & 0x3f) | 0x80
  const hex = [...bytes].map((byte) => byte.toString(16).padStart(2, '0')).join('')
  return `${hex.slice(0, 8)}-${hex.slice(8, 12)}-${hex.slice(12, 16)}-${hex.slice(16, 20)}-${hex.slice(20)}`
}

export function getIdempotencyAttempt(currentAttempt, request, createKey = createIdempotencyKey) {
  const fingerprint = JSON.stringify(request)
  if (currentAttempt?.fingerprint === fingerprint) return currentAttempt
  return { fingerprint, key: createKey() }
}

export function getApiError(error, fallback) {
  const problem = error.response?.data
  if (typeof problem?.detail === 'string') return problem.detail
  if (typeof problem?.message === 'string') return problem.message
  if (typeof problem?.title === 'string') return problem.title
  return fallback
}

export function formatDateTime(value) {
  const date = new Date(value)
  return Number.isNaN(date.getTime()) ? 'Date unavailable' : date.toLocaleString()
}
