import { useCallback, useEffect, useMemo, useState } from 'react';
import { bomApi, inventoryApi, itemApi, productionApi } from '../api/endpoints';
import StatusBadge, { workOrderStatusVariant } from '../components/StatusBadge';
import ConfirmModal from '../components/ui/ConfirmModal';
import EmptyState from '../components/ui/EmptyState';
import { IconAlert, IconCheckCircle, IconPause, IconPlay, IconRefresh } from '../components/ui/Icons';
import PauseWorkOrderModal from '../components/ui/PauseWorkOrderModal';
import SearchSortBar from '../components/ui/SearchSortBar';
import { useToast } from '../context/ToastContext';
import {
  formatDateTime,
  formatNumber,
  getDaysAgoIso,
  getTodayIso,
  notifyDataChanged,
  toLocalDateKey,
} from '../utils/format';

const SORT_OPTIONS = [
  { value: 'latest', label: '정렬: 최신 등록순' },
  { value: 'plannedAsc', label: '정렬: 작업 예정일순' },
  { value: 'progressAsc', label: '정렬: 진행률 낮은순' },
  { value: 'progressDesc', label: '정렬: 진행률 높은순' },
];

const RESULT_SORT_OPTIONS = [
  { value: 'latest', label: '정렬: 최신 등록순' },
  { value: 'goodDesc', label: '정렬: 양품 많은순' },
  { value: 'defectDesc', label: '정렬: 불량 많은순' },
  { value: 'totalDesc', label: '정렬: 총 생산량순' },
];

