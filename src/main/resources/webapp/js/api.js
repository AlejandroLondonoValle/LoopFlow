/**
 * LoopFlow — api.js
 * Cliente centralizado para consumir la API REST de LoopFlow.
 * Todas las páginas importan este módulo como script.
 */

const API_BASE = '/api';

// ============================================================
// Utilidad HTTP
// ============================================================

async function request(method, path, body = null) {
  const options = {
    method,
    headers: { 'Content-Type': 'application/json', 'Accept': 'application/json' },
  };
  if (body !== null) options.body = JSON.stringify(body);

  const res = await fetch(`${API_BASE}${path}`, options);

  if (res.status === 204) return null; // No Content

  const data = await res.json().catch(() => null);

  if (!res.ok) {
    const message = data?.error || `Error ${res.status}: ${res.statusText}`;
    throw new Error(message);
  }
  return data;
}

// ============================================================
// Categories API
// ============================================================

const CategoriesAPI = {
  getAll:    ()       => request('GET',    '/categories'),
  getById:   (id)     => request('GET',    `/categories/${id}`),
  create:    (data)   => request('POST',   '/categories', data),
  update:    (id, d)  => request('PUT',    `/categories/${id}`, d),
  delete:    (id)     => request('DELETE', `/categories/${id}`),
};

// ============================================================
// Habits API
// ============================================================

const HabitsAPI = {
  getAll:       (activeOnly = false) => request('GET', `/habits${activeOnly ? '?active=true' : ''}`),
  getById:      (id)     => request('GET',    `/habits/${id}`),
  create:       (data)   => request('POST',   '/habits', data),
  update:       (id, d)  => request('PUT',    `/habits/${id}`, d),
  delete:       (id)     => request('DELETE', `/habits/${id}`),
  archive:      (id)     => request('PATCH',  `/habits/${id}/archive`),
  complete:     (id, completed = true, notes = null) =>
    request('POST', `/habits/${id}/complete`, { completed, notes }),
  getLogs:      (id)     => request('GET', `/habits/${id}/logs`),
  getStats:     (id, from, to) => {
    const params = new URLSearchParams();
    if (from) params.set('from', from);
    if (to)   params.set('to', to);
    return request('GET', `/habits/${id}/stats?${params.toString()}`);
  },
};

// ============================================================
// Tasks API
// ============================================================

const TasksAPI = {
  getAll:      (status = null, priority = null) => {
    const params = new URLSearchParams();
    if (status)   params.set('status', status);
    if (priority) params.set('priority', priority);
    const q = params.toString();
    return request('GET', `/tasks${q ? '?' + q : ''}`);
  },
  getById:     (id)     => request('GET',    `/tasks/${id}`),
  create:      (data)   => request('POST',   '/tasks', data),
  update:      (id, d)  => request('PUT',    `/tasks/${id}`, d),
  move:        (id, status) => request('PATCH', `/tasks/${id}/move`, { status }),
  delete:      (id)     => request('DELETE', `/tasks/${id}`),
  getHistory:  (id)     => request('GET',    `/tasks/${id}/history`),
};

// ============================================================
// Dashboard API
// ============================================================

const DashboardAPI = {
  getSummary: () => request('GET', '/dashboard'),
};

// ============================================================
// Config API
// ============================================================

const ConfigAPI = {
  getAll:  ()         => request('GET',    '/config'),
  getKey:  (key)      => request('GET',    `/config/${key}`),
  set:     (key, value, description = null) =>
    request('PUT', `/config/${key}`, { value, description }),
  delete:  (key)      => request('DELETE', `/config/${key}`),
};

// ============================================================
// Exportar globalmente (sin módulos ES — compatible con CDN)
// ============================================================
window.LoopFlowAPI = {
  categories: CategoriesAPI,
  habits:     HabitsAPI,
  tasks:      TasksAPI,
  dashboard:  DashboardAPI,
  config:     ConfigAPI,
};
