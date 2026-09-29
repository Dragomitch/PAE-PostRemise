/**
 * Locales the application is built for (see `i18n` in angular.json). Each one is served under
 * its own sub-path (`/fr/`, `/en/`). Labels are endonyms and intentionally not translated.
 */
export interface SupportedLocale {
  code: string;
  label: string;
}

export const SUPPORTED_LOCALES: readonly SupportedLocale[] = [
  { code: 'fr', label: 'Français' },
  { code: 'en', label: 'English' },
];

export const DEFAULT_LOCALE = 'fr';

/** `en-US` -> `en`; unsupported languages fall back to the default locale. */
export function toSupportedLanguage(localeId: string): string {
  const language = localeId.split(/[-_]/)[0].toLowerCase();
  return SUPPORTED_LOCALES.some((l) => l.code === language) ? language : DEFAULT_LOCALE;
}
