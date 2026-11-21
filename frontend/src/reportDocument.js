import { resolveLanguage } from './localeConfig.js';

export function reportDocument(reportHtml, theme, language = 'en') {
  const dark = theme === 'dark';
  const background = dark ? '#151b26' : '#ffffff';
  const foreground = dark ? '#e7edf7' : '#172033';
  const border = dark ? '#465266' : '#cbd4e1';
  const alternate = dark ? '#202938' : '#f3f6fb';

  return `<!doctype html>
<html lang="${resolveLanguage(language)}">
  <head>
    <meta charset="utf-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <style>
      :root { color-scheme: ${dark ? 'dark' : 'light'}; }
      body { margin: 16px; background: ${background}; color: ${foreground}; font: 14px/1.5 system-ui, sans-serif; }
      table { width: 100%; border-collapse: collapse; }
      th, td { padding: 7px; border: 1px solid ${border}; text-align: left; }
      th { background: ${alternate}; }
      .cancelled-status { background: ${dark ? '#38272b' : '#f7c9c9'}; color: ${dark ? '#f0c1ba' : '#1b062e'}; }
      a { color: ${dark ? '#8dbdff' : '#1e4f91'}; }
      @media (max-width: 700px) { body { margin: 8px; } table { min-width: 850px; } }
    </style>
  </head>
  <body>${reportHtml}</body>
</html>`;
}
