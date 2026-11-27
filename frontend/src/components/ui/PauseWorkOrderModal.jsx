import { useEffect, useState } from 'react';
import { IconAlert, IconPause } from './Icons';

const PAUSE_PRESET_REASONS = [
  '원자재 부족',
  '설비 점검/고장',
  '품질 이상 확인',
  '긴급 주문 우선',
  '작업 교대/휴식',
];

export default function PauseWorkOrderModal({ order, loading = false, onConfirm, onCancel }) {
  const [reason, setReason] = useState('');

  useEffect(() => {
    if (order) {
      setReason('');
    }
  }, [order]);

  useEffect(() => {
    if (!order) return undefined;
    function handleKeyDown(e) {
      if (e.key === 'Escape' && !loading) {
        onCancel();
      }
    }
    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [order, loading, onCancel]);

  if (!order) return null;

  function handleSubmit(e) {
    e.preventDefault();
    if (!reason.trim()) return;
    onConfirm(reason.trim());
  }

  return (
    <div
      className="modal-backdrop"
      role="dialog"
      aria-modal="true"
      aria-labelledby="pause-modal-title"
      onClick={() => {
        if (!loading) onCancel();
      }}
    >
      <div className="modal-card" onClick={(e) => e.stopPropagation()}>
        <div className="modal-header">
          <span className="modal-icon modal-icon-warning">
            <IconAlert size={20} />
          </span>
          <h3 id="pause-modal-title" className="modal-title">
            작업지시 일시정지
          </h3>
        </div>

        <p className="modal-message">
          <strong>
            {order.orderNumber} ({order.itemName})
          </strong>{' '}
          작업을 일시정지합니다. 정지 사유를 선택하거나 직접 입력해 주세요.
        </p>

        <div className="pause-chip-grid" role="group" aria-label="빠른 사유 선택">
          {PAUSE_PRESET_REASONS.map((preset) => (
            <button
              key={preset}
              type="button"
              className={`pause-reason-chip ${reason === preset ? 'active' : ''}`}
              onClick={() => setReason(preset)}
            >
              {preset}
            </button>
          ))}
        </div>

        <form onSubmit={handleSubmit}>
          <div className="field" style={{ marginBottom: 16 }}>
            <label htmlFor="pause-reason-input">일시정지 상세 사유 *</label>
            <input
              id="pause-reason-input"
              type="text"
              maxLength={200}
              value={reason}
              onChange={(e) => setReason(e.target.value)}
              placeholder="예: AB골 원단 재고 입고 대기"
              required
            />
          </div>

          <div className="modal-actions">
            <button
              type="button"
              className="btn btn-secondary"
              onClick={onCancel}
              disabled={loading}
            >
              취소
            </button>
            <button
              type="submit"
              className="btn btn-warning-solid"
              disabled={loading || !reason.trim()}
            >
              <IconPause size={14} />
              <span>{loading ? '처리 중...' : '일시정지 실행'}</span>
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
