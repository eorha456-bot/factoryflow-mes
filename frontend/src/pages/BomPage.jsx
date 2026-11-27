import { useEffect, useState } from 'react';
import { bomApi, itemApi } from '../api/endpoints';
import { formatNumber } from '../utils/format';

export default function BomPage() {
  const [boms, setBoms] = useState([]);
  const [finishedItems, setFinishedItems] = useState([]);
  const [rawMaterials, setRawMaterials] = useState([]);
  const [filterFinishedId, setFilterFinishedId] = useState('');

  const [finishedItemId, setFinishedItemId] = useState('');
  const [rawMaterialId, setRawMaterialId] = useState('');
  const [requiredQuantity, setRequiredQuantity] = useState('1');

  const [editingBomId, setEditingBomId] = useState(null);
  const [editingQty, setEditingQty] = useState('1');

  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState('');
  const [notice, setNotice] = useState('');

  async function loadItems() {
    const [fgRes, rmRes] = await Promise.all([
      itemApi.list({ itemType: 'FINISHED_GOOD', active: true }),
      itemApi.list({ itemType: 'RAW_MATERIAL', active: true }),
    ]);
    setFinishedItems(fgRes.data || []);
    setRawMaterials(rmRes.data || []);
  }

  async function loadBoms(selectedFinishedId = filterFinishedId) {
    setLoading(true);
    setError('');
    try {
      const res = await bomApi.list(selectedFinishedId || undefined);
      setBoms(res.data || []);
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    loadItems().catch((err) => setError(err.message));
  }, []);

  useEffect(() => {
    loadBoms(filterFinishedId);
  }, [filterFinishedId]);

  async function handleCreate(e) {
    e.preventDefault();
    setSubmitting(true);
    setError('');
    setNotice('');
    try {
      const res = await bomApi.create({
        finishedItemId: Number(finishedItemId),
        rawMaterialId: Number(rawMaterialId),
        requiredQuantity: Number(requiredQuantity),
      });
      setNotice(res.message || 'BOM이 등록되었습니다.');
      setRawMaterialId('');
      setRequiredQuantity('1');
      await loadBoms();
    } catch (err) {
      setError(err.message);
    } finally {
      setSubmitting(false);
    }
  }

  async function handleUpdate(bomId) {
    setError('');
    setNotice('');
    try {
      const res = await bomApi.update(bomId, Number(editingQty));
      setNotice(res.message || '소요 수량이 수정되었습니다.');
      setEditingBomId(null);
      await loadBoms();
    } catch (err) {
      setError(err.message);
    }
  }

  async function handleDelete(bom) {
    if (!window.confirm(`${bom.finishedItemName} - ${bom.rawMaterialName} BOM을 삭제할까요?`)) {
      return;
    }
    setError('');
    setNotice('');
    try {
      const res = await bomApi.remove(bom.id);
      setNotice(res.message || 'BOM이 삭제되었습니다.');
      await loadBoms();
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
          <h3 className="card-title">신규 BOM 등록 (완제품 1개당 원재료 소요량)</h3>
        </div>

        <form className="form-grid" onSubmit={handleCreate}>
          <div className="field">
            <label htmlFor="bom-finished">완제품 *</label>
            <select
              id="bom-finished"
              value={finishedItemId}
              onChange={(e) => setFinishedItemId(e.target.value)}
              required
            >
              <option value="">완제품 선택</option>
              {finishedItems.map((item) => (
                <option key={item.id} value={item.id}>
                  [{item.itemCode}] {item.itemName}
                </option>
              ))}
            </select>
          </div>

          <div className="field">
            <label htmlFor="bom-raw">원재료 *</label>
            <select
              id="bom-raw"
              value={rawMaterialId}
              onChange={(e) => setRawMaterialId(e.target.value)}
              required
            >
              <option value="">원재료 선택</option>
              {rawMaterials.map((item) => (
                <option key={item.id} value={item.id}>
                  [{item.itemCode}] {item.itemName} ({item.unit})
                </option>
              ))}
            </select>
          </div>

          <div className="field">
            <label htmlFor="bom-qty">1단위당 소요 수량 *</label>
            <input
              id="bom-qty"
              type="number"
              min="1"
              value={requiredQuantity}
              onChange={(e) => setRequiredQuantity(e.target.value)}
              required
            />
          </div>

          <div className="form-actions">
            <button type="submit" className="btn btn-primary" disabled={submitting}>
              {submitting ? '등록 중...' : 'BOM 등록'}
            </button>
          </div>
        </form>
      </div>

      <div className="card">
        <div className="card-header">
          <h3 className="card-title">BOM 목록 ({boms.length}건)</h3>
          <div className="filter-bar">
            <select value={filterFinishedId} onChange={(e) => setFilterFinishedId(e.target.value)}>
              <option value="">전체 완제품</option>
              {finishedItems.map((item) => (
                <option key={item.id} value={item.id}>
                  [{item.itemCode}] {item.itemName}
                </option>
              ))}
            </select>
            <button className="btn btn-secondary btn-sm" onClick={() => loadBoms()} disabled={loading}>
              새로고침
            </button>
          </div>
        </div>

        {loading ? (
          <div className="empty-state">BOM 목록을 불러오는 중...</div>
        ) : boms.length === 0 ? (
          <div className="empty-state">
            등록된 BOM이 없습니다. 완제품에 원재료를 연결하면 생산실적 등록 시 원재료가 자동 차감됩니다.
          </div>
        ) : (
          <div className="table-wrap">
            <table>
              <thead>
                <tr>
                  <th>완제품 코드</th>
                  <th>완제품명</th>
                  <th>원재료 코드</th>
                  <th>원재료명</th>
                  <th className="text-right">단위당 소요량</th>
                  <th>관리</th>
                </tr>
              </thead>
              <tbody>
                {boms.map((bom) => (
                  <tr key={bom.id}>
                    <td>{bom.finishedItemCode}</td>
                    <td style={{ fontWeight: 600 }}>{bom.finishedItemName}</td>
                    <td>{bom.rawMaterialCode}</td>
                    <td>{bom.rawMaterialName}</td>
                    <td className="text-right">
                      {editingBomId === bom.id ? (
                        <input
                          type="number"
                          min="1"
                          value={editingQty}
                          onChange={(e) => setEditingQty(e.target.value)}
                          style={{ width: 80, padding: '4px 6px' }}
                        />
                      ) : (
                        <>
                          {formatNumber(bom.requiredQuantity)} {bom.rawMaterialUnit}
                        </>
                      )}
                    </td>
                    <td>
                      <div className="inline-actions">
                        {editingBomId === bom.id ? (
                          <>
                            <button
                              type="button"
                              className="btn btn-primary btn-sm"
                              onClick={() => handleUpdate(bom.id)}
                            >
                              저장
                            </button>
                            <button
                              type="button"
                              className="btn btn-secondary btn-sm"
                              onClick={() => setEditingBomId(null)}
                            >
                              취소
                            </button>
                          </>
                        ) : (
                          <>
                            <button
                              type="button"
                              className="btn btn-secondary btn-sm"
                              onClick={() => {
                                setEditingBomId(bom.id);
                                setEditingQty(String(bom.requiredQuantity));
                              }}
                            >
                              수정
                            </button>
                            <button
                              type="button"
                              className="btn btn-danger btn-sm"
                              onClick={() => handleDelete(bom)}
                            >
                              삭제
                            </button>
                          </>
                        )}
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
