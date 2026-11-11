/**
 * @file app.js
 * @description CodeForge Main Client Application, Single Page Router & Global State Manager.
 * @module CodeForgeApp
 *
 * <p>Architectural Role:</p>
 * Serves as the central frontend orchestration hub for the CodeForge SPA.
 * Responsibilities include:
 * <ul>
 *   <li>Client-side hash routing (`#dashboard`, `#library`, `#ide`, `#create-problem`, etc.).</li>
 *   <li>Session state management and token synchronization via Web Storage.</li>
 *   <li>Global header and sidebar user profile UI updates (initials, streak, full name).</li>
 *   <li>Problem catalog table rendering, dynamic text search, category filtering, and bookmark toggling.</li>
 *   <li>Cross-module coordination with {@link CodeForgeIDE} and {@link CodeForgeEditor}.</li>
 * </ul>
 */

window.CodeForgeApp = (function () {
  'use strict';

  /** Current active SPA route view */
  let currentRoute = 'dashboard'; // 'dashboard' | 'library' | 'ide' | 'create-problem' | 'login' | 'signup'

  /** Active library tab filter ('all' | 'saved' | 'my') */
  let activeTabFilter = 'all';

  /** Active library difficulty filter ('all' | 'Easy' | 'Medium' | 'Hard') */
  let filterDifficulty = 'all';

  /** Active text search query matching problem title, ID, or topics */
  let searchQuery = '';

  /** Web Storage key under which the JWT token is stored */
  const TOKEN_KEY = 'codeforge_token';

  /** Web Storage key under which serialized user profile JSON is stored */
  const USER_KEY  = 'codeforge_user';

  /**
   * Retrieves and deserializes the authenticated user object from Web Storage.
   *
   * @returns {Object|null} The cached user profile object, or null if unauthenticated.
   */
  function getStoredUser() {
    try {
      const raw = localStorage.getItem(USER_KEY) || sessionStorage.getItem(USER_KEY);
      return raw ? JSON.parse(raw) : null;
    } catch (_) { return null; }
  }

  /**
   * Reads the active JWT Bearer token from localStorage or sessionStorage.
   *
   * @returns {string|null} The raw JWT string, or null if absent.
   */
  function getToken() {
    try {
      return localStorage.getItem(TOKEN_KEY) || sessionStorage.getItem(TOKEN_KEY) || null;
    } catch (_) { return null; }
  }

  /** Authentication flag reflecting whether a valid token exists in storage */
  let isLoggedIn = !!getToken();

  /**
   * Derives uppercase two-letter initials from a user's full name.
   *
   * @param {string} fullName - Full name string (e.g. "Alex Morgan").
   * @returns {string} Initials string (e.g. "AM") or "?" if empty.
   */
  function buildInitials(fullName) {
    if (!fullName) return '?';
    const parts = fullName.trim().split(/\s+/);
    if (parts.length === 1) return parts[0][0].toUpperCase();
    return (parts[0][0] + parts[parts.length - 1][0]).toUpperCase();
  }

  /**
   * Extracts the first name for friendly personalized greetings.
   *
   * @param {string} fullName - Full name string (e.g. "Alex Morgan").
   * @returns {string} First name string (e.g. "Alex").
   */
  function buildGreeting(fullName) {
    if (!fullName) return 'there';
    return fullName.trim().split(/\s+/)[0];
  }

  /**
   * Main application bootstrapper.
   * Initializes routing, event listeners, user state, and child controllers.
   */
  function init() {
    // Re-verify login status from current Web Storage state
    isLoggedIn = !!getToken();

    setupHashRouting();
    bindGlobalEvents();
    renderProblemTable();
    updateAuthUI();
    updateDashboardForUser();
    handleRoute(window.location.hash.replace('#', '') || 'dashboard');
    
    // Initialize embedded submodules
    if (window.CodeForgeIDE) CodeForgeIDE.init();
    if (window.CodeForgeEditor) CodeForgeEditor.init();
  }

  /**
   * Sets up URL hash change listener for single-page client routing.
   */
  function setupHashRouting() {
    window.addEventListener('hashchange', () => {
      const route = window.location.hash.replace('#', '') || 'dashboard';
      handleRoute(route);
    });
  }

  /**
   * Navigates programmatically to a target SPA route.
   *
   * @param {string} routeName - Target route name (e.g. 'dashboard', 'library', 'ide').
   */
  function navigate(routeName) {
    window.location.hash = `#${routeName}`;
  }

  /**
   * Synchronizes header user profile elements with the current authentication state.
   */
  function updateAuthUI() {
    const userSection = document.getElementById('header-user-section');
    const guestSection = document.getElementById('header-guest-section');

    if (userSection && guestSection) {
      if (isLoggedIn) {
        userSection.classList.remove('hidden');
        guestSection.classList.add('hidden');

        // Populate header with real user profile data
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

  /**
   * Populates personalized dashboard greeting text and current calendar date.
   */
  function updateDashboardForUser() {
    const user = getStoredUser();
    if (!user) return;

    const greeting = buildGreeting(user.fullName);

    // Update greeting banner headline
    const greetingEl = document.getElementById('dashboard-greeting');
    if (greetingEl) {
      greetingEl.textContent = `Keep the momentum, ${greeting}.`;
    }

    // Update localized date string
    const dateEl = document.getElementById('dashboard-date');
    if (dateEl) {
      const now = new Date();
      const options = { weekday: 'long', month: 'long', day: 'numeric' };
      const formatted = now.toLocaleDateString('en-US', options);
      dateEl.textContent = `Your overview / ${formatted}`;
    }
  }

  /**
   * Handles interactive login button simulation in embedded modal.
   */
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

  /**
   * Handles interactive signup button simulation in embedded modal.
   */
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

  /**
   * Signs the user out, purges session storage, and redirects to the login screen.
   */
  function handleLogout() {
    isLoggedIn = false;
    // Clear stored session tokens
    try {
      localStorage.removeItem(TOKEN_KEY);
      localStorage.removeItem(USER_KEY);
      sessionStorage.removeItem(TOKEN_KEY);
      sessionStorage.removeItem(USER_KEY);
    } catch (_) {}
    updateAuthUI();
    const dropdown = document.getElementById('dropdown-user-menu');
    if (dropdown) dropdown.classList.add('hidden');
    showToast("Signed out successfully.", "info");
    // Redirect to separate login page
    window.location.href = 'login.html';
  }

  /**
   * Renders the view corresponding to the active route and updates navigation highlights.
   *
   * @param {string} route - Route string extracted from hash fragment.
   */
  function handleRoute(route) {
    currentRoute = route;

    // View Containers
    const pageDashboard = document.getElementById('page-dashboard');
    const pageLibrary = document.getElementById('page-library');
    const pageIde = document.getElementById('page-ide');
    const pageCreateProblem = document.getElementById('page-create-problem');
    const pageLogin = document.getElementById('page-login');
    const pageSignup = document.getElementById('page-signup');
    const sidebarContainer = document.getElementById('main-sidebar');

    // Hide all view panels first
    if (pageDashboard) pageDashboard.classList.add('hidden');
    if (pageLibrary) pageLibrary.classList.add('hidden');
    if (pageIde) pageIde.classList.add('hidden');
    if (pageCreateProblem) pageCreateProblem.classList.add('hidden');
    if (pageLogin) pageLogin.classList.add('hidden');
    if (pageSignup) pageSignup.classList.add('hidden');

    // Close any open avatar dropdown menus
    const dropdown = document.getElementById('dropdown-user-menu');
    if (dropdown) dropdown.classList.add('hidden');

    // Top Navigation Links highlight management
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

    // Reset Sidebar Navigation Links highlight
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

    // Route Switching Logic
    if (route === 'dashboard') {
      // Enforce authentication gate: redirect to login if session missing
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
    } else if (route === 'create-problem') {
      if (pageCreateProblem) pageCreateProblem.classList.remove('hidden');
      if (sidebarContainer) sidebarContainer.classList.remove('hidden');
      activateTopNav(navPractice);
      activateSidebarLink('sb-my-problems');
      document.body.classList.remove('focus-mode');
    } else if (route === 'ide' || route.startsWith('solve/')) {
      if (pageIde) pageIde.classList.remove('hidden');
      if (sidebarContainer) sidebarContainer.classList.add('hidden'); // IDE adopts full-width layout
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
      // Fallback default: dashboard
      if (pageDashboard) pageDashboard.classList.remove('hidden');
      if (sidebarContainer) sidebarContainer.classList.remove('hidden');
      activateTopNav(navOverview);
      activateSidebarLink('sb-dashboard');
    }

    // Scroll smoothly to top upon view transition
    window.scrollTo({ top: 0, behavior: 'smooth' });
  }

  /**
   * Filters and renders the problem catalog table rows into the DOM.
   *
   * <p>Applies active search query, tab filtering (All, Saved, My problems),
   * and difficulty level filters to the problems array.</p>
   */
  function renderProblemTable() {
    const tableBody = document.getElementById('problems-table-body');
    if (!tableBody) return;

    // Filter problems list according to active criteria
    let filtered = CodeForgeData.problems.filter(p => {
      // Text search filter
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

    // Render empty state if no problems match
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

    // Generate table rows HTML
    tableBody.innerHTML = filtered.map(p => {
      // Determine status icon
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

      // Determine difficulty styling class
      const diffClass = p.difficulty === 'Easy' 
        ? 'text-emerald-700 font-medium' 
        : p.difficulty === 'Medium' 
        ? 'text-amber-700 font-medium' 
        : 'text-rose-700 font-medium';

      // Build topic badge pills
      const topicsHtml = (p.topics || []).map(t => `
        <span class="px-2 py-0.5 rounded text-xs bg-gray-100 text-gray-700 border border-gray-200">${t}</span>
      `).join(' ');

      // Build bookmark icon
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

  /**
   * Navigates to the IDE workspace to solve a selected challenge.
   *
   * @param {number} problemId - Identifier of the problem to open.
   */
  function openProblem(problemId) {
    navigate(`ide`);
  }

  /**
   * Toggles the saved/bookmarked state of a problem in the user's library.
   *
   * @param {number} problemId - Identifier of the problem.
   */
  function toggleBookmark(problemId) {
    const p = CodeForgeData.problems.find(item => item.id === problemId);
    if (p) {
      p.saved = !p.saved;
      renderProblemTable();
      showToast(p.saved ? `Saved "${p.title}" to your list` : `Removed "${p.title}" from saved`, "info");
    }
  }

  /**
   * Selects a random challenge from the library and navigates to the IDE.
   */
  function pickRandomProblem() {
    const list = CodeForgeData.problems;
    const random = list[Math.floor(Math.random() * list.length)];
    showToast(`Picked random challenge: ${random.title}! Opening IDE...`, "success");
    setTimeout(() => {
      openProblem(random.id);
    }, 600);
  }

  /**
   * Binds global application event listeners across navigation items, buttons, and menus.
   */
  function bindGlobalEvents() {
    // Top logo click -> dashboard
    const brandLogo = document.getElementById('brand-logo');
    if (brandLogo) {
      brandLogo.addEventListener('click', () => navigate('dashboard'));
    }

    // Top navigation link clicks
    const navOverview = document.getElementById('nav-overview');
    if (navOverview) navOverview.addEventListener('click', () => navigate('dashboard'));

    const navPractice = document.getElementById('nav-practice');
    if (navPractice) navPractice.addEventListener('click', () => navigate('library'));

    // Sidebar navigation link clicks
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

    // Dashboard quick action buttons
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

    // Problem table search input
    const searchInput = document.getElementById('library-search-input');
    if (searchInput) {
      searchInput.addEventListener('input', (e) => {
        searchQuery = e.target.value;
        renderProblemTable();
      });
    }

    // Difficulty filter dropdown
    const diffSelect = document.getElementById('filter-difficulty-select');
    if (diffSelect) {
      diffSelect.addEventListener('change', (e) => {
        filterDifficulty = e.target.value;
        renderProblemTable();
      });
    }

    // User Avatar dropdown toggle
    const btnUserAvatar = document.getElementById('btn-user-avatar');
    const dropdownMenu = document.getElementById('dropdown-user-menu');
    if (btnUserAvatar && dropdownMenu) {
      btnUserAvatar.addEventListener('click', (e) => {
        e.stopPropagation();
        dropdownMenu.classList.toggle('hidden');
      });

      // Close dropdown when clicking outside
      document.addEventListener('click', (e) => {
        if (!btnUserAvatar.contains(e.target) && !dropdownMenu.contains(e.target)) {
          dropdownMenu.classList.add('hidden');
        }
      });
    }

    // Sign out button (header dropdown)
    const btnLogout = document.getElementById('btn-auth-logout');
    if (btnLogout) {
      btnLogout.addEventListener('click', handleLogout);
    }

    // Sign out button (sidebar)
    const sbLogout = document.getElementById('sb-logout');
    if (sbLogout) {
      sbLogout.addEventListener('click', handleLogout);
    }

    // Social authentication button simulation
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

    // Submission success modal close button
    const closeModalBtn = document.getElementById('btn-close-modal');
    if (closeModalBtn) {
      closeModalBtn.addEventListener('click', () => {
        const modal = document.getElementById('submission-success-modal');
        if (modal) modal.classList.add('hidden');
      });
    }
  }

  /**
   * Displays a floating notification toast.
   *
   * @param {string} message - Notification message string.
   * @param {string} [type='info'] - Severity type ('info', 'success', 'warn').
   */
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

  // Expose public API interface
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

// Boot application upon DOM ready
document.addEventListener('DOMContentLoaded', CodeForgeApp.init);
