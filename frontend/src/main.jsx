import React from 'react';
import { createRoot } from 'react-dom/client';
import { BookingPage } from './components/BookingPage.jsx';
import { ReportsPage } from './components/ReportsPage.jsx';

const rootElement = document.getElementById('root');

if (!rootElement) {
  throw new Error('Booking app root element was not found.');
}

const requestedPage = new URLSearchParams(window.location.search).get('page');
const App = requestedPage === 'reports' ? ReportsPage : BookingPage;

createRoot(rootElement).render(
  <React.StrictMode>
    <App />
  </React.StrictMode>,
);
