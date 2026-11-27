import { useCallback, useEffect, useMemo, useState } from 'react';
import { inventoryApi, itemApi, partnerApi } from '../api/endpoints';
import StatusBadge, { transactionTypeVariant } from '../components/StatusBadge';
import ConfirmModal from '../components/ui/ConfirmModal';
import EmptyState from '../components/ui/EmptyState';
import { IconRefresh } from '../components/ui/Icons';
import SearchSortBar from '../components/ui/SearchSortBar';
import { useToast } from '../context/ToastContext';
import { formatDateTime, formatNumber, notifyDataChanged } from '../utils/format';

const SORT_OPTIONS = [
  { value: 'shortageFirst', label: '정렬: 안전재고 부족 우선' },
  { value: 'qtyAsc', label: '정렬: 현재고 적은순' },
  { value: 'qtyDesc', label: '정렬: 현재고 많은순' },
  { value: 'codeAsc', label: '정렬: 품목코드순' },
];

export default function InventoryPage() {
  const toast = useToast();
  const [activeTab, setActiveTab] = useState('inbound');
  const [inventories, setInventories] = useState([]);
  const [histories, setHistories] = useState([]);
  const [activeItems, setActiveItems] = useState([]);
  const [activePartners, setActivePartners] = useState([]);

  const [filterItemType, setFilterItemType] = useState('');
  const [belowSafetyOnly, setBelowSafetyOnly] = useState(false);
  const [search, setSearch] = useState('');
  const [sortBy, setSortBy] = useState('shortageFirst');

  const [historyItemId, setHistoryItemId] = useState('');
  const [historyTxType, setHistoryTxType] = useState('');

  const [txForm, setTxForm] = useState({
    itemId: '',
    partnerId: '',
    quantity: '',
    targetQuantity: '',
    remark: '',
  });
  const [pendingAdjust, setPendingAdjust] = useState(null);

  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);

  const loadMasterData = useCallback(async () => {
    const [itemRes, partnerRes] = await Promise.all([
      itemApi.list({ active: true }),
      partnerApi.list({ active: true }),
    ]);
    setActiveItems(itemRes.data || []);
    setActivePartners(partnerRes.data || []);
  }, []);

  const loadInventories = useCallback(async () => {
    const res = await inventoryApi.list({
      itemType: filterItemType || undefined,
      belowSafetyOnly: belowSafetyOnly ? true : undefined,
    });
    setInventories(res.data || []);
  }, [filterItemType, belowSafetyOnly]);

  const loadHistories = useCallback(async () => {
    const res = await inventoryApi.histories({
      itemId: historyItemId || undefined,
      transactionType: historyTxType || undefined,
    });
    setHistories(res.data || []);
  }, [historyItemId, historyTxType]);

  const loadAll = useCallback(async () => {
    setLoading(true);
    try {
      await Promise.all([loadMasterData(), loadInventories(), loadHistories()]);
    } catch (err) {
      toast.error(err.message);
    } finally {
      setLoading(false);
    }
  }, [loadMasterData, loadInventories, loadHistories, toast]);

  useEffect(() => {
    loadAll();
  }, [loadAll]);

  useEffect(() => {
    loadInventories().catch((err) => toast.error(err.message));
  }, [loadInventories, toast]);

  useEffect(() => {
    loadHistories().catch((err) => toast.error(err.message));
  }, [loadHistories, toast]);

  const filteredInventories = useMemo(() => {
    const keyword = search.trim().toLowerCase();
    const list = keyword
      ? inventories.filter(
          (inv) =>
            inv.itemCode.toLowerCase().includes(keyword) ||
            inv.itemName.toLowerCase().includes(keyword) ||
            (inv.spec && inv.spec.toLowerCase().includes(keyword))
        )
      : [...inventories];

    switch (sortBy) {
      case 'shortageFirst':
        list.sort((a, b) => {
          const aAlert = a.active && a.belowSafetyStock ? 0 : 1;
          const bAlert = b.active && b.belowSafetyStock ? 0 : 1;
          if (aAlert !== bAlert) return aAlert - bAlert;
          return a.quantity - b.quantity;
        });
        break;
      case 'qtyAsc':
        list.sort((a, b) => a.quantity - b.quantity);
        break;
      case 'qtyDesc':
        list.sort((a, b) => b.quantity - a.quantity);
        break;
      case 'codeAsc':
        list.sort((a, b) => a.itemCode.localeCompare(b.itemCode));
        break;
      default:
        break;
    }
    return list;
  }, [inventories, search, sortBy]);

  function handleChange(e) {
    const { name, value } = e.target;
    setTxForm((prev) => ({ ...prev, [name]: value }));
  }

  async function executeAdjust(adjustData) {
    setSubmitting(true);
    try {
      const res = await inventoryApi.adjust({
        itemId: adjustData.itemId,
        targetQuantity: adjustData.targetQuantity,
        remark: adjustData.remark,
      });
      toast.success(res.message || '재고 조정이 완료되었습니다.');
      setPendingAdjust(null);
      setTxForm((prev) => ({
        ...prev,
        targetQuantity: '',
        remark: '',
      }));
      await Promise.all([loadInventories(), loadHistories()]);
      notifyDataChanged();
    } catch (err) {
      toast.error(err.message);
    } finally {
      setSubmitting(false);
    }
  }

  async function handleTransaction(e) {
    e.preventDefault();

    if (activeTab === 'adjust') {
      const itemId = Number(txForm.itemId);
      const targetQuantity = Number(txForm.targetQuantity);
      const item = activeItems.find((i) => i.id === itemId);
      const currentInv = inventories.find((inv) => inv.itemId === itemId);
      const currentQty = currentInv ? currentInv.quantity : 0;
      setPendingAdjust({
        itemId,
        itemLabel: item ? `[${item.itemCode}] ${item.itemName}` : `품목 #${itemId}`,
        unit: item ? item.unit : '',
        currentQty,
        targetQuantity,
        delta: targetQuantity - currentQty,
        remark: txForm.remark.trim(),
      });
      return;
    }

    setSubmitting(true);
    try {
      let res;
      if (activeTab === 'inbound') {
        res = await inventoryApi.inbound({
          itemId: Number(txForm.itemId),
          partnerId: txForm.partnerId ? Number(txForm.partnerId) : null,
          quantity: Number(txForm.quantity),
          remark: txForm.remark,
        });
      } else {
        res = await inventoryApi.outbound({
          itemId: Number(txForm.itemId),
          partnerId: txForm.partnerId ? Number(txForm.partnerId) : null,
          quantity: Number(txForm.quantity),
          remark: txForm.remark,
        });
      }

      toast.success(res.message || '재고 처리가 완료되었습니다.');
      setTxForm((prev) => ({
        ...prev,
        quantity: '',
        remark: '',
      }));
      await Promise.all([loadInventories(), loadHistories()]);
      notifyDataChanged();
    } catch (err) {
      toast.error(err.message);
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <div>
      <div className="card">
        <div className="card-header">
          <h3 className="card-title">재고 입출고 · 수량 조정</h3>
        </div>

        <div className="tabs">
          <button
            type="button"
            className={`tab-btn ${activeTab === 'inbound' ? 'active' : ''}`}
            onClick={() => setActiveTab('inbound')}
          >
            입고 (+)
          </button>
          <button
            type="button"
            className={`tab-btn ${activeTab === 'outbound' ? 'active' : ''}`}
            onClick={() => setActiveTab('outbound')}
          >
            출고 (-)
          </button>
          <button
            type="button"
            className={`tab-btn ${activeTab === 'adjust' ? 'active' : ''}`}
            onClick={() => setActiveTab('adjust')}
          >
            재고 조정 (실사)
          </button>
        </div>

        <form className="form-grid" onSubmit={handleTransaction}>
          <div className="field">
            <label htmlFor="inv-item">품목 *</label>
            <select
              id="inv-item"
              name="itemId"
              value={txForm.itemId}
              onChange={handleChange}
              required
            >
              <option value="">품목 선택</option>
              {activeItems.map((item) => (
                <option key={item.id} value={item.id}>
                  [{item.itemCode}] {item.itemName} ({item.itemTypeDescription})
                </option>
              ))}
            </select>
          </div>

          {activeTab !== 'adjust' ? (
            <>
              <div className="field">
                <label htmlFor="inv-partner">거래처 (선택)</label>
                <select
                  id="inv-partner"
                  name="partnerId"
                  value={txForm.partnerId}
                  onChange={handleChange}
                >
                  <option value="">거래처 미지정</option>
                  {activePartners.map((p) => (
                    <option key={p.id} value={p.id}>
                      [{p.partnerCode}] {p.partnerName} ({p.partnerTypeDescription})
                    </option>
                  ))}
                </select>
              </div>

              <div className="field">
                <label htmlFor="inv-qty">
                  {activeTab === 'inbound' ? '입고 수량 *' : '출고 수량 *'}
                </label>
                <input
                  id="inv-qty"
                  name="quantity"
                  type="number"
                  min="1"
                  value={txForm.quantity}
                  onChange={handleChange}
                  required
                />
              </div>
            </>
          ) : (
            <div className="field">
              <label htmlFor="inv-target">조정 후 실사 수량 *</label>
              <input
                id="inv-target"
                name="targetQuantity"
                type="number"
                min="0"
                value={txForm.targetQuantity}
                onChange={handleChange}
                required
              />
            </div>
          )}

          <div className="field">
            <label htmlFor="inv-remark">
              {activeTab === 'adjust' ? '조정 사유 (필수) *' : '비고'}
            </label>
            <input
              id="inv-remark"
              name="remark"
              value={txForm.remark}
              onChange={handleChange}
              placeholder={
                activeTab === 'adjust' ? '예: 월말 재고 실사 반영' : '예: 원단 정기 입고'
              }
              required={activeTab === 'adjust'}
            />
          </div>

          <div className="form-actions">
            <button type="submit" className="btn btn-primary" disabled={submitting}>
              {submitting
                ? '처리 중...'
                : activeTab === 'inbound'
                ? '입고 등록'
                : activeTab === 'outbound'
                ? '출고 등록'
                : '재고 조정 확인'}
            </button>
          </div>
        </form>
      </div>

      <div className="card">
        <div className="card-header">
          <h3 className="card-title">
            현재고 현황 ({filteredInventories.length}
            {filteredInventories.length !== inventories.length
              ? ` / 전체 ${inventories.length}`
              : ''}
            건)
          </h3>

          <SearchSortBar
            search={search}
            onSearchChange={setSearch}
            searchPlaceholder="품목코드·품목명·규격 검색"
            sortValue={sortBy}
            onSortChange={setSortBy}
            sortOptions={SORT_OPTIONS}
          >
            <select value={filterItemType} onChange={(e) => setFilterItemType(e.target.value)}>
              <option value="">전체 구분</option>
              <option value="RAW_MATERIAL">원재료</option>
              <option value="FINISHED_GOOD">완제품</option>
            </select>
            <label className="checkbox-label">
              <input
                type="checkbox"
                checked={belowSafetyOnly}
                onChange={(e) => setBelowSafetyOnly(e.target.checked)}
              />
              안전재고 미만만
            </label>
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
          <EmptyState loading message="현재고 정보를 불러오는 중..." />
        ) : filteredInventories.length === 0 ? (
          <EmptyState message="조건에 해당하는 재고 정보가 없습니다." />
        ) : (
          <div className="table-wrap">
            <table>
              <thead>
                <tr>
                  <th>품목코드</th>
                  <th>품목명</th>
                  <th>구분</th>
                  <th>규격</th>
                  <th className="text-right">현재고</th>
                  <th className="text-right">안전재고</th>
                  <th>재고 상태</th>
                  <th>최근 변동</th>
                </tr>
              </thead>
              <tbody>
                {filteredInventories.map((inv) => (
                  <tr
                    key={inv.itemId}
                    className={
                      !inv.active
                        ? 'row-inactive'
                        : inv.belowSafetyStock
                        ? 'row-warning'
                        : ''
                    }
                  >
                    <td style={{ fontWeight: 700 }}>{inv.itemCode}</td>
                    <td style={{ fontWeight: 600 }}>{inv.itemName}</td>
                    <td>
                      <StatusBadge variant={inv.itemType === 'RAW_MATERIAL' ? 'warning' : 'info'}>
                        {inv.itemTypeDescription}
                      </StatusBadge>
                    </td>
                    <td>{inv.spec || '-'}</td>
                    <td
                      className="text-right"
                      style={{
                        fontWeight: 800,
                        color: inv.belowSafetyStock && inv.active ? '#b91c1c' : '#0f172a',
                      }}
                    >
                      {formatNumber(inv.quantity)} {inv.unit}
                    </td>
                    <td className="text-right">
                      {formatNumber(inv.safetyStock)} {inv.unit}
                    </td>
                    <td>
                      {!inv.active ? (
                        <StatusBadge variant="neutral">비활성</StatusBadge>
                      ) : inv.belowSafetyStock ? (
                        <StatusBadge variant="danger">안전재고 미만</StatusBadge>
                      ) : (
                        <StatusBadge variant="success">정상</StatusBadge>
                      )}
                    </td>
                    <td>{formatDateTime(inv.updatedAt)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>

      <div className="card">
        <div className="card-header">
          <h3 className="card-title">재고 수불 이력 ({histories.length}건)</h3>
          <div className="filter-bar">
            <select value={historyItemId} onChange={(e) => setHistoryItemId(e.target.value)}>
              <option value="">전체 품목</option>
              {activeItems.map((item) => (
                <option key={item.id} value={item.id}>
                  [{item.itemCode}] {item.itemName}
                </option>
              ))}
            </select>
            <select value={historyTxType} onChange={(e) => setHistoryTxType(e.target.value)}>
              <option value="">전체 유형</option>
              <option value="INBOUND">입고</option>
              <option value="OUTBOUND">출고</option>
              <option value="PRODUCTION_IN">생산 입고</option>
              <option value="PRODUCTION_OUT">생산 투입</option>
              <option value="ADJUST">재고 조정</option>
            </select>
          </div>
        </div>

        {histories.length === 0 ? (
          <EmptyState message="수불 이력이 없습니다." />
        ) : (
          <div className="table-wrap">
            <table>
              <thead>
                <tr>
                  <th>일시</th>
                  <th>유형</th>
                  <th>품목</th>
                  <th>거래처</th>
                  <th className="text-right">변동 전</th>
                  <th className="text-right">변동량</th>
                  <th className="text-right">변동 후</th>
                  <th>비고</th>
                </tr>
              </thead>
              <tbody>
                {histories.map((h) => (
                  <tr key={h.id}>
                    <td>{formatDateTime(h.createdAt)}</td>
                    <td>
                      <StatusBadge variant={transactionTypeVariant(h.transactionType)}>
                        {h.transactionTypeDescription}
                      </StatusBadge>
                    </td>
                    <td>
                      [{h.itemCode}] {h.itemName}
                    </td>
                    <td>{h.partnerName || '-'}</td>
                    <td className="text-right">{formatNumber(h.beforeQuantity)}</td>
                    <td
                      className="text-right"
                      style={{
                        fontWeight: 800,
                        color: h.changeQuantity >= 0 ? '#15803d' : '#b91c1c',
                      }}
                    >
                      {h.changeQuantity >= 0 ? '+' : ''}
                      {formatNumber(h.changeQuantity)} {h.unit}
                    </td>
                    <td className="text-right" style={{ fontWeight: 700 }}>
                      {formatNumber(h.afterQuantity)} {h.unit}
                    </td>
                    <td>{h.remark || '-'}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>

      <ConfirmModal
        open={Boolean(pendingAdjust)}
        title="재고 실사 조정 확인"
        message="현재 재고 수량을 실사 수량으로 직접 조정하시겠습니까?"
        details={
          pendingAdjust
            ? [
                `대상 품목: ${pendingAdjust.itemLabel}`,
                `현재고 ${formatNumber(pendingAdjust.currentQty)} ${pendingAdjust.unit} → 조정 후 ${formatNumber(
                  pendingAdjust.targetQuantity
                )} ${pendingAdjust.unit} (차이 ${pendingAdjust.delta >= 0 ? '+' : ''}${formatNumber(
                  pendingAdjust.delta
                )} ${pendingAdjust.unit})`,
                `조정 사유: ${pendingAdjust.remark}`,
              ]
            : []
        }
        confirmLabel="조정 반영"
        tone="warning"
        loading={submitting}
        onConfirm={() => pendingAdjust && executeAdjust(pendingAdjust)}
        onCancel={() => setPendingAdjust(null)}
      />
    </div>
  );
}
