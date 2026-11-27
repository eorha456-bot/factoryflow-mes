import { useCallback, useEffect, useMemo, useState } from 'react';
import { bomApi, itemApi } from '../api/endpoints';
import ConfirmModal from '../components/ui/ConfirmModal';
import EmptyState from '../components/ui/EmptyState';
import { IconRefresh } from '../components/ui/Icons';
import SearchSortBar from '../components/ui/SearchSortBar';
import { useToast } from '../context/ToastContext';
import { formatNumber } from '../utils/format';

const SORT_OPTIONS = [
  { value: 'latest', label: '정렬: 기본순' },
  { value: 'finishedAsc', label: '정렬: 완제품명순' },
  { value: 'qtyDesc', label: '정렬: 소요량 많은순' },
];

export default function BomPage() {
  const toast = useToast();
  const [boms, setBoms] = useState([]);
  const [finishedItems, setFinishedItems] = useState([]);
  const [rawMaterials, setRawMaterials] = useState([]);
  const [filterFinishedId, setFilterFinishedId] = useState('');
  const [search, setSearch] = useState('');
  const [sortBy, setSortBy] = useState('latest');

  const [finishedItemId, setFinishedItemId] = useState('');
  const [rawMaterialId, setRawMaterialId] = useState('');
  const [requiredQuantity, setRequiredQuantity] = useState('1');

  const [editingBomId, setEditingBomId] = useState(null);
  const [editingQty, setEditingQty] = useState('1');
  const [deletingBom, setDeletingBom] = useState(null);

  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);

  useEffect(() => {
    Promise.all([
      itemApi.list({ itemType: 'FINISHED_GOOD', active: true }),
      itemApi.list({ itemType: 'RAW_MATERIAL', active: true }),
    ])
      .then(([fgRes, rmRes]) => {
        setFinishedItems(fgRes.data || []);
        setRawMaterials(rmRes.data || []);
      })
      .catch((err) => toast.error(err.message));
  }, [toast]);

  const loadBoms = useCallback(
    async (selectedFinishedId = filterFinishedId) => {
      setLoading(true);
      try {
        const res = await bomApi.list(selectedFinishedId || undefined);
        setBoms(res.data || []);
      } catch (err) {
        toast.error(err.message);
      } finally {
        setLoading(false);
      }
    },
    [filterFinishedId, toast]
  );

  useEffect(() => {
    loadBoms(filterFinishedId);
  }, [filterFinishedId, loadBoms]);

  const filteredBoms = useMemo(() => {
    const keyword = search.trim().toLowerCase();
    const list = keyword
      ? boms.filter(
          (b) =>
            b.finishedItemCode.toLowerCase().includes(keyword) ||
            b.finishedItemName.toLowerCase().includes(keyword) ||
            b.rawMaterialCode.toLowerCase().includes(keyword) ||
            b.rawMaterialName.toLowerCase().includes(keyword)
        )
      : [...boms];

    if (sortBy === 'finishedAsc') {
      list.sort((a, b) => a.finishedItemName.localeCompare(b.finishedItemName, 'ko'));
    } else if (sortBy === 'qtyDesc') {
      list.sort((a, b) => b.requiredQuantity - a.requiredQuantity);
    }
    return list;
  }, [boms, search, sortBy]);

  async function handleCreate(e) {
    e.preventDefault();
    setSubmitting(true);
    try {
      const res = await bomApi.create({
        finishedItemId: Number(finishedItemId),
        rawMaterialId: Number(rawMaterialId),
        requiredQuantity: Number(requiredQuantity),
      });
      toast.success(res.message || 'BOM이 등록되었습니다.');
      setRawMaterialId('');
      setRequiredQuantity('1');
      await loadBoms();
    } catch (err) {
      toast.error(err.message);
    } finally {
      setSubmitting(false);
    }
  }

  async function handleUpdate(bomId) {
    setSubmitting(true);
    try {
      const res = await bomApi.update(bomId, Number(editingQty));
      toast.success(res.message || '소요 수량이 수정되었습니다.');
      setEditingBomId(null);
      await loadBoms();
    } catch (err) {
      toast.error(err.message);
    } finally {
      setSubmitting(false);
    }
  }

  async function executeDelete(bom) {
    setSubmitting(true);
    try {
      const res = await bomApi.remove(bom.id);
      toast.success(res.message || 'BOM이 삭제되었습니다.');
      setDeletingBom(null);
      await loadBoms();
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
          <h3 className="card-title">
            BOM 목록 ({filteredBoms.length}
            {filteredBoms.length !== boms.length ? ` / 전체 ${boms.length}` : ''}건)
          </h3>

          <SearchSortBar
            search={search}
            onSearchChange={setSearch}
            searchPlaceholder="완제품·원재료 코드 또는 이름 검색"
            sortValue={sortBy}
            onSortChange={setSortBy}
            sortOptions={SORT_OPTIONS}
          >
            <select value={filterFinishedId} onChange={(e) => setFilterFinishedId(e.target.value)}>
              <option value="">전체 완제품</option>
              {finishedItems.map((item) => (
                <option key={item.id} value={item.id}>
                  [{item.itemCode}] {item.itemName}
                </option>
              ))}
            </select>
            <button
              type="button"
              className="btn btn-secondary btn-sm"
              onClick={() => loadBoms()}
              disabled={loading}
            >
              <IconRefresh size={14} />
              <span>새로고침</span>
            </button>
          </SearchSortBar>
        </div>

        {loading ? (
          <EmptyState loading message="BOM 목록을 불러오는 중..." />
        ) : filteredBoms.length === 0 ? (
          <EmptyState message="등록된 BOM이 없습니다. 완제품에 원재료를 연결하면 생산실적 등록 시 원재료가 자동 차감됩니다." />
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
                {filteredBoms.map((bom) => (
                  <tr key={bom.id}>
                    <td style={{ fontWeight: 700 }}>{bom.finishedItemCode}</td>
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
                          style={{ width: 90, padding: '6px 8px' }}
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
                              disabled={submitting}
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
                              onClick={() => setDeletingBom(bom)}
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

      <ConfirmModal
        open={Boolean(deletingBom)}
        title="BOM 삭제 확인"
        message="선택한 BOM 구성을 삭제하시겠습니까?"
        details={
          deletingBom
            ? [
                `완제품: [${deletingBom.finishedItemCode}] ${deletingBom.finishedItemName}`,
                `원재료: [${deletingBom.rawMaterialCode}] ${deletingBom.rawMaterialName} (소요량 ${deletingBom.requiredQuantity} ${deletingBom.rawMaterialUnit})`,
                '삭제 후에는 해당 원재료가 생산실적 등록 시 자동 차감되지 않습니다.',
              ]
            : []
        }
        confirmLabel="삭제"
        tone="danger"
        loading={submitting}
        onConfirm={() => deletingBom && executeDelete(deletingBom)}
        onCancel={() => setDeletingBom(null)}
      />
    </div>
  );
}
