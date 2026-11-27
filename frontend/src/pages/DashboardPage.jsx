import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { dashboardApi } from '../api/endpoints';
import StatusBadge, { workOrderStatusVariant } from '../components/StatusBadge';
import { formatNumber, getTodayIso } from '../utils/format';

export default function DashboardPage() {
  const [selectedDate, setSelectedDate] = useState(getTodayIso());
  const [summary, setSummary] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  async function loadSummary(dateValue = selectedDate) {
    setLoading(true);
    setError('');
    try {
      const res = await dashboardApi.getSummary(dateValue);
      setSummary(res.data);
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    loadSummary(selectedDate);
  }, [selectedDate]);

  return (
    <div>
      <div className="card" style={{ padding: '14px 20px' }}>
        <div className="card-header" style={{ marginBottom: 0 }}>
          <div className="filter-bar">
            <label htmlFor="dashboard-date" style={{ fontSize: 13, fontWeight: 600 }}>
              집계 기준일
            </label>
            <input
              id="dashboard-date"
              type="date"
              value={selectedDate}
              onChange={(e) => setSelectedDate(e.target.value)}
            />
          </div>
          <button className="btn btn-secondary" onClick={() => loadSummary(selectedDate)} disabled={loading}>
            {loading ? '조회 중...' : '새로고침'}
          </button>
        </div>
      </div>

      {error && <div className="alert alert-error">{error}</div>}

      {loading && !summary ? (
        <div className="card empty-state">대시보드 데이터를 불러오는 중입니다...</div>
      ) : summary ? (
        <>
          <section className="kpi-grid">
            <div className="kpi-card">
              <div className="kpi-label">진행 중 작업지시</div>
              <div className="kpi-value primary">{formatNumber(summary.inProgressWorkOrderCount)}건</div>
              <div className="kpi-sub">
                대기 {formatNumber(summary.waitingWorkOrderCount)}건 · 완료{' '}
                {formatNumber(summary.completedWorkOrderCount)}건
              </div>
            </div>

            <div className="kpi-card">
              <div className="kpi-label">기준일 총 생산량</div>
              <div className="kpi-value">{formatNumber(summary.todayTotalQuantity)}</div>
              <div className="kpi-sub">
                양품 {formatNumber(summary.todayGoodQuantity)} / 불량{' '}
                {formatNumber(summary.todayDefectQuantity)} (실적 {summary.todayResultCount}건)
              </div>
            </div>

            <div className="kpi-card">
              <div className="kpi-label">기준일 불량률</div>
              <div className={`kpi-value ${summary.todayDefectRate >= 5 ? 'danger' : ''}`}>
                {summary.todayDefectRate.toFixed(1)}%
              </div>
              <div className="kpi-sub">기준일: {summary.baseDate}</div>
            </div>

            <div className="kpi-card">
              <div className="kpi-label">안전재고 미만 품목</div>
              <div className={`kpi-value ${summary.belowSafetyStockCount > 0 ? 'danger' : ''}`}>
                {formatNumber(summary.belowSafetyStockCount)}건
              </div>
              <div className="kpi-sub">활성 품목 기준 실시간 재고 경고</div>
            </div>
          </section>

          <section className="panel-grid">
            <div className="card">
              <div className="card-header">
                <h3 className="card-title">진행 중 작업지시 ({summary.inProgressWorkOrders.length}건)</h3>
                <Link to="/production" className="btn btn-secondary btn-sm">
                  생산 관리로 이동
                </Link>
              </div>
              {summary.inProgressWorkOrders.length === 0 ? (
                <div className="empty-state">현재 진행 중인 작업지시가 없습니다.</div>
              ) : (
                <div className="table-wrap">
                  <table>
                    <thead>
                      <tr>
                        <th>지시번호</th>
                        <th>완제품</th>
                        <th className="text-right">목표/양품</th>
                        <th>진행률</th>
                        <th>상태</th>
                      </tr>
                    </thead>
                    <tbody>
                      {summary.inProgressWorkOrders.map((wo) => (
                        <tr key={wo.id}>
                          <td>{wo.orderNumber}</td>
                          <td>
                            {wo.itemName} <span style={{ color: '#64748b' }}>({wo.itemCode})</span>
                          </td>
                          <td className="text-right">
                            {formatNumber(wo.producedGoodQuantity)} / {formatNumber(wo.targetQuantity)} {wo.unit}
                          </td>
                          <td>
                            <div className="progress-wrap">
                              <div className="progress-bar">
                                <div
                                  className="progress-fill"
                                  style={{ width: `${Math.min(wo.progressRate, 100)}%` }}
                                />
                              </div>
                              <span>{wo.progressRate.toFixed(1)}%</span>
                            </div>
                          </td>
                          <td>
                            <StatusBadge variant={workOrderStatusVariant(wo.status)}>
                              {wo.statusDescription}
                            </StatusBadge>
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              )}
            </div>

            <div className="card">
              <div className="card-header">
                <h3 className="card-title">안전재고 미만 경고 ({summary.belowSafetyStockItems.length}건)</h3>
                <Link to="/inventories" className="btn btn-secondary btn-sm">
                  재고 · 입고로 이동
                </Link>
              </div>
              {summary.belowSafetyStockItems.length === 0 ? (
                <div className="empty-state">안전재고 미만 품목이 없습니다.</div>
              ) : (
                <div className="table-wrap">
                  <table>
                    <thead>
                      <tr>
                        <th>품목코드</th>
                        <th>품목명</th>
                        <th>구분</th>
                        <th className="text-right">현재고</th>
                        <th className="text-right">안전재고</th>
                        <th className="text-right">부족 수량</th>
                      </tr>
                    </thead>
                    <tbody>
                      {summary.belowSafetyStockItems.map((inv) => (
                        <tr key={inv.itemId} className="row-warning">
                          <td>{inv.itemCode}</td>
                          <td>{inv.itemName}</td>
                          <td>
                            <StatusBadge variant={inv.itemType === 'RAW_MATERIAL' ? 'warning' : 'info'}>
                              {inv.itemTypeDescription}
                            </StatusBadge>
                          </td>
                          <td className="text-right" style={{ fontWeight: 700, color: '#dc2626' }}>
                            {formatNumber(inv.quantity)} {inv.unit}
                          </td>
                          <td className="text-right">
                            {formatNumber(inv.safetyStock)} {inv.unit}
                          </td>
                          <td className="text-right" style={{ fontWeight: 700, color: '#d97706' }}>
                            -{formatNumber(inv.safetyStock - inv.quantity)} {inv.unit}
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              )}
            </div>
          </section>
        </>
      ) : null}
    </div>
  );
}
