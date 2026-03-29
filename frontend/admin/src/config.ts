const API_BASE = import.meta.env.VITE_API_BASE_URL || '/api/v1';

function getToken(): string | null {
  return localStorage.getItem('admin_token');
}

export function setToken(token: string) {
  localStorage.setItem('admin_token', token);
}

export function clearToken() {
  localStorage.removeItem('admin_token');
}

export function isAuthenticated(): boolean {
  return !!getToken();
}

export async function request<T>(path: string, options: {
  method?: 'GET' | 'POST' | 'PATCH' | 'DELETE';
  data?: unknown;
} = {}): Promise<T> {
  const token = getToken();
  const headers: Record<string, string> = {
    'Content-Type': 'application/json',
    'X-API-Version': '1'
  };

  if (token) {
    headers['Authorization'] = `Bearer ${token}`;
  }

  const res = await fetch(`${API_BASE}${path}`, {
    method: options.method || 'GET',
    headers,
    body: options.data ? JSON.stringify(options.data) : undefined
  });

  if (res.status === 401) {
    clearToken();
    window.location.href = '/login';
    throw new Error('登录已过期');
  }

  if (!res.ok) {
    const body = await res.json().catch(() => ({}));
    throw new Error(body.message || body.error || `请求失败 (${res.status})`);
  }

  return res.json();
}
