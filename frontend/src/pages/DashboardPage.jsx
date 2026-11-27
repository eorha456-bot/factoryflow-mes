import { useCallback, useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { dashboardApi } from '../api/endpoints';
import StatusBadge, { workOrderStatusVariant } from '../components/StatusBadge';
import EmptyState from '../components/ui/EmptyState';
import {
  IconAlert,
  IconCheckCircle,
  IconClipboard,
  IconPause,
  IconRefresh,
  IconWarehouse,
} from '../components/ui/Icons';
import KpiCard from '../components/ui/KpiCard';
import { useToast } from '../context/ToastContext';
import { formatDateTime, formatNumber, formatTimeOnly, getTodayIso } from '../utils/format';

export default function DashboardPage() {
  const toast = useToast();
  const [selectedDate, setSelectedDate] = useState(getTodayIso());
  const [autoRefresh, setAutoRefresh] = useState(true);
  const [lastUpdated, setLastUpdated] = useState(null);
  const [summary, setSummary] = useState(null);
  const [loading, setLoading] = useState(true);

  const loadSummary = useCallback(
    async (dateValue = selectedDate, silent = false) => {
      if (!silent) setLoading(true);
      try {
        const res = await dashboardApi.getSummary(dateValue);
        setSummary(res.data);
        setLastUpdated(new Date());
      } catch (err) {
        toast.error(err.message);
      } finally {
        if (!silent) setLoading(false);
      }
    },
    [selectedDate, toast]
  );

  useEffect(() => {
    loadSummary(selectedDate);
  }, [selectedDate, loadSummary]);

  useEffect(() => {
    if (!autoRefresh) return undefined;
    const timer = window.setInterval(() => {
      loadSummary(selectedDate, true);
    }, 30000);
    return () => window.clearInterval(timer);
  }, [autoRefresh, selectedDate, loadSummary]);

  const pausedList = summary?.pausedWorkOrders || [];
  const inProgressList = summary?.inProgressWorkOrders || [];
  const activeOrders = [...pausedList, ...inProgressList];
  const hasHighDefect = summary && summary.todayDefectRate >= 5.0;
  const hasLowStock = summary && summary.belowSafetyStockCount > 0;
  const hasPaused = summary && (summary.pausedWorkOrderCount || 0) > 0;
  const hasAnomaly = hasHighDefect || hasLowStock || hasPaused;

  const anomalyParts = [];
  if (hasPaused) anomalyParts.push(`일시정지 작업 ${summary.pausedWorkOrderCount}건`);
  if (hasLowStock) anomalyParts.push(`안전재고 미만 품목 ${summary.belowSafetyStockCount}건`);
  if (hasHighDefect) anomalyParts.push(`기준일 불량률 주의(${summary.todayDefectRate.toFixed(1)}%)`);

  return (
    <div>
      <div className="card" style={{ padding: '14px 20px' }}>
        <div className="card-header" style={{ marginBottom: 0 }}>
          <div className="filter-bar">
            <label htmlFor="dashboard-date" style={{ fontSize: 13.5, fontWeight: 700 }}>
              집계 기준일
            </label>
            <input
              id="dashboard-date"
              type="date"
              value={selectedDate}
              onChange={(e) => setSelectedDate(e.target.value)}
            />
            <label className="checkbox-label">
              <input
                type="checkbox"
                checked={autoRefresh}
                onChange={(e) => setAutoRefresh(e.target.checked)}
              />
              30초 자동 갱신
            </label>
          </div>

          <div className="filter-bar">
            {lastUpdated && (
              <span className="meta-text">최근 갱신: {formatTimeOnly(lastUpdated)}</span>
            )}
            <button
              type="button"
              className="btn btn-secondary btn-sm"
              onClick={() => loadSummary(selectedDate)}
              disabled={loading}
            >
              <IconRefresh size={14} />
              <span>{loading ? '조회 중...' : '새로고침'}</span>
            </button>
          </div>
        </div>
      </div>

      {hasAnomaly && (
        <div className="anomaly-banner" role="alert">
          <div className="anomaly-banner-main">
            <IconAlert size={20} />
            <span>이상 징후 감지: {anomalyParts.join(' · ')}</span>
          </div>
          <div className="anomaly-banner-actions">
            {hasPaused && (
              <Link to="/production" className="btn btn-warning btn-sm">
                일시정지 작업 확인
              </Link>
            )}
            {hasLowStock && (
              <Link to="/inventories" className="btn btn-danger btn-sm">
                재고 보충(입고) 바로가기
              </Link>
            )}
            {hasHighDefect && !hasPaused && (
              <Link to="/production" className="btn btn-secondary btn-sm">
                생산실적 점검
              </Link>
            )}
          </div>
        </div>
      )}

      {loading && !summary ? (
        <div className="card">
          <EmptyState loading message="대시보드 데이터를 불러오는 중입니다..." />
        </div>
      ) : summary ? (
        <>
          <section className="kpi-grid">
            <KpiCard
              label="진행 중 작업지시"
              value={`${formatNumber(summary.inProgressWorkOrderCount)}건`}
              sub={`일시정지 ${formatNumber(summary.pausedWorkOrderCount || 0)}건 · 대기 ${formatNumber(
                summary.waitingWorkOrderCount
              )}건 · 완료 ${formatNumber(summary.completedWorkOrderCount)}건`}
              tone={hasPaused ? 'warning' : 'primary'}
              icon={<IconClipboard size={18} />}
            />

            <KpiCard
              label="기준일 총 생산량"
              value={formatNumber(summary.todayTotalQuantity)}
              sub={`양품 ${formatNumber(summary.todayGoodQuantity)} / 불량 ${formatNumber(
                summary.todayDefectQuantity
              )} (실적 ${summary.todayResultCount}건)`}
              tone="success"
              icon={<IconCheckCircle size={18} />}
            />

            <KpiCard
              label="기준일 불량률"
              value={`${summary.todayDefectRate.toFixed(1)}%`}
              sub={hasHighDefect ? '주의 기준(5.0%) 초과' : `기준일: ${summary.baseDate} (양호)`}
              tone={hasHighDefect ? 'danger' : 'default'}
              icon={<IconAlert size={18} />}
            />

            <KpiCard
              label="안전재고 미만 품목"
              value={`${formatNumber(summary.belowSafetyStockCount)}건`}
              sub={hasLowStock ? '즉시 발주/입고 확인 필요' : '모든 활성 품목 안전재고 충족'}
              tone={hasLowStock ? 'danger' : 'success'}
              icon={<IconWarehouse size={18} />}
            />
          </section>

          <section className="dashboard-panels">
            <div className="card">
              <div className="card-header">
                <h3 className="card-title">
                  진행 · 일시정지 작업지시 ({activeOrders.length}건
                  {hasPaused ? ` · 일시정지 ${pausedList.length}건` : ''})
                </h3>
                <Link to="/production" className="btn btn-secondary btn-sm">
                  생산 관리로 이동
                </Link>
              </div>
              {activeOrders.length === 0 ? (
                <EmptyState message="현재 진행 중이거나 일시정지된 작업지시가 없습니다." />
              ) : (
                <div className="table-wrap dashboard-table-wrap">
                  <table className="dashboard-table">
                    <thead>
                      <tr>
                        <th className="col-nowrap">지시번호</th>
                        <th>완제품</th>
                        <th className="text-right col-nowrap">양품 / 목표</th>
                        <th className="col-progress">진행률</th>
                        <th className="col-nowrap">상태</th>
                        <th>정지 사유 / 비고</th>
                      </tr>
                    </thead>
                    <tbody>
                      {activeOrders.map((wo) => (
                        <tr key={wo.id} className={wo.status === 'PAUSED' ? 'row-paused' : ''}>
                          <td className="col-nowrap" style={{ fontWeight: 700 }}>
                            {wo.orderNumber}
                          </td>
                          <td className="col-wrap">
                            <span style={{ fontWeight: 600 }}>{wo.itemName}</span>
                            <div className="cell-sub">{wo.itemCode}</div>
                          </td>
                          <td className="text-right col-nowrap">
                            <strong>{formatNumber(wo.producedGoodQuantity)}</strong> /{' '}
                            {formatNumber(wo.targetQuantity)} {wo.unit}
                          </td>
                          <td className="col-progress">
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
                          <td className="col-nowrap">
                            <StatusBadge variant={workOrderStatusVariant(wo.status)}>
                              {wo.status === 'PAUSED' && <IconPause size={11} />}
                              <span>{wo.statusDescription}</span>
                            </StatusBadge>
                          </td>
                          <td className="col-wrap">
                            {wo.status === 'PAUSED' && wo.pauseReason ? (
                              <div className="pause-meta-inline">
                                <div className="pause-reason-text">{wo.pauseReason}</div>
                                {wo.pausedAt && (
                                  <div className="cell-sub">
                                    정지 시각: {formatDateTime(wo.pausedAt)}
                                  </div>
                                )}
                              </div>
                            ) : (
                              <span className="cell-sub">{wo.remark || '정상 진행 중'}</span>
                            )}
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
                <h3 className="card-title">
                  안전재고 미만 경고 ({summary.belowSafetyStockItems.length}건)
                </h3>
                <Link to="/inventories" className="btn btn-secondary btn-sm">
                  재고 · 입고로 이동
                </Link>
              </div>
              {summary.belowSafetyStockItems.length === 0 ? (
                <EmptyState message="안전재고 미만 품목이 없습니다." />
              ) : (
                <div className="table-wrap dashboard-table-wrap">
                  <table className="dashboard-table">
                    <thead>
                      <tr>
                        <th className="col-nowrap">품목코드</th>
                        <th>품목명</th>
                        <th className="col-nowrap">구분</th>
                        <th className="text-right col-nowrap">현재고</th>
                        <th className="text-right col-nowrap">안전재고</th>
                        <th className="text-right col-nowrap">부족 수량</th>
                      </tr>
                    </thead>
                    <tbody>
                      {summary.belowSafetyStockItems.map((inv) => (
                        <tr key={inv.itemId} className="row-warning">
                          <td className="col-nowrap" style={{ fontWeight: 700 }}>
                            {inv.itemCode}
                          </td>
                          <td className="col-wrap" style={{ fontWeight: 600 }}>
                            {inv.itemName}
                          </td>
                          <td className="col-nowrap">
                            <StatusBadge
                              variant={inv.itemType === 'RAW_MATERIAL' ? 'warning' : 'info'}
                            >
                              {inv.itemTypeDescription}
                            </StatusBadge>
                          </td>
                          <td
                            className="text-right col-nowrap"
                            style={{ fontWeight: 800, color: '#b91c1c' }}
                          >
                            {formatNumber(inv.quantity)} {inv.unit}
                          </td>
                          <td className="text-right col-nowrap">
                            {formatNumber(inv.safetyStock)} {inv.unit}
                          </td>
                          <td
                            className="text-right col-nowrap"
                            style={{ fontWeight: 800, color: '#b45309' }}
                          >
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
