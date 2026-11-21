import { createContext, useContext, useEffect, useMemo, useState } from 'react';
import i18n from 'i18next';
import { initReactI18next, useTranslation } from 'react-i18next';
import en from './locales/en/translation.json';
import el from './locales/el/translation.json';
import de from './locales/de/translation.json';
import nl from './locales/nl/translation.json';
import da from './locales/da/translation.json';
import fr from './locales/fr/translation.json';
import { interpolationOptions } from './i18nConfig.js';
import {
  defaultLanguage,
  getLocale,
  languageStorageKey,
  resolveLanguage,
  supportedLanguages,
} from './localeConfig.js';

i18n.use(initReactI18next).init({
  resources: {
    en: { translation: en },
    el: { translation: el },
    de: { translation: de },
    nl: { translation: nl },
    da: { translation: da },
    fr: { translation: fr },
  },
  lng: getStoredLanguage(),
  fallbackLng: defaultLanguage,
  supportedLngs: supportedLanguages.map(({ code }) => code),
  interpolation: interpolationOptions,
  returnNull: false,
  initImmediate: false,
});

function getStoredLanguage() {
  try {
    return resolveLanguage(window.localStorage.getItem(languageStorageKey));
  } catch {
    return defaultLanguage;
  }
}

const LanguageContext = createContext(null);

export function LanguageProvider({ children }) {
  const [language, setLanguageState] = useState(getStoredLanguage);
  useEffect(() => {
    i18n.changeLanguage(language);
    document.documentElement.lang = language;
  }, [language]);
  const value = useMemo(() => ({
    language,
    locale: getLocale(language),
    setLanguage(nextLanguage) {
      const resolved = resolveLanguage(nextLanguage);
      if (resolved !== nextLanguage) return;
      try {
        window.localStorage.setItem(languageStorageKey, resolved);
      } catch {
        // The current page still uses the selected language without persistence.
      }
      setLanguageState(resolved);
      i18n.changeLanguage(resolved);
    },
  }), [language]);
  return <LanguageContext.Provider value={value}>{children}</LanguageContext.Provider>;
}

export function useLanguage() {
  const context = useContext(LanguageContext);
  if (!context) throw new Error('useLanguage must be used within LanguageProvider');
  const { t } = useTranslation();
  return { ...context, t };
}

export function formatMoney(value, locale) {
  return new Intl.NumberFormat(locale, { style: 'currency', currency: 'EUR' }).format(value);
}
