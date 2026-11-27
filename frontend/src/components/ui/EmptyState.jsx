export default function EmptyState({ loading = false, message }) {
  return (
    <div className="empty-state">
      {loading ? message || '데이터를 불러오는 중입니다...' : message || '표시할 데이터가 없습니다.'}
    </div>
  );
}
