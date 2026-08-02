const TOKEN_KEY = 'taskManagerToken';
const API_BASE = '/api/v1';

function resolveApiUrl(path) {
    if (path.startsWith('/api/v1')) {
        return path;
    }
    if (path.startsWith('/api/')) {
        return API_BASE + path.substring(4);
    }
    if (path.startsWith('/')) {
        return API_BASE + path;
    }
    return API_BASE + '/' + path;
}

function getToken() {
    return sessionStorage.getItem(TOKEN_KEY);
}

function setToken(token) {
    sessionStorage.setItem(TOKEN_KEY, token);
}

function clearToken() {
    sessionStorage.removeItem(TOKEN_KEY);
}

function requireAuth() {
    if (!getToken()) {
        window.location.href = '/login';
        return false;
    }
    return true;
}

async function apiFetch(path, options = {}) {
    const url = resolveApiUrl(path);

    const headers = {
        'Content-Type': 'application/json',
        'Accept': 'application/json',
        ...(options.headers || {})
    };

    const token = getToken();
    if (token) {
        headers['Authorization'] = 'Bearer ' + token;
    }

    const response = await fetch(url, { ...options, headers });

    if (response.status === 401 || response.status === 403) {
        if (window.location.pathname !== '/login' && window.location.pathname !== '/register') {
            clearToken();
            window.location.href = '/login';
        }
    }

    if (!response.ok) {
        let message = 'Request failed';
        try {
            const error = await response.json();
            message = error.message || message;
        } catch (e) {
            // ignore
        }
        throw new Error(message);
    }

    if (response.status === 204) {
        return null;
    }

    return response.json();
}

function logout() {
    clearToken();
    window.location.href = '/login';
}

function showAlert(containerId, message, type = 'danger') {
    const container = document.getElementById(containerId);
    if (!container) return;
    container.innerHTML = `<div class="alert alert-${type}">${message}</div>`;
}

function escapeHtml(text) {
    if (text == null) return '';
    return String(text)
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;');
}
