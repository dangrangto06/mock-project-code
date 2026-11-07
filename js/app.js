// CodeForge Main Application Router & State Manager

window.CodeForgeApp = (function () {
  let currentRoute = 'dashboard'; // 'dashboard' | 'library' | 'ide' | 'create-problem' | 'login' | 'signup'
  let activeTabFilter = 'all'; // 'all' | 'saved' | 'my'
  let filterDifficulty = 'all';
  let searchQuery = '';
  let isLoggedIn = true; // User authentication state

  function init() {
    setupHashRouting();
    bindGlobalEvents();
    renderProblemTable();
    updateAuthUI();
    handleRoute(window.location.hash.replace('#', '') || 'dashboard');
    
    // Initialize child modules
    if (window.CodeForgeIDE) CodeForgeIDE.init();
    if (window.CodeForgeEditor) CodeForgeEditor.init();
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
      } else {
        userSection.classList.add('hidden');
        guestSection.classList.remove('hidden');
      }
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
    updateAuthUI();
    const dropdown = document.getElementById('dropdown-user-menu');
    if (dropdown) dropdown.classList.add('hidden');
    showToast("Signed out successfully.", "info");
    navigate('login');
  }

  function handleRoute(route) {
    currentRoute = route;

    // Elements
    const pageDashboard = document.getElementById('page-dashboard');
    const pageLibrary = document.getElementById('page-library');
    const pageIde = document.getElementById('page-ide');
    const pageCreateProblem = document.getElementById('page-create-problem');
    const pageLogin = document.getElementById('page-login');
    const pageSignup = document.getElementById('page-signup');
    const sidebarContainer = document.getElementById('main-sidebar');

    // Hide all pages first
    if (pageDashboard) pageDashboard.classList.add('hidden');
    if (pageLibrary) pageLibrary.classList.add('hidden');
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

    const resetTopNav = () => {
      [navOverview, navPractice].forEach(link => {
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
      item.className = "sidebar-nav-item flex items-center gap-3 px-3 py-2 rounded-lg text-sm text-gray-600 hover:bg-gray-100 hover:text-gray-900 transition";
    });

    const activateSidebarLink = (id) => {
      const el = document.getElementById(id);
      if (el) {
        el.className = "sidebar-nav-item flex items-center gap-3 px-3 py-2 rounded-lg text-sm font-medium bg-emerald-50 text-emerald-800 transition";
      }
    };

    // Route Switching
    if (route === 'dashboard') {
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

    window.scrollTo({ top: 0, behavior: 'smooth' });
  }

  function renderProblemTable() {
    const tableBody = document.getElementById('problems-table-body');
    if (!tableBody) return;

    let filtered = CodeForgeData.problems.filter(p => {
      // Search filter
      if (searchQuery) {
        const q = searchQuery.toLowerCase();
        const matchTitle = p.title.toLowerCase().includes(q);
        const matchId = String(p.id).includes(q);
        const matchTopic = p.topics && p.topics.some(t => t.toLowerCase().includes(q));
        if (!matchTitle && !matchId && !matchTopic) return false;
      }
      // Tab filter
      if (activeTabFilter === 'saved' && !p.saved) return false;
      if (activeTabFilter === 'my' && !p.isMine) return false;
      // Difficulty filter
      if (filterDifficulty !== 'all' && p.difficulty.toLowerCase() !== filterDifficulty.toLowerCase()) return false;

      return true;
    });

    if (filtered.length === 0) {
      tableBody.innerHTML = `
        <tr>
          <td colspan="7" class="py-12 text-center text-gray-500 text-sm">
            No problems found matching your criteria.
          </td>
        </tr>
      `;
      return;
    }

    tableBody.innerHTML = filtered.map(p => {
      // Status icon
      let statusIcon = '';
      if (p.status === 'solved') {
        statusIcon = `
          <span class="inline-flex items-center justify-center w-5 h-5 text-emerald-600 bg-emerald-50 rounded-full border border-emerald-200">
            <svg class="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2.5" d="M5 13l4 4L19 7"></path></svg>
          </span>
        `;
      } else if (p.status === 'in-progress') {
        statusIcon = `
          <span class="inline-flex items-center justify-center w-5 h-5 text-amber-600 bg-amber-50 rounded-full border border-amber-200">
            <span class="w-2 h-2 rounded-full bg-amber-500"></span>
          </span>
        `;
      } else {
        statusIcon = `
          <span class="inline-flex items-center justify-center w-5 h-5 text-gray-300 rounded-full border border-gray-300">
          </span>
        `;
      }

      // Difficulty class
      const diffClass = p.difficulty === 'Easy' 
        ? 'text-emerald-700 font-medium' 
        : p.difficulty === 'Medium' 
        ? 'text-amber-700 font-medium' 
        : 'text-rose-700 font-medium';

      // Topics pills
      const topicsHtml = (p.topics || []).map(t => `
        <span class="px-2 py-0.5 rounded text-xs bg-gray-100 text-gray-700 border border-gray-200">${t}</span>
      `).join(' ');

      // Bookmark
      const bookmarkIcon = p.saved
        ? `<svg class="w-4 h-4 text-emerald-600 fill-emerald-600" viewBox="0 0 24 24"><path d="M5 5a2 2 0 012-2h10a2 2 0 012 2v16l-7-3.5L5 21V5z"></path></svg>`
        : `<svg class="w-4 h-4 text-gray-400 hover:text-gray-600" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 5a2 2 0 012-2h10a2 2 0 012 2v16l-7-3.5L5 21V5z"></path></svg>`;

      return `
        <tr class="border-b border-gray-100 hover:bg-[#fcfdfd] transition group cursor-pointer" onclick="CodeForgeApp.openProblem(${p.id})">
          <td class="py-3.5 px-4 text-center w-12" onclick="event.stopPropagation()">
            ${statusIcon}
          </td>
          <td class="py-3.5 px-3 text-xs text-gray-400 font-mono w-14">
            ${p.id}
          </td>
          <td class="py-3.5 px-4">
            <div class="flex items-center gap-2">
              <span class="text-sm font-medium text-gray-900 group-hover:text-emerald-700 transition">${p.title}</span>
              ${p.isMine ? `<span class="px-1.5 py-0.2 rounded text-[11px] font-medium bg-emerald-50 text-emerald-700 border border-emerald-200">Yours</span>` : ''}
            </div>
          </td>
          <td class="py-3.5 px-4 text-xs ${diffClass} w-24">
            ${p.difficulty}
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
            <button type="button" class="p-1 rounded hover:bg-gray-100 transition focus:outline-none">
              ${bookmarkIcon}
            </button>
          </td>
        </tr>
      `;
    }).join('');
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
    const list = CodeForgeData.problems;
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

    // Sidebar navigation clicks
    const sbDashboard = document.getElementById('sb-dashboard');
    if (sbDashboard) sbDashboard.addEventListener('click', () => navigate('dashboard'));

    const sbLibrary = document.getElementById('sb-library');
    if (sbLibrary) sbLibrary.addEventListener('click', () => navigate('library'));

    const sbMyProblems = document.getElementById('sb-my-problems');
    if (sbMyProblems) sbMyProblems.addEventListener('click', () => navigate('create-problem'));

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

    // Library Tabs (All 240, Saved 18, My problems 3)
    const tabAll = document.getElementById('tab-filter-all');
    const tabSaved = document.getElementById('tab-filter-saved');
    const tabMy = document.getElementById('tab-filter-my');

    const setLibraryTab = (tab) => {
      activeTabFilter = tab;
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
        renderProblemTable();
      });
    }

    // Difficulty Filter Dropdown
    const diffSelect = document.getElementById('filter-difficulty-select');
    if (diffSelect) {
      diffSelect.addEventListener('change', (e) => {
        filterDifficulty = e.target.value;
        renderProblemTable();
      });
    }

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

    // Sign out button
    const btnLogout = document.getElementById('btn-auth-logout');
    if (btnLogout) {
      btnLogout.addEventListener('click', handleLogout);
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
    pickRandomProblem,
    showToast,
    handleLoginSubmit,
    handleSignupSubmit,
    handleLogout
  };
})();

// Boot on DOM ready
document.addEventListener('DOMContentLoaded', CodeForgeApp.init);

