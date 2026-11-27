import { useEffect, useState } from 'react';
import { itemApi } from '../api/endpoints';
import StatusBadge from '../components/StatusBadge';
import { formatNumber } from '../utils/format';

const INITIAL_FORM = {
  itemCode: '',
  itemName: '',
  itemType: 'RAW_MATERIAL',
  spec: '',
  unit: '매',
  safetyStock: '0',
};

export default function ItemsPage() {
  const [items, setItems] = useState([]);
  const [filterType, setFilterType] = useState('');
  const [filterActive, setFilterActive] = useState('');
  const [form, setForm] = useState(INITIAL_FORM);
  const [editingId, setEditingId] = useState(null);
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState('');
  const [notice, setNotice] = useState('');

  async function loadItems() {
    setLoading(true);
    setError('');
    try {
      const res = await itemApi.list({
        itemType: filterType || undefined,
        active: filterActive === '' ? undefined : filterActive,
      });
      setItems(res.data || []);
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    loadItems();
  }, [filterType, filterActive]);

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
    setNotice('');
    setError('');
  }

  function cancelEdit() {
    setEditingId(null);
    setForm(INITIAL_FORM);
  }

  async function handleSubmit(e) {
    e.preventDefault();
    setSubmitting(true);
    setError('');
    setNotice('');

    try {
      if (editingId) {
        const res = await itemApi.update(editingId, {
          itemName: form.itemName,
          itemType: form.itemType,
          spec: form.spec,
          unit: form.unit,
          safetyStock: Number(form.safetyStock),
        });
        setNotice(res.message || '품목 정보가 수정되었습니다.');
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
        setNotice(res.message || '품목이 등록되었습니다.');
        setForm(INITIAL_FORM);
      }
      await loadItems();
    } catch (err) {
      setError(err.message);
    } finally {
      setSubmitting(false);
    }
  }

  async function toggleStatus(item) {
    setError('');
    setNotice('');
    try {
      const res = await itemApi.updateStatus(item.id, !item.active);
      setNotice(res.message || '품목 상태가 변경되었습니다.');
      await loadItems();
    } catch (err) {
      setError(err.message);
    }
  }

  return (
    <div>
      {error && <div className="alert alert-error">{error}</div>}
      {notice && <div className="alert alert-success">{notice}</div>}

      <div className="card">
        <div className="card-header">
          <h3 className="card-title">{editingId ? '품목 수정' : '신규 품목 등록'}</h3>
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
          <h3 className="card-title">품목 목록 ({items.length}건)</h3>
          <div className="filter-bar">
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
            <button className="btn btn-secondary btn-sm" onClick={loadItems} disabled={loading}>
              새로고침
            </button>
          </div>
        </div>

        {loading ? (
          <div className="empty-state">품목 목록을 불러오는 중...</div>
        ) : items.length === 0 ? (
          <div className="empty-state">등록된 품목이 없습니다. 위 폼에서 첫 품목을 등록해 보세요.</div>
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
                {items.map((item) => (
                  <tr key={item.id} className={!item.active ? 'row-inactive' : ''}>
                    <td>{item.itemCode}</td>
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
                          onClick={() => toggleStatus(item)}
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
    </div>
  );
}
