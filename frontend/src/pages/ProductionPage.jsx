import { useEffect, useState } from 'react';
import { itemApi, productionApi } from '../api/endpoints';
import StatusBadge, { workOrderStatusVariant } from '../components/StatusBadge';
import { formatDateTime, formatNumber, getTodayIso } from '../utils/format';

export default function ProductionPage() {
  const [workOrders, setWorkOrders] = useState([]);
  const [results, setResults] = useState([]);
  const [finishedItems, setFinishedItems] = useState([]);
  const [filterStatus, setFilterStatus] = useState('');

  const [orderForm, setOrderForm] = useState({
    orderNumber: '',
    itemId: '',
    targetQuantity: '',
    plannedDate: getTodayIso(),
    remark: '',
  });

  const [selectedOrder, setSelectedOrder] = useState(null);
  const [resultForm, setResultForm] = useState({
    goodQuantity: '',
    defectQuantity: '0',
    workerName: '',
    remark: '',
  });

  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState('');
  const [notice, setNotice] = useState('');

  async function loadFinishedItems() {
    const res = await itemApi.list({ itemType: 'FINISHED_GOOD', active: true });
    setFinishedItems(res.data || []);
  }

  async function loadWorkOrders(statusValue = filterStatus) {
    const res = await productionApi.listWorkOrders({
      status: statusValue || undefined,
    });
    setWorkOrders(res.data || []);
  }

  async function loadResults() {
    const res = await productionApi.listAllResults();
    setResults(res.data || []);
  }

  async function loadAll() {
    setLoading(true);
    setError('');
    try {
      await Promise.all([loadFinishedItems(), loadWorkOrders(), loadResults()]);
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    loadAll();
  }, []);

  useEffect(() => {
    loadWorkOrders(filterStatus).catch((err) => setError(err.message));
  }, [filterStatus]);

  async function handleCreateOrder(e) {
    e.preventDefault();
    setSubmitting(true);
    setError('');
    setNotice('');
    try {
      const res = await productionApi.createWorkOrder({
        orderNumber: orderForm.orderNumber.trim() || null,
        itemId: Number(orderForm.itemId),
        targetQuantity: Number(orderForm.targetQuantity),
        plannedDate: orderForm.plannedDate,
        remark: orderForm.remark,
      });
      setNotice(res.message || '작업지시가 등록되었습니다.');
      setOrderForm({
        orderNumber: '',
        itemId: '',
        targetQuantity: '',
        plannedDate: getTodayIso(),
        remark: '',
      });
      await loadWorkOrders();
    } catch (err) {
      setError(err.message);
    } finally {
      setSubmitting(false);
    }
  }

  async function handleChangeStatus(order, nextStatus) {
    setError('');
    setNotice('');
    try {
      const res = await productionApi.updateWorkOrderStatus(order.id, nextStatus);
      setNotice(res.message || '작업지시 상태가 변경되었습니다.');
      if (selectedOrder && selectedOrder.id === order.id) {
        setSelectedOrder(res.data);
      }
      await loadWorkOrders();
    } catch (err) {
      setError(err.message);
    }
  }

  async function handleRecordResult(e) {
    e.preventDefault();
    if (!selectedOrder) return;
    setSubmitting(true);
    setError('');
    setNotice('');
    try {
      const res = await productionApi.recordResult(selectedOrder.id, {
        goodQuantity: Number(resultForm.goodQuantity),
        defectQuantity: Number(resultForm.defectQuantity),
        workerName: resultForm.workerName,
        remark: resultForm.remark,
      });
      setNotice(res.message || '생산실적이 등록되고 재고가 반영되었습니다.');
      setResultForm({
        goodQuantity: '',
        defectQuantity: '0',
        workerName: resultForm.workerName,
        remark: '',
      });
      if (res.data?.workOrderStatus === 'COMPLETED') {
        setSelectedOrder(null);
      }
      await Promise.all([loadWorkOrders(), loadResults()]);
    } catch (err) {
      setError(err.message);
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <div>
      {error && <div className="alert alert-error">{error}</div>}
      {notice && <div className="alert alert-success">{notice}</div>}

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
        <div className="card" style={{ borderColor: '#93c5fd', backgroundColor: '#f8fbff' }}>
          <div className="card-header">
            <h3 className="card-title">
              생산실적 등록 — {selectedOrder.orderNumber} ({selectedOrder.itemName})
            </h3>
            <button
              type="button"
              className="btn btn-secondary btn-sm"
              onClick={() => setSelectedOrder(null)}
            >
              닫기
            </button>
          </div>
          <p style={{ marginTop: 0, fontSize: 13, color: '#475569' }}>
            실적을 등록하면 <strong>BOM에 설정된 원재료가 (양품 + 불량) 기준으로 자동 차감</strong>되고,{' '}
            <strong>완제품 재고가 양품 수량만큼 자동 증가</strong>합니다. 누적 양품이 목표 수량(
            {formatNumber(selectedOrder.targetQuantity)} {selectedOrder.unit})에 도달하면 작업이 자동 완료됩니다.
          </p>

          <form className="form-grid" onSubmit={handleRecordResult}>
            <div className="field">
              <label htmlFor="res-good">양품 수량 *</label>
              <input
                id="res-good"
                type="number"
                min="0"
                value={resultForm.goodQuantity}
                onChange={(e) => setResultForm((p) => ({ ...p, goodQuantity: e.target.value }))}
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
                {submitting ? '반영 중...' : '실적 등록 및 재고 반영'}
              </button>
            </div>
          </form>
        </div>
      )}

      <div className="card">
        <div className="card-header">
          <h3 className="card-title">작업지시 목록 ({workOrders.length}건)</h3>
          <div className="filter-bar">
            <select value={filterStatus} onChange={(e) => setFilterStatus(e.target.value)}>
              <option value="">전체 상태</option>
              <option value="WAITING">대기</option>
              <option value="IN_PROGRESS">작업중</option>
              <option value="COMPLETED">완료</option>
              <option value="CANCELED">취소</option>
            </select>
            <button className="btn btn-secondary btn-sm" onClick={loadAll} disabled={loading}>
              새로고침
            </button>
          </div>
        </div>

        {loading ? (
          <div className="empty-state">작업지시 목록을 불러오는 중...</div>
        ) : workOrders.length === 0 ? (
          <div className="empty-state">등록된 작업지시가 없습니다.</div>
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
                {workOrders.map((wo) => {
                  const canRecord = wo.status === 'WAITING' || wo.status === 'IN_PROGRESS';
                  return (
                    <tr key={wo.id}>
                      <td style={{ fontWeight: 600 }}>{wo.orderNumber}</td>
                      <td>
                        [{wo.itemCode}] {wo.itemName}
                      </td>
                      <td>{wo.plannedDate}</td>
                      <td className="text-right">
                        {formatNumber(wo.targetQuantity)} {wo.unit}
                      </td>
                      <td className="text-right" style={{ fontWeight: 600, color: '#16a34a' }}>
                        {formatNumber(wo.producedGoodQuantity)}
                      </td>
                      <td className="text-right" style={{ color: wo.producedDefectQuantity > 0 ? '#dc2626' : '#64748b' }}>
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
                          {wo.statusDescription}
                        </StatusBadge>
                      </td>
                      <td>
                        <div className="inline-actions">
                          {canRecord && (
                            <button
                              type="button"
                              className="btn btn-primary btn-sm"
                              onClick={() => setSelectedOrder(wo)}
                            >
                              실적 등록
                            </button>
                          )}
                          {wo.status === 'WAITING' && (
                            <button
                              type="button"
                              className="btn btn-secondary btn-sm"
                              onClick={() => handleChangeStatus(wo, 'IN_PROGRESS')}
                            >
                              시작
                            </button>
                          )}
                          {wo.status === 'IN_PROGRESS' && (
                            <button
                              type="button"
                              className="btn btn-secondary btn-sm"
                              onClick={() => handleChangeStatus(wo, 'COMPLETED')}
                            >
                              완료
                            </button>
                          )}
                          {wo.status === 'WAITING' &&
                            wo.producedGoodQuantity === 0 &&
                            wo.producedDefectQuantity === 0 && (
                              <button
                                type="button"
                                className="btn btn-danger btn-sm"
                                onClick={() => handleChangeStatus(wo, 'CANCELED')}
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
          <h3 className="card-title">최근 생산실적 이력 ({results.length}건)</h3>
        </div>

        {results.length === 0 ? (
          <div className="empty-state">등록된 생산실적이 없습니다.</div>
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
              <tbody>
                {results.map((r) => (
                  <tr key={r.id}>
                    <td>{formatDateTime(r.createdAt)}</td>
                    <td>{r.orderNumber}</td>
                    <td>
                      [{r.itemCode}] {r.itemName}
                    </td>
                    <td className="text-right" style={{ fontWeight: 700, color: '#16a34a' }}>
                      {formatNumber(r.goodQuantity)} {r.unit}
                    </td>
                    <td className="text-right" style={{ color: r.defectQuantity > 0 ? '#dc2626' : '#64748b' }}>
                      {formatNumber(r.defectQuantity)}
                    </td>
                    <td className="text-right">{formatNumber(r.totalQuantity)}</td>
                    <td>{r.workerName || '-'}</td>
                    <td>{r.remark || '-'}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>
    </div>
  );
}
