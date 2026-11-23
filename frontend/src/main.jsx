import React, { Suspense, lazy, useEffect, useState } from 'react';
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

function pageFromLocation() {
  const params = new URLSearchParams(window.location.search);
  return {
    page: params.get('page') ?? '',
    type: params.get('type') ?? '',
    search: window.location.search,
  };
}

function resolveApp(page) {
  if (page === 'reports') return ReportsPage;
  if (page === 'resources') return ResourcesPage;
  if (page === 'users') return UserManagement;
  return BookingPage;
}

function shouldHandleClientNavigation(event) {
  if (event.defaultPrevented) return false;
  if (event.button !== 0) return false;
  if (event.metaKey || event.ctrlKey || event.shiftKey || event.altKey) return false;
  const anchor = event.target.closest('a[href]');
  if (!anchor) return false;
  if (anchor.target && anchor.target !== '_self') return false;
  if (anchor.hasAttribute('download')) return false;
  const url = new URL(anchor.href, window.location.href);
  if (url.origin !== window.location.origin) return false;
  if (url.pathname !== window.location.pathname) return false;
  return true;
}

function AppRouter() {
  const [location, setLocation] = useState(pageFromLocation);

  useEffect(() => {
    const onPopState = () => setLocation(pageFromLocation());
    window.addEventListener('popstate', onPopState);
    return () => window.removeEventListener('popstate', onPopState);
  }, []);

  useEffect(() => {
    const onClick = (event) => {
      if (!shouldHandleClientNavigation(event)) return;
      const anchor = event.target.closest('a[href]');
      const url = new URL(anchor.href, window.location.href);
      const nextSearch = url.search;
      if (nextSearch === window.location.search) return;
      event.preventDefault();
      window.history.pushState({}, '', `${url.pathname}${nextSearch}${url.hash}`);
      setLocation(pageFromLocation());
      window.scrollTo({ top: 0, behavior: 'instant' });
    };
    document.addEventListener('click', onClick);
    return () => document.removeEventListener('click', onClick);
  }, []);

  const App = resolveApp(location.page);

  return (
    <LanguageProvider>
      <Suspense fallback={<main className="app-shell"><p className="feedback" role="status">Loading…</p></main>}>
        <App key={location.search || 'default'} />
      </Suspense>
    </LanguageProvider>
  );
}

document.documentElement.dataset.theme = getInitialTheme();

createRoot(rootElement).render(
  <React.StrictMode>
    <AppRouter />
  </React.StrictMode>,
);
