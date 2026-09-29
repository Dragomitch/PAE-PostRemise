import { DEFAULT_LOCALE, SUPPORTED_LOCALES, toSupportedLanguage } from './locales';

describe('locales', () => {
  it('supports French (default) and English', () => {
    expect(SUPPORTED_LOCALES.map((l) => l.code)).toEqual(['fr', 'en']);
    expect(DEFAULT_LOCALE).toBe('fr');
  });

  it('normalises locale ids to a supported language', () => {
    expect(toSupportedLanguage('fr')).toBe('fr');
    expect(toSupportedLanguage('fr-BE')).toBe('fr');
    expect(toSupportedLanguage('EN_gb')).toBe('en');
    expect(toSupportedLanguage('en-US')).toBe('en');
    expect(toSupportedLanguage('nl')).toBe('fr');
  });
});
