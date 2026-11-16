// CodeForge Learner Community Module
// Handles discussions, questions, algorithmic solutions, upvotes, and thread replies.

window.CodeForgeCommunity = (function () {
  const STORAGE_KEY = 'codeforge_discussions';
  let posts = [];
  let activeCategory = 'all';
  let searchQuery = '';
  let currentViewingPostId = null;

  function escapeHtml(str) {
    if (!str) return '';
    return String(str).replace(/[&<>"']/g, ch => (
      { '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[ch]
    ));
  }

  function formatContent(text) {
    if (!text) return '';
    let safe = escapeHtml(text);
    // Inline code `...`
    safe = safe.replace(/`([^`]+)`/g, '<code class="px-1.5 py-0.5 rounded bg-gray-100 text-emerald-800 font-mono text-[11px] border border-gray-200">$1</code>');
    // Bold **...**
    safe = safe.replace(/\*\*([^*]+)\*\*/g, '<strong class="font-semibold text-gray-900">$1</strong>');
    // Line breaks
    safe = safe.replace(/\n\n/g, '<div class="h-2"></div>').replace(/\n/g, '<br/>');
    return safe;
  }

  function getCategoryColor(category) {
    switch (category) {
      case 'Solutions':
        return 'bg-emerald-50 text-emerald-800 border-emerald-200';
      case 'Questions':
        return 'bg-blue-50 text-blue-800 border-blue-200';
      case 'Interview Prep':
        return 'bg-purple-50 text-purple-800 border-purple-200';
      case 'Tips':
        return 'bg-amber-50 text-amber-800 border-amber-200';
      default:
        return 'bg-gray-50 text-gray-700 border-gray-200';
    }
  }

  function getRoleBadge(role) {
    if (!role) return '';
    if (role === 'Admin') {
      return '<span class="px-1.5 py-0.5 rounded text-[10px] font-semibold bg-emerald-100 text-emerald-800 border border-emerald-200">Staff</span>';
    }
    if (role === 'Top Contributor') {
      return '<span class="px-1.5 py-0.5 rounded text-[10px] font-semibold bg-amber-100 text-amber-800 border border-amber-200">★ Top Contributor</span>';
    }
    return '<span class="px-1.5 py-0.5 rounded text-[10px] font-medium bg-gray-100 text-gray-600">Learner</span>';
  }

  function loadDiscussions() {
    try {
      const stored = localStorage.getItem(STORAGE_KEY);
      if (stored) {
        posts = JSON.parse(stored);
      } else {
        posts = (window.CodeForgeData && window.CodeForgeData.initialDiscussions) ? [...window.CodeForgeData.initialDiscussions] : [];
        saveDiscussionsToStorage();
      }
    } catch (err) {
      console.warn('Failed to load discussions from localStorage:', err);
      posts = (window.CodeForgeData && window.CodeForgeData.initialDiscussions) ? [...window.CodeForgeData.initialDiscussions] : [];
    }
  }

  function saveDiscussionsToStorage() {
    try {
      localStorage.setItem(STORAGE_KEY, JSON.stringify(posts));
    } catch (err) {
      console.error('Failed to save discussions to localStorage:', err);
    }
  }

  function populateProblemSelect() {
    const select = document.getElementById('disc-problem-select');
    if (!select) return;

    select.innerHTML = '<option value="">None / General Discussion</option>';
    if (window.CodeForgeData && Array.isArray(window.CodeForgeData.problems)) {
      window.CodeForgeData.problems.forEach(p => {
        const opt = document.createElement('option');
        opt.value = p.id;
        opt.textContent = `#${p.id} ${p.title} (${p.difficulty})`;
        select.appendChild(opt);
      });
    }
  }

  function renderDiscussions() {
    const feed = document.getElementById('community-posts-feed');
    if (!feed) return;

    const q = searchQuery.trim().toLowerCase();
    const filtered = posts.filter(post => {
      // Category filter
      if (activeCategory !== 'all' && post.category !== activeCategory) {
        return false;
      }
      // Search query
      if (q) {
        const inTitle = (post.title || '').toLowerCase().includes(q);
        const inContent = (post.content || '').toLowerCase().includes(q);
        const inAuthor = (post.authorName || '').toLowerCase().includes(q);
        const inProblem = (post.problemTitle || '').toLowerCase().includes(q);
        if (!inTitle && !inContent && !inAuthor && !inProblem) return false;
      }
      return true;
    });

    if (filtered.length === 0) {
      feed.innerHTML = `
        <div class="text-center py-16 bg-white rounded-2xl border border-dashed border-gray-300 p-8 space-y-3">
          <div class="w-12 h-12 rounded-full bg-emerald-50 text-emerald-600 mx-auto flex items-center justify-center">
            <svg class="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8 12h.01M12 12h.01M16 12h.01M21 12c0 4.418-4.03 8-9 8a9.863 9.863 0 01-4.255-.949L3 20l1.395-3.72C3.512 15.042 3 13.574 3 12c0-4.418 4.03-8 9-8s9 3.582 9 8z"/></svg>
          </div>
          <h3 class="text-sm font-bold text-gray-800">No discussions found</h3>
          <p class="text-xs text-gray-500 max-w-sm mx-auto">Be the first to share an optimal approach or ask a question in this topic.</p>
          <button onclick="CodeForgeCommunity.openCreateModal()" class="px-4 py-2 rounded-lg bg-emerald-600 hover:bg-emerald-700 text-white text-xs font-semibold shadow-xs transition">
            Start a Discussion
          </button>
        </div>
      `;
      return;
    }

    feed.innerHTML = filtered.map(post => {
      const catColor = getCategoryColor(post.category);
      const roleBadge = getRoleBadge(post.authorRole);
      const isUpvoted = !!post.isUpvoted;

      const upvoteBtnClass = isUpvoted
        ? 'bg-emerald-50 text-emerald-800 border-emerald-300 shadow-xs'
        : 'bg-gray-50 text-gray-600 hover:bg-gray-100 hover:text-gray-900 border-gray-200';

      const upvoteIconFill = isUpvoted ? 'fill-emerald-600 text-emerald-600' : 'fill-none text-current';

      // Problem badge
      let problemBadgeHtml = '';
      if (post.problemId) {
        problemBadgeHtml = `
          <a href="#solve/${post.problemId}" onclick="event.stopPropagation()" class="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-md text-[11px] font-semibold bg-emerald-50 hover:bg-emerald-100 text-emerald-800 border border-emerald-200 transition" title="Open in IDE">
            <svg class="w-3 h-3 text-emerald-600" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M10 20l4-16m4 4l4 4-4 4M6 16l-4-4 4-4"/></svg>
            <span>${escapeHtml(post.problemTitle || `Problem #${post.problemId}`)}</span>
          </a>
        `;
      }

      const repliesCount = post.replies ? post.replies.length : (post.repliesCount || 0);

      return `
        <article class="bg-white rounded-xl border border-[#e5eae7] p-5 shadow-xs hover:shadow-md transition space-y-3 cursor-pointer group" onclick="CodeForgeCommunity.openViewModal('${post.id}')">
          <div class="flex items-start gap-4">
            
            <!-- Upvote Column -->
            <button type="button" onclick="CodeForgeCommunity.toggleUpvote('${post.id}', event)" class="flex flex-col items-center justify-center w-12 py-2 rounded-xl border ${upvoteBtnClass} transition shrink-0 group/vote">
              <svg class="w-4 h-4 transition-transform group-hover/vote:-translate-y-0.5 ${upvoteIconFill}" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2.5" d="M5 15l7-7 7 7"/></svg>
              <span class="text-xs font-bold font-mono mt-0.5">${post.upvotes || 0}</span>
            </button>

            <!-- Main Content -->
            <div class="flex-1 min-w-0 space-y-2">
              
              <!-- Meta Row -->
              <div class="flex flex-wrap items-center gap-2 text-xs">
                <span class="px-2 py-0.5 rounded text-[11px] font-semibold border ${catColor}">${escapeHtml(post.category)}</span>
                ${problemBadgeHtml}
                <span class="text-gray-300">&middot;</span>
                
                <div class="flex items-center gap-1.5">
                  <div class="w-5 h-5 rounded-full bg-emerald-800 text-white font-mono text-[10px] font-bold flex items-center justify-center">
                    ${escapeHtml(post.authorInitials || 'U')}
                  </div>
                  <span class="font-semibold text-gray-800 text-xs">${escapeHtml(post.authorName || 'Anonymous')}</span>
                  ${roleBadge}
                </div>

                <span class="text-gray-300">&middot;</span>
                <span class="text-[11px] text-gray-400 font-medium">${escapeHtml(post.createdAt || 'Recently')}</span>
              </div>

              <!-- Post Title -->
              <h3 class="text-sm sm:text-base font-bold text-gray-900 group-hover:text-emerald-800 transition leading-snug">
                ${escapeHtml(post.title)}
              </h3>

              <!-- Post Snippet -->
              <p class="text-xs text-gray-600 line-clamp-2 leading-relaxed">
                ${escapeHtml(post.content)}
              </p>

              <!-- Footer info -->
              <div class="pt-2 flex items-center gap-4 text-xs text-gray-500">
                <span class="inline-flex items-center gap-1.5 hover:text-emerald-700 transition font-medium">
                  <svg class="w-3.5 h-3.5 text-gray-400 group-hover:text-emerald-600" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8 12h.01M12 12h.01M16 12h.01M21 12c0 4.418-4.03 8-9 8a9.863 9.863 0 01-4.255-.949L3 20l1.395-3.72C3.512 15.042 3 13.574 3 12c0-4.418 4.03-8 9-8s9 3.582 9 8z"/></svg>
                  <span>${repliesCount} ${repliesCount === 1 ? 'reply' : 'replies'}</span>
                </span>
                <span class="text-emerald-700 font-semibold group-hover:underline inline-flex items-center gap-1 text-[11px]">
                  Join conversation &rarr;
                </span>
              </div>

            </div>

          </div>
        </article>
      `;
    }).join('');
  }

  function toggleUpvote(postId, e) {
    if (e) e.stopPropagation();

    const post = posts.find(p => p.id === postId);
    if (!post) return;

    if (post.isUpvoted) {
      post.isUpvoted = false;
      post.upvotes = Math.max(0, (post.upvotes || 1) - 1);
      if (window.CodeForgeApp) CodeForgeApp.showToast('Upvote removed', 'info');
    } else {
      post.isUpvoted = true;
      post.upvotes = (post.upvotes || 0) + 1;
      if (window.CodeForgeApp) CodeForgeApp.showToast('Upvoted!', 'success');
    }

    saveDiscussionsToStorage();
    renderDiscussions();

    // If modal is open for this post, refresh modal upvote button
    if (currentViewingPostId === postId) {
      updateModalUpvoteUI(post);
    }
  }

  function updateModalUpvoteUI(post) {
    const btn = document.getElementById('view-post-upvote-btn');
    const count = document.getElementById('view-post-upvote-count');
    if (!btn || !count) return;

    count.textContent = post.upvotes || 0;
    if (post.isUpvoted) {
      btn.className = 'inline-flex items-center gap-1.5 px-3 py-1.5 rounded-lg bg-emerald-50 text-emerald-800 border border-emerald-300 font-semibold text-xs transition';
    } else {
      btn.className = 'inline-flex items-center gap-1.5 px-3 py-1.5 rounded-lg bg-gray-50 text-gray-700 hover:bg-gray-100 border border-gray-200 font-medium text-xs transition';
    }
  }

  function filterByCategory(category) {
    activeCategory = category;
    const buttons = document.querySelectorAll('.comm-cat-btn');
    buttons.forEach(btn => {
      const bCat = btn.getAttribute('data-category');
      if (bCat === category) {
        btn.className = 'comm-cat-btn px-3 py-1.5 rounded-lg text-xs font-semibold bg-emerald-50 text-emerald-800 border border-emerald-200 transition';
      } else {
        btn.className = 'comm-cat-btn px-3 py-1.5 rounded-lg text-xs font-medium text-gray-600 hover:bg-gray-100 transition';
      }
    });
    renderDiscussions();
  }

  function openCreateModal() {
    const modal = document.getElementById('discussion-modal');
    const titleInput = document.getElementById('disc-title-input');
    const catSelect = document.getElementById('disc-category-select');
    const probSelect = document.getElementById('disc-problem-select');
    const contentInput = document.getElementById('disc-content-input');

    if (!modal) return;
    if (titleInput) titleInput.value = '';
    if (catSelect) catSelect.value = 'Solutions';
    if (probSelect) probSelect.value = '';
    if (contentInput) contentInput.value = '';

    modal.classList.remove('hidden');
    modal.classList.add('flex');
    if (titleInput) titleInput.focus();
  }

  function closeCreateModal() {
    const modal = document.getElementById('discussion-modal');
    if (modal) {
      modal.classList.add('hidden');
      modal.classList.remove('flex');
    }
  }

  function handleCreateDiscussion(e) {
    if (e) e.preventDefault();

    const titleInput = document.getElementById('disc-title-input');
    const catSelect = document.getElementById('disc-category-select');
    const probSelect = document.getElementById('disc-problem-select');
    const contentInput = document.getElementById('disc-content-input');

    const title = (titleInput ? titleInput.value : '').trim();
    const content = (contentInput ? contentInput.value : '').trim();
    const category = catSelect ? catSelect.value : 'Solutions';

    if (!title) {
      if (window.CodeForgeApp) CodeForgeApp.showToast('Please enter a discussion title', 'warn');
      return;
    }
    if (!content) {
      if (window.CodeForgeApp) CodeForgeApp.showToast('Please enter your post content', 'warn');
      return;
    }

    const probVal = probSelect ? probSelect.value : '';
    let problemId = null;
    let problemTitle = null;
    if (probVal) {
      problemId = parseInt(probVal, 10);
      const found = window.CodeForgeData && window.CodeForgeData.problems ? window.CodeForgeData.problems.find(p => p.id === problemId) : null;
      problemTitle = found ? found.title : `Problem #${problemId}`;
    }

    // Determine current user
    let authorName = 'Alex Morgan';
    let authorInitials = 'AM';
    let authorRole = 'Learner';
    try {
      const stored = localStorage.getItem('codeforge_user') || sessionStorage.getItem('codeforge_user');
      if (stored) {
        const u = JSON.parse(stored);
        if (u.fullName) {
          authorName = u.fullName;
          const parts = u.fullName.trim().split(/\s+/);
          authorInitials = parts.length > 1 ? (parts[0][0] + parts[parts.length - 1][0]).toUpperCase() : parts[0][0].toUpperCase();
        }
        if (u.roles && u.roles.includes('ADMIN')) authorRole = 'Admin';
      }
    } catch (_) {}

    const newPost = {
      id: 'post-' + Date.now(),
      title,
      authorName,
      authorInitials,
      authorRole,
      category,
      problemId,
      problemTitle,
      content,
      upvotes: 1,
      isUpvoted: true,
      repliesCount: 0,
      createdAt: 'Just now',
      replies: []
    };

    posts.unshift(newPost);
    saveDiscussionsToStorage();
    closeCreateModal();
    renderDiscussions();
    if (window.CodeForgeApp) CodeForgeApp.showToast('Discussion published!', 'success');
  }

  function openViewModal(postId) {
    const post = posts.find(p => p.id === postId);
    if (!post) return;

    currentViewingPostId = postId;
    const modal = document.getElementById('discussion-view-modal');
    if (!modal) return;

    // Header Meta
    const catBadge = document.getElementById('view-post-cat-badge');
    const titleEl = document.getElementById('view-post-title');
    const authorAvatar = document.getElementById('view-post-author-avatar');
    const authorName = document.getElementById('view-post-author-name');
    const authorRole = document.getElementById('view-post-author-role');
    const timeEl = document.getElementById('view-post-time');
    const probContainer = document.getElementById('view-post-problem-container');
    const bodyEl = document.getElementById('view-post-body');

    if (catBadge) {
      catBadge.textContent = post.category;
      catBadge.className = `px-2.5 py-0.5 rounded text-xs font-semibold border ${getCategoryColor(post.category)}`;
    }
    if (titleEl) titleEl.textContent = post.title;
    if (authorAvatar) authorAvatar.textContent = post.authorInitials || 'U';
    if (authorName) authorName.textContent = post.authorName || 'Anonymous';
    if (authorRole) authorRole.innerHTML = getRoleBadge(post.authorRole);
    if (timeEl) timeEl.textContent = post.createdAt || 'Recently';
    if (bodyEl) bodyEl.innerHTML = formatContent(post.content);

    // Problem badge
    if (probContainer) {
      if (post.problemId) {
        probContainer.innerHTML = `
          <a href="#solve/${post.problemId}" onclick="CodeForgeCommunity.closeViewModal()" class="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-md text-xs font-semibold bg-emerald-50 hover:bg-emerald-100 text-emerald-800 border border-emerald-200 transition">
            <svg class="w-3.5 h-3.5 text-emerald-600" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M10 20l4-16m4 4l4 4-4 4M6 16l-4-4 4-4"/></svg>
            <span>Linked Problem: ${escapeHtml(post.problemTitle || `#${post.problemId}`)}</span>
          </a>
        `;
        probContainer.classList.remove('hidden');
      } else {
        probContainer.innerHTML = '';
        probContainer.classList.add('hidden');
      }
    }

    updateModalUpvoteUI(post);
    renderRepliesList(post);

    modal.classList.remove('hidden');
    modal.classList.add('flex');
  }

  function closeViewModal() {
    const modal = document.getElementById('discussion-view-modal');
    if (modal) {
      modal.classList.add('hidden');
      modal.classList.remove('flex');
    }
    currentViewingPostId = null;
  }

  function renderRepliesList(post) {
    const list = document.getElementById('disc-replies-list');
    const countEl = document.getElementById('view-post-replies-count');
    if (!list) return;

    const replies = post.replies || [];
    if (countEl) countEl.textContent = `${replies.length} ${replies.length === 1 ? 'Reply' : 'Replies'}`;

    if (replies.length === 0) {
      list.innerHTML = `
        <div class="text-center py-6 text-xs text-gray-400 bg-gray-50 rounded-xl border border-gray-100">
          No replies yet. Be the first to add your perspective!
        </div>
      `;
      return;
    }

    list.innerHTML = replies.map(r => `
      <div class="flex items-start gap-3 p-3.5 rounded-xl bg-gray-50/80 border border-gray-200/60 space-y-1">
        <div class="w-7 h-7 rounded-full bg-emerald-800 text-white font-mono text-[11px] font-bold flex items-center justify-center shrink-0 mt-0.5">
          ${escapeHtml(r.authorInitials || 'U')}
        </div>
        <div class="flex-1 min-w-0 space-y-1">
          <div class="flex items-center gap-2">
            <span class="text-xs font-bold text-gray-900">${escapeHtml(r.authorName)}</span>
            ${getRoleBadge(r.authorRole)}
            <span class="text-[11px] text-gray-400 font-medium">${escapeHtml(r.createdAt || 'Recently')}</span>
          </div>
          <div class="text-xs text-gray-700 leading-relaxed font-sans">
            ${formatContent(r.content)}
          </div>
        </div>
      </div>
    `).join('');
  }

  function handleSubmitReply(e) {
    if (e) e.preventDefault();
    if (!currentViewingPostId) return;

    const post = posts.find(p => p.id === currentViewingPostId);
    if (!post) return;

    const input = document.getElementById('disc-reply-input');
    const content = (input ? input.value : '').trim();
    if (!content) {
      if (window.CodeForgeApp) CodeForgeApp.showToast('Please type a reply before submitting', 'warn');
      return;
    }

    let authorName = 'Alex Morgan';
    let authorInitials = 'AM';
    let authorRole = 'Learner';
    try {
      const stored = localStorage.getItem('codeforge_user') || sessionStorage.getItem('codeforge_user');
      if (stored) {
        const u = JSON.parse(stored);
        if (u.fullName) {
          authorName = u.fullName;
          const parts = u.fullName.trim().split(/\s+/);
          authorInitials = parts.length > 1 ? (parts[0][0] + parts[parts.length - 1][0]).toUpperCase() : parts[0][0].toUpperCase();
        }
        if (u.roles && u.roles.includes('ADMIN')) authorRole = 'Admin';
      }
    } catch (_) {}

    if (!post.replies) post.replies = [];
    const newReply = {
      id: 'rep-' + Date.now(),
      authorName,
      authorInitials,
      authorRole,
      content,
      createdAt: 'Just now',
      upvotes: 0
    };

    post.replies.push(newReply);
    post.repliesCount = post.replies.length;
    saveDiscussionsToStorage();

    if (input) input.value = '';
    renderRepliesList(post);
    renderDiscussions();
    if (window.CodeForgeApp) CodeForgeApp.showToast('Reply added!', 'success');
  }

  function bindEvents() {
    // Start Discussion Button
    const btnCreate = document.getElementById('btn-create-discussion');
    if (btnCreate) btnCreate.addEventListener('click', openCreateModal);

    // Modal Close and Cancel
    const btnClose = document.getElementById('btn-close-disc-modal');
    if (btnClose) btnClose.addEventListener('click', closeCreateModal);

    const btnCancel = document.getElementById('btn-cancel-disc-modal');
    if (btnCancel) btnCancel.addEventListener('click', closeCreateModal);

    // Discussion Form Submit
    const form = document.getElementById('discussion-form');
    if (form) form.addEventListener('submit', handleCreateDiscussion);

    // Search Input
    const searchInput = document.getElementById('community-search-input');
    if (searchInput) {
      searchInput.addEventListener('input', (e) => {
        searchQuery = e.target.value;
        renderDiscussions();
      });
    }

    // Category Tabs
    const catContainer = document.getElementById('community-category-tabs');
    if (catContainer) {
      catContainer.addEventListener('click', (e) => {
        const btn = e.target.closest('.comm-cat-btn');
        if (!btn) return;
        const category = btn.getAttribute('data-category');
        if (category) filterByCategory(category);
      });
    }

    // View Modal Close
    const btnCloseView = document.getElementById('btn-close-disc-view-modal');
    if (btnCloseView) btnCloseView.addEventListener('click', closeViewModal);

    // View Modal Upvote Button
    const btnViewUpvote = document.getElementById('view-post-upvote-btn');
    if (btnViewUpvote) {
      btnViewUpvote.addEventListener('click', () => {
        if (currentViewingPostId) toggleUpvote(currentViewingPostId);
      });
    }

    // View Modal Reply Form
    const replyForm = document.getElementById('disc-reply-form');
    if (replyForm) replyForm.addEventListener('submit', handleSubmitReply);

    // Close on backdrop clicks
    const modalCreate = document.getElementById('discussion-modal');
    if (modalCreate) {
      modalCreate.addEventListener('click', (e) => {
        if (e.target === modalCreate) closeCreateModal();
      });
    }

    const modalView = document.getElementById('discussion-view-modal');
    if (modalView) {
      modalView.addEventListener('click', (e) => {
        if (e.target === modalView) closeViewModal();
      });
    }
  }

  function init() {
    loadDiscussions();
    populateProblemSelect();
    bindEvents();
    renderDiscussions();
  }

  return {
    init,
    renderDiscussions,
    openCreateModal,
    closeCreateModal,
    openViewModal,
    closeViewModal,
    toggleUpvote,
    filterByCategory
  };
})();
