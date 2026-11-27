import { apiRequest } from './client';

export const healthApi = {
  check: () => apiRequest('/api/health'),
};

export const dashboardApi = {
  getSummary: (date) => apiRequest('/api/dashboard', { params: { date } }),
};

export const itemApi = {
  list: (params) => apiRequest('/api/items', { params }),
  create: (body) => apiRequest('/api/items', { method: 'POST', body }),
  update: (id, body) => apiRequest(`/api/items/${id}`, { method: 'PUT', body }),
  updateStatus: (id, active) =>
    apiRequest(`/api/items/${id}/status`, { method: 'PATCH', body: { active } }),
};

export const partnerApi = {
  list: (params) => apiRequest('/api/partners', { params }),
  create: (body) => apiRequest('/api/partners', { method: 'POST', body }),
  update: (id, body) => apiRequest(`/api/partners/${id}`, { method: 'PUT', body }),
  updateStatus: (id, active) =>
    apiRequest(`/api/partners/${id}/status`, { method: 'PATCH', body: { active } }),
};

export const bomApi = {
  list: (finishedItemId) => apiRequest('/api/boms', { params: { finishedItemId } }),
  create: (body) => apiRequest('/api/boms', { method: 'POST', body }),
  update: (id, requiredQuantity) =>
    apiRequest(`/api/boms/${id}`, { method: 'PUT', body: { requiredQuantity } }),
  remove: (id) => apiRequest(`/api/boms/${id}`, { method: 'DELETE' }),
};

export const inventoryApi = {
  list: (params) => apiRequest('/api/inventories', { params }),
  histories: (params) => apiRequest('/api/inventories/histories', { params }),
  inbound: (body) => apiRequest('/api/inventories/inbound', { method: 'POST', body }),
  outbound: (body) => apiRequest('/api/inventories/outbound', { method: 'POST', body }),
  adjust: (body) => apiRequest('/api/inventories/adjust', { method: 'POST', body }),
};

export const productionApi = {
  listWorkOrders: (params) => apiRequest('/api/work-orders', { params }),
  createWorkOrder: (body) => apiRequest('/api/work-orders', { method: 'POST', body }),
  updateWorkOrderStatus: (id, status) =>
    apiRequest(`/api/work-orders/${id}/status`, { method: 'PATCH', body: { status } }),
  recordResult: (workOrderId, body) =>
    apiRequest(`/api/work-orders/${workOrderId}/results`, { method: 'POST', body }),
  listResultsByOrder: (workOrderId) => apiRequest(`/api/work-orders/${workOrderId}/results`),
  listAllResults: () => apiRequest('/api/production-results'),
};
