import { useCallback, useEffect, useMemo, useState } from 'react';
import { partnerApi } from '../api/endpoints';
import StatusBadge from '../components/StatusBadge';
import ConfirmModal from '../components/ui/ConfirmModal';
import EmptyState from '../components/ui/EmptyState';
import { IconRefresh } from '../components/ui/Icons';
import SearchSortBar from '../components/ui/SearchSortBar';
import { useToast } from '../context/ToastContext';

const INITIAL_FORM = {
  partnerCode: '',
  partnerName: '',
  partnerType: 'SUPPLIER',
  businessNumber: '',
  phone: '',
  address: '',
};

const SORT_OPTIONS = [
  { value: 'latest', label: '정렬: 최신 등록순' },
  { value: 'codeAsc', label: '정렬: 거래처코드순' },
  { value: 'nameAsc', label: '정렬: 거래처명순' },
];

export default function PartnersPage() {
  const toast = useToast();
  const [partners, setPartners] = useState([]);
  const [filterType, setFilterType] = useState('');
  const [filterActive, setFilterActive] = useState('');
  const [search, setSearch] = useState('');
  const [sortBy, setSortBy] = useState('latest');

  const [form, setForm] = useState(INITIAL_FORM);
  const [editingId, setEditingId] = useState(null);
  const [confirmPartner, setConfirmPartner] = useState(null);
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);

  const loadPartners = useCallback(async () => {
    setLoading(true);
    try {
      const res = await partnerApi.list({
        partnerType: filterType || undefined,
        active: filterActive === '' ? undefined : filterActive,
      });
      setPartners(res.data || []);
    } catch (err) {
      toast.error(err.message);
    } finally {
      setLoading(false);
    }
  }, [filterType, filterActive, toast]);

  useEffect(() => {
    loadPartners();
  }, [loadPartners]);

  const filteredPartners = useMemo(() => {
    const keyword = search.trim().toLowerCase();
    const list = keyword
      ? partners.filter(
          (p) =>
            p.partnerCode.toLowerCase().includes(keyword) ||
            p.partnerName.toLowerCase().includes(keyword) ||
            (p.phone && p.phone.toLowerCase().includes(keyword)) ||
            (p.address && p.address.toLowerCase().includes(keyword))
        )
      : [...partners];

    if (sortBy === 'codeAsc') {
      list.sort((a, b) => a.partnerCode.localeCompare(b.partnerCode));
    } else if (sortBy === 'nameAsc') {
      list.sort((a, b) => a.partnerName.localeCompare(b.partnerName, 'ko'));
    }
    return list;
  }, [partners, search, sortBy]);

  function handleChange(e) {
    const { name, value } = e.target;
    setForm((prev) => ({ ...prev, [name]: value }));
  }

  function startEdit(partner) {
    setEditingId(partner.id);
    setForm({
      partnerCode: partner.partnerCode,
      partnerName: partner.partnerName,
      partnerType: partner.partnerType,
      businessNumber: partner.businessNumber || '',
      phone: partner.phone || '',
      address: partner.address || '',
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
        const res = await partnerApi.update(editingId, {
          partnerName: form.partnerName,
          partnerType: form.partnerType,
          businessNumber: form.businessNumber,
          phone: form.phone,
          address: form.address,
        });
        toast.success(res.message || '거래처 정보가 수정되었습니다.');
        cancelEdit();
      } else {
        const res = await partnerApi.create(form);
        toast.success(res.message || '거래처가 등록되었습니다.');
        setForm(INITIAL_FORM);
      }
      await loadPartners();
    } catch (err) {
      toast.error(err.message);
    } finally {
      setSubmitting(false);
    }
  }

  async function executeToggleStatus(partner) {
    setSubmitting(true);
    try {
      const res = await partnerApi.updateStatus(partner.id, !partner.active);
      toast.success(res.message || '거래처 상태가 변경되었습니다.');
      setConfirmPartner(null);
      await loadPartners();
    } catch (err) {
      toast.error(err.message);
    } finally {
      setSubmitting(false);
    }
  }

  function requestToggleStatus(partner) {
    if (partner.active) {
      setConfirmPartner(partner);
    } else {
      executeToggleStatus(partner);
    }
  }

  return (
    <div>
      <div className="card">
        <div className="card-header">
          <h3 className="card-title">{editingId ? '거래처 정보 수정' : '신규 거래처 등록'}</h3>
          {editingId && (
            <button type="button" className="btn btn-secondary btn-sm" onClick={cancelEdit}>
              수정 취소
            </button>
          )}
        </div>

        <form className="form-grid" onSubmit={handleSubmit}>
          <div className="field">
            <label htmlFor="partnerCode">거래처 코드 *</label>
            <input
              id="partnerCode"
              name="partnerCode"
              value={form.partnerCode}
              onChange={handleChange}
              placeholder="예: SUP-001 / CUS-001"
              disabled={Boolean(editingId)}
              required
            />
          </div>

          <div className="field">
            <label htmlFor="partnerName">거래처명 *</label>
            <input
              id="partnerName"
              name="partnerName"
              value={form.partnerName}
              onChange={handleChange}
              placeholder="예: 대한골판지 / 쿠팡물류"
              required
            />
          </div>

          <div className="field">
            <label htmlFor="partnerType">구분 *</label>
            <select
              id="partnerType"
              name="partnerType"
              value={form.partnerType}
              onChange={handleChange}
            >
              <option value="SUPPLIER">매입처 (원자재 공급)</option>
              <option value="CUSTOMER">매출처 (완제품 납품)</option>
              <option value="BOTH">매입/매출 겸용</option>
            </select>
          </div>

          <div className="field">
            <label htmlFor="businessNumber">사업자번호</label>
            <input
              id="businessNumber"
              name="businessNumber"
              value={form.businessNumber}
              onChange={handleChange}
              placeholder="000-00-00000"
            />
          </div>

          <div className="field">
            <label htmlFor="phone">연락처</label>
            <input
              id="phone"
              name="phone"
              value={form.phone}
              onChange={handleChange}
              placeholder="02-0000-0000"
            />
          </div>

          <div className="field">
            <label htmlFor="address">주소</label>
            <input
              id="address"
              name="address"
              value={form.address}
              onChange={handleChange}
              placeholder="경기도 시흥시..."
            />
          </div>

          <div className="form-actions">
            <button type="submit" className="btn btn-primary" disabled={submitting}>
              {submitting ? '저장 중...' : editingId ? '거래처 수정' : '거래처 등록'}
            </button>
          </div>
        </form>
      </div>

      <div className="card">
        <div className="card-header">
          <h3 className="card-title">
            거래처 목록 ({filteredPartners.length}
            {filteredPartners.length !== partners.length ? ` / 전체 ${partners.length}` : ''}건)
          </h3>

          <SearchSortBar
            search={search}
            onSearchChange={setSearch}
            searchPlaceholder="거래처코드·거래처명·연락처 검색"
            sortValue={sortBy}
            onSortChange={setSortBy}
            sortOptions={SORT_OPTIONS}
          >
            <select value={filterType} onChange={(e) => setFilterType(e.target.value)}>
              <option value="">전체 구분</option>
              <option value="SUPPLIER">매입처</option>
              <option value="CUSTOMER">매출처</option>
              <option value="BOTH">겸용</option>
            </select>
            <select value={filterActive} onChange={(e) => setFilterActive(e.target.value)}>
              <option value="">전체 상태</option>
              <option value="true">사용 중</option>
              <option value="false">비활성</option>
            </select>
            <button
              type="button"
              className="btn btn-secondary btn-sm"
              onClick={loadPartners}
              disabled={loading}
            >
              <IconRefresh size={14} />
              <span>새로고침</span>
            </button>
          </SearchSortBar>
        </div>

        {loading ? (
          <EmptyState loading message="거래처 목록을 불러오는 중..." />
        ) : filteredPartners.length === 0 ? (
          <EmptyState message="조건에 맞는 거래처가 없습니다." />
        ) : (
          <div className="table-wrap">
            <table>
              <thead>
                <tr>
                  <th>거래처 코드</th>
                  <th>거래처명</th>
                  <th>구분</th>
                  <th>사업자번호</th>
                  <th>연락처</th>
                  <th>주소</th>
                  <th>상태</th>
                  <th>관리</th>
                </tr>
              </thead>
              <tbody>
                {filteredPartners.map((p) => (
                  <tr key={p.id} className={!p.active ? 'row-inactive' : ''}>
                    <td style={{ fontWeight: 700 }}>{p.partnerCode}</td>
                    <td style={{ fontWeight: 600 }}>{p.partnerName}</td>
                    <td>
                      <StatusBadge
                        variant={
                          p.partnerType === 'SUPPLIER'
                            ? 'warning'
                            : p.partnerType === 'CUSTOMER'
                            ? 'info'
                            : 'neutral'
                        }
                      >
                        {p.partnerTypeDescription}
                      </StatusBadge>
                    </td>
                    <td>{p.businessNumber || '-'}</td>
                    <td>{p.phone || '-'}</td>
                    <td>{p.address || '-'}</td>
                    <td>
                      <StatusBadge variant={p.active ? 'success' : 'neutral'}>
                        {p.active ? '사용' : '비활성'}
                      </StatusBadge>
                    </td>
                    <td>
                      <div className="inline-actions">
                        <button
                          type="button"
                          className="btn btn-secondary btn-sm"
                          onClick={() => startEdit(p)}
                        >
                          수정
                        </button>
                        <button
                          type="button"
                          className={`btn btn-sm ${p.active ? 'btn-danger' : 'btn-secondary'}`}
                          onClick={() => requestToggleStatus(p)}
                        >
                          {p.active ? '비활성화' : '활성화'}
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
        open={Boolean(confirmPartner)}
        title="거래처 비활성화 확인"
        message="이 거래처를 비활성화하시겠습니까?"
        details={
          confirmPartner
            ? [
                `거래처: [${confirmPartner.partnerCode}] ${confirmPartner.partnerName} (${confirmPartner.partnerTypeDescription})`,
                '비활성화하면 신규 입출고 거래처 선택 목록에서 제외됩니다.',
              ]
            : []
        }
        confirmLabel="비활성화"
        tone="danger"
        loading={submitting}
        onConfirm={() => confirmPartner && executeToggleStatus(confirmPartner)}
        onCancel={() => setConfirmPartner(null)}
      />
    </div>
  );
}
