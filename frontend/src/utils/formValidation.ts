export function getEmailError(email: string): string | null {
  const normalizedEmail = email.trim()
  if (!normalizedEmail) {
    return 'Enter your email address.'
  }
  if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(normalizedEmail)) {
    return 'Enter a valid email address.'
  }
  return null
}

/** Sign-in accepts a full email address or the username (its local part). */
export function getLoginIdentifierError(value: string): string | null {
  const identifier = value.trim()
  if (!identifier) {
    return 'Enter your username or email address.'
  }
  if (identifier.includes('@')) {
    return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(identifier)
      ? null
      : 'Enter a valid email address, or your username.'
  }
  if (!/^[a-zA-Z0-9._-]{1,64}$/.test(identifier)) {
    return 'Use a username with letters, numbers, dots, dashes or underscores, or a full email address.'
  }
  return null
}

export function getPasswordError(password: string): string | null {
  if (!password) {
    return 'Enter your password.'
  }
  if (password.length < 8) {
    return 'Use at least 8 characters for your password.'
  }
  return null
}
