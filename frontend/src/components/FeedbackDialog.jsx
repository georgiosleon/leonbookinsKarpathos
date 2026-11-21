import { useEffect, useRef } from 'react';
import { useLanguage } from '../i18n.jsx';

export function FeedbackDialog({ message, severity = 'error', onClose }) {
  const closeButtonRef = useRef(null);
  const onCloseRef = useRef(onClose);
  const { t } = useLanguage();
  onCloseRef.current = onClose;

  useEffect(() => {
    if (!message) return undefined;
    const previousFocus = document.activeElement;
    closeButtonRef.current?.focus();
    const handleKeyDown = (event) => {
      if (event.key === 'Escape') {
        onCloseRef.current();
      } else if (event.key === 'Tab') {
        event.preventDefault();
        closeButtonRef.current?.focus();
      }
    };
    document.addEventListener('keydown', handleKeyDown);

    return () => {
      document.removeEventListener('keydown', handleKeyDown);
      previousFocus?.focus?.();
    };
  }, [message]);

  if (!message) return null;

  const title = severity === 'warning' ? t('Warning') : t('Error');

  return (
    <div
      className="dialog-backdrop"
      role="presentation"
      onMouseDown={(event) => {
        if (event.target === event.currentTarget) onClose();
      }}
    >
      <section
        className={`feedback-dialog feedback-dialog-${severity}`}
        role="alertdialog"
        aria-modal="true"
        aria-labelledby="feedback-dialog-heading"
        aria-describedby="feedback-dialog-message"
      >
        <div className="feedback-dialog-heading">
          <h2 id="feedback-dialog-heading">{title}</h2>
          <button
            ref={closeButtonRef}
            type="button"
            className="button button-muted"
            onClick={onClose}
          >
            {t('Close')}
          </button>
        </div>
        <p id="feedback-dialog-message">{message}</p>
      </section>
    </div>
  );
}
