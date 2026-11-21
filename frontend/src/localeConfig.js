export const supportedLanguages = [
  { code: 'en', name: 'English', locale: 'en-GB' },
  { code: 'el', name: 'Greek', locale: 'el-GR' },
  { code: 'de', name: 'German', locale: 'de-DE' },
  { code: 'nl', name: 'Dutch', locale: 'nl-NL' },
  { code: 'da', name: 'Danish', locale: 'da-DK' },
  { code: 'fr', name: 'French', locale: 'fr-FR' },
];

export const defaultLanguage = 'en';
export const languageStorageKey = 'booking-language';

export function resolveLanguage(value) {
  return supportedLanguages.some(({ code }) => code === value)
    ? value
    : defaultLanguage;
}

export function getLocale(language) {
  return supportedLanguages.find(({ code }) => code === resolveLanguage(language)).locale;
}
