/**
 * CodeForge - Administration & Moderation Controller
 * Handles platform KPIs, moderation queue for problem submissions & community discussions,
 * and user role management.
 */
(function() {
  'use strict';

  let stats = null;
  let pendingProblems = [];
  let pendingDiscussions = [];
  let pendingReports = [];
  let usersList = [];
  let feedbacksList = [];
  let trackingSummary = null;
  let loginLogs = [];
  let pageVisits = [];
  let activeTab = 'moderation'; // 'moderation' | 'feedbacks' | 'users' | 'tracking'
  let currentRejectTarget = null; // { type: 'problem'|'discussion', id, title }
  let userSearchQuery = '';
  let feedbackSearchQuery = '';
  let feedbackStatusFilter = 'ALL';
  let loginSearchQuery = '';
  let loginMethodFilter = 'ALL';
  let visitSearchQuery = '';
  let visitRouteFilter = 'ALL';
  let eventsBound = false;
  let trackingPollInterval = null;

  function escapeHtml(str) {
    if (!str) return '';
    return String(str).replace(/[&<>"']/g, ch => (
      { '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[ch]
    ));
  }

  function formatDate(isoStr) {
    if (!isoStr) return 'Recently';
    try {
      const d = new Date(isoStr);
      if (isNaN(d.getTime())) return isoStr;
      return d.toLocaleDateString('en-US', { month: 'short', day: 'numeric', year: 'numeric' });
    } catch (_) {
      return isoStr;
    }
  }

  function formatDateTime(isoStr) {
    if (!isoStr) return '--';
    try {
      const d = new Date(isoStr);
      if (isNaN(d.getTime())) return isoStr;
      return d.toLocaleString('en-US', {
        month: 'short',
        day: 'numeric',
        year: 'numeric',
        hour: '2-digit',
        minute: '2-digit',
        second: '2-digit',
        hour12: false
      });
    } catch (_) {
      return isoStr;
    }
  }

  function parseUserAgent(ua) {
    if (!ua) return 'Unknown Device';
    let os = 'Unknown OS';
    if (/Windows NT 10.0|Windows NT 11.0/i.test(ua)) os = 'Windows';
    else if (/Windows NT/i.test(ua)) os = 'Windows';
    else if (/Macintosh|Mac OS X/i.test(ua)) os = 'macOS';
    else if (/iPhone|iPad|iPod/i.test(ua)) os = 'iOS';
    else if (/Android/i.test(ua)) os = 'Android';
    else if (/Linux/i.test(ua)) os = 'Linux';

    let browser = 'Browser';
    if (/Edg\//i.test(ua)) browser = 'Edge';
    else if (/Chrome\//i.test(ua) && !/Edg\//i.test(ua)) browser = 'Chrome';
    else if (/Safari\//i.test(ua) && !/Chrome\//i.test(ua)) browser = 'Safari';
    else if (/Firefox\//i.test(ua)) browser = 'Firefox';
    else if (/Opera|OPR\//i.test(ua)) browser = 'Opera';

    return `${browser} on ${os}`;
  }

  function getInitials(name) {
    if (!name) return 'U';
    const parts = name.trim().split(/\s+/);
    if (parts.length > 1) {
      return (parts[0][0] + parts[parts.length - 1][0]).toUpperCase();
    }
    return parts[0].substring(0, 2).toUpperCase();
  }

  async function init() {
    bindEvents();
    await loadStats();
    if (activeTab === 'moderation') {
      await loadModerationQueue();
    } else if (activeTab === 'feedbacks') {
      await loadFeedbacks();
    } else if (activeTab === 'users') {
      await loadUsers();
    } else if (activeTab === 'tracking') {
      await loadTrackingData();
    }
  }

  function bindEvents() {
    if (eventsBound) return;
    eventsBound = true;

    const tabMod = document.getElementById('tab-admin-mod');
    const tabFeedbacks = document.getElementById('tab-admin-feedbacks');
    const tabUsers = document.getElementById('tab-admin-users');
    const tabTracking = document.getElementById('tab-admin-tracking');
    if (tabMod) tabMod.addEventListener('click', () => switchTab('moderation'));
    if (tabFeedbacks) tabFeedbacks.addEventListener('click', () => switchTab('feedbacks'));
    if (tabUsers) tabUsers.addEventListener('click', () => switchTab('users'));
    if (tabTracking) tabTracking.addEventListener('click', () => switchTab('tracking'));

    const btnRefresh = document.getElementById('btn-admin-refresh');
    if (btnRefresh) {
      btnRefresh.addEventListener('click', async () => {
        btnRefresh.classList.add('opacity-50');
        await loadStats();
        if (activeTab === 'moderation') await loadModerationQueue();
        else if (activeTab === 'feedbacks') await loadFeedbacks();
        else if (activeTab === 'users') await loadUsers();
        else if (activeTab === 'tracking') await loadTrackingData();
        btnRefresh.classList.remove('opacity-50');
        if (window.CodeForgeApp) CodeForgeApp.showToast('Administration queue refreshed.', 'info');
      });
    }

    const searchInput = document.getElementById('admin-users-search-input');
    if (searchInput) {
      searchInput.addEventListener('input', (e) => {
        userSearchQuery = e.target.value.trim().toLowerCase();
        renderUsersTable();
      });
    }

    const feedbackSearch = document.getElementById('admin-feedback-search-input');
    if (feedbackSearch) {
      feedbackSearch.addEventListener('input', (e) => {
        feedbackSearchQuery = e.target.value.trim().toLowerCase();
        renderFeedbacksTable();
      });
    }

    const feedbackFilter = document.getElementById('admin-feedback-filter-status');
    if (feedbackFilter) {
      feedbackFilter.addEventListener('change', (e) => {
        feedbackStatusFilter = e.target.value;
        renderFeedbacksTable();
      });
    }

    // Tracking filters and search
    const loginSearch = document.getElementById('admin-login-search-input');
    if (loginSearch) {
      loginSearch.addEventListener('input', (e) => {
        loginSearchQuery = e.target.value.trim().toLowerCase();
        renderLoginLogsTable();
      });
    }

    const loginFilter = document.getElementById('admin-login-filter-method');
    if (loginFilter) {
      loginFilter.addEventListener('change', (e) => {
        loginMethodFilter = e.target.value;
        renderLoginLogsTable();
      });
    }

    const visitSearch = document.getElementById('admin-visit-search-input');
    if (visitSearch) {
      visitSearch.addEventListener('input', (e) => {
        visitSearchQuery = e.target.value.trim().toLowerCase();
        renderPageVisitsTable();
      });
    }

    const visitFilter = document.getElementById('admin-visit-filter-route');
    if (visitFilter) {
      visitFilter.addEventListener('change', (e) => {
        visitRouteFilter = e.target.value;
        renderPageVisitsTable();
      });
    }

    // Rejection modal events
    const btnCloseReject = document.getElementById('btn-close-reject-modal');
    const btnCancelReject = document.getElementById('btn-cancel-reject');
    const btnConfirmReject = document.getElementById('btn-confirm-reject');
    if (btnCloseReject) btnCloseReject.addEventListener('click', closeRejectModal);
    if (btnCancelReject) btnCancelReject.addEventListener('click', closeRejectModal);
    if (btnConfirmReject) btnConfirmReject.addEventListener('click', confirmReject);
  }

  function switchTab(tab) {
    activeTab = tab;
    const tabMod = document.getElementById('tab-admin-mod');
    const tabFeedbacks = document.getElementById('tab-admin-feedbacks');
    const tabUsers = document.getElementById('tab-admin-users');
    const tabTracking = document.getElementById('tab-admin-tracking');
    const viewMod = document.getElementById('admin-view-moderation');
    const viewFeedbacks = document.getElementById('admin-view-feedbacks');
    const viewUsers = document.getElementById('admin-view-users');
    const viewTracking = document.getElementById('admin-view-tracking');

    const activeClass = 'pb-3 text-sm font-semibold text-emerald-800 border-b-2 border-emerald-600 transition flex items-center gap-2';
    const inactiveClass = 'pb-3 text-sm text-gray-500 hover:text-gray-800 border-b-2 border-transparent transition flex items-center gap-2 font-medium';

    if (tabMod) tabMod.className = tab === 'moderation' ? activeClass : inactiveClass;
    if (tabFeedbacks) tabFeedbacks.className = tab === 'feedbacks' ? activeClass : inactiveClass;
    if (tabUsers) tabUsers.className = tab === 'users' ? activeClass : inactiveClass;
    if (tabTracking) tabTracking.className = tab === 'tracking' ? activeClass : inactiveClass;

    if (viewMod) viewMod.classList.toggle('hidden', tab !== 'moderation');
    if (viewFeedbacks) viewFeedbacks.classList.toggle('hidden', tab !== 'feedbacks');
    if (viewUsers) viewUsers.classList.toggle('hidden', tab !== 'users');
    if (viewTracking) viewTracking.classList.toggle('hidden', tab !== 'tracking');

    if (tab === 'moderation') {
      stopTrackingPolling();
      loadModerationQueue();
    } else if (tab === 'feedbacks') {
      stopTrackingPolling();
      loadFeedbacks();
    } else if (tab === 'users') {
      stopTrackingPolling();
      loadUsers();
    } else if (tab === 'tracking') {
      loadTrackingData();
      startTrackingPolling();
    }
  }

  function startTrackingPolling() {
    stopTrackingPolling();
    trackingPollInterval = setInterval(async () => {
      if (activeTab === 'tracking') {
        await loadTrackingData(true);
      } else {
        stopTrackingPolling();
      }
    }, 4000);
  }

  function stopTrackingPolling() {
    if (trackingPollInterval) {
      clearInterval(trackingPollInterval);
      trackingPollInterval = null;
    }
  }

  async function loadStats() {
    if (!window.CodeForgeAPI || !CodeForgeAPI.getAdminStats) return;
    try {
      stats = await CodeForgeAPI.getAdminStats();
      if (!stats) return;

      const kpiUsers = document.getElementById('admin-kpi-users');
      const kpiProblems = document.getElementById('admin-kpi-problems');
      const kpiPending = document.getElementById('admin-kpi-pending');
      const kpiPendingSub = document.getElementById('admin-kpi-pending-sub');
      const kpiSubmissions = document.getElementById('admin-kpi-submissions');
      const kpiFeedbacks = document.getElementById('admin-kpi-feedbacks');
      const tabModCount = document.getElementById('tab-admin-mod-count');
      const tabFeedbacksCount = document.getElementById('tab-admin-feedbacks-count');
      const tabUsersCount = document.getElementById('tab-admin-users-count');

      const totalPending = (stats.pendingProblemsCount || 0) + (stats.pendingDiscussionsCount || 0) + (stats.pendingReportsCount || 0);

      if (kpiUsers) kpiUsers.textContent = (stats.totalUsers || 0).toLocaleString();
      if (kpiProblems) kpiProblems.textContent = (stats.totalProblems || 0).toLocaleString();
      if (kpiPending) kpiPending.textContent = totalPending;
      if (kpiPendingSub) {
        kpiPendingSub.textContent = `${stats.pendingProblemsCount || 0} problems, ${stats.pendingDiscussionsCount || 0} posts, ${stats.pendingReportsCount || 0} reports`;
      }
      if (kpiSubmissions) kpiSubmissions.textContent = (stats.totalSubmissions || 0).toLocaleString();
      if (kpiFeedbacks) kpiFeedbacks.textContent = (stats.totalFeedbacks || 0).toLocaleString();
      if (tabModCount) tabModCount.textContent = totalPending;
      if (tabFeedbacksCount) tabFeedbacksCount.textContent = (stats.totalFeedbacks || 0).toLocaleString();
      if (tabUsersCount) tabUsersCount.textContent = (stats.totalUsers || 0).toLocaleString();

      if (window.CodeForgeAPI && CodeForgeAPI.getAdminTrackingSummary) {
        try {
          const ts = await CodeForgeAPI.getAdminTrackingSummary();
          const tabTrackingCount = document.getElementById('tab-admin-tracking-count');
          if (tabTrackingCount && ts) {
            tabTrackingCount.textContent = (ts.totalLogins || 0).toLocaleString();
          }
        } catch (_) {}
      }

      // Update Nav badges
      const navBadge = document.getElementById('nav-admin-badge');
      const sbBadge = document.getElementById('sb-admin-badge');
      if (navBadge) {
        navBadge.textContent = totalPending;
        if (totalPending > 0) navBadge.classList.remove('hidden');
        else navBadge.classList.add('hidden');
      }
      if (sbBadge) {
        sbBadge.textContent = totalPending;
        if (totalPending > 0) sbBadge.classList.remove('hidden');
        else sbBadge.classList.add('hidden');
      }
    } catch (err) {
      console.warn('Failed to load admin stats:', err);
    }
  }

  async function loadModerationQueue() {
    if (!window.CodeForgeAPI || !CodeForgeAPI.getModerationQueue) return;
    try {
      const data = await CodeForgeAPI.getModerationQueue();
      pendingProblems = (data && data.pendingProblems) ? data.pendingProblems : [];
      pendingDiscussions = (data && data.pendingDiscussions) ? data.pendingDiscussions : [];
      pendingReports = (data && data.pendingReports) ? data.pendingReports : [];
      renderModerationQueue();
    } catch (err) {
      console.warn('Failed to load moderation queue:', err);
    }
  }

  function renderModerationQueue() {
    const probList = document.getElementById('admin-pending-problems-list');
    const discList = document.getElementById('admin-pending-discussions-list');
    const reportsList = document.getElementById('admin-reported-items-list');
    const probBadge = document.getElementById('admin-pending-prob-badge');
    const discBadge = document.getElementById('admin-pending-disc-badge');
    const reportsBadge = document.getElementById('admin-pending-reports-badge');

    if (probBadge) probBadge.textContent = `${pendingProblems.length} ${pendingProblems.length === 1 ? 'item' : 'items'}`;
    if (discBadge) discBadge.textContent = `${pendingDiscussions.length} ${pendingDiscussions.length === 1 ? 'item' : 'items'}`;
    if (reportsBadge) reportsBadge.textContent = `${pendingReports.length} ${pendingReports.length === 1 ? 'report' : 'reports'}`;

    // 1. Pending Problems List
    if (probList) {
      if (pendingProblems.length === 0) {
        probList.innerHTML = `
          <div class="text-center py-8 bg-white rounded-xl border border-dashed border-gray-300 p-6 space-y-2">
            <div class="w-9 h-9 rounded-full bg-emerald-50 text-emerald-600 mx-auto flex items-center justify-center">
              <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7"/></svg>
            </div>
            <div class="text-xs font-bold text-gray-800">No problem submissions pending review</div>
            <p class="text-[11px] text-gray-400">All submitted coding problems have been moderated and published.</p>
          </div>
        `;
      } else {
        probList.innerHTML = pendingProblems.map(p => {
          const diffBadge = p.difficulty === 'Easy'
            ? 'badge-easy'
            : p.difficulty === 'Medium'
              ? 'badge-medium'
              : 'badge-hard';
          const topicsHtml = (p.topics || []).map(t => `
            <span class="px-2 py-0.5 rounded text-[10px] bg-gray-100 text-gray-700 border border-gray-200">${escapeHtml(t)}</span>
          `).join(' ');

          return `
            <div class="bg-white rounded-xl border border-[#e5eae7] p-5 shadow-xs space-y-3">
              <div class="flex flex-col sm:flex-row sm:items-start justify-between gap-3">
                <div class="space-y-1.5 flex-1 min-w-0">
                  <div class="flex flex-wrap items-center gap-2">
                    <span class="px-2 py-0.5 rounded text-xs font-semibold ${diffBadge}">${escapeHtml(p.difficulty)}</span>
                    <h4 class="text-sm sm:text-base font-bold text-gray-900">${escapeHtml(p.title)}</h4>
                  </div>
                  <div class="flex flex-wrap items-center gap-2 text-xs text-gray-500">
                    <span class="font-medium text-gray-700">Author: ${escapeHtml(p.authorName || p.authorUsername || 'Anonymous')}</span>
                    <span class="text-gray-300">&middot;</span>
                    <span class="text-gray-400">${escapeHtml(p.authorEmail || '')}</span>
                    <span class="text-gray-300">&middot;</span>
                    <span class="text-gray-400 font-mono text-[11px]">${formatDate(p.createdAt)}</span>
                  </div>
                  <div class="flex flex-wrap gap-1.5 pt-1">
                    ${topicsHtml}
                  </div>
                </div>

                <!-- Action Buttons -->
                <div class="flex items-center gap-2 shrink-0 pt-1 sm:pt-0">
                  <button type="button" onclick="CodeForgeAdmin.approveProblem(${p.id})" class="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-lg bg-emerald-600 hover:bg-emerald-700 text-white font-semibold text-xs transition shadow-xs">
                    <svg class="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2.5" d="M5 13l4 4L19 7"/></svg>
                    <span>Approve</span>
                  </button>
                  <button type="button" onclick="CodeForgeAdmin.openRejectModal('problem', ${p.id}, '${escapeHtml(p.title)}')" class="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-lg border border-rose-200 text-rose-600 hover:bg-rose-50 font-semibold text-xs transition">
                    <svg class="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"/></svg>
                    <span>Reject</span>
                  </button>
                </div>
              </div>

              <!-- Problem statement preview -->
              <div class="p-3 rounded-lg bg-gray-50 border border-gray-100 text-xs text-gray-700 leading-relaxed font-sans max-h-28 overflow-y-auto">
                <span class="font-semibold text-gray-900 block mb-1">Problem Statement:</span>
                ${escapeHtml(p.description || p.statement || 'No statement provided.')}
              </div>
            </div>
          `;
        }).join('');
      }
    }

    // 2. Pending Discussions List
    if (discList) {
      if (pendingDiscussions.length === 0) {
        discList.innerHTML = `
          <div class="text-center py-8 bg-white rounded-xl border border-dashed border-gray-300 p-6 space-y-2">
            <div class="w-9 h-9 rounded-full bg-emerald-50 text-emerald-600 mx-auto flex items-center justify-center">
              <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7"/></svg>
            </div>
            <div class="text-xs font-bold text-gray-800">No community discussions pending review</div>
            <p class="text-[11px] text-gray-400">All community discussion posts meet platform standards.</p>
          </div>
        `;
      } else {
        discList.innerHTML = pendingDiscussions.map(d => {
          let problemBadge = '';
          if (d.problemId) {
            problemBadge = `<span class="px-2 py-0.5 rounded text-[10px] bg-emerald-50 text-emerald-700 border border-emerald-200 font-semibold">${escapeHtml(d.problemTitle || `Problem #${d.problemId}`)}</span>`;
          }

          return `
            <div class="bg-white rounded-xl border border-[#e5eae7] p-5 shadow-xs space-y-3">
              <div class="flex flex-col sm:flex-row sm:items-start justify-between gap-3">
                <div class="space-y-1.5 flex-1 min-w-0">
                  <div class="flex flex-wrap items-center gap-2">
                    <span class="px-2 py-0.5 rounded text-xs font-semibold bg-gray-100 text-gray-800 border border-gray-200">${escapeHtml(d.category)}</span>
                    ${problemBadge}
                    <h4 class="text-sm sm:text-base font-bold text-gray-900">${escapeHtml(d.title)}</h4>
                  </div>
                  <div class="flex flex-wrap items-center gap-2 text-xs text-gray-500">
                    <span class="font-medium text-gray-700">Author: ${escapeHtml(d.authorName || d.authorUsername || 'Anonymous')}</span>
                    <span class="text-gray-300">&middot;</span>
                    <span class="text-gray-400">${escapeHtml(d.authorEmail || '')}</span>
                    <span class="text-gray-300">&middot;</span>
                    <span class="text-gray-400 font-mono text-[11px]">${formatDate(d.createdAt)}</span>
                  </div>
                </div>

                <!-- Action Buttons -->
                <div class="flex items-center gap-2 shrink-0 pt-1 sm:pt-0">
                  <button type="button" onclick="CodeForgeAdmin.approveDiscussion(${d.id})" class="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-lg bg-emerald-600 hover:bg-emerald-700 text-white font-semibold text-xs transition shadow-xs" title="Approve and publish discussion to community">
                    <svg class="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2.5" d="M5 13l4 4L19 7"/></svg>
                    <span>Approve</span>
                  </button>
                  <button type="button" onclick="CodeForgeAdmin.deleteDiscussion(${d.id})" class="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-lg border border-rose-200 text-rose-600 hover:bg-rose-50 font-semibold text-xs transition" title="Reject and remove discussion from database">
                    <svg class="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16"/></svg>
                    <span>Reject / Remove</span>
                  </button>
                </div>
              </div>

              <!-- Discussion content preview -->
              <div class="p-3 rounded-lg bg-gray-50 border border-gray-100 text-xs text-gray-700 leading-relaxed font-sans max-h-24 overflow-y-auto">
                ${escapeHtml(d.content || '')}
              </div>
            </div>
          `;
        }).join('');
      }
    }

    // 3. Reported Content List
    if (reportsList) {
      if (pendingReports.length === 0) {
        reportsList.innerHTML = `
          <div class="text-center py-8 bg-white rounded-xl border border-dashed border-gray-300 p-6 space-y-2">
            <div class="w-9 h-9 rounded-full bg-emerald-50 text-emerald-600 mx-auto flex items-center justify-center">
              <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7"/></svg>
            </div>
            <div class="text-xs font-bold text-gray-800">No pending reports</div>
            <p class="text-[11px] text-gray-400">All user-flagged content and reports have been resolved.</p>
          </div>
        `;
      } else {
        reportsList.innerHTML = pendingReports.map(r => {
          const typeBadge = r.reportType === 'DISCUSSION'
            ? '<span class="px-2 py-0.5 rounded text-[10px] font-bold bg-purple-50 text-purple-700 border border-purple-200">Discussion</span>'
            : '<span class="px-2 py-0.5 rounded text-[10px] font-bold bg-blue-50 text-blue-700 border border-blue-200">Comment / Reply</span>';

          const reasonBadge = '<span class="px-2 py-0.5 rounded text-[10px] font-semibold bg-rose-50 text-rose-700 border border-rose-200">' + escapeHtml(r.reason) + '</span>';

          return `
            <div class="bg-white rounded-xl border border-[#e5eae7] p-5 shadow-xs space-y-3">
              <div class="flex flex-col sm:flex-row sm:items-start justify-between gap-3">
                <div class="space-y-1.5 flex-1 min-w-0">
                  <div class="flex flex-wrap items-center gap-2">
                    ${typeBadge}
                    ${reasonBadge}
                    <h4 class="text-sm sm:text-base font-bold text-gray-900">${escapeHtml(r.targetTitle || 'Reported Item')}</h4>
                  </div>
                  <div class="flex flex-wrap items-center gap-2 text-xs text-gray-500">
                    <span class="font-medium text-gray-700">Author: ${escapeHtml(r.targetAuthorName || 'Unknown')}</span>
                    <span class="text-gray-300">&middot;</span>
                    <span class="text-gray-500">Reported by: <span class="font-semibold text-gray-700">${escapeHtml(r.reporterName || 'Anonymous')}</span> (${escapeHtml(r.reporterEmail || '')})</span>
                    <span class="text-gray-300">&middot;</span>
                    <span class="text-gray-400 font-mono text-[11px]">${formatDate(r.createdAt)}</span>
                  </div>
                  ${r.details ? `<div class="text-xs text-gray-600 bg-amber-50/70 border border-amber-200/60 p-2 rounded-lg"><span class="font-semibold text-amber-800">Reporter's notes:</span> ${escapeHtml(r.details)}</div>` : ''}
                </div>

                <!-- Action Buttons: Keep or Remove -->
                <div class="flex items-center gap-2 shrink-0 pt-1 sm:pt-0">
                  <button type="button" onclick="CodeForgeAdmin.dismissReport(${r.id})" class="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-lg border border-gray-200 bg-white hover:bg-gray-50 text-gray-700 font-semibold text-xs transition shadow-xs" title="Dismiss report and keep content">
                    <svg class="w-3.5 h-3.5 text-gray-500" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7"/></svg>
                    <span>Keep Content</span>
                  </button>
                  <button type="button" onclick="CodeForgeAdmin.deleteReportedItem(${r.id}, '${r.reportType}')" class="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-lg bg-rose-600 hover:bg-rose-700 text-white font-semibold text-xs transition shadow-xs" title="Remove content from database">
                    <svg class="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16"/></svg>
                    <span>Remove from DB</span>
                  </button>
                </div>
              </div>

              <!-- Reported content snippet -->
              <div class="p-3 rounded-lg bg-gray-50 border border-gray-100 text-xs text-gray-700 leading-relaxed font-sans max-h-24 overflow-y-auto">
                <span class="font-semibold text-gray-900 block mb-1">Reported content:</span>
                ${escapeHtml(r.targetContent || 'No content')}
              </div>
            </div>
          `;
        }).join('');
      }
    }
  }

  async function approveProblem(id) {
    if (!window.CodeForgeAPI || !CodeForgeAPI.approveProblem) return;
    try {
      await CodeForgeAPI.approveProblem(id);
      if (window.CodeForgeApp) CodeForgeApp.showToast('Problem approved and published to the catalog!', 'success');
      await loadStats();
      await loadModerationQueue();
    } catch (err) {
      console.error('Failed to approve problem:', err);
      if (window.CodeForgeApp) CodeForgeApp.showToast('Failed to approve problem: ' + err.message, 'warn');
    }
  }

  async function approveDiscussion(id) {
    if (!window.CodeForgeAPI || !CodeForgeAPI.approveDiscussion) return;
    try {
      await CodeForgeAPI.approveDiscussion(id);
      if (window.CodeForgeApp) CodeForgeApp.showToast('Discussion approved and published to community!', 'success');
      await loadStats();
      await loadModerationQueue();
    } catch (err) {
      console.error('Failed to approve discussion:', err);
      if (window.CodeForgeApp) CodeForgeApp.showToast('Failed to approve discussion: ' + err.message, 'warn');
    }
  }

  function openRejectModal(type, id, title) {
    currentRejectTarget = { type, id, title };
    const modal = document.getElementById('admin-reject-modal');
    const titleEl = document.getElementById('admin-reject-item-title');
    const reasonInput = document.getElementById('admin-reject-reason-input');

    if (titleEl) titleEl.textContent = title || `Item #${id}`;
    if (reasonInput) reasonInput.value = '';
    if (modal) modal.classList.remove('hidden');
  }

  function closeRejectModal() {
    currentRejectTarget = null;
    const modal = document.getElementById('admin-reject-modal');
    if (modal) modal.classList.add('hidden');
  }

  async function confirmReject() {
    if (!currentRejectTarget) return;
    const reasonInput = document.getElementById('admin-reject-reason-input');
    const reason = (reasonInput ? reasonInput.value : '').trim();
    if (!reason) {
      if (window.CodeForgeApp) CodeForgeApp.showToast('Please provide a reason for rejection.', 'warn');
      return;
    }

    const { type, id } = currentRejectTarget;
    try {
      if (type === 'problem') {
        await CodeForgeAPI.rejectProblem(id, reason);
        if (window.CodeForgeApp) CodeForgeApp.showToast('Problem submission rejected.', 'info');
      } else {
        await CodeForgeAPI.deleteDiscussion(id);
        if (window.CodeForgeApp) CodeForgeApp.showToast('Discussion rejected and deleted from database.', 'info');
      }
      closeRejectModal();
      await loadStats();
      await loadModerationQueue();
    } catch (err) {
      console.error('Failed to reject item:', err);
      if (window.CodeForgeApp) CodeForgeApp.showToast('Rejection failed: ' + err.message, 'warn');
    }
  }

  async function deleteDiscussion(id) {
    if (!confirm('Are you sure you want to permanently delete this discussion from the database?')) {
      return;
    }
    try {
      if (window.CodeForgeAPI && CodeForgeAPI.deleteDiscussion) {
        await CodeForgeAPI.deleteDiscussion(id);
      }
      if (window.CodeForgeApp) CodeForgeApp.showToast('Discussion removed and deleted from database!', 'info');
      await loadStats();
      await loadModerationQueue();
    } catch (err) {
      console.error('Failed to delete discussion:', err);
      if (window.CodeForgeApp) CodeForgeApp.showToast('Failed to delete discussion: ' + err.message, 'warn');
    }
  }

  async function dismissReport(id) {
    try {
      if (window.CodeForgeAPI && CodeForgeAPI.dismissReport) {
        await CodeForgeAPI.dismissReport(id);
      }
      if (window.CodeForgeApp) CodeForgeApp.showToast('Report dismissed. Content has been retained.', 'info');
      await loadStats();
      await loadModerationQueue();
    } catch (err) {
      console.error('Failed to dismiss report:', err);
      if (window.CodeForgeApp) CodeForgeApp.showToast('Failed to dismiss report: ' + err.message, 'warn');
    }
  }

  async function deleteReportedItem(id, reportType) {
    const label = reportType === 'DISCUSSION' ? 'discussion' : 'comment';
    if (!confirm(`Are you sure you want to permanently delete this reported ${label} from the database?`)) {
      return;
    }
    try {
      if (window.CodeForgeAPI && CodeForgeAPI.deleteReportedItem) {
        await CodeForgeAPI.deleteReportedItem(id);
      }
      if (window.CodeForgeApp) CodeForgeApp.showToast(`Reported ${label} deleted from database!`, 'success');
      await loadStats();
      await loadModerationQueue();
    } catch (err) {
      console.error('Failed to delete reported item:', err);
      if (window.CodeForgeApp) CodeForgeApp.showToast('Failed to delete content: ' + err.message, 'warn');
    }
  }

  // ===== User Feedbacks Management =====
  async function loadFeedbacks() {
    if (!window.CodeForgeAPI || !CodeForgeAPI.getAdminFeedbacks) return;
    try {
      feedbacksList = await CodeForgeAPI.getAdminFeedbacks();
      renderFeedbacksTable();
    } catch (err) {
      console.warn('Failed to load feedbacks:', err);
    }
  }

  function renderFeedbacksTable() {
    const tableBody = document.getElementById('admin-feedbacks-table-body');
    if (!tableBody) return;

    let filtered = feedbacksList || [];

    // Apply status filter
    if (feedbackStatusFilter && feedbackStatusFilter !== 'ALL') {
      filtered = filtered.filter(f => (f.status || 'NEW').toUpperCase() === feedbackStatusFilter);
    }

    // Apply search filter
    if (feedbackSearchQuery) {
      filtered = filtered.filter(f =>
        (f.name && f.name.toLowerCase().includes(feedbackSearchQuery)) ||
        (f.email && f.email.toLowerCase().includes(feedbackSearchQuery)) ||
        (f.subject && f.subject.toLowerCase().includes(feedbackSearchQuery)) ||
        (f.message && f.message.toLowerCase().includes(feedbackSearchQuery))
      );
    }

    if (filtered.length === 0) {
      tableBody.innerHTML = `
        <tr>
          <td colspan="6" class="py-8 text-center text-gray-400">
            <svg class="w-8 h-8 mx-auto mb-2 text-gray-300" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M8 10h.01M12 10h.01M16 10h.01M9 16H5a2 2 0 01-2-2V6a2 2 0 012-2h14a2 2 0 012 2v8a2 2 0 01-2 2h-5l-5 5v-5z"/>
            </svg>
            No user feedback entries matching your criteria.
          </td>
        </tr>
      `;
      return;
    }

    tableBody.innerHTML = filtered.map(f => {
      const status = (f.status || 'NEW').toUpperCase();
      let statusBadge = '';
      if (status === 'NEW') {
        statusBadge = '<span class="inline-flex items-center px-2 py-0.5 rounded text-[10px] font-bold bg-emerald-50 text-emerald-800 border border-emerald-200">NEW</span>';
      } else if (status === 'REVIEWED') {
        statusBadge = '<span class="inline-flex items-center px-2 py-0.5 rounded text-[10px] font-bold bg-blue-50 text-blue-700 border border-blue-200">REVIEWED</span>';
      } else {
        statusBadge = '<span class="inline-flex items-center px-2 py-0.5 rounded text-[10px] font-bold bg-gray-100 text-gray-700 border border-gray-200">RESOLVED</span>';
      }

      const initials = getInitials(f.name || 'User');

      return `
        <tr class="hover:bg-gray-50/75 transition">
          <td class="py-3 px-4">
            <div class="flex items-center gap-2.5">
              <div class="w-7 h-7 rounded-full bg-emerald-100 text-emerald-800 font-semibold flex items-center justify-center text-[11px] flex-shrink-0">
                ${escapeHtml(initials)}
              </div>
              <div class="min-w-0">
                <div class="font-semibold text-gray-900 truncate">${escapeHtml(f.name || 'Anonymous')}</div>
                <div class="text-[11px] text-gray-500 font-mono truncate">${escapeHtml(f.email || '')}</div>
              </div>
            </div>
          </td>
          <td class="py-3 px-4">
            <span class="inline-flex items-center px-2 py-0.5 rounded text-[11px] font-medium bg-gray-100 text-gray-700 border border-gray-200">
              ${escapeHtml(f.subject || 'General')}
            </span>
          </td>
          <td class="py-3 px-4 max-w-xs sm:max-w-md">
            <div class="text-xs text-gray-800 leading-snug line-clamp-2" title="${escapeHtml(f.message)}">
              ${escapeHtml(f.message)}
            </div>
          </td>
          <td class="py-3 px-4 text-gray-400 font-mono text-[11px] whitespace-nowrap">
            ${formatDate(f.createdAt)}
          </td>
          <td class="py-3 px-4 whitespace-nowrap">
            ${statusBadge}
          </td>
          <td class="py-3 px-4 text-right whitespace-nowrap">
            <div class="flex items-center justify-end gap-1.5">
              ${status === 'NEW' ? `
                <button onclick="CodeForgeAdmin.updateFeedbackStatus(${f.id}, 'REVIEWED')" type="button"
                  title="Mark as Reviewed" class="px-2 py-1 rounded bg-blue-50 text-blue-700 hover:bg-blue-100 text-[11px] font-semibold border border-blue-200 transition">
                  Review
                </button>
              ` : ''}
              ${status !== 'RESOLVED' ? `
                <button onclick="CodeForgeAdmin.updateFeedbackStatus(${f.id}, 'RESOLVED')" type="button"
                  title="Mark as Resolved" class="px-2 py-1 rounded bg-emerald-50 text-emerald-800 hover:bg-emerald-100 text-[11px] font-semibold border border-emerald-200 transition">
                  Resolve
                </button>
              ` : `
                <button onclick="CodeForgeAdmin.updateFeedbackStatus(${f.id}, 'NEW')" type="button"
                  title="Reopen as New" class="px-2 py-1 rounded bg-gray-50 text-gray-600 hover:bg-gray-100 text-[11px] font-medium border border-gray-200 transition">
                  Reopen
                </button>
              `}
              <button onclick="CodeForgeAdmin.deleteFeedback(${f.id})" type="button"
                title="Delete Feedback" class="p-1 rounded text-gray-400 hover:text-rose-600 hover:bg-rose-50 transition">
                <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16"/>
                </svg>
              </button>
            </div>
          </td>
        </tr>
      `;
    }).join('');
  }

  async function updateFeedbackStatus(id, newStatus) {
    if (!window.CodeForgeAPI || !CodeForgeAPI.updateAdminFeedbackStatus) return;
    try {
      await CodeForgeAPI.updateAdminFeedbackStatus(id, newStatus);
      if (window.CodeForgeApp) CodeForgeApp.showToast(`Feedback status updated to ${newStatus}`, 'success');
      await loadFeedbacks();
      await loadStats();
    } catch (err) {
      console.error('Failed to update feedback status:', err);
      if (window.CodeForgeApp) CodeForgeApp.showToast('Failed to update feedback status', 'warn');
    }
  }

  async function deleteFeedback(id) {
    if (!confirm('Are you sure you want to delete this user feedback?')) return;
    if (!window.CodeForgeAPI || !CodeForgeAPI.deleteAdminFeedback) return;
    try {
      await CodeForgeAPI.deleteAdminFeedback(id);
      if (window.CodeForgeApp) CodeForgeApp.showToast('Feedback deleted from database.', 'info');
      await loadFeedbacks();
      await loadStats();
    } catch (err) {
      console.error('Failed to delete feedback:', err);
      if (window.CodeForgeApp) CodeForgeApp.showToast('Failed to delete feedback', 'warn');
    }
  }

  async function loadUsers() {
    if (!window.CodeForgeAPI || !CodeForgeAPI.getAdminUsers) return;
    try {
      usersList = await CodeForgeAPI.getAdminUsers();
      renderUsersTable();
    } catch (err) {
      console.warn('Failed to load users:', err);
    }
  }

  function renderUsersTable() {
    const tableBody = document.getElementById('admin-users-table-body');
    if (!tableBody) return;

    let filtered = usersList || [];
    if (userSearchQuery) {
      filtered = filtered.filter(u => {
        const name = (u.fullName || '').toLowerCase();
        const username = (u.username || '').toLowerCase();
        const email = (u.email || '').toLowerCase();
        return name.includes(userSearchQuery) || username.includes(userSearchQuery) || email.includes(userSearchQuery);
      });
    }

    if (filtered.length === 0) {
      tableBody.innerHTML = `
        <tr>
          <td colspan="6" class="py-10 text-center text-gray-400">No users found matching query.</td>
        </tr>
      `;
      return;
    }

    tableBody.innerHTML = filtered.map(u => {
      const initials = getInitials(u.fullName || u.username);
      const role = u.role || 'Learner';
      const isRoleAdmin = role.toLowerCase() === 'admin';
      const roleBadge = isRoleAdmin
        ? `<span class="px-2 py-0.5 rounded text-[10px] font-bold bg-amber-50 text-amber-800 border border-amber-200">Admin</span>`
        : `<span class="px-2 py-0.5 rounded text-[10px] font-semibold bg-blue-50 text-blue-700 border border-blue-200">Learner</span>`;

      return `
        <tr class="hover:bg-gray-50/75 transition">
          <td class="py-3 px-4">
            <div class="flex items-center gap-2.5">
              <div class="w-7 h-7 rounded-full bg-emerald-100 text-emerald-800 border border-emerald-200 flex items-center justify-center font-bold text-[10px]">
                ${initials}
              </div>
              <div>
                <div class="font-semibold text-gray-900">${escapeHtml(u.fullName || u.username)}</div>
                <div class="text-[11px] text-gray-400">${escapeHtml(u.email || '')}</div>
              </div>
            </div>
          </td>
          <td class="py-3 px-4 font-mono text-gray-600">${escapeHtml(u.username)}</td>
          <td class="py-3 px-4">${roleBadge}</td>
          <td class="py-3 px-4 text-center font-mono font-semibold text-gray-700">${u.solvedCount != null ? u.solvedCount : 0}</td>
          <td class="py-3 px-4 text-gray-400 font-mono text-[11px]">${formatDate(u.createdAt)}</td>
          <td class="py-3 px-4 text-right">
            <select onchange="CodeForgeAdmin.changeRole(${u.id}, this.value)" class="text-xs border border-gray-200 rounded-lg px-2.5 py-1 bg-white focus:outline-none focus:ring-1 focus:ring-emerald-500 font-medium text-gray-700">
              <option value="Learner" ${!isRoleAdmin ? 'selected' : ''}>Learner</option>
              <option value="Admin" ${isRoleAdmin ? 'selected' : ''}>Admin</option>
            </select>
          </td>
        </tr>
      `;
    }).join('');
  }

  async function changeRole(userId, newRole) {
    if (!window.CodeForgeAPI || !CodeForgeAPI.updateUserRole) return;
    try {
      await CodeForgeAPI.updateUserRole(userId, newRole);
      if (window.CodeForgeApp) CodeForgeApp.showToast(`User role updated to ${newRole}!`, 'success');

      // If updating currently logged in user, update local storage role
      try {
        const stored = localStorage.getItem('codeforge_user') || sessionStorage.getItem('codeforge_user');
        if (stored) {
          const u = JSON.parse(stored);
          if (u.id === userId) {
            u.role = newRole;
            localStorage.setItem('codeforge_user', JSON.stringify(u));
            if (window.CodeForgeApp && CodeForgeApp.updateAuthUI) CodeForgeApp.updateAuthUI();
          }
        }
      } catch (_) {}

      await loadUsers();
    } catch (err) {
      console.error('Failed to change user role:', err);
      if (window.CodeForgeApp) CodeForgeApp.showToast('Role update failed: ' + err.message, 'warn');
    }
  }

  // ===== User Tracking Dashboard =====
  async function loadTrackingData() {
    if (!window.CodeForgeAPI) return;
    try {
      const [summaryData, loginsData, visitsData] = await Promise.all([
        CodeForgeAPI.getAdminTrackingSummary ? CodeForgeAPI.getAdminTrackingSummary() : null,
        CodeForgeAPI.getAdminLoginLogs ? CodeForgeAPI.getAdminLoginLogs() : [],
        CodeForgeAPI.getAdminPageVisits ? CodeForgeAPI.getAdminPageVisits() : []
      ]);

      trackingSummary = summaryData;
      loginLogs = loginsData || [];
      pageVisits = visitsData || [];

      renderTrackingSummary();
      renderLoginLogsTable();
      populateRouteFilterOptions();
      renderPageVisitsTable();
    } catch (err) {
      console.warn('Failed to load tracking data:', err);
    }
  }

  function renderTrackingSummary() {
    const kpiLogins = document.getElementById('tracking-total-logins');
    const kpiLocations = document.getElementById('tracking-unique-locations');
    const kpiVisits = document.getElementById('tracking-total-visits');
    const kpiTopPage = document.getElementById('tracking-top-page');
    const kpiTopPageSub = document.getElementById('tracking-top-page-sub');
    const tabTrackingCount = document.getElementById('tab-admin-tracking-count');

    const totalLogins = trackingSummary && trackingSummary.totalLogins != null ? trackingSummary.totalLogins : loginLogs.length;
    const uniqueLocations = trackingSummary && trackingSummary.uniqueLocations != null ? trackingSummary.uniqueLocations : 0;
    const totalVisits = trackingSummary && trackingSummary.totalPageVisits != null ? trackingSummary.totalPageVisits : pageVisits.length;

    if (kpiLogins) kpiLogins.textContent = totalLogins.toLocaleString();
    if (kpiLocations) kpiLocations.textContent = uniqueLocations.toLocaleString();
    if (kpiVisits) kpiVisits.textContent = totalVisits.toLocaleString();
    if (tabTrackingCount) tabTrackingCount.textContent = totalLogins.toLocaleString();

    if (trackingSummary && Array.isArray(trackingSummary.topPages) && trackingSummary.topPages.length > 0) {
      const top = trackingSummary.topPages[0];
      if (kpiTopPage) kpiTopPage.textContent = top.pageTitle || top.pageRoute || '--';
      if (kpiTopPageSub) kpiTopPageSub.textContent = `${(top.visits || 0).toLocaleString()} views (${top.pageRoute})`;
    } else {
      if (kpiTopPage) kpiTopPage.textContent = '--';
      if (kpiTopPageSub) kpiTopPageSub.textContent = 'No visit records yet';
    }
  }

  function renderLoginLogsTable() {
    const tableBody = document.getElementById('admin-login-table-body');
    const countBadge = document.getElementById('admin-login-count-badge');
    if (!tableBody) return;

    let filtered = loginLogs || [];

    // Filter by login method
    if (loginMethodFilter && loginMethodFilter !== 'ALL') {
      filtered = filtered.filter(l => (l.loginMethod || '').toUpperCase() === loginMethodFilter);
    }

    // Filter by search query
    if (loginSearchQuery) {
      filtered = filtered.filter(l =>
        (l.username && l.username.toLowerCase().includes(loginSearchQuery)) ||
        (l.email && l.email.toLowerCase().includes(loginSearchQuery)) ||
        (l.ipAddress && l.ipAddress.toLowerCase().includes(loginSearchQuery)) ||
        (l.location && l.location.toLowerCase().includes(loginSearchQuery))
      );
    }

    if (countBadge) {
      countBadge.textContent = `${filtered.length} ${filtered.length === 1 ? 'session' : 'sessions'}`;
    }

    if (filtered.length === 0) {
      tableBody.innerHTML = `
        <tr>
          <td colspan="5" class="py-8 text-center text-gray-400">
            <svg class="w-8 h-8 mx-auto mb-2 text-gray-300" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M12 11c0 3.517-1.009 6.799-2.753 9.571m-3.44-2.04l.054-.09A13.916 13.916 0 008 11a4 4 0 118 0c0 1.017-.07 2.019-.203 3m-2.118 6.844A21.88 21.88 0 0015.171 17m3.839 1.132c.645-2.266.99-4.659.99-7.132A8 8 0 004 11c0 2.473.345 4.866.99 7.132"/>
            </svg>
            No login history records found matching your filters.
          </td>
        </tr>
      `;
      return;
    }

    tableBody.innerHTML = filtered.map(log => {
      const initials = getInitials(log.username || 'User');
      const method = (log.loginMethod || 'PASSWORD').toUpperCase();
      let methodBadge = '';
      if (method === 'GOOGLE') {
        methodBadge = '<span class="inline-flex items-center px-2 py-0.5 rounded text-[10px] font-bold bg-red-50 text-red-700 border border-red-200">Google</span>';
      } else if (method === 'GITHUB') {
        methodBadge = '<span class="inline-flex items-center px-2 py-0.5 rounded text-[10px] font-bold bg-slate-100 text-slate-800 border border-slate-300">GitHub</span>';
      } else if (method === 'REGISTER') {
        methodBadge = '<span class="inline-flex items-center px-2 py-0.5 rounded text-[10px] font-bold bg-emerald-50 text-emerald-800 border border-emerald-200">Register</span>';
      } else if (method === 'LOGOUT') {
        methodBadge = '<span class="inline-flex items-center px-2 py-0.5 rounded text-[10px] font-bold bg-amber-50 text-amber-800 border border-amber-200">Logout</span>';
      } else {
        methodBadge = '<span class="inline-flex items-center px-2 py-0.5 rounded text-[10px] font-bold bg-gray-100 text-gray-700 border border-gray-200">Password</span>';
      }

      const deviceLabel = parseUserAgent(log.userAgent);

      return `
        <tr class="hover:bg-gray-50/75 transition">
          <td class="py-3 px-4">
            <div class="flex items-center gap-2.5">
              <div class="w-7 h-7 rounded-full bg-blue-100 text-blue-800 font-semibold flex items-center justify-center text-[11px] flex-shrink-0">
                ${escapeHtml(initials)}
              </div>
              <div class="min-w-0">
                <div class="font-semibold text-gray-900 truncate">${escapeHtml(log.username || 'Unknown')}</div>
                <div class="text-[11px] text-gray-400 font-mono truncate">${escapeHtml(log.email || '--')}</div>
              </div>
            </div>
          </td>
          <td class="py-3 px-4 whitespace-nowrap">
            ${methodBadge}
          </td>
          <td class="py-3 px-4 text-gray-500 font-mono text-[11px] whitespace-nowrap">
            ${formatDateTime(log.loggedInAt)}
          </td>
          <td class="py-3 px-4">
            <div class="font-medium text-gray-800 truncate">${escapeHtml(log.location || 'Unknown')}</div>
            <div class="text-[11px] text-gray-400 font-mono truncate">${escapeHtml(log.ipAddress || '--')}</div>
          </td>
          <td class="py-3 px-4 text-xs text-gray-600 truncate max-w-xs" title="${escapeHtml(log.userAgent || '')}">
            <span class="inline-flex items-center gap-1.5">
              <svg class="w-3.5 h-3.5 text-gray-400 shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9.75 17L9 20l-1 1h8l-1-1-.75-3M3 13h18M5 17h14a2 2 0 002-2V5a2 2 0 00-2-2H5a2 2 0 00-2 2v10a2 2 0 002 2z"/>
              </svg>
              <span>${escapeHtml(deviceLabel)}</span>
            </span>
          </td>
        </tr>
      `;
    }).join('');
  }

  function populateRouteFilterOptions() {
    const routeSelect = document.getElementById('admin-visit-filter-route');
    if (!routeSelect) return;

    const routes = new Set();
    routes.add('#dashboard');
    routes.add('#library');
    routes.add('#ide');
    routes.add('#learning-paths');
    routes.add('#notes');
    routes.add('#community');
    routes.add('#submissions');
    routes.add('#create-problem');

    (pageVisits || []).forEach(v => {
      if (v.pageRoute) routes.add(v.pageRoute);
    });

    const currentVal = routeSelect.value || 'ALL';
    let html = '<option value="ALL">All Routes</option>';
    routes.forEach(r => {
      const selected = r === currentVal ? 'selected' : '';
      html += `<option value="${escapeHtml(r)}" ${selected}>${escapeHtml(r)}</option>`;
    });
    routeSelect.innerHTML = html;
  }

  function renderPageVisitsTable() {
    const tableBody = document.getElementById('admin-visit-table-body');
    const countBadge = document.getElementById('admin-visit-count-badge');
    if (!tableBody) return;

    let filtered = pageVisits || [];

    // Filter by route
    if (visitRouteFilter && visitRouteFilter !== 'ALL') {
      filtered = filtered.filter(v => (v.pageRoute || '') === visitRouteFilter);
    }

    // Filter by search query
    if (visitSearchQuery) {
      filtered = filtered.filter(v =>
        (v.username && v.username.toLowerCase().includes(visitSearchQuery)) ||
        (v.pageRoute && v.pageRoute.toLowerCase().includes(visitSearchQuery)) ||
        (v.pageTitle && v.pageTitle.toLowerCase().includes(visitSearchQuery)) ||
        (v.location && v.location.toLowerCase().includes(visitSearchQuery)) ||
        (v.ipAddress && v.ipAddress.toLowerCase().includes(visitSearchQuery))
      );
    }

    if (countBadge) {
      countBadge.textContent = `${filtered.length} ${filtered.length === 1 ? 'visit' : 'visits'}`;
    }

    if (filtered.length === 0) {
      tableBody.innerHTML = `
        <tr>
          <td colspan="4" class="py-8 text-center text-gray-400">
            <svg class="w-8 h-8 mx-auto mb-2 text-gray-300" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z"/>
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z"/>
            </svg>
            No navigation records found matching your filters.
          </td>
        </tr>
      `;
      return;
    }

    tableBody.innerHTML = filtered.map(visit => {
      const isGuest = !visit.username || visit.username.toLowerCase() === 'guest';
      const userHtml = isGuest
        ? `<div class="flex items-center gap-2.5">
             <div class="w-7 h-7 rounded-full bg-gray-100 text-gray-500 font-semibold flex items-center justify-center text-[10px] flex-shrink-0">
               ?
             </div>
             <div>
               <div class="font-medium text-gray-500 italic">Guest User</div>
               <div class="text-[10px] text-gray-400">Unauthenticated</div>
             </div>
           </div>`
        : `<div class="flex items-center gap-2.5">
             <div class="w-7 h-7 rounded-full bg-purple-100 text-purple-800 font-semibold flex items-center justify-center text-[11px] flex-shrink-0">
               ${escapeHtml(getInitials(visit.username))}
             </div>
             <div>
               <div class="font-semibold text-gray-900">${escapeHtml(visit.username)}</div>
               <div class="text-[10px] text-purple-700 font-medium">Logged-in User</div>
             </div>
           </div>`;

      return `
        <tr class="hover:bg-gray-50/75 transition">
          <td class="py-3 px-4 whitespace-nowrap">
            ${userHtml}
          </td>
          <td class="py-3 px-4">
            <div class="font-medium text-gray-900 truncate">${escapeHtml(visit.pageTitle || 'Untitled Page')}</div>
            <div class="mt-0.5">
              <span class="inline-flex items-center px-1.5 py-0.5 rounded text-[10px] font-mono bg-gray-100 text-gray-700 border border-gray-200">
                ${escapeHtml(visit.pageRoute || '#')}
              </span>
            </div>
          </td>
          <td class="py-3 px-4">
            <div class="font-medium text-gray-800 truncate">${escapeHtml(visit.location || 'Unknown')}</div>
            <div class="text-[11px] text-gray-400 font-mono truncate">${escapeHtml(visit.ipAddress || '--')}</div>
          </td>
          <td class="py-3 px-4 text-gray-500 font-mono text-[11px] whitespace-nowrap">
            ${formatDateTime(visit.visitedAt)}
          </td>
        </tr>
      `;
    }).join('');
  }

  window.CodeForgeAdmin = {
    init,
    loadStats,
    loadModerationQueue,
    approveProblem,
    approveDiscussion,
    deleteDiscussion,
    dismissReport,
    deleteReportedItem,
    openRejectModal,
    closeRejectModal,
    confirmReject,
    loadUsers,
    loadFeedbacks,
    updateFeedbackStatus,
    deleteFeedback,
    changeRole,
    switchTab,
    loadTrackingData
  };

})();

