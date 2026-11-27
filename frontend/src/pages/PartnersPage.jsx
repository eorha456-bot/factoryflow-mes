import { useEffect, useState } from 'react';
import { partnerApi } from '../api/endpoints';
import StatusBadge from '../components/StatusBadge';

const INITIAL_FORM = {
  partnerCode: '',
  partnerName: '',
  partnerType: 'SUPPLIER',
  businessNumber: '',
  phone: '',
  address: '',
};

export default function PartnersPage() {
  const [partners, setPartners] = useState([]);
  const [filterType, setFilterType] = useState('');
  const [filterActive, setFilterActive] = useState('');
  const [form, setForm] = useState(INITIAL_FORM);
  const [editingId, setEditingId] = useState(null);
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState('');
  const [notice, setNotice] = useState('');

  async function loadPartners() {
    setLoading(true);
    setError('');
    try {
      const res = await partnerApi.list({
        partnerType: filterType || undefined,
        active: filterActive === '' ? undefined : filterActive,
      });
      setPartners(res.data || []);
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    loadPartners();
  }, [filterType, filterActive]);

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
    setError('');
    setNotice('');
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
        const res = await partnerApi.update(editingId, {
          partnerName: form.partnerName,
          partnerType: form.partnerType,
          businessNumber: form.businessNumber,
          phone: form.phone,
          address: form.address,
        });
        setNotice(res.message || '거래처 정보가 수정되었습니다.');
        cancelEdit();
      } else {
        const res = await partnerApi.create(form);
        setNotice(res.message || '거래처가 등록되었습니다.');
        setForm(INITIAL_FORM);
      }
      await loadPartners();
    } catch (err) {
      setError(err.message);
    } finally {
      setSubmitting(false);
    }
  }

  async function toggleStatus(partner) {
    setError('');
    setNotice('');
    try {
      const res = await partnerApi.updateStatus(partner.id, !partner.active);
      setNotice(res.message || '거래처 상태가 변경되었습니다.');
      await loadPartners();
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
          <h3 className="card-title">{editingId ? '거래처 수정' : '신규 거래처 등록'}</h3>
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
          <h3 className="card-title">거래처 목록 ({partners.length}건)</h3>
          <div className="filter-bar">
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
            <button className="btn btn-secondary btn-sm" onClick={loadPartners} disabled={loading}>
              새로고침
            </button>
          </div>
        </div>

        {loading ? (
          <div className="empty-state">거래처 목록을 불러오는 중...</div>
        ) : partners.length === 0 ? (
          <div className="empty-state">등록된 거래처가 없습니다.</div>
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
                {partners.map((p) => (
                  <tr key={p.id} className={!p.active ? 'row-inactive' : ''}>
                    <td>{p.partnerCode}</td>
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
                          onClick={() => toggleStatus(p)}
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
    </div>
  );
}
