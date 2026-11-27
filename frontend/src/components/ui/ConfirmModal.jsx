import { useEffect } from 'react';
import { IconAlert, IconInfo } from './Icons';

export default function ConfirmModal({
  open,
  title,
  message,
  details = [],
  confirmLabel = '확인',
  cancelLabel = '취소',
  tone = 'primary',
  loading = false,
  onConfirm,
  onCancel,
}) {
  useEffect(() => {
    if (!open) return undefined;
    function handleKeyDown(e) {
      if (e.key === 'Escape' && !loading) {
        onCancel();
      }
    }
    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [open, loading, onCancel]);

  if (!open) return null;

  const isWarningTone = tone === 'danger' || tone === 'warning';

  return (
    <div
      className="modal-backdrop"
      role="dialog"
      aria-modal="true"
      aria-labelledby="confirm-modal-title"
      onClick={() => {
        if (!loading) onCancel();
      }}
    >
      <div className="modal-card" onClick={(e) => e.stopPropagation()}>
        <div className="modal-header">
          <span className={`modal-icon modal-icon-${tone}`}>
            {isWarningTone ? <IconAlert size={20} /> : <IconInfo size={20} />}
          </span>
          <h3 id="confirm-modal-title" className="modal-title">
            {title}
          </h3>
        </div>

        <p className="modal-message">{message}</p>

        {details.length > 0 && (
          <ul className="modal-detail-list">
            {details.map((line, index) => (
              <li key={index}>{line}</li>
            ))}
          </ul>
        )}

        <div className="modal-actions">
          <button
            type="button"
            className="btn btn-secondary"
            onClick={onCancel}
            disabled={loading}
          >
            {cancelLabel}
          </button>
          <button
            type="button"
            className={`btn ${
              tone === 'danger'
                ? 'btn-danger-solid'
                : tone === 'success'
                ? 'btn-success'
                : 'btn-primary'
            }`}
            onClick={onConfirm}
            disabled={loading}
          >
            {loading ? '처리 중...' : confirmLabel}
          </button>
        </div>
      </div>
    </div>
  );
}
