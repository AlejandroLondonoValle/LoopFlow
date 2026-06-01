/**
 * LoopFlow — app.js
 * Utilidades globales: dark mode, toast notifications, Ctrl+K search,
 * mobile sidebar toggle, shared helpers.
 */

// ============================================================
// Dark Mode
// ============================================================

const DarkMode = {
  KEY: 'loopflow-theme',

  init() {
    const saved = localStorage.getItem(this.KEY);
    const prefersDark = window.matchMedia('(prefers-color-scheme: dark)').matches;
    const isDark = saved ? saved === 'dark' : prefersDark;
    this.apply(isDark);
    // Escuchar cambios del sistema
    window.matchMedia('(prefers-color-scheme: dark)').addEventListener('change', e => {
      if (!localStorage.getItem(this.KEY)) this.apply(e.matches);
    });
  },

  toggle() {
    const isDark = document.documentElement.classList.contains('dark');
    this.apply(!isDark);
    localStorage.setItem(this.KEY, !isDark ? 'dark' : 'light');
  },

  apply(isDark) {
    document.documentElement.classList.toggle('dark', isDark);
    // Actualizar icono del botón si existe
    const icons = document.querySelectorAll('[data-theme-icon]');
    icons.forEach(icon => {
      icon.textContent = isDark ? '☀️' : '🌙';
      icon.setAttribute('title', isDark ? 'Cambiar a modo claro' : 'Cambiar a modo oscuro');
    });
  },

  isDark() {
    return document.documentElement.classList.contains('dark');
  }
};

// ============================================================
// Toast Notifications
// ============================================================

const Toast = {
  container: null,

  init() {
    this.container = document.getElementById('toast-container');
    if (!this.container) {
      this.container = document.createElement('div');
      this.container.id = 'toast-container';
      document.body.appendChild(this.container);
    }
  },

  show(message, type = 'info', duration = 3500) {
    const toast = document.createElement('div');
    toast.className = `toast ${type}`;

    const icons = { success: '✅', error: '❌', info: 'ℹ️' };
    toast.innerHTML = `
      <span style="font-size:1rem">${icons[type] || '📌'}</span>
      <span style="flex:1;color:var(--text-primary)">${message}</span>
      <button onclick="this.parentElement.remove()" style="background:none;border:none;cursor:pointer;color:var(--text-muted);font-size:1rem;padding:0;line-height:1">×</button>
    `;

    this.container.appendChild(toast);

    setTimeout(() => {
      toast.style.opacity = '0';
      toast.style.transform = 'translateX(20px)';
      toast.style.transition = 'all 0.3s ease';
      setTimeout(() => toast.remove(), 300);
    }, duration);
  },

  success: (msg) => Toast.show(msg, 'success'),
  error:   (msg) => Toast.show(msg, 'error'),
  info:    (msg) => Toast.show(msg, 'info'),
};

// ============================================================
// Sidebar Mobile Toggle
// ============================================================

const Sidebar = {
  el: null,
  overlay: null,

  init() {
    this.el = document.querySelector('.sidebar');
    if (!this.el) return;

    // Crear overlay para cerrar sidebar en mobile
    this.overlay = document.createElement('div');
    this.overlay.style.cssText = `
      display:none;position:fixed;inset:0;background:rgba(0,0,0,0.5);
      z-index:35;backdrop-filter:blur(2px);`;
    this.overlay.addEventListener('click', () => this.close());
    document.body.appendChild(this.overlay);

    // Botón hamburguesa
    document.querySelectorAll('[data-sidebar-toggle]').forEach(btn => {
      btn.addEventListener('click', () => this.toggle());
    });
  },

  toggle() {
    const isOpen = this.el.classList.contains('open');
    isOpen ? this.close() : this.open();
  },

  open() {
    this.el.classList.add('open');
    this.overlay.style.display = 'block';
    setTimeout(() => this.overlay.style.opacity = '1', 10);
  },

  close() {
    this.el.classList.remove('open');
    this.overlay.style.opacity = '0';
    setTimeout(() => this.overlay.style.display = 'none', 300);
  }
};

// ============================================================
// Ctrl+K Search
// ============================================================

const SearchBar = {
  init() {
    document.addEventListener('keydown', e => {
      if ((e.ctrlKey || e.metaKey) && e.key === 'k') {
        e.preventDefault();
        const input = document.querySelector('.navbar-search input');
        if (input) {
          input.focus();
          input.select();
        }
      }
    });
  }
};

// ============================================================
// Active Sidebar Link
// ============================================================

function setActiveSidebarLink() {
  const currentPath = window.location.pathname.split('/').pop() || 'index.html';
  document.querySelectorAll('.sidebar-link[href]').forEach(link => {
    const linkPath = link.getAttribute('href').split('/').pop();
    if (linkPath === currentPath) {
      link.classList.add('active');
    }
  });
}

// ============================================================
// Utilidades de Fecha / Formato
// ============================================================

const Format = {
  date(dateStr) {
    if (!dateStr) return '—';
    const d = new Date(dateStr);
    return d.toLocaleDateString('es-CO', { day: '2-digit', month: 'short', year: 'numeric' });
  },

  dateRelative(dateStr) {
    if (!dateStr) return '—';
    const d = new Date(dateStr);
    const now = new Date();
    const diff = Math.round((d - now) / 86400000);
    if (diff === 0) return 'Hoy';
    if (diff === 1) return 'Mañana';
    if (diff === -1) return 'Ayer';
    if (diff > 1 && diff < 7) return `En ${diff} días`;
    if (diff < 0) return `Hace ${Math.abs(diff)} días`;
    return this.date(dateStr);
  },

  frequency(freq) {
    const map = { DAILY: 'Diario', WEEKLY: 'Semanal', MONTHLY: 'Mensual' };
    return map[freq] || freq;
  },

  priority(p) {
    const map = { CRITICAL: 'Crítica', HIGH: 'Alta', MEDIUM: 'Media', LOW: 'Baja' };
    return map[p] || p;
  },

  status(s) {
    const map = { TODO: 'Por hacer', IN_PROGRESS: 'En progreso', DONE: 'Hecho' };
    return map[s] || s;
  }
};

// ============================================================
// Escape HTML helper (XSS prevention)
// ============================================================

function escapeHtml(str) {
  if (!str) return '';
  return String(str)
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#039;');
}

// ============================================================
// Priority badge helper
// ============================================================

function priorityBadge(priority) {
  const map = {
    CRITICAL: ['priority-critical', '🔴 Crítica'],
    HIGH:     ['priority-high',     '🟠 Alta'],
    MEDIUM:   ['priority-medium',   '🟡 Media'],
    LOW:      ['priority-low',      '🟢 Baja'],
  };
  const [cls, label] = map[priority] || ['badge-gray', priority];
  return `<span class="badge ${cls}">${label}</span>`;
}

// ============================================================
// Init Global
// ============================================================

document.addEventListener('DOMContentLoaded', () => {
  DarkMode.init();
  Toast.init();
  Sidebar.init();
  SearchBar.init();
  setActiveSidebarLink();

  // Toggle de tema
  document.querySelectorAll('[data-toggle-theme]').forEach(btn => {
    btn.addEventListener('click', () => DarkMode.toggle());
  });
});

// Exponer globalmente
window.DarkMode   = DarkMode;
window.Toast      = Toast;
window.Sidebar    = Sidebar;
window.Format     = Format;
window.escapeHtml = escapeHtml;
window.priorityBadge = priorityBadge;
