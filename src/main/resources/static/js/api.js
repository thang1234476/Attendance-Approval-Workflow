/**
 * api.js - Tiện ích gọi API và quản lý authentication
 */

const API_BASE = '/api';

// === Auth helpers ===
function getToken() { return localStorage.getItem('jwt_token'); }
function setToken(t) { localStorage.setItem('jwt_token', t); }
function getRole() { return localStorage.getItem('user_role'); }
function setRole(r) { localStorage.setItem('user_role', r); }
function getUsername() { return localStorage.getItem('username'); }
function setUsername(u) { localStorage.setItem('username', u); }
function clearAuth() { localStorage.removeItem('jwt_token'); localStorage.removeItem('user_role'); localStorage.removeItem('username'); }
function isLoggedIn() { return !!getToken(); }
function isAdmin() { return getRole() === 'ADMIN'; }

// === Redirect helpers ===
function requireAuth() { if (!isLoggedIn()) { window.location.href = '/login.html'; } }
function requireAdmin() { requireAuth(); if (!isAdmin()) { window.location.href = '/dashboard.html'; } }
function redirectIfLoggedIn() { if (isLoggedIn()) { window.location.href = '/dashboard.html'; } }

// // === HTTP client ===
// async function api(method, path, body = null) {
//     const headers = { 'Content-Type': 'application/json' };
//     const token = getToken();
//     if (token) headers['Authorization'] = `Bearer ${token}`;

//     const opts = { method, headers };
//     if (body) opts.body = JSON.stringify(body);

//     const res = await fetch(API_BASE + path, opts);

//     if (res.status === 401) { clearAuth(); window.location.href = '/login.html'; return; }

//     const data = await res.json().catch(() => ({}));
//     if (!res.ok) throw new Error(data.error || data.message || 'Lỗi không xác định');
//     return data;
// }
// === HTTP client ===
async function api(method, path, body = null) {
    const headers = { 'Content-Type': 'application/json' };
    const token = getToken();
    if (token) headers['Authorization'] = `Bearer ${token}`;

    const opts = { method, headers };
    if (body) opts.body = JSON.stringify(body);

    const res = await fetch(API_BASE + path, opts);

    if (res.status === 401) { 
        clearAuth(); 
        window.location.href = '/login.html'; 
        return; 
    }

    // SỬA: Xử lý response đúng cách
    let data;
    const contentType = res.headers.get('content-type');
    if (contentType && contentType.includes('application/json')) {
        data = await res.json().catch(() => ({}));
    } else {
        data = { error: await res.text() };
    }
    
    if (!res.ok) {
        // SỬA: Hiển thị lỗi chi tiết hơn
        const errorMsg = data.error || data.message || `HTTP ${res.status}: ${res.statusText}`;
        console.error('API Error:', errorMsg);
        throw new Error(errorMsg);
    }
    
    return data;
}

const apiGet = (path) => api('GET', path);
const apiPost = (path, body) => api('POST', path, body);
const apiPut = (path, body) => api('PUT', path, body);
const apiDelete = (path) => api('DELETE', path);

// === Toast notifications ===
function showToast(msg, type = 'info') {
    let container = document.getElementById('toast-container');
    if (!container) {
        container = document.createElement('div');
        container.id = 'toast-container';
        container.className = 'toast-container';
        document.body.appendChild(container);
    }
    const t = document.createElement('div');
    t.className = `toast ${type}`;
    t.textContent = msg;
    container.appendChild(t);
    setTimeout(() => t.remove(), 3500);
}

// === Sidebar ===
function renderSidebar(activePage) {
    const role = getRole();
    const username = getUsername();
    const initial = username ? username[0].toUpperCase() : '?';

    const adminLinks = `
        <a href="users.html" class="nav-item ${activePage === 'users' ? 'active' : ''}">
            <span class="icon">👥</span><span class="label">Nhân Viên</span>
        </a>
        <a href="scanqr.html" class="nav-item ${activePage === 'scanqr' ? 'active' : ''}">
            <span class="icon">🫆</span><span class="label">Quét mã điểm danh</span>
        </a>
        <a href="config.html" class="nav-item ${activePage === 'config' ? 'active' : ''}">
            <span class="icon">⚙️</span><span class="label">Cấu Hình</span>
        </a>`;

    return `
    <div class="sidebar">
      <div class="sidebar-brand">⏱ AttendSys <span>v1</span></div>
      <nav class="sidebar-nav">
        <a href="dashboard.html" class="nav-item ${activePage === 'dashboard' ? 'active' : ''}">
            <span class="icon">📊</span><span class="label">Dashboard</span>
        </a>
        <a href="attendance.html" class="nav-item ${activePage === 'attendance' ? 'active' : ''}">
            <span class="icon">🕐</span><span class="label">Điểm Danh</span>
        </a>
        <a href="leave.html" class="nav-item ${activePage === 'leave' ? 'active' : ''}">
            <span class="icon">📅</span><span class="label">Nghỉ Phép</span>
        </a>
        ${role === 'ADMIN' || role === 'MANAGER' ? adminLinks : ''}
      </nav>
      <div class="sidebar-footer">
        <div class="user-info">
          <div class="user-avatar">${initial}</div>
          <div>
            <div class="user-name">${username || ''}</div>
            <div class="user-role">${role || ''}</div>
          </div>
        </div>
        <button class="btn-logout" onclick="logout()">🚪 Đăng xuất</button>
      </div>
    </div>`;
}

function logout() { clearAuth(); window.location.href = '/login.html'; }

// === Date/Time helpers ===
function formatDateTime(dt) {
    if (!dt) return '-';
    return new Date(dt).toLocaleString('vi-VN', { hour12: false });
}
function formatDate(d) {
    if (!d) return '-';
    return new Date(d).toLocaleDateString('vi-VN');
}
function todayISO() { return new Date().toISOString().split('T')[0]; }

function setUserId(id) { localStorage.setItem('user_id', id); }
function getUserId() { return localStorage.getItem('user_id'); }





// === Export helpers ===
function downloadFile(blob, filename) {
    const url = window.URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = filename;
    document.body.appendChild(a);
    a.click();
    document.body.removeChild(a);
    window.URL.revokeObjectURL(url);
}

// === User info helper ===
async function getCurrentUser() {
    try {
        const user = await apiGet('/auth/me');
        return user;
    } catch (err) {
        console.error('Cannot get current user:', err);
        return null;
    }
}

// === Export attendance Excel ===
async function exportAttendanceExcel(year, month) {
    const token = getToken();
    if (!token) {
        showToast('Vui lòng đăng nhập lại', 'error');
        return;
    }
    
    try {
        const response = await fetch(`${API_BASE}/attendance/export?month=${month}&year=${year}`, {
            method: 'GET',
            headers: {
                'Authorization': `Bearer ${token}`
            }
        });
        
        if (!response.ok) {
            const error = await response.text();
            throw new Error(error || 'Export thất bại');
        }
        
        const blob = await response.blob();
        downloadFile(blob, `attendance_${year}_${month}.xlsx`);
        showToast('Export thành công!', 'success');
    } catch (err) {
        showToast(err.message, 'error');
        throw err;
    }
}

// === Lấy thông tin user hiện tại (có id) ===
async function getCurrentUserWithId() {
    try {
        const user = await apiGet('/auth/me');
        return user;
    } catch (err) {
        console.error('Cannot get current user:', err);
        return null;
    }
}
