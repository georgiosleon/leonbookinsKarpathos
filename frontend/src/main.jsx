import React, { Suspense, lazy } from 'react';
import { createRoot } from 'react-dom/client';
import { BookingPage } from './components/BookingPage.jsx';
import { getInitialTheme } from './theme.js';
import { LanguageProvider } from './i18n.jsx';

const ReportsPage = lazy(() => import('./components/ReportsPage.jsx').then((m) => ({ default: m.ReportsPage })));
const ResourcesPage = lazy(() => import('./components/ResourcesPage.jsx').then((m) => ({ default: m.ResourcesPage })));
const UserManagement = lazy(() => import('./components/admin/UserManagement.jsx').then((m) => ({ default: m.UserManagement })));

const rootElement = document.getElementById('root');

if (!rootElement) {
  throw new Error('Booking app root element was not found.');
}

const requestedPage = new URLSearchParams(window.location.search).get('page');
const App = requestedPage === 'reports'
  ? ReportsPage
  : requestedPage === 'resources'
    ? ResourcesPage
    : requestedPage === 'users'
      ? UserManagement
      : BookingPage;
document.documentElement.dataset.theme = getInitialTheme();

createRoot(rootElement).render(
  <React.StrictMode>
    <LanguageProvider>
      <Suspense fallback={null}>
        <App />
      </Suspense>
    </LanguageProvider>
  </React.StrictMode>,
);
