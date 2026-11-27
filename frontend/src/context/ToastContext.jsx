import { createContext, useCallback, useContext, useMemo, useState } from 'react';
import { IconAlert, IconCheckCircle, IconInfo, IconXCircle } from '../components/ui/Icons';

const ToastContext = createContext(null);

let toastSeq = 1;

export function ToastProvider({ children }) {
  const [toasts, setToasts] = useState([]);

  const removeToast = useCallback((id) => {
    setToasts((prev) => prev.filter((t) => t.id !== id));
  }, []);

  const pushToast = useCallback(
    (type, message, durationMs) => {
      const id = toastSeq++;
      setToasts((prev) => [...prev.slice(-3), { id, type, message }]);
      if (durationMs > 0) {
        window.setTimeout(() => {
          removeToast(id);
        }, durationMs);
      }
    },
    [removeToast]
  );

  const api = useMemo(
    () => ({
      success: (message) => pushToast('success', message, 3500),
      error: (message) => pushToast('error', message, 0),
      warning: (message) => pushToast('warning', message, 5000),
      info: (message) => pushToast('info', message, 3500),
    }),
    [pushToast]
  );

  return (
    <ToastContext.Provider value={api}>
      {children}
      <div className="toast-container" role="region" aria-label="알림 메시지">
        {toasts.map((toast) => (
          <div key={toast.id} className={`toast toast-${toast.type}`} role="alert">
            <span className="toast-icon">
              {toast.type === 'success' && <IconCheckCircle size={18} />}
              {toast.type === 'error' && <IconXCircle size={18} />}
              {toast.type === 'warning' && <IconAlert size={18} />}
              {toast.type === 'info' && <IconInfo size={18} />}
            </span>
            <span className="toast-message">{toast.message}</span>
            <button
              type="button"
              className="toast-close"
              onClick={() => removeToast(toast.id)}
              aria-label="알림 닫기"
            >
              ×
            </button>
          </div>
        ))}
      </div>
    </ToastContext.Provider>
  );
}

export function useToast() {
  const ctx = useContext(ToastContext);
  if (!ctx) {
    throw new Error('useToast must be used within ToastProvider');
  }
  return ctx;
}
