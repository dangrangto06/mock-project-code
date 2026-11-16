// CodeForge Main Application Router & State Manager

window.CodeForgeApp = (function () {
  let currentRoute = 'dashboard'; // 'dashboard' | 'library' | 'ide' | 'create-problem' | 'login' | 'signup'
  let activeTabFilter = 'all'; // 'all' | 'saved' | 'my'
  let filterDifficulty = 'all';
  let searchQuery = '';
  let filterTopic = 'all';
  let filterStatus = 'all';
  let sortAcceptance = null; // null (catalog order) | 'desc' | 'asc'
  let currentPage = 1;
  const PAGE_SIZE = 10;

  const TOKEN_KEY = 'codeforge_token';
  const USER_KEY = 'codeforge_user';

  // Detect login state from storage (set by login.js / signup.js)
  function getStoredUser() {
    try {
      const raw = localStorage.getItem(USER_KEY) || sessionStorage.getItem(USER_KEY);
      return raw ? JSON.parse(raw) : null;
    } catch (_) { return null; }
  }

  function getToken() {
    try {
      return localStorage.getItem(TOKEN_KEY) || sessionStorage.getItem(TOKEN_KEY) || null;
    } catch (_) { return null; }
  }

  let isLoggedIn = !!getToken();

  // Build initials from a full name, e.g. "Dav Nguyen" → "DN"
  function buildInitials(fullName) {
    if (!fullName) return '?';
    const parts = fullName.trim().split(/\s+/);
    if (parts.length === 1) return parts[0][0].toUpperCase();
    return (parts[0][0] + parts[parts.length - 1][0]).toUpperCase();
  }

  // Build greeting (first name)
  function buildGreeting(fullName) {
    if (!fullName) return 'there';
    return fullName.trim().split(/\s+/)[0];
  }

  function init() {
    // Re-check login state on init (in case storage changed)
    isLoggedIn = !!getToken();

    setupHashRouting();
    bindGlobalEvents();
    renderProblemTable();
    updateAuthUI();
    updateDashboardForUser();
    handleRoute(window.location.hash.replace('#', '') || 'dashboard');

    // Initialize child modules
    if (window.CodeForgeIDE) CodeForgeIDE.init();
    if (window.CodeForgeEditor) CodeForgeEditor.init();
    if (window.CodeForgeNotes) CodeForgeNotes.init();
    if (window.CodeForgeCommunity) CodeForgeCommunity.init();
  }

  function setupHashRouting() {
    window.addEventListener('hashchange', () => {
      const route = window.location.hash.replace('#', '') || 'dashboard';
      handleRoute(route);
    });
  }

  function navigate(routeName) {
    window.location.hash = `#${routeName}`;
  }

  function updateAuthUI() {
    const userSection = document.getElementById('header-user-section');
    const guestSection = document.getElementById('header-guest-section');

    if (userSection && guestSection) {
      if (isLoggedIn) {
        userSection.classList.remove('hidden');
        guestSection.classList.add('hidden');

        // Populate header with real user data
        const user = getStoredUser();
        if (user) {
          const initials = buildInitials(user.fullName);
          const avatarEl = document.getElementById('header-avatar-initials');
          const nameEl = document.getElementById('header-user-name');
          const dropdownNameEl = document.getElementById('dropdown-user-fullname');
          const dropdownEmailEl = document.getElementById('dropdown-user-email');
          const streakEl = document.getElementById('header-streak-text');

          if (avatarEl) avatarEl.textContent = initials;
          if (nameEl) nameEl.textContent = user.fullName || user.username;
          if (dropdownNameEl) dropdownNameEl.textContent = user.fullName || user.username;
          if (dropdownEmailEl) dropdownEmailEl.textContent = user.email || '';
        }
      } else {
        userSection.classList.add('hidden');
        guestSection.classList.remove('hidden');
      }
    }
  }

  // Update dashboard greeting and stats with the logged-in user's data
  function updateDashboardForUser() {
    const user = getStoredUser();
    if (!user) return;

    const greeting = buildGreeting(user.fullName);

    // Update the greeting heading
    const greetingEl = document.getElementById('dashboard-greeting');
    if (greetingEl) {
      greetingEl.textContent = `Keep the momentum, ${greeting}.`;
    }

    // Update the date line
    const dateEl = document.getElementById('dashboard-date');
    if (dateEl) {
      const now = new Date();
      const options = { weekday: 'long', month: 'long', day: 'numeric' };
      const formatted = now.toLocaleDateString('en-US', options);
      dateEl.textContent = `Your overview / ${formatted}`;
    }
  }

  function handleLoginSubmit() {
    const btn = document.getElementById('btn-login-submit');
    const emailInput = document.getElementById('login-email');
    if (btn) {
      const oldHtml = btn.innerHTML;
      btn.disabled = true;
      btn.innerHTML = `<svg class="animate-spin w-4 h-4 text-white" fill="none" viewBox="0 0 24 24"><circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle><path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path></svg> Signing in...`;

      setTimeout(() => {
        btn.disabled = false;
        btn.innerHTML = oldHtml;
        isLoggedIn = true;
        updateAuthUI();
        showToast("Signed in successfully! Welcome back.", "success");
        navigate('dashboard');
      }, 600);
    }
  }

  function handleSignupSubmit() {
    const btn = document.getElementById('btn-signup-submit');
    const nameInput = document.getElementById('signup-name');
    if (btn) {
      const oldHtml = btn.innerHTML;
      btn.disabled = true;
      btn.innerHTML = `<svg class="animate-spin w-4 h-4 text-white" fill="none" viewBox="0 0 24 24"><circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle><path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path></svg> Creating account...`;

      setTimeout(() => {
        btn.disabled = false;
        btn.innerHTML = oldHtml;
        isLoggedIn = true;
        updateAuthUI();
        showToast("Account created successfully! Welcome to CodeForge.", "success");
        navigate('dashboard');
      }, 700);
    }
  }

  function handleLogout() {
    isLoggedIn = false;
    // Clear stored session
    try {
      localStorage.removeItem(TOKEN_KEY);
      localStorage.removeItem(USER_KEY);
      sessionStorage.removeItem(TOKEN_KEY);
      sessionStorage.removeItem(USER_KEY);
    } catch (_) { }
    updateAuthUI();
    const dropdown = document.getElementById('dropdown-user-menu');
    if (dropdown) dropdown.classList.add('hidden');
    showToast("Signed out successfully.", "info");
    // Redirect to separate login page
    window.location.href = 'login.html';
  }

  function handleRoute(route) {
    currentRoute = route;

    // Elements
    const pageDashboard = document.getElementById('page-dashboard');
    const pageLibrary = document.getElementById('page-library');
    const pageNotes = document.getElementById('page-notes');
    const pageCommunity = document.getElementById('page-community');
    const pageIde = document.getElementById('page-ide');
    const pageCreateProblem = document.getElementById('page-create-problem');
    const pageLogin = document.getElementById('page-login');
    const pageSignup = document.getElementById('page-signup');
    const sidebarContainer = document.getElementById('main-sidebar');

    // Hide all pages first
    if (pageDashboard) pageDashboard.classList.add('hidden');
    if (pageLibrary) pageLibrary.classList.add('hidden');
    if (pageNotes) pageNotes.classList.add('hidden');
    if (pageCommunity) pageCommunity.classList.add('hidden');
    if (pageIde) pageIde.classList.add('hidden');
    if (pageCreateProblem) pageCreateProblem.classList.add('hidden');
    if (pageLogin) pageLogin.classList.add('hidden');
    if (pageSignup) pageSignup.classList.add('hidden');

    // Close any open dropdowns
    const dropdown = document.getElementById('dropdown-user-menu');
    if (dropdown) dropdown.classList.add('hidden');

    // Top Navigation Links highlight
    const navOverview = document.getElementById('nav-overview');
    const navPractice = document.getElementById('nav-practice');
    const navNotes = document.getElementById('nav-notes');
    const navCommunity = document.getElementById('nav-community');

    const resetTopNav = () => {
      [navOverview, navPractice, navNotes, navCommunity].forEach(link => {
        if (!link) return;
        link.className = "text-sm text-gray-600 hover:text-gray-900 transition font-normal pb-4 border-b-2 border-transparent";
      });
    };

    const activateTopNav = (link) => {
      if (!link) return;
      link.className = "text-sm text-emerald-800 font-semibold pb-4 border-b-2 border-emerald-600 transition";
    };

    resetTopNav();

    // Reset Sidebar Links highlight
    const sidebarLinks = document.querySelectorAll('.sidebar-nav-item');
    sidebarLinks.forEach(item => {
      item.classList.remove('bg-emerald-50', 'text-emerald-800', 'font-medium');
      item.classList.add('text-gray-600', 'hover:bg-gray-100', 'hover:text-gray-900');
    });

    const activateSidebarLink = (id) => {
      const el = document.getElementById(id);
      if (el) {
        el.classList.remove('text-gray-600', 'hover:bg-gray-100', 'hover:text-gray-900');
        el.classList.add('bg-emerald-50', 'text-emerald-800', 'font-medium');
      }
    };

    // Route Switching
    if (route === 'dashboard') {
      // If not logged in, redirect to login page
      if (!isLoggedIn) {
        window.location.href = 'login.html';
        return;
      }
      if (pageDashboard) pageDashboard.classList.remove('hidden');
      if (sidebarContainer) sidebarContainer.classList.remove('hidden');
      activateTopNav(navOverview);
      activateSidebarLink('sb-dashboard');
      document.body.classList.remove('focus-mode');
    } else if (route === 'library') {
      if (pageLibrary) pageLibrary.classList.remove('hidden');
      if (sidebarContainer) sidebarContainer.classList.remove('hidden');
      activateTopNav(navPractice);
      activateSidebarLink('sb-library');
      document.body.classList.remove('focus-mode');
      renderProblemTable();
    } else if (route === 'notes') {
      if (pageNotes) pageNotes.classList.remove('hidden');
      if (sidebarContainer) sidebarContainer.classList.remove('hidden');
      activateTopNav(navNotes);
      activateSidebarLink('sb-notes');
      document.body.classList.remove('focus-mode');
      if (window.CodeForgeNotes) CodeForgeNotes.renderNotes();
    } else if (route === 'community') {
      if (pageCommunity) pageCommunity.classList.remove('hidden');
      if (sidebarContainer) sidebarContainer.classList.remove('hidden');
      activateTopNav(navCommunity);
      activateSidebarLink('sb-community');
      document.body.classList.remove('focus-mode');
      if (window.CodeForgeCommunity) CodeForgeCommunity.renderDiscussions();
    } else if (route === 'create-problem') {
      if (pageCreateProblem) pageCreateProblem.classList.remove('hidden');
      if (sidebarContainer) sidebarContainer.classList.remove('hidden');
      activateTopNav(navPractice);
      activateSidebarLink('sb-my-problems');
      document.body.classList.remove('focus-mode');
    } else if (route === 'ide' || route.startsWith('solve/')) {
      if (pageIde) pageIde.classList.remove('hidden');
      if (sidebarContainer) sidebarContainer.classList.add('hidden'); // IDE has split full width layout
      activateTopNav(navPractice);
    } else if (route === 'login') {
      if (pageLogin) pageLogin.classList.remove('hidden');
      if (sidebarContainer) sidebarContainer.classList.add('hidden');
      document.body.classList.remove('focus-mode');
    } else if (route === 'signup') {
      if (pageSignup) pageSignup.classList.remove('hidden');
      if (sidebarContainer) sidebarContainer.classList.add('hidden');
      document.body.classList.remove('focus-mode');
    } else {
      // Default to dashboard
      if (pageDashboard) pageDashboard.classList.remove('hidden');
      if (sidebarContainer) sidebarContainer.classList.remove('hidden');
      activateTopNav(navOverview);
      activateSidebarLink('sb-dashboard');
    }

    window.scrollTo({ top: 0, behavior: 'smooth' });
  }

  // ---------- Problem library (practice page) ----------

  function escapeHtml(value) {
    return String(value).replace(/[&<>"']/g, ch => (
      { '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[ch]
    ));
  }

  function setText(id, text) {
    const el = document.getElementById(id);
    if (el) el.textContent = text;
  }

  // Header, progress banner and tab counters are all derived from the data,
  // so they stay correct whatever the size of the catalog or the solved count.
  function updateLibraryStats() {
    const all = CodeForgeData.problems;
    const total = all.length;
    const solved = all.filter(p => p.status === 'solved').length;
    const pct = total ? Math.round((solved / total) * 100) : 0;

    ['Easy', 'Medium', 'Hard'].forEach(level => {
      const inLevel = all.filter(p => p.difficulty === level);
      const done = inLevel.filter(p => p.status === 'solved').length;
      setText(`lib-count-${level.toLowerCase()}`, `${done}/${inLevel.length}`);
    });

    setText('lib-solved-title', solved === 0 ? '0 solved. Your first one is waiting.' : `${solved} solved. Keep going.`);
    setText('lib-solved-sub', `You've completed ${pct}% of the library.`);
    setText('library-total-copy', total);
    setText('tab-count-all', total);
    setText('tab-count-saved', all.filter(p => p.saved).length);
    setText('tab-count-my', all.filter(p => p.isMine).length);
    setText('sb-my-problems-count', all.filter(p => p.isMine).length);
  }

  // Topic dropdown is built from the catalog so it never goes out of date.
  function syncTopicOptions() {
    const select = document.getElementById('filter-topic-select');
    if (!select) return;
    const topics = [...new Set(CodeForgeData.problems.flatMap(p => p.topics || []))]
      .sort((x, y) => x.localeCompare(y));
    const signature = topics.join('|');
    if (select.dataset.signature === signature) return;
    select.dataset.signature = signature;
    select.innerHTML = '<option value="all">Topics</option>' +
      topics.map(t => `<option value="${escapeHtml(t)}">${escapeHtml(t)}</option>`).join('');
    select.value = topics.includes(filterTopic) ? filterTopic : 'all';
  }

  function getFilteredProblems() {
    const q = searchQuery.trim().toLowerCase();

    const list = CodeForgeData.problems.filter(p => {
      if (q) {
        const matchTitle = p.title.toLowerCase().includes(q);
        const matchId = String(p.id).includes(q);
        const matchTopic = (p.topics || []).some(t => t.toLowerCase().includes(q));
        if (!matchTitle && !matchId && !matchTopic) return false;
      }
      if (activeTabFilter === 'saved' && !p.saved) return false;
      if (activeTabFilter === 'my' && !p.isMine) return false;
      if (filterDifficulty !== 'all' && p.difficulty.toLowerCase() !== filterDifficulty.toLowerCase()) return false;
      if (filterTopic !== 'all' && !(p.topics || []).includes(filterTopic)) return false;
      if (filterStatus !== 'all' && p.status !== filterStatus) return false;
      return true;
    });

    if (sortAcceptance) {
      const rate = p => parseFloat(p.acceptance) || 0;
      list.sort((x, y) => sortAcceptance === 'desc' ? rate(y) - rate(x) : rate(x) - rate(y));
    }
    return list;
  }

  // 1 2 3 … 10   |   1 … 4 5 6 … 10   |   1 … 8 9 10
  function getPageItems(current, total) {
    const keep = new Set([1, total, current - 1, current, current + 1]);
    if (current <= 3) { keep.add(2); keep.add(3); }
    if (current >= total - 2) { keep.add(total - 1); keep.add(total - 2); }
    const pages = [...keep].filter(n => n >= 1 && n <= total).sort((x, y) => x - y);

    const items = [];
    pages.forEach((n, i) => {
      if (i > 0) {
        const gap = n - pages[i - 1];
        if (gap === 2) items.push(n - 1);
        else if (gap > 2) items.push('…');
      }
      items.push(n);
    });
    return items;
  }

  function renderPagination(totalItems) {
    const nav = document.getElementById('library-pagination');
    const info = document.getElementById('library-showing');
    if (!nav || !info) return;

    if (totalItems === 0) {
      info.textContent = 'Showing 0 problems';
      nav.innerHTML = '';
      return;
    }

    const totalPages = Math.ceil(totalItems / PAGE_SIZE);
    const start = (currentPage - 1) * PAGE_SIZE + 1;
    const end = Math.min(currentPage * PAGE_SIZE, totalItems);
    info.textContent = `Showing ${start}\u2013${end} of ${totalItems} problem${totalItems === 1 ? '' : 's'}`;

    if (totalPages === 1) {
      nav.innerHTML = '';
      return;
    }

    const edgeBtn = (label, page, disabled) => `
      <button type="button" data-page="${page}" ${disabled ? 'disabled' : ''}
        class="px-3 py-1.5 text-xs font-medium rounded-lg border border-[#d1d9d4] bg-white text-gray-700 transition ${disabled ? 'opacity-40 cursor-not-allowed' : 'hover:bg-gray-50'}">${label}</button>`;

    const pageBtns = getPageItems(currentPage, totalPages).map(item => {
      if (item === '…') return `<span class="w-7 text-center text-xs text-gray-400 select-none">…</span>`;
      const active = item === currentPage;
      return `<button type="button" data-page="${item}" ${active ? 'aria-current="page"' : ''}
        class="w-7 h-7 text-xs font-medium rounded-md transition ${active ? 'bg-emerald-700 text-white' : 'text-gray-600 hover:bg-gray-100'}">${item}</button>`;
    }).join('');

    nav.innerHTML = edgeBtn('Previous', currentPage - 1, currentPage === 1) + pageBtns + edgeBtn('Next', currentPage + 1, currentPage === totalPages);
  }

  function updateSortIndicator() {
    const arrow = document.getElementById('sort-acceptance-arrow');
    const th = document.getElementById('th-acceptance');
    if (arrow) {
      arrow.textContent = sortAcceptance === 'desc' ? '\u2193' : sortAcceptance === 'asc' ? '\u2191' : '\u2195';
      arrow.className = sortAcceptance ? 'text-emerald-700' : 'text-gray-400';
    }
    if (th) th.setAttribute('aria-sort', sortAcceptance === 'desc' ? 'descending' : sortAcceptance === 'asc' ? 'ascending' : 'none');
  }

  function emptyStateMessage() {
    if (activeTabFilter === 'saved' && !searchQuery && filterDifficulty === 'all' && filterTopic === 'all' && filterStatus === 'all') {
      return `<div class="font-medium text-gray-700">No saved problems yet</div><div class="mt-1 text-xs">Tap the bookmark on any problem to keep it here.</div>`;
    }
    if (activeTabFilter === 'my' && !searchQuery && filterDifficulty === 'all' && filterTopic === 'all' && filterStatus === 'all') {
      return `<div class="font-medium text-gray-700">You haven't created a problem yet</div><div class="mt-1 text-xs">Use &ldquo;Create problem&rdquo; to add your own challenge.</div>`;
    }
    return `<div class="font-medium text-gray-700">No problems match your filters</div>
      <button type="button" class="mt-2 text-xs font-semibold text-emerald-700 hover:text-emerald-800 underline underline-offset-2" onclick="CodeForgeApp.clearLibraryFilters()">Clear filters</button>`;
  }

  function renderProblemTable() {
    const tableBody = document.getElementById('problems-table-body');
    if (!tableBody) return;

    syncTopicOptions();
    updateLibraryStats();
    updateSortIndicator();

    const filtered = getFilteredProblems();
    const totalPages = Math.max(1, Math.ceil(filtered.length / PAGE_SIZE));
    if (currentPage > totalPages) currentPage = totalPages;
    renderPagination(filtered.length);

    if (filtered.length === 0) {
      tableBody.innerHTML = `
        <tr>
          <td colspan="7" class="py-14 text-center text-gray-500 text-sm">${emptyStateMessage()}</td>
        </tr>
      `;
      return;
    }

    const pageItems = filtered.slice((currentPage - 1) * PAGE_SIZE, currentPage * PAGE_SIZE);

    tableBody.innerHTML = pageItems.map(p => {
      // Status icon
      let statusIcon = '';
      if (p.status === 'solved') {
        statusIcon = `
          <span class="inline-flex items-center justify-center w-5 h-5 text-emerald-600 bg-emerald-50 rounded-full border border-emerald-200" title="Solved">
            <svg class="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2.5" d="M5 13l4 4L19 7"></path></svg>
          </span>
        `;
      } else if (p.status === 'in-progress') {
        statusIcon = `
          <span class="inline-flex items-center justify-center w-5 h-5 text-amber-600 bg-amber-50 rounded-full border border-amber-200" title="In progress">
            <span class="w-2 h-2 rounded-full bg-amber-500"></span>
          </span>
        `;
      } else {
        statusIcon = `
          <span class="inline-flex items-center justify-center w-5 h-5 text-gray-300 rounded-full border border-gray-300" title="Not started"></span>
        `;
      }

      // Difficulty pill (badge colours live in styles.css)
      const diffBadge = p.difficulty === 'Easy' ? 'badge-easy' : p.difficulty === 'Medium' ? 'badge-medium' : 'badge-hard';

      // Topics pills
      const topicsHtml = (p.topics || []).map(t => `
        <span class="px-2 py-0.5 rounded text-xs bg-gray-100 text-gray-700 border border-gray-200">${escapeHtml(t)}</span>
      `).join(' ');

      // Bookmark
      const bookmarkIcon = p.saved
        ? `<svg class="w-4 h-4 text-emerald-600 fill-emerald-600" viewBox="0 0 24 24"><path d="M5 5a2 2 0 012-2h10a2 2 0 012 2v16l-7-3.5L5 21V5z"></path></svg>`
        : `<svg class="w-4 h-4 text-gray-400 hover:text-gray-600" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 5a2 2 0 012-2h10a2 2 0 012 2v16l-7-3.5L5 21V5z"></path></svg>`;

      return `
        <tr class="border-b border-gray-100 hover:bg-[#fcfdfd] transition group cursor-pointer ${p.status === 'in-progress' ? 'bg-emerald-50/40' : ''}" onclick="CodeForgeApp.openProblem(${p.id})">
          <td class="py-3.5 px-4 text-center w-12" onclick="event.stopPropagation()">
            ${statusIcon}
          </td>
          <td class="py-3.5 px-3 text-xs text-gray-400 font-mono w-14">
            ${p.id}
          </td>
          <td class="py-3.5 px-4">
            <div class="flex items-center gap-2">
              <span class="text-sm font-medium text-gray-900 group-hover:text-emerald-700 transition">${escapeHtml(p.title)}</span>
              ${p.isMine ? `<span class="px-1.5 py-0.2 rounded text-[11px] font-medium bg-emerald-50 text-emerald-700 border border-emerald-200">Yours</span>` : ''}
            </div>
          </td>
          <td class="py-3.5 px-4 w-24">
            <span class="inline-block px-2 py-0.5 rounded text-xs font-medium ${diffBadge}">${p.difficulty}</span>
          </td>
          <td class="py-3.5 px-4">
            <div class="flex flex-wrap gap-1.5">
              ${topicsHtml}
            </div>
          </td>
          <td class="py-3.5 px-4 text-xs text-gray-600 w-28 text-right font-mono">
            ${p.acceptance}
          </td>
          <td class="py-3.5 px-4 text-center w-12" onclick="event.stopPropagation(); CodeForgeApp.toggleBookmark(${p.id})">
            <button type="button" class="p-1 rounded hover:bg-gray-100 transition focus:outline-none" aria-label="${p.saved ? 'Remove from saved' : 'Save problem'}">
              ${bookmarkIcon}
            </button>
          </td>
        </tr>
      `;
    }).join('');
  }

  function clearLibraryFilters() {
    searchQuery = '';
    filterDifficulty = 'all';
    filterTopic = 'all';
    filterStatus = 'all';
    currentPage = 1;
    const searchInput = document.getElementById('library-search-input');
    if (searchInput) searchInput.value = '';
    ['filter-difficulty-select', 'filter-topic-select', 'filter-status-select'].forEach(id => {
      const el = document.getElementById(id);
      if (el) el.value = 'all';
    });
    renderProblemTable();
  }

  function openProblem(problemId) {
    navigate(`ide`);
  }

  function toggleBookmark(problemId) {
    const p = CodeForgeData.problems.find(item => item.id === problemId);
    if (p) {
      p.saved = !p.saved;
      renderProblemTable();
      showToast(p.saved ? `Saved "${p.title}" to your list` : `Removed "${p.title}" from saved`, "info");
    }
  }

  function pickRandomProblem() {
    const unsolved = CodeForgeData.problems.filter(p => p.status !== 'solved');
    const list = unsolved.length ? unsolved : CodeForgeData.problems;
    const random = list[Math.floor(Math.random() * list.length)];
    showToast(`Picked random challenge: ${random.title}! Opening IDE...`, "success");
    setTimeout(() => {
      openProblem(random.id);
    }, 600);
  }

  function bindGlobalEvents() {
    // Top logo click
    const brandLogo = document.getElementById('brand-logo');
    if (brandLogo) {
      brandLogo.addEventListener('click', () => navigate('dashboard'));
    }

    // Top nav clicks
    const navOverview = document.getElementById('nav-overview');
    if (navOverview) navOverview.addEventListener('click', () => navigate('dashboard'));

    const navPractice = document.getElementById('nav-practice');
    if (navPractice) navPractice.addEventListener('click', () => navigate('library'));

    const navNotes = document.getElementById('nav-notes');
    if (navNotes) navNotes.addEventListener('click', () => navigate('notes'));

    const navCommunity = document.getElementById('nav-community');
    if (navCommunity) navCommunity.addEventListener('click', () => navigate('community'));

    // Sidebar navigation clicks
    const sbDashboard = document.getElementById('sb-dashboard');
    if (sbDashboard) sbDashboard.addEventListener('click', () => navigate('dashboard'));

    const sbLibrary = document.getElementById('sb-library');
    if (sbLibrary) sbLibrary.addEventListener('click', () => navigate('library'));

    const sbMyProblems = document.getElementById('sb-my-problems');
    if (sbMyProblems) sbMyProblems.addEventListener('click', () => navigate('create-problem'));

    const sbNotes = document.getElementById('sb-notes');
    if (sbNotes) sbNotes.addEventListener('click', () => navigate('notes'));

    const sbCommunity = document.getElementById('sb-community');
    if (sbCommunity) sbCommunity.addEventListener('click', () => navigate('community'));

    const sbSaved = document.getElementById('sb-saved');
    if (sbSaved) {
      sbSaved.addEventListener('click', () => {
        navigate('library');
        setTimeout(() => {
          const tabSaved = document.getElementById('tab-filter-saved');
          if (tabSaved) tabSaved.click();
        }, 100);
      });
    }

    // Dashboard Buttons
    const btnBrowseProblems = document.getElementById('btn-browse-problems');
    if (btnBrowseProblems) btnBrowseProblems.addEventListener('click', () => navigate('library'));

    const btnContinueSolving = document.getElementById('btn-continue-solving');
    if (btnContinueSolving) btnContinueSolving.addEventListener('click', () => navigate('ide'));

    // Library "+ Create problem" button
    const btnCreateProblem = document.getElementById('btn-create-problem-lib');
    if (btnCreateProblem) btnCreateProblem.addEventListener('click', () => navigate('create-problem'));

    // Library Random Button
    const btnRandom = document.getElementById('btn-pick-random');
    if (btnRandom) btnRandom.addEventListener('click', pickRandomProblem);

    // IDE Back to Library button
    const btnBackLibrary = document.getElementById('btn-back-to-library');
    if (btnBackLibrary) btnBackLibrary.addEventListener('click', () => navigate('library'));

    // Library Tabs (All / Saved / My problems)
    const tabAll = document.getElementById('tab-filter-all');
    const tabSaved = document.getElementById('tab-filter-saved');
    const tabMy = document.getElementById('tab-filter-my');

    const setLibraryTab = (tab) => {
      activeTabFilter = tab;
      currentPage = 1;
      [tabAll, tabSaved, tabMy].forEach(t => {
        if (!t) return;
        t.className = "px-3 py-2 text-sm text-gray-500 hover:text-gray-800 font-normal border-b-2 border-transparent transition";
      });
      if (tab === 'all' && tabAll) tabAll.className = "px-3 py-2 text-sm text-emerald-800 font-semibold border-b-2 border-emerald-600 transition flex items-center gap-1.5";
      if (tab === 'saved' && tabSaved) tabSaved.className = "px-3 py-2 text-sm text-emerald-800 font-semibold border-b-2 border-emerald-600 transition flex items-center gap-1.5";
      if (tab === 'my' && tabMy) tabMy.className = "px-3 py-2 text-sm text-emerald-800 font-semibold border-b-2 border-emerald-600 transition flex items-center gap-1.5";
      renderProblemTable();
    };

    if (tabAll) tabAll.addEventListener('click', () => setLibraryTab('all'));
    if (tabSaved) tabSaved.addEventListener('click', () => setLibraryTab('saved'));
    if (tabMy) tabMy.addEventListener('click', () => setLibraryTab('my'));

    // Search Input
    const searchInput = document.getElementById('library-search-input');
    if (searchInput) {
      searchInput.addEventListener('input', (e) => {
        searchQuery = e.target.value;
        currentPage = 1;
        renderProblemTable();
      });
    }

    // Difficulty Filter Dropdown
    const diffSelect = document.getElementById('filter-difficulty-select');
    if (diffSelect) {
      diffSelect.addEventListener('change', (e) => {
        filterDifficulty = e.target.value;
        currentPage = 1;
        renderProblemTable();
      });
    }

    // Topic + status dropdowns
    const topicSelect = document.getElementById('filter-topic-select');
    if (topicSelect) {
      topicSelect.addEventListener('change', (e) => {
        filterTopic = e.target.value;
        currentPage = 1;
        renderProblemTable();
      });
    }
    const statusSelect = document.getElementById('filter-status-select');
    if (statusSelect) {
      statusSelect.addEventListener('change', (e) => {
        filterStatus = e.target.value;
        currentPage = 1;
        renderProblemTable();
      });
    }

    // Acceptance column sort: catalog order -> high to low -> low to high
    const sortBtn = document.getElementById('sort-acceptance');
    if (sortBtn) {
      sortBtn.addEventListener('click', () => {
        sortAcceptance = sortAcceptance === null ? 'desc' : sortAcceptance === 'desc' ? 'asc' : null;
        currentPage = 1;
        renderProblemTable();
      });
    }

    // Pagination (event delegation)
    const pagination = document.getElementById('library-pagination');
    if (pagination) {
      pagination.addEventListener('click', (e) => {
        const btn = e.target.closest('button[data-page]');
        if (!btn || btn.disabled) return;
        currentPage = parseInt(btn.dataset.page, 10);
        renderProblemTable();
        const card = document.getElementById('problems-table-body');
        if (card) card.closest('.rounded-xl').scrollIntoView({ behavior: 'smooth', block: 'start' });
      });
    }

    // Cmd/Ctrl + K focuses the library search (matches the hint in the design)
    document.addEventListener('keydown', (e) => {
      if ((e.metaKey || e.ctrlKey) && e.key.toLowerCase() === 'k' && currentRoute === 'library') {
        e.preventDefault();
        const input = document.getElementById('library-search-input');
        if (input) input.focus();
      }
    });

    // User Avatar Dropdown Toggle
    const btnUserAvatar = document.getElementById('btn-user-avatar');
    const dropdownMenu = document.getElementById('dropdown-user-menu');
    if (btnUserAvatar && dropdownMenu) {
      btnUserAvatar.addEventListener('click', (e) => {
        e.stopPropagation();
        dropdownMenu.classList.toggle('hidden');
      });

      // Close when clicking outside
      document.addEventListener('click', (e) => {
        if (!btnUserAvatar.contains(e.target) && !dropdownMenu.contains(e.target)) {
          dropdownMenu.classList.add('hidden');
        }
      });
    }

    // Sign out button (dropdown)
    const btnLogout = document.getElementById('btn-auth-logout');
    if (btnLogout) {
      btnLogout.addEventListener('click', handleLogout);
    }

    // Sign out button (sidebar)
    const sbLogout = document.getElementById('sb-logout');
    if (sbLogout) {
      sbLogout.addEventListener('click', handleLogout);
    }

    // Social Auth buttons
    document.querySelectorAll('.btn-social-auth').forEach(btn => {
      btn.addEventListener('click', () => {
        showToast("Social login simulated! Redirecting to Dashboard...", "success");
        setTimeout(() => {
          isLoggedIn = true;
          updateAuthUI();
          navigate('dashboard');
        }, 500);
      });
    });

    // Modal Close
    const closeModalBtn = document.getElementById('btn-close-modal');
    if (closeModalBtn) {
      closeModalBtn.addEventListener('click', () => {
        const modal = document.getElementById('submission-success-modal');
        if (modal) modal.classList.add('hidden');
      });
    }
  }

  function showToast(message, type = 'info') {
    const container = document.getElementById('toast-container');
    if (!container) return;

    const toast = document.createElement('div');
    const bg = type === 'success' ? 'bg-emerald-800 text-white' : type === 'warn' ? 'bg-amber-800 text-white' : 'bg-gray-900 text-white';
    toast.className = `flex items-center gap-2.5 px-4 py-2.5 rounded-lg shadow-lg text-xs font-medium ${bg} transform translate-y-2 opacity-0 transition-all duration-300`;
    toast.innerHTML = `
      <svg class="w-4 h-4 text-emerald-300 flex-shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z"></path></svg>
      <span>${message}</span>
    `;

    container.appendChild(toast);
    requestAnimationFrame(() => {
      toast.classList.remove('translate-y-2', 'opacity-0');
    });

    setTimeout(() => {
      toast.classList.add('opacity-0', 'translate-y-2');
      setTimeout(() => toast.remove(), 300);
    }, 3200);
  }

  return {
    init,
    navigate,
    openProblem,
    toggleBookmark,
    clearLibraryFilters,
    pickRandomProblem,
    showToast,
    handleLoginSubmit,
    handleSignupSubmit,
    handleLogout
  };
})();

// Boot on DOM ready
document.addEventListener('DOMContentLoaded', CodeForgeApp.init);

