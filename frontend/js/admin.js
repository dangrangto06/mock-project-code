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
  let activeTab = 'moderation'; // 'moderation' | 'users'
  let currentRejectTarget = null; // { type: 'problem'|'discussion', id, title }
  let userSearchQuery = '';

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
    } else {
      await loadUsers();
    }
  }

  function bindEvents() {
    const tabMod = document.getElementById('tab-admin-mod');
    const tabUsers = document.getElementById('tab-admin-users');
    if (tabMod) tabMod.addEventListener('click', () => switchTab('moderation'));
    if (tabUsers) tabUsers.addEventListener('click', () => switchTab('users'));

    const btnRefresh = document.getElementById('btn-admin-refresh');
    if (btnRefresh) {
      btnRefresh.addEventListener('click', async () => {
        btnRefresh.classList.add('opacity-50');
        await loadStats();
        if (activeTab === 'moderation') await loadModerationQueue();
        else await loadUsers();
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
    const tabUsers = document.getElementById('tab-admin-users');
    const viewMod = document.getElementById('admin-view-moderation');
    const viewUsers = document.getElementById('admin-view-users');

    if (tab === 'moderation') {
      if (tabMod) tabMod.className = 'pb-3 text-sm font-semibold text-emerald-800 border-b-2 border-emerald-600 transition flex items-center gap-2';
      if (tabUsers) tabUsers.className = 'pb-3 text-sm text-gray-500 hover:text-gray-800 border-b-2 border-transparent transition flex items-center gap-2 font-medium';
      if (viewMod) viewMod.classList.remove('hidden');
      if (viewUsers) viewUsers.classList.add('hidden');
      loadModerationQueue();
    } else {
      if (tabUsers) tabUsers.className = 'pb-3 text-sm font-semibold text-emerald-800 border-b-2 border-emerald-600 transition flex items-center gap-2';
      if (tabMod) tabMod.className = 'pb-3 text-sm text-gray-500 hover:text-gray-800 border-b-2 border-transparent transition flex items-center gap-2 font-medium';
      if (viewUsers) viewUsers.classList.remove('hidden');
      if (viewMod) viewMod.classList.add('hidden');
      loadUsers();
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
      const tabModCount = document.getElementById('tab-admin-mod-count');
      const tabUsersCount = document.getElementById('tab-admin-users-count');

      const totalPending = (stats.pendingProblemsCount || 0) + (stats.pendingDiscussionsCount || 0) + (stats.pendingReportsCount || 0);

      if (kpiUsers) kpiUsers.textContent = (stats.totalUsers || 0).toLocaleString();
      if (kpiProblems) kpiProblems.textContent = (stats.totalProblems || 0).toLocaleString();
      if (kpiPending) kpiPending.textContent = totalPending;
      if (kpiPendingSub) {
        kpiPendingSub.textContent = `${stats.pendingProblemsCount || 0} problems, ${stats.pendingDiscussionsCount || 0} posts, ${stats.pendingReportsCount || 0} reports`;
      }
      if (kpiSubmissions) kpiSubmissions.textContent = (stats.totalSubmissions || 0).toLocaleString();
      if (tabModCount) tabModCount.textContent = totalPending;
      if (tabUsersCount) tabUsersCount.textContent = (stats.totalUsers || 0).toLocaleString();

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
    changeRole,
    switchTab
  };

})();

