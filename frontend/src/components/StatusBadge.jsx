export default function StatusBadge({ variant = 'neutral', children }) {
  return <span className={`badge badge-${variant}`}>{children}</span>;
}

export function workOrderStatusVariant(status) {
  switch (status) {
    case 'WAITING':
      return 'neutral';
    case 'IN_PROGRESS':
      return 'info';
    case 'COMPLETED':
      return 'success';
    case 'CANCELED':
      return 'danger';
    default:
      return 'neutral';
  }
}

export function transactionTypeVariant(type) {
  switch (type) {
    case 'INBOUND':
    case 'PRODUCTION_IN':
      return 'success';
    case 'OUTBOUND':
    case 'PRODUCTION_OUT':
      return 'warning';
    case 'ADJUST':
      return 'info';
    default:
      return 'neutral';
  }
}
