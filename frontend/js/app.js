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

  function initTheme() {
    const savedTheme = localStorage.getItem('codeforge_theme') || 'light';
    applyTheme(savedTheme === 'dark');
  }

  function applyTheme(isDark) {
    document.documentElement.classList.toggle('dark-theme', isDark);
    document.body.classList.toggle('dark-theme', isDark);
    const moonIcon = document.getElementById('icon-theme-moon');
    const sunIcon = document.getElementById('icon-theme-sun');
    if (moonIcon) moonIcon.classList.toggle('hidden', isDark);
    if (sunIcon) sunIcon.classList.toggle('hidden', !isDark);
  }

  function toggleTheme() {
    const isCurrentlyDark = document.body.classList.contains('dark-theme');
    const nextDark = !isCurrentlyDark;
    localStorage.setItem('codeforge_theme', nextDark ? 'dark' : 'light');
    applyTheme(nextDark);
    showToast(nextDark ? 'Dark mode enabled' : 'Light mode enabled', 'info');
  }

  function init() {
    // Re-check login state on init (in case storage changed)
    isLoggedIn = !!getToken();
    initTheme();

    setupHashRouting();
    bindGlobalEvents();
    renderProblemTable();
    if (window.CodeForgeAPI) {
      CodeForgeAPI.getProblems({ tab: 'all' }).then(apiProblems => {
        if (Array.isArray(apiProblems) && apiProblems.length > 0) {
          CodeForgeData.problems = apiProblems.map(p => ({
            ...p,
            isMine: !!p.isMine,
            starterCode: p.starterCode || { python: '# Write your solution here\n' }
          }));
          updateLibraryStats();
          renderProblemTable();
        }
      });
    }
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
    const navAdmin = document.getElementById('nav-admin');
    const dropdownAdmin = document.getElementById('dropdown-admin-link');
    const sbAdminSection = document.getElementById('sb-admin-section');

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

          // Role check for admin elements
          const isAdmin = (user.role && user.role.toLowerCase() === 'admin') || (user.roles && user.roles.includes('ADMIN'));
          if (isAdmin) {
            if (navAdmin) navAdmin.classList.remove('hidden');
            if (dropdownAdmin) dropdownAdmin.classList.remove('hidden');
            if (sbAdminSection) sbAdminSection.classList.remove('hidden');
            if (window.CodeForgeAdmin && CodeForgeAdmin.loadStats) {
              CodeForgeAdmin.loadStats();
            }
          } else {
            if (navAdmin) navAdmin.classList.add('hidden');
            if (dropdownAdmin) dropdownAdmin.classList.add('hidden');
            if (sbAdminSection) sbAdminSection.classList.add('hidden');
          }
        }
      } else {
        userSection.classList.add('hidden');
        guestSection.classList.remove('hidden');
        if (navAdmin) navAdmin.classList.add('hidden');
        if (dropdownAdmin) dropdownAdmin.classList.add('hidden');
        if (sbAdminSection) sbAdminSection.classList.add('hidden');
      }
    }
  }

  // Update dashboard greeting and stats with the logged-in user's data and backend overview
  async function updateDashboardForUser() {
    const user = getStoredUser();

    // Update the date line
    const dateEl = document.getElementById('dashboard-date');
    if (dateEl) {
      const now = new Date();
      const options = { weekday: 'long', month: 'long', day: 'numeric' };
      const formatted = now.toLocaleDateString('en-US', options);
      dateEl.textContent = `Your overview / ${formatted}`;
    }

    if (user) {
      const greeting = buildGreeting(user.fullName);
      const greetingEl = document.getElementById('dashboard-greeting');
      if (greetingEl) {
        greetingEl.textContent = `Keep the momentum, ${greeting}.`;
      }
    }

    // Call CodeForgeAPI.getOverview() to fetch real telemetry from backend
    if (window.CodeForgeAPI) {
      try {
        const overview = await CodeForgeAPI.getOverview();
        if (overview) {
          applyOverviewData(overview);
        }
      } catch (e) {
        console.warn("Could not load overview metrics from API:", e);
      }
    }
  }

  function applyOverviewData(overview) {
    if (!overview) return;

    // 1. Dashboard Greeting & User Stats Cards
    if (overview.user) {
      const u = overview.user;
      const greetingEl = document.getElementById('dashboard-greeting');
      if (greetingEl) {
        const name = u.greetingName || (u.name ? u.name.split(' ')[0] : 'there');
        greetingEl.textContent = `Keep the momentum, ${name}.`;
      }

      // Card 1: Problems Solved & Delta
      const solvedEl = document.getElementById('dashboard-solved-count');
      if (solvedEl && u.solvedCount !== undefined && u.solvedCount !== null) {
        solvedEl.textContent = u.solvedCount;
      }
      const solvedDeltaEl = document.getElementById('dashboard-solved-delta');
      if (solvedDeltaEl) {
        const weekSolved = u.solvedWeek !== undefined && u.solvedWeek !== null ? u.solvedWeek : 0;
        solvedDeltaEl.textContent = `+${weekSolved} this week`;
      }

      // Card 2: Current Streak & Personal Best
      const streakEl = document.getElementById('dashboard-streak');
      if (streakEl && u.streakDays !== undefined && u.streakDays !== null) {
        streakEl.textContent = `${u.streakDays} ${u.streakDays === 1 ? 'day' : 'days'}`;
      }
      const streakBestEl = document.getElementById('dashboard-streak-best');
      if (streakBestEl) {
        const streak = u.streakDays || 0;
        streakBestEl.textContent = `Personal best: ${streak}`;
      }
      const headerStreakEl = document.getElementById('header-streak-text');
      if (headerStreakEl && u.streakDays !== undefined && u.streakDays !== null) {
        headerStreakEl.textContent = `${u.streakDays}-day streak`;
      }

      // Card 3: Acceptance Rate & Delta
      const acceptanceEl = document.getElementById('dashboard-acceptance');
      if (acceptanceEl && u.acceptanceRate !== undefined && u.acceptanceRate !== null) {
        acceptanceEl.textContent = u.acceptanceRate;
      }
      const acceptanceDeltaEl = document.getElementById('dashboard-acceptance-delta');
      if (acceptanceDeltaEl) {
        const delta = u.acceptanceDelta || '+0%';
        acceptanceDeltaEl.textContent = delta;
      }

      // Card 4: Practice Time & Delta
      const practiceEl = document.getElementById('dashboard-practice-time');
      if (practiceEl && u.practiceTime !== undefined && u.practiceTime !== null) {
        practiceEl.textContent = u.practiceTime;
      }
      const practiceDeltaEl = document.getElementById('dashboard-practice-delta');
      if (practiceDeltaEl) {
        const pDelta = u.practiceDelta || '+0h';
        practiceDeltaEl.textContent = `${pDelta} this week`;
      }
    }

    // 2. Your Next Challenge Card
    if (overview.nextChallenge) {
      const nc = overview.nextChallenge;
      setText('dashboard-next-title', nc.title || 'Coding Challenge');
      setText('dashboard-next-meta', nc.meta || nc.stepInfo || 'Arrays & hashing');
      setText('dashboard-next-desc', nc.description || 'Solve this recommended problem to practice your problem-solving intuition.');
      setText('dashboard-next-last-opened', nc.lastOpened || 'Not started yet');

      // Next Challenge Status Badge
      const statusBadge = document.getElementById('dashboard-next-status');
      if (statusBadge) {
        const statusText = nc.status || 'Start';
        statusBadge.textContent = statusText;
        statusBadge.className = 'px-2 py-0.5 rounded text-xs font-medium border';
        if (statusText === 'Solved') {
          statusBadge.classList.add('bg-emerald-50', 'text-emerald-700', 'border-emerald-200');
        } else if (statusText === 'Continue') {
          statusBadge.classList.add('bg-amber-50', 'text-amber-700', 'border-amber-200');
        } else {
          statusBadge.classList.add('bg-gray-100', 'text-gray-700', 'border-gray-200');
        }
      }

      // Next Challenge Difficulty Badge
      const diffBadge = document.getElementById('dashboard-next-diff');
      if (diffBadge) {
        const diff = nc.difficulty || 'Easy';
        diffBadge.textContent = diff;
        diffBadge.className = 'px-2 py-0.5 rounded text-xs font-medium border ' +
          (diff === 'Easy' ? 'badge-easy' : (diff === 'Medium' ? 'badge-medium' : 'badge-hard'));
      }

      // Next Challenge Button
      const solveBtn = document.getElementById('btn-continue-solving');
      if (solveBtn) {
        const actionText = nc.status === 'Solved'
          ? '&rarr; Solve again'
          : (nc.status === 'Continue' ? '&rarr; Continue solving' : '&rarr; Start challenge');
        solveBtn.innerHTML = `<span>${actionText}</span>`;
        solveBtn.onclick = () => {
          if (nc.id) {
            CodeForgeApp.openProblem(nc.id);
          } else {
            CodeForgeApp.navigate('library');
          }
        };
      }
    }

    // 3. Your Weekly Goal
    if (overview.weeklyGoal) {
      const wg = overview.weeklyGoal;
      setText('dashboard-weekly-done', String(wg.done ?? 0));
      setText('dashboard-weekly-target', String(wg.target ?? 5));

      const barEl = document.getElementById('dashboard-weekly-bar');
      if (barEl) {
        const pct = Math.min(100, Math.max(0, wg.pct ?? 0));
        barEl.style.width = `${pct}%`;
      }

      const captionEl = document.getElementById('dashboard-weekly-caption');
      if (captionEl) {
        const done = wg.done ?? 0;
        const target = wg.target ?? 5;
        if (done >= target) {
          captionEl.textContent = "Weekly goal achieved! Outstanding work.";
        } else if (done > 0) {
          const remaining = target - done;
          captionEl.textContent = `Just ${remaining} more to reach your goal. You've got this.`;
        } else {
          captionEl.textContent = "Solve problems this week to build momentum towards your goal.";
        }
      }

      const daysEl = document.getElementById('dashboard-weekly-days');
      if (daysEl && Array.isArray(wg.days)) {
        daysEl.innerHTML = wg.days.map(d => {
          const letter = d.day ? d.day.substring(0, 1) : 'D';
          if (d.done) {
            return `
              <div class="space-y-1">
                <div class="w-6 h-6 rounded-full bg-emerald-50 text-emerald-700 flex items-center justify-center text-xs border border-emerald-200 mx-auto">
                  &check;
                </div>
                <span class="text-[10px] text-gray-500 font-medium">${escapeHtml(letter)}</span>
              </div>
            `;
          } else {
            return `
              <div class="space-y-1">
                <div class="w-6 h-6 rounded-full bg-gray-50 text-gray-300 flex items-center justify-center text-xs border border-gray-200 mx-auto">
                  &bull;
                </div>
                <span class="text-[10px] text-gray-400">${escapeHtml(letter)}</span>
              </div>
            `;
          }
        }).join('');
      }
    }

    // 4. Learning Paths (Dynamic Cards & Empty State)
    if (overview.learningPaths && Array.isArray(overview.learningPaths)) {
      renderDashboardLearningPaths(overview.learningPaths);
    }

    // 5. Recent Activity List
    if (overview.recentActivity) {
      renderRecentActivity(overview.recentActivity);
    }
  }

  function renderDashboardLearningPaths(paths) {
    const container = document.getElementById('dashboard-learning-paths');
    if (!container) return;

    const hasAnyProgress = paths.some(p => p.completed && p.completed > 0);

    if (!hasAnyProgress) {
      container.innerHTML = `
        <div class="col-span-1 md:col-span-3 bg-white p-8 rounded-xl border border-[#e5eae7] shadow-sm text-center flex flex-col items-center justify-center gap-3">
          <div class="w-12 h-12 rounded-full bg-emerald-50 text-emerald-700 flex items-center justify-center font-mono text-xl border border-emerald-200">
            🎯
          </div>
          <div>
            <h4 class="text-sm font-bold text-gray-900">You haven't started any learning path yet.</h4>
            <p class="text-xs text-gray-500 mt-1">Structured learning paths guide you from fundamentals to advanced algorithmic techniques step-by-step.</p>
          </div>
          <button type="button" onclick="CodeForgeApp.navigate('learning-paths')" class="mt-2 inline-flex items-center gap-2 px-4 py-2 rounded-lg bg-emerald-600 hover:bg-emerald-700 text-white font-medium text-xs sm:text-sm transition shadow-sm">
            <span>Explore Learning paths &rarr;</span>
          </button>
        </div>
      `;
      return;
    }

    container.innerHTML = paths.map(lp => {
      const completed = lp.completed || 0;
      const total = lp.total || 0;
      const pct = lp.pct || 0;
      const icon = lp.icon || '🧩';

      return `
        <div class="bg-white p-4 rounded-xl border border-[#e5eae7] shadow-sm hover:border-emerald-300 transition cursor-pointer group" onclick="CodeForgeApp.openLearningPath('${escapeHtml(lp.id)}')">
          <div class="flex items-start justify-between">
            <div class="w-8 h-8 rounded-lg bg-emerald-50 text-emerald-700 flex items-center justify-center font-mono text-sm border border-emerald-200">
              ${escapeHtml(icon)}
            </div>
            <svg class="w-4 h-4 text-gray-400 group-hover:text-emerald-600 transition transform group-hover:translate-x-0.5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M10 6H6a2 2 0 00-2 2v10a2 2 0 002 2h10a2 2 0 002-2v-4M14 4h6m0 0v6m0-6L10 14"></path></svg>
          </div>
          <div class="mt-3">
            <h4 class="text-sm font-semibold text-gray-900 group-hover:text-emerald-800 transition">${escapeHtml(lp.title)}</h4>
            <p class="text-xs text-gray-500">${escapeHtml(lp.subtitle || 'Build your foundations')}</p>
          </div>
          <div class="mt-4 pt-3 border-t border-gray-100">
            <div class="w-full h-1.5 rounded-full bg-gray-100 overflow-hidden mb-1.5">
              <div class="h-full bg-emerald-600 rounded-full transition-all" style="width: ${pct}%"></div>
            </div>
            <div class="flex items-center justify-between text-[11px] text-gray-400">
              <span>${completed} of ${total} completed</span>
              <span class="font-medium text-gray-600">${pct}%</span>
            </div>
          </div>
        </div>
      `;
    }).join('');
  }

  function renderRecentActivity(activities) {
    const container = document.getElementById('dashboard-recent-activity');
    if (!container) return;

    if (!activities || activities.length === 0) {
      container.innerHTML = `<div class="py-4 text-center text-gray-400 text-xs">No recent submissions yet. Start solving problems to track your activity!</div>`;
      return;
    }

    container.innerHTML = activities.map(item => {
      const isAccepted = item.status === 'Accepted';
      const icon = isAccepted
        ? `<span class="w-5 h-5 rounded-full bg-emerald-50 text-emerald-700 flex items-center justify-center font-bold text-[10px]">&check;</span>`
        : `<span class="w-5 h-5 rounded-full bg-rose-50 text-rose-700 flex items-center justify-center font-bold text-[10px]">&times;</span>`;

      return `
        <div class="py-2.5 flex items-center justify-between hover:bg-gray-50 px-2 rounded transition cursor-pointer" onclick="CodeForgeApp.openProblem(${item.id})">
          <div class="flex items-center gap-3">
            ${icon}
            <span class="font-medium text-gray-900">${escapeHtml(item.title || 'Problem')}</span>
            <span class="text-gray-400 font-mono text-[11px]">#${item.id}</span>
          </div>
          <div class="flex items-center gap-4 text-gray-500">
            <span class="font-mono text-emerald-700">${escapeHtml(item.runtime || '—')}</span>
            <span class="font-mono">${escapeHtml(item.memory || '—')}</span>
            <span class="text-gray-400">${escapeHtml(item.date || '')}</span>
          </div>
        </div>
      `;
    }).join('');
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
    const pageLearningPaths = document.getElementById('page-learning-paths');
    const pageSubmissions = document.getElementById('page-submissions');
    const pageNotes = document.getElementById('page-notes');
    const pageCommunity = document.getElementById('page-community');
    const pageAdmin = document.getElementById('page-admin');
    const pageIde = document.getElementById('page-ide');
    const pageCreateProblem = document.getElementById('page-create-problem');
    const pageLogin = document.getElementById('page-login');
    const pageSignup = document.getElementById('page-signup');
    const sidebarContainer = document.getElementById('main-sidebar');
    const appLayoutContainer = document.getElementById('app-layout-container');

    // Hide all pages first
    if (pageDashboard) pageDashboard.classList.add('hidden');
    if (pageLibrary) pageLibrary.classList.add('hidden');
    if (pageLearningPaths) pageLearningPaths.classList.add('hidden');
    if (pageSubmissions) pageSubmissions.classList.add('hidden');
    if (pageNotes) pageNotes.classList.add('hidden');
    if (pageCommunity) pageCommunity.classList.add('hidden');
    if (pageAdmin) pageAdmin.classList.add('hidden');
    if (pageIde) pageIde.classList.add('hidden');
    if (pageCreateProblem) pageCreateProblem.classList.add('hidden');
    if (pageLogin) pageLogin.classList.add('hidden');
    if (pageSignup) pageSignup.classList.add('hidden');

    // Close any open dropdowns
    const dropdown = document.getElementById('dropdown-user-menu');
    if (dropdown) dropdown.classList.add('hidden');
    const notifDropdown = document.getElementById('dropdown-notifications');
    if (notifDropdown) notifDropdown.classList.add('hidden');

    // Top Navigation Links highlight
    const navOverview = document.getElementById('nav-overview');
    const navPractice = document.getElementById('nav-practice');
    const navNotes = document.getElementById('nav-notes');
    const navCommunity = document.getElementById('nav-community');
    const navAdmin = document.getElementById('nav-admin');

    const resetTopNav = () => {
      [navOverview, navPractice, navNotes, navCommunity, navAdmin].forEach(link => {
        if (!link) return;
        link.classList.remove('text-emerald-800', 'font-semibold', 'border-emerald-600');
        link.classList.add('text-gray-600', 'hover:text-gray-900', 'font-normal', 'border-transparent');
      });
    };

    const activateTopNav = (link) => {
      if (!link) return;
      link.classList.remove('text-gray-600', 'hover:text-gray-900', 'font-normal', 'border-transparent');
      link.classList.add('text-emerald-800', 'font-semibold', 'border-emerald-600');
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

    const showStandardSidebarLayout = () => {
      if (sidebarContainer) {
        sidebarContainer.classList.remove('hidden');
        sidebarContainer.classList.add('lg:flex');
      }
      if (appLayoutContainer) {
        appLayoutContainer.classList.remove('max-w-[1536px]');
        appLayoutContainer.classList.add('w-full', 'max-w-full');
      }
    };

    const showFullWidthWorkspaceLayout = () => {
      if (sidebarContainer) {
        sidebarContainer.classList.add('hidden');
        sidebarContainer.classList.remove('lg:flex');
      }
      if (appLayoutContainer) {
        appLayoutContainer.classList.remove('max-w-[1536px]');
        appLayoutContainer.classList.add('w-full', 'max-w-full');
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
      showStandardSidebarLayout();
      activateTopNav(navOverview);
      activateSidebarLink('sb-dashboard');
      document.body.classList.remove('focus-mode');
      updateDashboardForUser();
    } else if (route === 'library') {
      if (pageLibrary) pageLibrary.classList.remove('hidden');
      showStandardSidebarLayout();
      activateTopNav(navPractice);
      activateSidebarLink(activeTabFilter === 'my' ? 'sb-my-problems' : (activeTabFilter === 'saved' ? 'sb-saved' : 'sb-library'));
      document.body.classList.remove('focus-mode');
      renderProblemTable();
    } else if (route === 'learning-paths') {
      if (pageLearningPaths) pageLearningPaths.classList.remove('hidden');
      showStandardSidebarLayout();
      activateTopNav(navPractice);
      activateSidebarLink('sb-learning-paths');
      document.body.classList.remove('focus-mode');
      renderLearningPathsPage();
    } else if (route === 'submissions') {
      if (pageSubmissions) pageSubmissions.classList.remove('hidden');
      showStandardSidebarLayout();
      activateTopNav(navPractice);
      activateSidebarLink('sb-submissions');
      document.body.classList.remove('focus-mode');
      renderUserSubmissionsPage();
    } else if (route === 'notes') {
      if (pageNotes) pageNotes.classList.remove('hidden');
      showStandardSidebarLayout();
      activateTopNav(navNotes);
      activateSidebarLink('sb-notes');
      document.body.classList.remove('focus-mode');
      if (window.CodeForgeNotes) CodeForgeNotes.renderNotes();
    } else if (route === 'community') {
      if (pageCommunity) pageCommunity.classList.remove('hidden');
      showStandardSidebarLayout();
      activateTopNav(navCommunity);
      activateSidebarLink('sb-community');
      document.body.classList.remove('focus-mode');
      if (window.CodeForgeCommunity) CodeForgeCommunity.renderDiscussions();
    } else if (route === 'create-problem') {
      if (pageCreateProblem) pageCreateProblem.classList.remove('hidden');
      showStandardSidebarLayout();
      activateTopNav(navPractice);
      activateSidebarLink('sb-my-problems');
      document.body.classList.remove('focus-mode');
    } else if (route === 'ide' || route.startsWith('solve/')) {
      if (pageIde) pageIde.classList.remove('hidden');
      showFullWidthWorkspaceLayout();
      activateTopNav(navPractice);
      if (route.startsWith('solve/')) {
        const idPart = route.replace('solve/', '');
        const numId = parseInt(idPart, 10);
        if (numId) window.CodeForgeCurrentProblemId = numId;
      }
      if (window.CodeForgeIDE && window.CodeForgeIDE.loadProblem) {
        window.CodeForgeIDE.loadProblem(window.CodeForgeCurrentProblemId);
      }
    } else if (route === 'login') {
      if (pageLogin) pageLogin.classList.remove('hidden');
      showFullWidthWorkspaceLayout();
      document.body.classList.remove('focus-mode');
    } else if (route === 'signup') {
      if (pageSignup) pageSignup.classList.remove('hidden');
      showFullWidthWorkspaceLayout();
      document.body.classList.remove('focus-mode');
    } else if (route === 'admin') {
      const user = getStoredUser();
      const isAdmin = user && ((user.role && user.role.toLowerCase() === 'admin') || (user.roles && user.roles.includes('ADMIN')));
      if (!isAdmin) {
        showToast("Admin access required. Please sign in as an Administrator.", "warn");
        navigate('dashboard');
        return;
      }
      if (pageAdmin) pageAdmin.classList.remove('hidden');
      showStandardSidebarLayout();
      activateTopNav(navAdmin);
      activateSidebarLink('sb-admin');
      document.body.classList.remove('focus-mode');
      if (window.CodeForgeAdmin && CodeForgeAdmin.init) CodeForgeAdmin.init();
    } else {
      // Default to dashboard
      if (pageDashboard) pageDashboard.classList.remove('hidden');
      showStandardSidebarLayout();
      activateTopNav(navOverview);
      activateSidebarLink('sb-dashboard');
      updateDashboardForUser();
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
    const approved = all.filter(p => !p.moderationStatus || p.moderationStatus === 'APPROVED');
    const total = approved.length;
    const solved = approved.filter(p => p.status === 'solved').length;
    const pct = total ? Math.round((solved / total) * 100) : 0;

    ['Easy', 'Medium', 'Hard'].forEach(level => {
      const inLevel = approved.filter(p => p.difficulty === level);
      const done = inLevel.filter(p => p.status === 'solved').length;
      setText(`lib-count-${level.toLowerCase()}`, `${done}/${inLevel.length}`);
    });

    setText('lib-solved-title', solved === 0 ? '0 solved. Your first one is waiting.' : `${solved} solved. Keep going.`);
    setText('lib-solved-sub', `You've completed ${pct}% of the library.`);
    setText('library-total-copy', total);
    setText('tab-count-all', total);
    setText('tab-count-saved', all.filter(p => p.saved && (!p.moderationStatus || p.moderationStatus === 'APPROVED')).length);
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
      if (activeTabFilter === 'saved') {
        if (!p.saved) return false;
        if (p.moderationStatus && p.moderationStatus !== 'APPROVED') return false;
      } else if (activeTabFilter === 'my') {
        if (!p.isMine) return false;
      } else {
        // 'all' tab: exclude PENDING or REJECTED problems so All problems only shows APPROVED challenges
        if (p.moderationStatus && p.moderationStatus !== 'APPROVED') return false;
      }
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
              ${p.moderationStatus === 'PENDING' ? `<span class="px-2 py-0.5 rounded text-[10px] font-semibold bg-amber-50 text-amber-700 border border-amber-200">Pending Review</span>` : ''}
              ${p.moderationStatus === 'REJECTED' ? `<span class="px-2 py-0.5 rounded text-[10px] font-semibold bg-red-50 text-red-700 border border-red-200" title="${escapeHtml(p.rejectionReason || 'Rejected by moderator')}">Rejected</span>` : ''}
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
    window.CodeForgeCurrentProblemId = problemId;
    navigate(`ide`);
    if (window.CodeForgeIDE && window.CodeForgeIDE.loadProblem) {
      window.CodeForgeIDE.loadProblem(problemId);
    }
  }

  async function toggleBookmark(problemId) {
    const p = CodeForgeData.problems.find(item => item.id === problemId);
    if (!p) return;

    try {
      if (window.CodeForgeAPI) {
        const res = await CodeForgeAPI.toggleBookmark(problemId);
        if (res && typeof res.saved === 'boolean') {
          p.saved = res.saved;
        } else {
          p.saved = !p.saved;
        }
      } else {
        p.saved = !p.saved;
      }
      renderProblemTable();
      updateLibraryStats();
      showToast(p.saved ? `Saved "${p.title}" to your list` : `Removed "${p.title}" from saved`, "info");
    } catch (e) {
      if (e.message === 'UNAUTHORIZED') {
        showToast("Please sign in to bookmark problems", "warn");
      } else {
        p.saved = !p.saved;
        renderProblemTable();
        updateLibraryStats();
        showToast(p.saved ? `Saved "${p.title}" to your list` : `Removed "${p.title}" from saved`, "info");
      }
    }
  }

  function pickRandomProblem() {
    const approved = CodeForgeData.problems.filter(p => !p.moderationStatus || p.moderationStatus === 'APPROVED');
    const unsolved = approved.filter(p => p.status !== 'solved');
    const list = unsolved.length ? unsolved : approved;
    const random = list[Math.floor(Math.random() * list.length)];
    if (!random) return;
    showToast(`Picked random challenge: ${random.title}! Opening IDE...`, "success");
    setTimeout(() => {
      openProblem(random.id);
    }, 600);
  }

  let activeLearningTrack = 'arrays';

  const LEARNING_TRACKS = [
    {
      id: 'arrays',
      icon: '{ }',
      title: 'Arrays & Hashing Foundations',
      subtitle: 'Master O(1) hash lookups, prefix sums, and linear array traversal.',
      topics: ['Arrays', 'Hash map', 'Hash set', 'Prefix sum']
    },
    {
      id: 'strings',
      icon: '↔',
      title: 'Strings & Sorting Essentials',
      subtitle: 'Character frequency maps, anagram grouping, and custom sorting.',
      topics: ['Strings', 'Sorting']
    },
    {
      id: 'stacks',
      icon: '∼',
      title: 'Stacks, Heaps & Problem Solving',
      subtitle: 'LIFO bracket validation, priority queues, and top-K patterns.',
      topics: ['Stacks', 'Heap']
    },
    {
      id: 'sprint',
      icon: '⚡',
      title: 'Interview Sprint (All Curated Challenges)',
      subtitle: 'Complete roadmap ordered progressively from Easy to Hard.',
      topics: null // all approved problems ordered Easy -> Medium -> Hard
    }
  ];

  function getTrackProblems(track) {
    const approved = (CodeForgeData.problems || []).filter(p => !p.moderationStatus || p.moderationStatus === 'APPROVED');
    if (!track || !track.topics) {
      const rank = { Easy: 1, Medium: 2, Hard: 3 };
      return [...approved].sort((a, b) => (rank[a.difficulty] || 2) - (rank[b.difficulty] || 2));
    }
    const lowerTopics = track.topics.map(t => t.toLowerCase());
    return approved.filter(p => (p.topics || []).some(t => lowerTopics.includes(t.toLowerCase())));
  }

  function renderLearningPathsPage(trackId) {
    if (trackId) activeLearningTrack = trackId;
    const gridEl = document.getElementById('learning-paths-cards-grid');
    const titleEl = document.getElementById('learning-path-detail-title');
    const subEl = document.getElementById('learning-path-detail-sub');
    const badgeEl = document.getElementById('learning-path-detail-badge');
    const tbodyEl = document.getElementById('learning-path-problems-tbody');
    if (!gridEl || !tbodyEl) return;

    gridEl.innerHTML = LEARNING_TRACKS.map(track => {
      const probs = getTrackProblems(track);
      const total = probs.length;
      const solved = probs.filter(p => p.status === 'solved').length;
      const pct = total > 0 ? Math.round((solved * 100) / total) : 0;
      const isSelected = track.id === activeLearningTrack;
      const borderClass = isSelected
        ? 'border-emerald-600 ring-2 ring-emerald-500/20'
        : 'border-[#e5eae7] hover:border-emerald-300';

      return `
        <div data-track-id="${track.id}" class="learning-track-card bg-white p-5 rounded-xl border ${borderClass} shadow-xs transition cursor-pointer group">
          <div class="flex items-start justify-between gap-3">
            <div class="flex items-center gap-3">
              <div class="w-10 h-10 rounded-xl bg-emerald-50 text-emerald-700 flex items-center justify-center font-mono text-base font-bold border border-emerald-200">
                ${escapeHtml(track.icon)}
              </div>
              <div>
                <h3 class="text-sm font-bold text-gray-900 group-hover:text-emerald-700 transition">${escapeHtml(track.title)}</h3>
                <p class="text-xs text-gray-500 mt-0.5">${escapeHtml(track.subtitle)}</p>
              </div>
            </div>
            ${isSelected ? `<span class="px-2 py-0.5 rounded text-[10px] font-bold bg-emerald-50 text-emerald-700 border border-emerald-200">Selected</span>` : ''}
          </div>
          <div class="mt-4 pt-3 border-t border-gray-100">
            <div class="w-full h-2 rounded-full bg-gray-100 overflow-hidden mb-1.5">
              <div class="h-full bg-emerald-600 rounded-full transition-all" style="width: ${pct}%"></div>
            </div>
            <div class="flex items-center justify-between text-[11px] text-gray-500">
              <span>${solved} / ${total} solved</span>
              <span class="font-semibold text-emerald-700">${pct}%</span>
            </div>
          </div>
        </div>
      `;
    }).join('');

    gridEl.querySelectorAll('.learning-track-card').forEach(card => {
      card.addEventListener('click', () => {
        const id = card.getAttribute('data-track-id');
        renderLearningPathsPage(id);
      });
    });

    const selectedTrack = LEARNING_TRACKS.find(t => t.id === activeLearningTrack) || LEARNING_TRACKS[0];
    const trackProbs = getTrackProblems(selectedTrack);
    const solvedCount = trackProbs.filter(p => p.status === 'solved').length;

    if (titleEl) titleEl.textContent = selectedTrack.title;
    if (subEl) subEl.textContent = selectedTrack.subtitle;
    if (badgeEl) badgeEl.textContent = `${solvedCount} / ${trackProbs.length} Solved`;

    if (trackProbs.length === 0) {
      tbodyEl.innerHTML = `<tr><td colspan="6" class="py-10 text-center text-gray-400">No challenges in this track yet.</td></tr>`;
      return;
    }

    tbodyEl.innerHTML = trackProbs.map(p => {
      let statusIcon = `<span class="inline-block w-4 h-4 rounded-full border border-gray-300" title="Not started"></span>`;
      if (p.status === 'solved') {
        statusIcon = `<span class="inline-flex items-center justify-center w-5 h-5 text-emerald-600 bg-emerald-50 rounded-full border border-emerald-200" title="Solved"><svg class="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2.5" d="M5 13l4 4L19 7"></path></svg></span>`;
      } else if (p.status === 'in-progress') {
        statusIcon = `<span class="inline-flex items-center justify-center w-5 h-5 text-amber-600 bg-amber-50 rounded-full border border-amber-200" title="In progress"><span class="w-2 h-2 rounded-full bg-amber-500"></span></span>`;
      }

      const diffBadge = p.difficulty === 'Easy' ? 'badge-easy' : p.difficulty === 'Medium' ? 'badge-medium' : 'badge-hard';
      const topicsHtml = (p.topics || []).map(t => `<span class="px-2 py-0.5 rounded text-[11px] bg-gray-100 text-gray-700 border border-gray-200">${escapeHtml(t)}</span>`).join(' ');

      return `
        <tr class="hover:bg-gray-50/75 transition cursor-pointer" onclick="CodeForgeApp.openProblem(${p.id})">
          <td class="py-3.5 px-4 text-center">${statusIcon}</td>
          <td class="py-3.5 px-4 font-semibold text-gray-900">${escapeHtml(p.id + '. ' + p.title)}</td>
          <td class="py-3.5 px-4"><span class="inline-block px-2 py-0.5 rounded text-xs font-medium ${diffBadge}">${escapeHtml(p.difficulty)}</span></td>
          <td class="py-3.5 px-4"><div class="flex flex-wrap gap-1.5">${topicsHtml}</div></td>
          <td class="py-3.5 px-4 font-mono text-gray-600">${escapeHtml(p.acceptance || '—')}</td>
          <td class="py-3.5 px-4 text-right">
            <span class="inline-flex items-center gap-1 px-3 py-1 rounded-lg bg-emerald-50 text-emerald-700 border border-emerald-200 font-semibold text-xs hover:bg-emerald-100 transition">Solve &rarr;</span>
          </td>
        </tr>
      `;
    }).join('');
  }

  function openLearningPath(trackOrTopic) {
    const normalized = String(trackOrTopic || 'arrays').toLowerCase();
    if (normalized.includes('string')) activeLearningTrack = 'strings';
    else if (normalized.includes('stack') || normalized.includes('heap')) activeLearningTrack = 'stacks';
    else if (normalized.includes('sprint') || normalized === 'all') activeLearningTrack = 'sprint';
    else activeLearningTrack = 'arrays';

    if (currentRoute === 'learning-paths') {
      renderLearningPathsPage(activeLearningTrack);
    } else {
      navigate('learning-paths');
    }
  }

  async function renderUserSubmissionsPage() {
    const tbody = document.getElementById('user-submissions-table-body');
    const totalEl = document.getElementById('sub-page-total');
    const acceptedEl = document.getElementById('sub-page-accepted');
    const rateEl = document.getElementById('sub-page-rate');
    if (!tbody) return;

    tbody.innerHTML = `<tr><td colspan="7" class="py-10 text-center text-gray-400">Loading your submissions...</td></tr>`;

    let subs = [];
    if (window.CodeForgeAPI && CodeForgeAPI.getUserSubmissions) {
      try {
        subs = await CodeForgeAPI.getUserSubmissions();
      } catch (e) {
        console.warn("Could not load user submissions:", e);
      }
    }

    const total = subs.length;
    const accepted = subs.filter(s => s.status === 'Accepted').length;
    const rate = total > 0 ? ((accepted * 100) / total).toFixed(1) + '%' : '0.0%';

    if (totalEl) totalEl.textContent = String(total);
    if (acceptedEl) acceptedEl.textContent = String(accepted);
    if (rateEl) rateEl.textContent = rate;

    if (total === 0) {
      tbody.innerHTML = `
        <tr>
          <td colspan="7" class="py-12 text-center text-gray-500">
            No submissions recorded yet. Solve a challenge in the <a href="#library" class="text-emerald-700 font-semibold hover:underline">Problem library</a>!
          </td>
        </tr>
      `;
      return;
    }

    tbody.innerHTML = subs.map(s => {
      const isAcc = s.status === 'Accepted';
      const badge = isAcc
        ? `<span class="px-2 py-0.5 rounded text-[11px] font-semibold bg-emerald-50 text-emerald-700 border border-emerald-200">Accepted</span>`
        : `<span class="px-2 py-0.5 rounded text-[11px] font-semibold bg-rose-50 text-rose-700 border border-rose-200">${escapeHtml(s.status || 'Wrong Answer')}</span>`;
      const exId = s.exerciseId || 104;
      const exTitle = s.exerciseTitle || `Problem #${exId}`;

      return `
        <tr class="hover:bg-gray-50/75 transition cursor-pointer" onclick="CodeForgeApp.openProblem(${exId})">
          <td class="py-3.5 px-4">${badge}</td>
          <td class="py-3.5 px-4 font-semibold text-gray-900">${escapeHtml(exId + '. ' + exTitle)}</td>
          <td class="py-3.5 px-4 font-mono text-gray-600">${escapeHtml(s.language || 'python')}</td>
          <td class="py-3.5 px-4 font-mono text-emerald-700 font-semibold">${escapeHtml(s.runtime || '—')}</td>
          <td class="py-3.5 px-4 font-mono text-gray-600">${escapeHtml(s.memory || '—')}</td>
          <td class="py-3.5 px-4 text-gray-400">${escapeHtml(s.date || 'Recently')}</td>
          <td class="py-3.5 px-4 text-right">
            <span class="text-emerald-700 font-semibold hover:underline">Open in IDE &rarr;</span>
          </td>
        </tr>
      `;
    }).join('');
  }

  function renderNotificationsList() {
    const listEl = document.getElementById('notifications-list');
    if (!listEl) return;

    const myProbs = (CodeForgeData.problems || []).filter(p => p.isMine);
    const items = [];

    myProbs.forEach(p => {
      if (p.moderationStatus === 'PENDING') {
        items.push({
          title: `Problem "${p.title}" is Pending Review`,
          sub: 'Your challenge is awaiting administrator verification.',
          badge: 'Pending',
          color: 'amber',
          action: () => {
            navigate('library');
            setTimeout(() => {
              const tabMy = document.getElementById('tab-filter-my');
              if (tabMy) tabMy.click();
            }, 60);
          }
        });
      } else if (p.moderationStatus === 'REJECTED') {
        items.push({
          title: `Problem "${p.title}" was Rejected`,
          sub: p.rejectionReason ? `Reason: ${p.rejectionReason}` : 'Check My problems for moderator feedback.',
          badge: 'Rejected',
          color: 'rose',
          action: () => {
            navigate('library');
            setTimeout(() => {
              const tabMy = document.getElementById('tab-filter-my');
              if (tabMy) tabMy.click();
            }, 60);
          }
        });
      } else {
        items.push({
          title: `Problem "${p.title}" is Published`,
          sub: 'Your challenge is live in the public CodeForge library.',
          badge: 'Approved',
          color: 'emerald',
          action: () => openProblem(p.id)
        });
      }
    });

    items.push({
      title: 'Keep your coding streak alive!',
      sub: 'Solve 1 challenge today to maintain your daily momentum.',
      badge: 'Streak',
      color: 'emerald',
      action: () => navigate('library')
    });

    listEl.innerHTML = items.map((item, idx) => {
      const badgeCls = item.color === 'amber'
        ? 'bg-amber-50 text-amber-700 border-amber-200'
        : item.color === 'rose'
          ? 'bg-rose-50 text-rose-700 border-rose-200'
          : 'bg-emerald-50 text-emerald-700 border-emerald-200';
      return `
        <div data-notif-idx="${idx}" class="notif-item px-3.5 py-2.5 hover:bg-gray-50 cursor-pointer transition space-y-1">
          <div class="flex items-center justify-between gap-2">
            <span class="font-semibold text-gray-900 text-xs truncate">${escapeHtml(item.title)}</span>
            <span class="px-1.5 py-0.2 rounded text-[10px] font-semibold border ${badgeCls}">${escapeHtml(item.badge)}</span>
          </div>
          <p class="text-[11px] text-gray-500 leading-snug">${escapeHtml(item.sub)}</p>
        </div>
      `;
    }).join('');

    listEl.querySelectorAll('.notif-item').forEach(el => {
      el.addEventListener('click', () => {
        const i = parseInt(el.getAttribute('data-notif-idx'), 10);
        const dropdown = document.getElementById('dropdown-notifications');
        if (dropdown) dropdown.classList.add('hidden');
        if (items[i] && items[i].action) items[i].action();
      });
    });
  }

  function bindGlobalEvents() {
    // Theme toggle button
    const btnTheme = document.getElementById('btn-theme-toggle');
    if (btnTheme) {
      btnTheme.addEventListener('click', toggleTheme);
    }

    // Notification Bell & Dropdown
    const btnBell = document.getElementById('btn-notification-bell');
    const notifDropdown = document.getElementById('dropdown-notifications');
    const btnMarkRead = document.getElementById('btn-mark-notifications-read');
    const unreadDot = document.getElementById('notification-unread-dot');

    if (btnBell && notifDropdown) {
      btnBell.addEventListener('click', (e) => {
        e.stopPropagation();
        const isHidden = notifDropdown.classList.contains('hidden');
        if (isHidden) {
          renderNotificationsList();
          notifDropdown.classList.remove('hidden');
        } else {
          notifDropdown.classList.add('hidden');
        }
      });
      document.addEventListener('click', (e) => {
        if (!btnBell.contains(e.target) && !notifDropdown.contains(e.target)) {
          notifDropdown.classList.add('hidden');
        }
      });
    }

    if (btnMarkRead) {
      btnMarkRead.addEventListener('click', (e) => {
        e.stopPropagation();
        if (unreadDot) unreadDot.classList.add('hidden');
        if (notifDropdown) notifDropdown.classList.add('hidden');
        showToast('All notifications marked as read', 'info');
      });
    }

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
    if (sbLibrary) {
      sbLibrary.addEventListener('click', () => {
        navigate('library');
        setTimeout(() => {
          const tabAll = document.getElementById('tab-filter-all');
          if (tabAll) tabAll.click();
        }, 60);
      });
    }

    const sbLearningPaths = document.getElementById('sb-learning-paths');
    if (sbLearningPaths) {
      sbLearningPaths.addEventListener('click', () => navigate('learning-paths'));
    }

    const sbSubmissions = document.getElementById('sb-submissions');
    if (sbSubmissions) {
      sbSubmissions.addEventListener('click', () => navigate('submissions'));
    }

    const openMyProblemsTab = () => {
      navigate('library');
      setTimeout(() => {
        const tabMy = document.getElementById('tab-filter-my');
        if (tabMy) tabMy.click();
      }, 60);
    };

    const sbMyProblems = document.getElementById('sb-my-problems');
    if (sbMyProblems) sbMyProblems.addEventListener('click', openMyProblemsTab);

    const dropdownMyProblems = document.getElementById('dropdown-my-problems-link');
    if (dropdownMyProblems) dropdownMyProblems.addEventListener('click', openMyProblemsTab);

    const sbNotes = document.getElementById('sb-notes');
    if (sbNotes) sbNotes.addEventListener('click', () => navigate('notes'));

    const sbCommunity = document.getElementById('sb-community');
    if (sbCommunity) sbCommunity.addEventListener('click', () => navigate('community'));

    const sbAdmin = document.getElementById('sb-admin');
    if (sbAdmin) sbAdmin.addEventListener('click', () => navigate('admin'));

    const dropdownAdmin = document.getElementById('dropdown-admin-link');
    if (dropdownAdmin) dropdownAdmin.addEventListener('click', () => navigate('admin'));

    const sbSaved = document.getElementById('sb-saved');
    if (sbSaved) {
      sbSaved.addEventListener('click', () => {
        navigate('library');
        setTimeout(() => {
          const tabSaved = document.getElementById('tab-filter-saved');
          if (tabSaved) tabSaved.click();
        }, 60);
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

      if (window.CodeForgeAPI && CodeForgeAPI.getProblems) {
        CodeForgeAPI.getProblems({ tab }).then(apiProblems => {
          if (Array.isArray(apiProblems)) {
            apiProblems.forEach(ap => {
              const idx = CodeForgeData.problems.findIndex(p => p.id === ap.id);
              if (idx >= 0) {
                CodeForgeData.problems[idx] = {
                  ...CodeForgeData.problems[idx],
                  ...ap,
                  isMine: ap.isMine !== undefined ? !!ap.isMine : !!CodeForgeData.problems[idx].isMine
                };
              } else {
                CodeForgeData.problems.unshift({
                  ...ap,
                  isMine: !!ap.isMine
                });
              }
            });
            updateLibraryStats();
            renderProblemTable();
          }
        }).catch(() => {});
      }
      updateLibraryStats();
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

    // Prevent duplicate identical toast messages from stacking
    const isDuplicate = Array.from(container.querySelectorAll('span')).some(s => s.textContent === message);
    if (isDuplicate) return;

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
    openLearningPath,
    renderProblemTable,
    toggleTheme,
    toggleBookmark,
    clearLibraryFilters,
    pickRandomProblem,
    showToast,
    handleLoginSubmit,
    handleSignupSubmit,
    handleLogout,
    updateAuthUI,
    getUser: getStoredUser
  };
})();

// Boot on DOM ready
document.addEventListener('DOMContentLoaded', CodeForgeApp.init);

