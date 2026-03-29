const MAX_STORAGE_ITEMS = 10;

export function sanitizeTextInput(value: unknown, maxLength = 100) {
  return String(value || '')
    .replace(/[\u0000-\u001F\u007F]/g, ' ')
    .replace(/[<>"'`\\]/g, '')
    .replace(/\s+/g, ' ')
    .trim()
    .slice(0, maxLength);
}

export function sanitizePhoneInput(value: unknown) {
  return String(value || '')
    .replace(/\D/g, '')
    .slice(0, 11);
}

export function sanitizeStorageText(value: unknown, maxLength = 50) {
  return sanitizeTextInput(value, maxLength);
}

export function sanitizeStorageList(value: unknown, maxLength = 50) {
  if (!Array.isArray(value)) {
    return [];
  }

  return Array.from(
    new Set(
      value
        .map((item) => sanitizeStorageText(item, maxLength))
        .filter(Boolean)
    )
  ).slice(0, MAX_STORAGE_ITEMS);
}
