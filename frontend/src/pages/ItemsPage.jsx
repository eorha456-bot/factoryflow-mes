import { useCallback, useEffect, useMemo, useState } from 'react';
import { itemApi } from '../api/endpoints';
import StatusBadge from '../components/StatusBadge';
import ConfirmModal from '../components/ui/ConfirmModal';
import EmptyState from '../components/ui/EmptyState';
import { IconRefresh } from '../components/ui/Icons';
import SearchSortBar from '../components/ui/SearchSortBar';
import { useToast } from '../context/ToastContext';
import { formatNumber, notifyDataChanged } from '../utils/format';

const INITIAL_FORM = {
  itemCode: '',
  itemName: '',
  itemType: 'RAW_MATERIAL',
  spec: '',
  unit: '매',
  safetyStock: '0',
};

const SORT_OPTIONS = [
  { value: 'latest', label: '정렬: 최신 등록순' },
  { value: 'codeAsc', label: '정렬: 품목코드순' },
  { value: 'nameAsc', label: '정렬: 품목명순' },
  { value: 'safetyDesc', label: '정렬: 안전재고 많은순' },
];

export default function ItemsPage() {
  const toast = useToast();
  const [items, setItems] = useState([]);
  const [filterType, setFilterType] = useState('');
  const [filterActive, setFilterActive] = useState('');
  const [search, setSearch] = useState('');
  const [sortBy, setSortBy] = useState('latest');

  const [form, setForm] = useState(INITIAL_FORM);
  const [editingId, setEditingId] = useState(null);
  const [confirmItem, setConfirmItem] = useState(null);
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);

  const loadItems = useCallback(async () => {
    setLoading(true);
    try {
      const res = await itemApi.list({
        itemType: filterType || undefined,
        active: filterActive === '' ? undefined : filterActive,
      });
      setItems(res.data || []);
    } catch (err) {
      toast.error(err.message);
    } finally {
      setLoading(false);
    }
  }, [filterType, filterActive, toast]);

  useEffect(() => {
    loadItems();
  }, [loadItems]);

  const filteredItems = useMemo(() => {
    const keyword = search.trim().toLowerCase();
    const list = keyword
      ? items.filter(
          (item) =>
            item.itemCode.toLowerCase().includes(keyword) ||
            item.itemName.toLowerCase().includes(keyword) ||
            (item.spec && item.spec.toLowerCase().includes(keyword))
        )
      : [...items];

    switch (sortBy) {
      case 'codeAsc':
        list.sort((a, b) => a.itemCode.localeCompare(b.itemCode));
        break;
      case 'nameAsc':
        list.sort((a, b) => a.itemName.localeCompare(b.itemName, 'ko'));
        break;
      case 'safetyDesc':
        list.sort((a, b) => b.safetyStock - a.safetyStock);
        break;
      default:
        break;
    }
    return list;
  }, [items, search, sortBy]);

  function handleChange(e) {
    const { name, value } = e.target;
    setForm((prev) => ({ ...prev, [name]: value }));
  }

  function startEdit(item) {
    setEditingId(item.id);
    setForm({
      itemCode: item.itemCode,
      itemName: item.itemName,
      itemType: item.itemType,
      spec: item.spec || '',
      unit: item.unit,
      safetyStock: String(item.safetyStock),
    });
    window.scrollTo({ top: 0, behavior: 'smooth' });
  }

  function cancelEdit() {
    setEditingId(null);
    setForm(INITIAL_FORM);
  }

  async function handleSubmit(e) {
    e.preventDefault();
    setSubmitting(true);
    try {
      if (editingId) {
        const res = await itemApi.update(editingId, {
          itemName: form.itemName,
          itemType: form.itemType,
          spec: form.spec,
          unit: form.unit,
          safetyStock: Number(form.safetyStock),
        });
        toast.success(res.message || '품목 정보가 수정되었습니다.');
        cancelEdit();
      } else {
        const res = await itemApi.create({
          itemCode: form.itemCode,
          itemName: form.itemName,
          itemType: form.itemType,
          spec: form.spec,
          unit: form.unit,
          safetyStock: Number(form.safetyStock),
        });
        toast.success(res.message || '품목이 등록되었습니다.');
        setForm(INITIAL_FORM);
      }
      await loadItems();
      notifyDataChanged();
    } catch (err) {
      toast.error(err.message);
    } finally {
      setSubmitting(false);
    }
  }

  async function executeToggleStatus(item) {
    setSubmitting(true);
    try {
      const res = await itemApi.updateStatus(item.id, !item.active);
      toast.success(res.message || '품목 상태가 변경되었습니다.');
      setConfirmItem(null);
      await loadItems();
      notifyDataChanged();
    } catch (err) {
      toast.error(err.message);
    } finally {
      setSubmitting(false);
    }
  }

  function requestToggleStatus(item) {
    if (item.active) {
      setConfirmItem(item);
    } else {
      executeToggleStatus(item);
    }
  }

  return (
    <div>
      <div className="card">
        <div className="card-header">
          <h3 className="card-title">{editingId ? '품목 정보 수정' : '신규 품목 등록'}</h3>
          {editingId && (
            <button type="button" className="btn btn-secondary btn-sm" onClick={cancelEdit}>
              수정 취소
            </button>
          )}
        </div>

        <form className="form-grid" onSubmit={handleSubmit}>
          <div className="field">
            <label htmlFor="itemCode">품목 코드 *</label>
            <input
              id="itemCode"
              name="itemCode"
              value={form.itemCode}
              onChange={handleChange}
              placeholder="예: RAW-AB-01 / BOX-003"
              disabled={Boolean(editingId)}
              required
            />
          </div>

          <div className="field">
            <label htmlFor="itemName">품목명 *</label>
            <input
              id="itemName"
              name="itemName"
              value={form.itemName}
              onChange={handleChange}
              placeholder="예: AB골 원단 / 택배박스 3호"
              required
            />
          </div>

          <div className="field">
            <label htmlFor="itemType">품목 구분 *</label>
            <select id="itemType" name="itemType" value={form.itemType} onChange={handleChange}>
              <option value="RAW_MATERIAL">원재료 (원단/부자재)</option>
              <option value="FINISHED_GOOD">완제품 (박스)</option>
            </select>
          </div>

          <div className="field">
            <label htmlFor="spec">규격</label>
            <input
              id="spec"
              name="spec"
              value={form.spec}
              onChange={handleChange}
              placeholder="예: 340x250x210 / AB골"
            />
          </div>

          <div className="field">
            <label htmlFor="unit">단위 *</label>
            <input
              id="unit"
              name="unit"
              value={form.unit}
              onChange={handleChange}
              placeholder="매 / 개 / 롤"
              required
            />
          </div>

          <div className="field">
            <label htmlFor="safetyStock">안전재고 *</label>
            <input
              id="safetyStock"
              name="safetyStock"
              type="number"
              min="0"
              value={form.safetyStock}
              onChange={handleChange}
              required
            />
          </div>

          <div className="form-actions">
            <button type="submit" className="btn btn-primary" disabled={submitting}>
              {submitting ? '저장 중...' : editingId ? '품목 수정' : '품목 등록'}
            </button>
          </div>
        </form>
      </div>

      <div className="card">
        <div className="card-header">
          <h3 className="card-title">
            품목 목록 ({filteredItems.length}
            {filteredItems.length !== items.length ? ` / 전체 ${items.length}` : ''}건)
          </h3>

          <SearchSortBar
            search={search}
            onSearchChange={setSearch}
            searchPlaceholder="품목코드·품목명·규격 검색"
            sortValue={sortBy}
            onSortChange={setSortBy}
            sortOptions={SORT_OPTIONS}
          >
            <select value={filterType} onChange={(e) => setFilterType(e.target.value)}>
              <option value="">전체 구분</option>
              <option value="RAW_MATERIAL">원재료</option>
              <option value="FINISHED_GOOD">완제품</option>
            </select>
            <select value={filterActive} onChange={(e) => setFilterActive(e.target.value)}>
              <option value="">전체 상태</option>
              <option value="true">사용 중</option>
              <option value="false">비활성</option>
            </select>
            <button
              type="button"
              className="btn btn-secondary btn-sm"
              onClick={loadItems}
              disabled={loading}
            >
              <IconRefresh size={14} />
              <span>새로고침</span>
            </button>
          </SearchSortBar>
        </div>

        {loading ? (
          <EmptyState loading message="품목 목록을 불러오는 중..." />
        ) : filteredItems.length === 0 ? (
          <EmptyState message="조건에 맞는 품목이 없습니다." />
        ) : (
          <div className="table-wrap">
            <table>
              <thead>
                <tr>
                  <th>품목코드</th>
                  <th>품목명</th>
                  <th>구분</th>
                  <th>규격</th>
                  <th>단위</th>
                  <th className="text-right">안전재고</th>
                  <th>상태</th>
                  <th>관리</th>
                </tr>
              </thead>
              <tbody>
                {filteredItems.map((item) => (
                  <tr key={item.id} className={!item.active ? 'row-inactive' : ''}>
                    <td style={{ fontWeight: 700 }}>{item.itemCode}</td>
                    <td style={{ fontWeight: 600 }}>{item.itemName}</td>
                    <td>
                      <StatusBadge variant={item.itemType === 'RAW_MATERIAL' ? 'warning' : 'info'}>
                        {item.itemTypeDescription}
                      </StatusBadge>
                    </td>
                    <td>{item.spec || '-'}</td>
                    <td>{item.unit}</td>
                    <td className="text-right">{formatNumber(item.safetyStock)}</td>
                    <td>
                      <StatusBadge variant={item.active ? 'success' : 'neutral'}>
                        {item.active ? '사용' : '비활성'}
                      </StatusBadge>
                    </td>
                    <td>
                      <div className="inline-actions">
                        <button
                          type="button"
                          className="btn btn-secondary btn-sm"
                          onClick={() => startEdit(item)}
                        >
                          수정
                        </button>
                        <button
                          type="button"
                          className={`btn btn-sm ${item.active ? 'btn-danger' : 'btn-secondary'}`}
                          onClick={() => requestToggleStatus(item)}
                        >
                          {item.active ? '비활성화' : '활성화'}
                        </button>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>

      <ConfirmModal
        open={Boolean(confirmItem)}
        title="품목 비활성화 확인"
        message="이 품목을 비활성화하시겠습니까?"
        details={
          confirmItem
            ? [
                `품목: [${confirmItem.itemCode}] ${confirmItem.itemName} (${confirmItem.itemTypeDescription})`,
                '비활성화하면 신규 작업지시, BOM 등록, 재고 입출고 대상에서 제외됩니다.',
                '기존 거래/수불 이력은 보존되며 언제든 다시 활성화할 수 있습니다.',
              ]
            : []
        }
        confirmLabel="비활성화"
        tone="danger"
        loading={submitting}
        onConfirm={() => confirmItem && executeToggleStatus(confirmItem)}
        onCancel={() => setConfirmItem(null)}
      />
    </div>
  );
}
