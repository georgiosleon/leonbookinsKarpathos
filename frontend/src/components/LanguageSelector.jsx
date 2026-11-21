import { useLanguage } from '../i18n.jsx';
import { supportedLanguages } from '../localeConfig.js';

export function LanguageSelector() {
  const { language, setLanguage, t } = useLanguage();

  return (
    <label className="language-selector">
      <span className="visually-hidden">{t('Switch language')}</span>
      <select
        aria-label={t('Switch language')}
        value={language}
        onChange={(event) => setLanguage(event.target.value)}
      >
        {supportedLanguages.map(({ code, name }) => (
          <option key={code} value={code}>{t(name)}</option>
        ))}
      </select>
    </label>
  );
}
