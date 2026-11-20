import { useLanguage } from '../i18n.jsx';

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
        <option value="en">{t('English')}</option>
        <option value="el">{t('Greek')}</option>
      </select>
    </label>
  );
}