export default function ProductionPage() {
  const toast = useToast();
  const [workOrders, setWorkOrders] = useState([]);
  const [results, setResults] = useState([]);
  const [finishedItems, setFinishedItems] = useState([]);
  const [filterStatus, setFilterStatus] = useState('');
  const [search, setSearch] = useState('');
  const [sortBy, setSortBy] = useState('latest');

  const [resultPeriod, setResultPeriod] = useState('all');
  const [resultCustomDate, setResultCustomDate] = useState(getTodayIso());
  const [resultSearch, setResultSearch] = useState('');
  const [resultSortBy, setResultSortBy] = useState('latest');
  const [groupByDate, setGroupByDate] = useState(true);
  const [collapsedDates, setCollapsedDates] = useState({});

  const [orderForm, setOrderForm] = useState({
    orderNumber: '',
    itemId: '',
    targetQuantity: '',
    plannedDate: getTodayIso(),
    remark: '',
  });

  const [selectedOrder, setSelectedOrder] = useState(null);
  const [selectedOrderBoms, setSelectedOrderBoms] = useState([]);
  const [inventoryByItemId, setInventoryByItemId] = useState({});
  const [bomPreviewLoading, setBomPreviewLoading] = useState(false);

  const [resultForm, setResultForm] = useState({
    goodQuantity: '',
    defectQuantity: '0',
    workerName: '',
    remark: '',
  });

  const [pendingStatusChange, setPendingStatusChange] = useState(null);
  const [pausingOrder, setPausingOrder] = useState(null);
  const [pendingResultSubmit, setPendingResultSubmit] = useState(null);

  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);

  const loadFinishedItems = useCallback(async () => {
    const res = await itemApi.list({ itemType: 'FINISHED_GOOD', active: true });
    setFinishedItems(res.data || []);
  }, []);

  const loadWorkOrders = useCallback(
    async (statusValue = filterStatus) => {
      const res = await productionApi.listWorkOrders({
        status: statusValue || undefined,
      });
      setWorkOrders(res.data || []);
    },
    [filterStatus]
  );

  const loadResults = useCallback(async () => {
    const res = await productionApi.listAllResults();
    setResults(res.data || []);
  }, []);

  const loadAll = useCallback(async () => {
    setLoading(true);
    try {
      await Promise.all([loadFinishedItems(), loadWorkOrders(), loadResults()]);
    } catch (err) {
      toast.error(err.message);
    } finally {
      setLoading(false);
    }
  }, [loadFinishedItems, loadWorkOrders, loadResults, toast]);

  useEffect(() => {
    loadAll();
  }, [loadAll]);

  useEffect(() => {
    loadWorkOrders(filterStatus).catch((err) => toast.error(err.message));
  }, [filterStatus, loadWorkOrders, toast]);

  // 선택된 작업지시의 BOM + 원재료 현재고 미리보기 로드
  const loadBomPreviewData = useCallback(
    async (order) => {
      if (!order) {
        setSelectedOrderBoms([]);
        return;
      }
      setBomPreviewLoading(true);
      try {
        const [bomRes, invRes] = await Promise.all([
          bomApi.list(order.itemId),
          inventoryApi.list({ itemType: 'RAW_MATERIAL' }),
        ]);
        setSelectedOrderBoms(bomRes.data || []);
        const map = {};
        (invRes.data || []).forEach((inv) => {
          map[inv.itemId] = inv.quantity;
        });
        setInventoryByItemId(map);
      } catch (err) {
        toast.error(err.message);
      } finally {
        setBomPreviewLoading(false);
      }
    },
    [toast]
  );

  function selectOrderForResult(order) {
    setSelectedOrder(order);
    setResultForm((prev) => ({
      goodQuantity: '',
      defectQuantity: '0',
      workerName: prev.workerName,
      remark: '',
    }));
    loadBomPreviewData(order);
  }

  const filteredWorkOrders = useMemo(() => {
    const keyword = search.trim().toLowerCase();
    const list = keyword
      ? workOrders.filter(
          (wo) =>
            wo.orderNumber.toLowerCase().includes(keyword) ||
            wo.itemCode.toLowerCase().includes(keyword) ||
            wo.itemName.toLowerCase().includes(keyword)
        )
      : [...workOrders];

    switch (sortBy) {
      case 'plannedAsc':
        list.sort((a, b) => a.plannedDate.localeCompare(b.plannedDate));
        break;
      case 'progressAsc':
        list.sort((a, b) => a.progressRate - b.progressRate);
        break;
      case 'progressDesc':
        list.sort((a, b) => b.progressRate - a.progressRate);
        break;
      default:
        break;
    }
    return list;
  }, [workOrders, search, sortBy]);

  const filteredResults = useMemo(() => {
    const todayIso = getTodayIso();
    const weekAgoIso = getDaysAgoIso(6);
    const keyword = resultSearch.trim().toLowerCase();

    const list = results.filter((r) => {
      const dateKey = toLocalDateKey(r.createdAt);
      if (resultPeriod === 'today' && dateKey !== todayIso) return false;
      if (resultPeriod === 'week' && (dateKey < weekAgoIso || dateKey > todayIso)) return false;
      if (resultPeriod === 'custom' && resultCustomDate && dateKey !== resultCustomDate) return false;

      if (!keyword) return true;
      return (
        (r.orderNumber && r.orderNumber.toLowerCase().includes(keyword)) ||
        (r.itemCode && r.itemCode.toLowerCase().includes(keyword)) ||
        (r.itemName && r.itemName.toLowerCase().includes(keyword)) ||
        (r.workerName && r.workerName.toLowerCase().includes(keyword)) ||
        (r.remark && r.remark.toLowerCase().includes(keyword))
      );
    });

    const sorted = [...list];
    switch (resultSortBy) {
      case 'goodDesc':
        sorted.sort((a, b) => b.goodQuantity - a.goodQuantity);
        break;
      case 'defectDesc':
        sorted.sort((a, b) => b.defectQuantity - a.defectQuantity);
        break;
      case 'totalDesc':
        sorted.sort((a, b) => b.totalQuantity - a.totalQuantity);
        break;
      default:
        break;
    }
    return sorted;
  }, [results, resultPeriod, resultCustomDate, resultSearch, resultSortBy]);

  const groupedResults = useMemo(() => {
    const groups = new Map();
    filteredResults.forEach((r) => {
      const dateKey = toLocalDateKey(r.createdAt);
      if (!groups.has(dateKey)) {
        groups.set(dateKey, {
          dateKey,
          items: [],
          goodSum: 0,
          defectSum: 0,
          totalSum: 0,
        });
      }
      const group = groups.get(dateKey);
      group.items.push(r);
      group.goodSum += r.goodQuantity;
      group.defectSum += r.defectQuantity;
      group.totalSum += r.totalQuantity;
    });

    return Array.from(groups.values())
      .sort((a, b) => b.dateKey.localeCompare(a.dateKey))
      .map((g) => ({
        ...g,
        defectRate: g.totalSum > 0 ? Math.round((g.defectSum * 1000) / g.totalSum) / 10 : 0.0,
      }));
  }, [filteredResults]);

  const resultSummary = useMemo(() => {
    let goodSum = 0;
    let defectSum = 0;
    filteredResults.forEach((r) => {
      goodSum += r.goodQuantity;
      defectSum += r.defectQuantity;
    });
    const totalSum = goodSum + defectSum;
    const defectRate = totalSum > 0 ? Math.round((defectSum * 1000) / totalSum) / 10 : 0.0;
    return { count: filteredResults.length, goodSum, defectSum, totalSum, defectRate };
  }, [filteredResults]);

  function toggleDateGroup(dateKey) {
    setCollapsedDates((prev) => ({ ...prev, [dateKey]: !prev[dateKey] }));
  }

  const goodInput = Math.max(0, Number(resultForm.goodQuantity) || 0);
  const defectInput = Math.max(0, Number(resultForm.defectQuantity) || 0);
  const totalInput = goodInput + defectInput;
  const remainingTarget = selectedOrder
    ? Math.max(0, selectedOrder.targetQuantity - selectedOrder.producedGoodQuantity)
    : 0;

  const bomRequirements = useMemo(() => {
    return selectedOrderBoms.map((bom) => {
      const needed = bom.requiredQuantity * totalInput;
      const available = inventoryByItemId[bom.rawMaterialId] ?? 0;
      const remainingAfter = available - needed;
      return {
        ...bom,
        needed,
        available,
        remainingAfter,
        shortage: needed > available,
      };
    });
  }, [selectedOrderBoms, inventoryByItemId, totalInput]);

  const hasBomShortage = bomRequirements.some((item) => item.shortage);

  async function handleCreateOrder(e) {
    e.preventDefault();
    setSubmitting(true);
    try {
      const res = await productionApi.createWorkOrder({
        orderNumber: orderForm.orderNumber.trim() || null,
        itemId: Number(orderForm.itemId),
        targetQuantity: Number(orderForm.targetQuantity),
        plannedDate: orderForm.plannedDate,
        remark: orderForm.remark,
      });
      toast.success(res.message || '작업지시가 등록되었습니다.');
      setOrderForm({
        orderNumber: '',
        itemId: '',
        targetQuantity: '',
        plannedDate: getTodayIso(),
        remark: '',
      });
      await loadWorkOrders();
      notifyDataChanged();
    } catch (err) {
      toast.error(err.message);
    } finally {
      setSubmitting(false);
    }
  }

  async function executeStatusChange(order, nextStatus, reason) {
    setSubmitting(true);
    try {
      const res = await productionApi.updateWorkOrderStatus(order.id, nextStatus, reason);
      toast.success(res.message || '작업지시 상태가 변경되었습니다.');
      if (selectedOrder && selectedOrder.id === order.id) {
        if (nextStatus === 'PAUSED' || nextStatus === 'COMPLETED' || nextStatus === 'CANCELED') {
          setSelectedOrder(null);
        } else {
          setSelectedOrder(res.data);
        }
      }
      setPendingStatusChange(null);
      setPausingOrder(null);
      await loadWorkOrders();
      notifyDataChanged();
    } catch (err) {
      toast.error(err.message);
    } finally {
      setSubmitting(false);
    }
  }

  function requestStatusChange(order, nextStatus) {
    if (nextStatus === 'PAUSED') {
      setPausingOrder(order);
    } else if (
      nextStatus === 'COMPLETED' ||
      nextStatus === 'CANCELED' ||
      (order.status === 'PAUSED' && nextStatus === 'IN_PROGRESS')
    ) {
      setPendingStatusChange({ order, nextStatus });
    } else {
      executeStatusChange(order, nextStatus);
    }
  }

  function handleRecordResultSubmit(e) {
    e.preventDefault();
    if (!selectedOrder) return;
    if (totalInput <= 0) {
      toast.warning('양품 수량과 불량 수량의 합은 1 이상이어야 합니다.');
      return;
    }
    setPendingResultSubmit({
      order: selectedOrder,
      goodQuantity: goodInput,
      defectQuantity: defectInput,
      workerName: resultForm.workerName,
      remark: resultForm.remark,
      willAutoComplete: selectedOrder.producedGoodQuantity + goodInput >= selectedOrder.targetQuantity,
      bomRequirements,
      hasBomShortage,
    });
  }

  async function executeRecordResult() {
    if (!pendingResultSubmit) return;
    setSubmitting(true);
    try {
      const res = await productionApi.recordResult(pendingResultSubmit.order.id, {
        goodQuantity: pendingResultSubmit.goodQuantity,
        defectQuantity: pendingResultSubmit.defectQuantity,
        workerName: pendingResultSubmit.workerName,
        remark: pendingResultSubmit.remark,
      });
      toast.success(res.message || '생산실적이 등록되고 재고가 반영되었습니다.');
      setPendingResultSubmit(null);
      setResultForm((prev) => ({
        goodQuantity: '',
        defectQuantity: '0',
        workerName: prev.workerName,
        remark: '',
      }));

      const [ordersRes] = await Promise.all([
        productionApi.listWorkOrders({ status: filterStatus || undefined }),
        loadResults(),
      ]);
      const updatedOrders = ordersRes.data || [];
      setWorkOrders(updatedOrders);
      notifyDataChanged();

      if (res.data?.workOrderStatus === 'COMPLETED') {
        setSelectedOrder(null);
      } else {
        const refreshedOrder = updatedOrders.find((wo) => wo.id === pendingResultSubmit.order.id);
        if (refreshedOrder) {
          setSelectedOrder(refreshedOrder);
          loadBomPreviewData(refreshedOrder);
        }
      }
    } catch (err) {
      setPendingResultSubmit(null);
      toast.error(err.message);
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <div>
      <div className="card">
        <div className="card-header">
          <h3 className="card-title">신규 작업지시 등록</h3>
        </div>

        <form className="form-grid" onSubmit={handleCreateOrder}>
          <div className="field">
            <label htmlFor="wo-number">지시번호 (비우면 자동생성)</label>
            <input
              id="wo-number"
              value={orderForm.orderNumber}
              onChange={(e) => setOrderForm((p) => ({ ...p, orderNumber: e.target.value }))}
              placeholder="예: WO-20261127-001"
            />
          </div>

          <div className="field">
            <label htmlFor="wo-item">생산 완제품 *</label>
            <select
              id="wo-item"
              value={orderForm.itemId}
              onChange={(e) => setOrderForm((p) => ({ ...p, itemId: e.target.value }))}
              required
            >
              <option value="">완제품 선택</option>
              {finishedItems.map((item) => (
                <option key={item.id} value={item.id}>
                  [{item.itemCode}] {item.itemName} ({item.unit})
                </option>
              ))}
            </select>
          </div>

          <div className="field">
            <label htmlFor="wo-target">목표 수량 *</label>
            <input
              id="wo-target"
              type="number"
              min="1"
              value={orderForm.targetQuantity}
              onChange={(e) => setOrderForm((p) => ({ ...p, targetQuantity: e.target.value }))}
              required
            />
          </div>

          <div className="field">
            <label htmlFor="wo-date">작업 예정일 *</label>
            <input
              id="wo-date"
              type="date"
              value={orderForm.plannedDate}
              onChange={(e) => setOrderForm((p) => ({ ...p, plannedDate: e.target.value }))}
              required
            />
          </div>

          <div className="field">
            <label htmlFor="wo-remark">비고</label>
            <input
              id="wo-remark"
              value={orderForm.remark}
              onChange={(e) => setOrderForm((p) => ({ ...p, remark: e.target.value }))}
              placeholder="예: 오후 긴급 납품분"
            />
          </div>

          <div className="form-actions">
            <button type="submit" className="btn btn-primary" disabled={submitting}>
              {submitting ? '등록 중...' : '작업지시 등록'}
            </button>
          </div>
        </form>
      </div>

      {selectedOrder && (
        <div className="card" style={{ borderColor: '#60a5fa', backgroundColor: '#f8fbff' }}>
          <div className="card-header">
            <div>
              <h3 className="card-title">
                생산실적 등록 — {selectedOrder.orderNumber} ({selectedOrder.itemName})
              </h3>
              <div className="meta-text" style={{ marginTop: 4 }}>
                목표 {formatNumber(selectedOrder.targetQuantity)} {selectedOrder.unit} · 누적 양품{' '}
                {formatNumber(selectedOrder.producedGoodQuantity)} {selectedOrder.unit} ·{' '}
                <strong>
                  잔여 목표 {formatNumber(remainingTarget)} {selectedOrder.unit}
                </strong>
              </div>
            </div>
            <div className="inline-actions">
              {remainingTarget > 0 && (
                <button
                  type="button"
                  className="btn btn-secondary btn-sm"
                  onClick={() =>
                    setResultForm((prev) => ({
                      ...prev,
                      goodQuantity: String(remainingTarget),
                    }))
                  }
                >
                  잔여 수량({formatNumber(remainingTarget)}) 채우기
                </button>
              )}
              <button
                type="button"
                className="btn btn-secondary btn-sm"
                onClick={() => setSelectedOrder(null)}
              >
                닫기
              </button>
            </div>
          </div>

          <form className="form-grid" onSubmit={handleRecordResultSubmit}>
            <div className="field">
              <label htmlFor="res-good">양품 수량 *</label>
              <input
                id="res-good"
                type="number"
                min="0"
                value={resultForm.goodQuantity}
                onChange={(e) => setResultForm((p) => ({ ...p, goodQuantity: e.target.value }))}
                placeholder={`예: ${remainingTarget || 10}`}
                required
              />
            </div>

            <div className="field">
              <label htmlFor="res-defect">불량 수량 *</label>
              <input
                id="res-defect"
                type="number"
                min="0"
                value={resultForm.defectQuantity}
                onChange={(e) => setResultForm((p) => ({ ...p, defectQuantity: e.target.value }))}
                required
              />
            </div>

            <div className="field">
              <label htmlFor="res-worker">작업자명</label>
              <input
                id="res-worker"
                value={resultForm.workerName}
                onChange={(e) => setResultForm((p) => ({ ...p, workerName: e.target.value }))}
                placeholder="예: 김작업"
              />
            </div>

            <div className="field">
              <label htmlFor="res-remark">비고</label>
              <input
                id="res-remark"
                value={resultForm.remark}
                onChange={(e) => setResultForm((p) => ({ ...p, remark: e.target.value }))}
                placeholder="예: 1차 생산 완료"
              />
            </div>

            <div className="form-actions">
              <button type="submit" className="btn btn-success" disabled={submitting}>
                실적 등록 확인
              </button>
            </div>
          </form>

          {/* BOM 원재료 예상 소모량 실시간 미리보기 */}
          <div className="bom-preview-box">
            <div className="bom-preview-header">
              <span className="bom-preview-title">
                BOM 원재료 예상 소모량 미리보기 (총 생산 {formatNumber(totalInput)}{' '}
                {selectedOrder.unit} 기준)
              </span>
              {bomPreviewLoading ? (
                <span className="meta-text">BOM 및 재고 조회 중...</span>
              ) : selectedOrderBoms.length === 0 ? (
                <StatusBadge variant="neutral">등록된 BOM 없음 (완제품만 증가)</StatusBadge>
              ) : hasBomShortage ? (
                <StatusBadge variant="danger">
                  <IconAlert size={13} /> 원재료 재고 부족 (등록 불가 예상)
                </StatusBadge>
              ) : (
                <StatusBadge variant="success">
                  <IconCheckCircle size={13} /> 원재료 재고 충분
                </StatusBadge>
              )}
            </div>

            {selectedOrderBoms.length > 0 && (
              <div className="bom-preview-list">
                {bomRequirements.map((req) => (
                  <div
                    key={req.id}
                    className={`bom-preview-item ${req.shortage ? 'shortage' : ''}`}
                  >
                    <div>
                      <div style={{ fontWeight: 700 }}>
                        [{req.rawMaterialCode}] {req.rawMaterialName}
                      </div>
                      <div className="meta-text">
                        단위 소요 {req.requiredQuantity} {req.rawMaterialUnit} · 현재고{' '}
                        {formatNumber(req.available)} {req.rawMaterialUnit}
                      </div>
                    </div>
                    <div className="text-right">
                      <div
                        style={{
                          fontWeight: 800,
                          color: req.shortage ? '#b91c1c' : '#0f172a',
                        }}
                      >
                        -{formatNumber(req.needed)} {req.rawMaterialUnit}
                      </div>
                      <div
                        className="meta-text"
                        style={{
                          color: req.shortage ? '#b91c1c' : '#15803d',
                          fontWeight: 700,
                        }}
                      >
                        {req.shortage
                          ? `부족 ${formatNumber(Math.abs(req.remainingAfter))} ${req.rawMaterialUnit}`
                          : `잔여 ${formatNumber(req.remainingAfter)} ${req.rawMaterialUnit}`}
                      </div>
                    </div>
                  </div>
                ))}
              </div>
            )}
          </div>
        </div>
      )}

      <div className="card">
        <div className="card-header">
          <h3 className="card-title">
            작업지시 목록 ({filteredWorkOrders.length}
            {filteredWorkOrders.length !== workOrders.length
              ? ` / 전체 ${workOrders.length}`
              : ''}
            건)
          </h3>

          <SearchSortBar
            search={search}
            onSearchChange={setSearch}
            searchPlaceholder="지시번호·품목코드·완제품명 검색"
            sortValue={sortBy}
            onSortChange={setSortBy}
            sortOptions={SORT_OPTIONS}
          >
            <select value={filterStatus} onChange={(e) => setFilterStatus(e.target.value)}>
              <option value="">전체 상태</option>
              <option value="WAITING">대기</option>
              <option value="IN_PROGRESS">작업중</option>
              <option value="PAUSED">일시정지</option>
              <option value="COMPLETED">완료</option>
              <option value="CANCELED">취소</option>
            </select>
            <button
              type="button"
              className="btn btn-secondary btn-sm"
              onClick={loadAll}
              disabled={loading}
            >
              <IconRefresh size={14} />
              <span>새로고침</span>
            </button>
          </SearchSortBar>
        </div>

        {loading ? (
          <EmptyState loading message="작업지시 목록을 불러오는 중..." />
        ) : filteredWorkOrders.length === 0 ? (
          <EmptyState message="조건에 해당하는 작업지시가 없습니다." />
        ) : (
          <div className="table-wrap">
            <table>
              <thead>
                <tr>
                  <th>지시번호</th>
                  <th>완제품</th>
                  <th>예정일</th>
                  <th className="text-right">목표</th>
                  <th className="text-right">누적 양품</th>
                  <th className="text-right">누적 불량</th>
                  <th>진행률</th>
                  <th>상태</th>
                  <th>작업 처리</th>
                </tr>
              </thead>
              <tbody>
                {filteredWorkOrders.map((wo) => {
                  const canRecord = wo.status === 'WAITING' || wo.status === 'IN_PROGRESS';
                  const isPaused = wo.status === 'PAUSED';
                  const canCancel =
                    (wo.status === 'WAITING' || isPaused) &&
                    wo.producedGoodQuantity === 0 &&
                    wo.producedDefectQuantity === 0;
                  return (
                    <tr key={wo.id} className={isPaused ? 'row-paused' : ''}>
                      <td style={{ fontWeight: 700 }}>{wo.orderNumber}</td>
                      <td>
                        [{wo.itemCode}] {wo.itemName}
                      </td>
                      <td>{wo.plannedDate}</td>
                      <td className="text-right">
                        {formatNumber(wo.targetQuantity)} {wo.unit}
                      </td>
                      <td className="text-right" style={{ fontWeight: 700, color: '#15803d' }}>
                        {formatNumber(wo.producedGoodQuantity)}
                      </td>
                      <td
                        className="text-right"
                        style={{ color: wo.producedDefectQuantity > 0 ? '#b91c1c' : '#64748b' }}
                      >
                        {formatNumber(wo.producedDefectQuantity)}
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
                          {isPaused && <IconPause size={12} />}
                          {wo.statusDescription}
                        </StatusBadge>
                        {isPaused && wo.pauseReason && (
                          <div className="pause-meta">
                            {wo.pauseReason}
                            {wo.pausedAt ? ` · ${formatDateTime(wo.pausedAt)}` : ''}
                          </div>
                        )}
                      </td>
                      <td>
                        <div className="inline-actions">
                          {canRecord && (
                            <button
                              type="button"
                              className="btn btn-primary btn-sm"
                              onClick={() => selectOrderForResult(wo)}
                            >
                              실적 등록
                            </button>
                          )}
                          {wo.status === 'WAITING' && (
                            <button
                              type="button"
                              className="btn btn-secondary btn-sm"
                              onClick={() => requestStatusChange(wo, 'IN_PROGRESS')}
                            >
                              시작
                            </button>
                          )}
                          {wo.status === 'IN_PROGRESS' && (
                            <>
                              <button
                                type="button"
                                className="btn btn-warning btn-sm"
                                onClick={() => requestStatusChange(wo, 'PAUSED')}
                              >
                                <IconPause size={13} />
                                <span>일시정지</span>
                              </button>
                              <button
                                type="button"
                                className="btn btn-secondary btn-sm"
                                onClick={() => requestStatusChange(wo, 'COMPLETED')}
                              >
                                완료
                              </button>
                            </>
                          )}
                          {isPaused && (
                            <>
                              <button
                                type="button"
                                className="btn btn-primary btn-sm"
                                onClick={() => requestStatusChange(wo, 'IN_PROGRESS')}
                              >
                                <IconPlay size={13} />
                                <span>작업 재개</span>
                              </button>
                              <button
                                type="button"
                                className="btn btn-secondary btn-sm"
                                onClick={() => requestStatusChange(wo, 'COMPLETED')}
                              >
                                완료
                              </button>
                            </>
                          )}
                          {canCancel && (
                            <button
                              type="button"
                              className="btn btn-danger btn-sm"
                              onClick={() => requestStatusChange(wo, 'CANCELED')}
                            >
                              취소
                            </button>
                          )}
                        </div>
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        )}
      </div>

      <div className="card">
        <div className="card-header">
          <h3 className="card-title">
            생산실적 이력 ({filteredResults.length}
            {filteredResults.length !== results.length ? ` / 전체 ${results.length}` : ''}건)
          </h3>

          <SearchSortBar
            search={resultSearch}
            onSearchChange={setResultSearch}
            searchPlaceholder="지시번호·품목·작업자·비고 검색"
            sortValue={resultSortBy}
            onSortChange={setResultSortBy}
            sortOptions={RESULT_SORT_OPTIONS}
          >
            <label className="checkbox-label">
              <input
                type="checkbox"
                checked={groupByDate}
                onChange={(e) => setGroupByDate(e.target.checked)}
              />
              날짜별 분류 보기
            </label>
          </SearchSortBar>
        </div>

        <div className="result-toolbar">
          <div className="filter-bar">
            <div className="tabs" style={{ marginBottom: 0 }}>
              <button
                type="button"
                className={`tab-btn ${resultPeriod === 'all' ? 'active' : ''}`}
                onClick={() => setResultPeriod('all')}
              >
                전체 기간
              </button>
              <button
                type="button"
                className={`tab-btn ${resultPeriod === 'today' ? 'active' : ''}`}
                onClick={() => setResultPeriod('today')}
              >
                오늘
              </button>
              <button
                type="button"
                className={`tab-btn ${resultPeriod === 'week' ? 'active' : ''}`}
                onClick={() => setResultPeriod('week')}
              >
                최근 7일
              </button>
              <button
                type="button"
                className={`tab-btn ${resultPeriod === 'custom' ? 'active' : ''}`}
                onClick={() => setResultPeriod('custom')}
              >
                날짜 지정
              </button>
            </div>

            {resultPeriod === 'custom' && (
              <input
                type="date"
                aria-label="조회할 생산실적 날짜"
                value={resultCustomDate}
                onChange={(e) => setResultCustomDate(e.target.value)}
              />
            )}
          </div>

          {filteredResults.length > 0 && (
            <div className="result-summary-strip">
              <span className="result-summary-item">
                조회 실적 <strong>{formatNumber(resultSummary.count)}건</strong>
              </span>
              <span className="result-summary-item">
                총 생산량 <strong>{formatNumber(resultSummary.totalSum)}</strong>
              </span>
              <span className="result-summary-item">
                양품{' '}
                <strong style={{ color: '#15803d' }}>
                  {formatNumber(resultSummary.goodSum)}
                </strong>
              </span>
              <span className="result-summary-item">
                불량{' '}
                <strong style={{ color: resultSummary.defectSum > 0 ? '#b91c1c' : '#475569' }}>
                  {formatNumber(resultSummary.defectSum)}
                </strong>
              </span>
              <span className="result-summary-item">
                불량률{' '}
                <StatusBadge variant={resultSummary.defectRate >= 5 ? 'danger' : 'success'}>
                  {resultSummary.defectRate.toFixed(1)}%
                </StatusBadge>
              </span>
            </div>
          )}
        </div>

        {filteredResults.length === 0 ? (
          <EmptyState
            message={
              results.length === 0
                ? '등록된 생산실적이 없습니다.'
                : '선택한 날짜 또는 검색 조건에 해당하는 생산실적이 없습니다.'
            }
          />
        ) : (
          <div className="table-wrap">
            <table>
              <thead>
                <tr>
                  <th>등록일시</th>
                  <th>지시번호</th>
                  <th>완제품</th>
                  <th className="text-right">양품</th>
                  <th className="text-right">불량</th>
                  <th className="text-right">합계</th>
                  <th>작업자</th>
                  <th>비고</th>
                </tr>
              </thead>
              {groupByDate ? (
                groupedResults.map((group) => {
                  const isCollapsed = Boolean(collapsedDates[group.dateKey]);
                  return (
                    <tbody key={group.dateKey}>
                      <tr
                        className="date-group-row"
                        onClick={() => toggleDateGroup(group.dateKey)}
                        title="클릭하여 해당 날짜 실적 접기/펼치기"
                      >
                        <td colSpan={8}>
                          <div className="date-group-header">
                            <span className="date-group-title">
                              <span>{isCollapsed ? '▸' : '▾'}</span>
                              <span>{group.dateKey}</span>
                              <StatusBadge variant="info">{group.items.length}건</StatusBadge>
                            </span>
                            <span className="date-group-metrics">
                              <span style={{ color: '#15803d' }}>
                                양품 {formatNumber(group.goodSum)}
                              </span>
                              <span
                                style={{
                                  color: group.defectSum > 0 ? '#b91c1c' : '#475569',
                                }}
                              >
                                불량 {formatNumber(group.defectSum)}
                              </span>
                              <span>합계 {formatNumber(group.totalSum)}</span>
                              <StatusBadge variant={group.defectRate >= 5 ? 'danger' : 'neutral'}>
                                불량률 {group.defectRate.toFixed(1)}%
                              </StatusBadge>
                            </span>
                          </div>
                        </td>
                      </tr>
                      {!isCollapsed &&
                        group.items.map((r) => (
                          <tr key={r.id}>
                            <td>{formatDateTime(r.createdAt)}</td>
                            <td style={{ fontWeight: 700 }}>{r.orderNumber}</td>
                            <td>
                              [{r.itemCode}] {r.itemName}
                            </td>
                            <td className="text-right" style={{ fontWeight: 800, color: '#15803d' }}>
                              {formatNumber(r.goodQuantity)} {r.unit}
                            </td>
                            <td
                              className="text-right"
                              style={{ color: r.defectQuantity > 0 ? '#b91c1c' : '#64748b' }}
                            >
                              {formatNumber(r.defectQuantity)}
                            </td>
                            <td className="text-right">{formatNumber(r.totalQuantity)}</td>
                            <td>{r.workerName || '-'}</td>
                            <td>{r.remark || '-'}</td>
                          </tr>
                        ))}
                    </tbody>
                  );
                })
              ) : (
                <tbody>
                  {filteredResults.map((r) => (
                    <tr key={r.id}>
                      <td>{formatDateTime(r.createdAt)}</td>
                      <td style={{ fontWeight: 700 }}>{r.orderNumber}</td>
                      <td>
                        [{r.itemCode}] {r.itemName}
                      </td>
                      <td className="text-right" style={{ fontWeight: 800, color: '#15803d' }}>
                        {formatNumber(r.goodQuantity)} {r.unit}
                      </td>
                      <td
                        className="text-right"
                        style={{ color: r.defectQuantity > 0 ? '#b91c1c' : '#64748b' }}
                      >
                        {formatNumber(r.defectQuantity)}
                      </td>
                      <td className="text-right">{formatNumber(r.totalQuantity)}</td>
                      <td>{r.workerName || '-'}</td>
                      <td>{r.remark || '-'}</td>
                    </tr>
                  ))}
                </tbody>
              )}
            </table>
          </div>
        )}
      </div>

      {/* 작업지시 재개/완료/취소 확인 모달 */}
      <ConfirmModal
        open={Boolean(pendingStatusChange)}
        title={
          pendingStatusChange?.nextStatus === 'IN_PROGRESS'
            ? '작업지시 재개 확인'
            : pendingStatusChange?.nextStatus === 'COMPLETED'
            ? '작업지시 완료 확인'
            : '작업지시 취소 확인'
        }
        message={
          pendingStatusChange?.nextStatus === 'IN_PROGRESS'
            ? '일시정지된 작업지시를 작업중(IN_PROGRESS) 상태로 재개하시겠습니까?'
            : pendingStatusChange?.nextStatus === 'COMPLETED'
            ? '이 작업지시를 완료 상태로 마감하시겠습니까?'
            : '이 작업지시를 취소하시겠습니까?'
        }
        details={
          pendingStatusChange
            ? pendingStatusChange.nextStatus === 'IN_PROGRESS'
              ? [
                  `작업지시: ${pendingStatusChange.order.orderNumber} (${pendingStatusChange.order.itemName})`,
                  ...(pendingStatusChange.order.pauseReason
                    ? [`일시정지 사유: ${pendingStatusChange.order.pauseReason}`]
                    : []),
                  ...(pendingStatusChange.order.pausedAt
                    ? [`일시정지 시각: ${formatDateTime(pendingStatusChange.order.pausedAt)}`]
                    : []),
                  '재개 즉시 생산실적 등록이 다시 가능해지며, 일시정지 사유는 해제됩니다.',
                ]
              : [
                  `작업지시: ${pendingStatusChange.order.orderNumber} (${pendingStatusChange.order.itemName})`,
                  `현재 누적 양품: ${formatNumber(
                    pendingStatusChange.order.producedGoodQuantity
                  )} / 목표 ${formatNumber(pendingStatusChange.order.targetQuantity)} ${
                    pendingStatusChange.order.unit
                  }`,
                  '완료 또는 취소된 작업지시는 다시 되돌리거나 추가 실적을 등록할 수 없습니다.',
                ]
            : []
        }
        confirmLabel={
          pendingStatusChange?.nextStatus === 'IN_PROGRESS'
            ? '작업 재개'
            : pendingStatusChange?.nextStatus === 'COMPLETED'
            ? '작업 완료 처리'
            : '작업 취소'
        }
        tone={
          pendingStatusChange?.nextStatus === 'IN_PROGRESS'
            ? 'primary'
            : pendingStatusChange?.nextStatus === 'COMPLETED'
            ? 'warning'
            : 'danger'
        }
        loading={submitting}
        onConfirm={() =>
          pendingStatusChange &&
          executeStatusChange(pendingStatusChange.order, pendingStatusChange.nextStatus)
        }
        onCancel={() => setPendingStatusChange(null)}
      />

      {/* 작업지시 일시정지 사유 선택/입력 모달 */}
      <PauseWorkOrderModal
        order={pausingOrder}
        loading={submitting}
        onConfirm={(reason) =>
          pausingOrder && executeStatusChange(pausingOrder, 'PAUSED', reason)
        }
        onCancel={() => setPausingOrder(null)}
      />

      {/* 생산실적 등록 및 재고 반영 확인 모달 */}
      <ConfirmModal
        open={Boolean(pendingResultSubmit)}
        title="생산실적 등록 및 재고 반영 확인"
        message="아래 내용으로 생산실적을 등록하고 원재료·완제품 재고를 반영하시겠습니까?"
        details={
          pendingResultSubmit
            ? [
                `작업지시: ${pendingResultSubmit.order.orderNumber} (${pendingResultSubmit.order.itemName})`,
                `양품 +${formatNumber(pendingResultSubmit.goodQuantity)} ${
                  pendingResultSubmit.order.unit
                } (완제품 재고 증가) / 불량 ${formatNumber(pendingResultSubmit.defectQuantity)} ${
                  pendingResultSubmit.order.unit
                }`,
                ...(pendingResultSubmit.bomRequirements.length > 0
                  ? pendingResultSubmit.bomRequirements.map(
                      (req) =>
                        `원재료 차감: ${req.rawMaterialName} -${formatNumber(req.needed)} ${
                          req.rawMaterialUnit
                        } (${
                          req.shortage
                            ? `재고 부족! 현재 ${formatNumber(req.available)}`
                            : `차감 후 ${formatNumber(req.remainingAfter)}`
                        })`
                    )
                  : ['등록된 BOM 없음: 원재료 차감 없이 완제품 재고만 증가합니다.']),
                ...(pendingResultSubmit.willAutoComplete
                  ? ['목표 수량 달성으로 작업지시가 자동 완료(COMPLETED) 처리됩니다.']
                  : []),
              ]
            : []
        }
        confirmLabel="실적 등록 실행"
        tone={pendingResultSubmit?.hasBomShortage ? 'danger' : 'success'}
        loading={submitting}
        onConfirm={executeRecordResult}
        onCancel={() => setPendingResultSubmit(null)}
      />
    </div>
  );
}
