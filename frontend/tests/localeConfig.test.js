import test from 'node:test';
import assert from 'node:assert/strict';
import i18next from 'i18next';
import en from '../src/locales/en/translation.json' with { type: 'json' };
import el from '../src/locales/el/translation.json' with { type: 'json' };
import de from '../src/locales/de/translation.json' with { type: 'json' };
import nl from '../src/locales/nl/translation.json' with { type: 'json' };
import da from '../src/locales/da/translation.json' with { type: 'json' };
import fr from '../src/locales/fr/translation.json' with { type: 'json' };
import { interpolationOptions } from '../src/i18nConfig.js';
import {
  defaultLanguage,
  getLocale,
  resolveLanguage,
  supportedLanguages,
} from '../src/localeConfig.js';

test('supports English, Greek, German, Dutch, Danish, and French locales', () => {
  assert.deepEqual(
    supportedLanguages.map(({ code }) => code),
    ['en', 'el', 'de', 'nl', 'da', 'fr'],
  );
  assert.equal(defaultLanguage, 'en');
});

test('falls back to English for unsupported language codes', () => {
  assert.equal(resolveLanguage('fr'), 'fr');
  assert.equal(resolveLanguage('unknown'), 'en');
  assert.equal(resolveLanguage(undefined), 'en');
});

test('maps language codes to locale-aware date and currency formats', () => {
  assert.equal(getLocale('en'), 'en-GB');
  assert.equal(getLocale('el'), 'el-GR');
  assert.equal(getLocale('de'), 'de-DE');
  assert.equal(getLocale('nl'), 'nl-NL');
  assert.equal(getLocale('da'), 'da-DK');
  assert.equal(getLocale('fr'), 'fr-FR');
  assert.equal(getLocale('unknown'), 'en-GB');
});

test('interpolates every dynamic translation in all supported languages', async () => {
  const instance = i18next.createInstance();
  await instance.init({
    lng: defaultLanguage,
    fallbackLng: 'en',
    resources: {
      en: { translation: en },
      el: { translation: el },
      de: { translation: de },
      nl: { translation: nl },
      da: { translation: da },
      fr: { translation: fr },
    },
    interpolation: interpolationOptions,
    initImmediate: false,
  });

  const interpolatedEntries = Object.entries(en).filter(([, value]) => /\{\w+\}/.test(value));
  for (const { code } of supportedLanguages) {
    await instance.changeLanguage(code);
    for (const [key, value] of interpolatedEntries) {
      const placeholders = [...value.matchAll(/\{(\w+)\}/g)].map(([, name]) => name);
      const options = Object.fromEntries(placeholders.map((name) => [
        name,
        name === 'count' ? 7 : `${name}-value`,
      ]));
      const translated = instance.t(key, options);
      assert.doesNotMatch(translated, /\{\w+\}/, `${code}: ${key}`);
    }
  }
});
