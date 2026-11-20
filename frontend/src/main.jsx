import React from 'react';
import { createRoot } from 'react-dom/client';
import { BookingPage } from './components/BookingPage.jsx';
import { ReportsPage } from './components/ReportsPage.jsx';
import { ResourcesPage } from './components/ResourcesPage.jsx';
import { getInitialTheme } from './theme.js';
import { LanguageProvider } from './i18n.jsx';

const rootElement = document.getElementById('root');

if (!rootElement) {
  throw new Error('Booking app root element was not found.');
}

const requestedPage = new URLSearchParams(window.location.search).get('page');
const App = requestedPage === 'reports'
  ? ReportsPage
  : requestedPage === 'resources'
    ? ResourcesPage
    : BookingPage;
document.documentElement.dataset.theme = getInitialTheme();

createRoot(rootElement).render(
  <React.StrictMode>
    <LanguageProvider>
      <App />
    </LanguageProvider>
  </React.StrictMode>,
);
