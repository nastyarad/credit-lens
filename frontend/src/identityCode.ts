const PERSONAL_IDENTITY_CODE_PATTERN = /^[0-9]{6}[+\-A-FYXWVU][0-9]{3}[0-9A-FHJ-NPR-Y]$/

export function normalizePersonalIdentityCode(value: string) {
  return value.trim().toUpperCase()
}

export function validatePersonalIdentityCode(value: string) {
  const normalized = normalizePersonalIdentityCode(value)
  if (!normalized) return 'Enter a Finnish personal identity code.'
  if (!PERSONAL_IDENTITY_CODE_PATTERN.test(normalized)) {
    return 'Enter a valid Finnish personal identity code.'
  }
  return ''
}
