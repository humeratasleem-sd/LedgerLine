import test from 'node:test'
import assert from 'node:assert/strict'
import { formatPaise } from './money.js'
import { getIdempotencyAttempt, parseRupeesToPaise } from './transactions.js'

test('rupee input is converted to integer paise without rounding', () => {
  assert.equal(parseRupeesToPaise('0.01'), 1)
  assert.equal(parseRupeesToPaise('19'), 1900)
  assert.equal(parseRupeesToPaise('123.4'), 12340)
  assert.equal(parseRupeesToPaise('123.45'), 12345)
})

test('rupee input rejects zero, negatives, excess precision, and unsafe JSON integers', () => {
  for (const value of ['0', '0.00', '-1', '1.001', '01.00', '1e3', '90071992547409.92']) {
    assert.throws(() => parseRupeesToPaise(value), { name: 'Error' })
  }
})

test('paise formatting preserves integer precision for large balances', () => {
  assert.equal(formatPaise('9007199254740991', 'INR'), 'INR 9,00,71,99,25,47,409.91')
})

test('uncertain retries reuse the idempotency key until request details change', () => {
  let keyNumber = 0
  const createKey = () => `key-${++keyNumber}`
  const request = { sourceAccountId: 1, destinationAccountId: 2, amountPaise: 100 }
  const first = getIdempotencyAttempt(null, request, createKey)
  const retry = getIdempotencyAttempt(first, request, createKey)
  const changed = getIdempotencyAttempt(retry, { ...request, amountPaise: 200 }, createKey)

  assert.equal(retry.key, first.key)
  assert.equal(changed.key, 'key-2')
  assert.equal(keyNumber, 2)
})
