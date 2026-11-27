const API_BASE_URL = (import.meta.env.VITE_API_BASE_URL || '').replace(/\/$/, '');

function buildQuery(params = {}) {
  const searchParams = new URLSearchParams();
  Object.entries(params).forEach(([key, value]) => {
    if (value !== undefined && value !== null && value !== '') {
      searchParams.append(key, String(value));
    }
  });
  const query = searchParams.toString();
  return query ? `?${query}` : '';
}

function formatErrorMessage(payload, fallbackMessage) {
  if (!payload) {
    return fallbackMessage;
  }
  const baseMessage = payload.message || fallbackMessage;
  if (payload.data && typeof payload.data === 'object' && !Array.isArray(payload.data)) {
    const details = Object.values(payload.data).filter(Boolean).join(' / ');
    if (details) {
      return `${baseMessage} (${details})`;
    }
  }
  return baseMessage;
}

export async function apiRequest(path, { method = 'GET', params, body } = {}) {
  const url = `${API_BASE_URL}${path}${buildQuery(params)}`;
  const options = {
    method,
    headers: {
      Accept: 'application/json',
    },
  };

  if (body !== undefined) {
    options.headers['Content-Type'] = 'application/json';
    options.body = JSON.stringify(body);
  }

  let response;
  try {
    response = await fetch(url, options);
  } catch {
    throw new Error('백엔드 서버(http://localhost:8080)에 연결할 수 없습니다. 서버 실행 상태를 확인해 주세요.');
  }

  let payload = null;
  try {
    payload = await response.json();
  } catch {
    payload = null;
  }

  if (!response.ok || (payload && payload.success === false)) {
    throw new Error(formatErrorMessage(payload, `요청에 실패했습니다. (HTTP ${response.status})`));
  }

  return payload;
}
