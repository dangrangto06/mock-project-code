// CodeForge Learner Notes Module
// Handles personal notes, algorithm templates, takeaway sheets, local persistence and search/filter.

window.CodeForgeNotes = (function () {
  const STORAGE_KEY = 'codeforge_notes';
  let notes = [];
  let activeTag = 'all';
  let searchQuery = '';
  let editingNoteId = null;

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
      if (isNaN(d.getTime())) return 'Recently';
      return d.toLocaleDateString('en-US', { month: 'short', day: 'numeric', year: 'numeric' });
    } catch (_) {
      return 'Recently';
    }
  }

  function formatContent(text) {
    if (!text) return '';
    // Format basic markdown: inline code `...`, bold **...**, newlines, list items
    let safe = escapeHtml(text);
    // Inline code `...`
    safe = safe.replace(/`([^`]+)`/g, '<code class="px-1.5 py-0.5 rounded bg-gray-100 text-emerald-800 font-mono text-[11px] border border-gray-200">$1</code>');
    // Bold **...**
    safe = safe.replace(/\*\*([^*]+)\*\*/g, '<strong class="font-semibold text-gray-900">$1</strong>');
    // Numbered lists 1. 2. 3.
    safe = safe.replace(/^(\d+\.\s+)(.+)$/gm, '<div class="flex items-start gap-1.5 my-1"><span class="font-bold text-emerald-700 text-[11px]">$1</span><span>$2</span></div>');
    // Unordered lists -
    safe = safe.replace(/^(-\s+)(.+)$/gm, '<div class="flex items-start gap-1.5 my-1"><span class="text-emerald-600 text-xs">•</span><span>$2</span></div>');
    // Paragraph breaks
    safe = safe.replace(/\n\n/g, '<div class="h-2"></div>');
    return safe;
  }

  function loadNotes() {
    try {
      const stored = localStorage.getItem(STORAGE_KEY);
      if (stored) {
        notes = JSON.parse(stored);
      } else {
        notes = (window.CodeForgeData && window.CodeForgeData.initialNotes) ? [...window.CodeForgeData.initialNotes] : [];
        saveNotesToStorage();
      }
    } catch (err) {
      console.warn('Failed to load notes from localStorage:', err);
      notes = (window.CodeForgeData && window.CodeForgeData.initialNotes) ? [...window.CodeForgeData.initialNotes] : [];
    }
  }

  function saveNotesToStorage() {
    try {
      localStorage.setItem(STORAGE_KEY, JSON.stringify(notes));
    } catch (err) {
      console.error('Failed to save notes to localStorage:', err);
    }
    updateSidebarCount();
  }

  function updateSidebarCount() {
    const badge = document.getElementById('sb-notes-count');
    if (badge) {
      badge.textContent = notes.length;
    }
  }

  function populateProblemSelect() {
    const select = document.getElementById('note-problem-select');
    if (!select) return;

    select.innerHTML = '<option value="">None / General Algorithmic Pattern</option>';
    if (window.CodeForgeData && Array.isArray(window.CodeForgeData.problems)) {
      window.CodeForgeData.problems.forEach(p => {
        const opt = document.createElement('option');
        opt.value = p.id;
        opt.textContent = `#${p.id} ${p.title} (${p.difficulty})`;
        select.appendChild(opt);
      });
    }
  }

  function renderNotes() {
    const grid = document.getElementById('notes-grid');
    const emptyState = document.getElementById('notes-empty-state');
    if (!grid) return;

    // Filter notes
    const q = searchQuery.trim().toLowerCase();
    const filtered = notes.filter(n => {
      // Tag filter
      if (activeTag !== 'all') {
        const hasTag = n.tags && n.tags.some(t => t.toLowerCase() === activeTag.toLowerCase());
        if (!hasTag) return false;
      }
      // Search query
      if (q) {
        const inTitle = (n.title || '').toLowerCase().includes(q);
        const inContent = (n.content || '').toLowerCase().includes(q);
        const inTags = (n.tags || []).some(t => t.toLowerCase().includes(q));
        const inProblem = (n.problemTitle || '').toLowerCase().includes(q);
        if (!inTitle && !inContent && !inTags && !inProblem) return false;
      }
      return true;
    });

    if (filtered.length === 0) {
      grid.classList.add('hidden');
      if (emptyState) emptyState.classList.remove('hidden');
      return;
    }

    grid.classList.remove('hidden');
    if (emptyState) emptyState.classList.add('hidden');

    grid.innerHTML = filtered.map(n => {
      const formattedDate = formatDate(n.updatedAt);
      const formattedBody = formatContent(n.content);

      // Problem badge
      let problemBadgeHtml = '';
      if (n.problemId) {
        problemBadgeHtml = `
          <a href="#solve/${n.problemId}" class="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-md text-[11px] font-semibold bg-emerald-50 hover:bg-emerald-100 text-emerald-800 border border-emerald-200 transition group" title="Open in IDE">
            <svg class="w-3.5 h-3.5 text-emerald-600 group-hover:scale-110 transition-transform" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M10 20l4-16m4 4l4 4-4 4M6 16l-4-4 4-4"/></svg>
            <span>${escapeHtml(n.problemTitle || `Problem #${n.problemId}`)}</span>
          </a>
        `;
      } else {
        problemBadgeHtml = `
          <span class="inline-flex items-center gap-1 px-2.5 py-1 rounded-md text-[11px] font-medium bg-gray-100 text-gray-600 border border-gray-200">
            <svg class="w-3.5 h-3.5 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13 10V3L4 14h7v7l9-11h-7z"/></svg>
            <span>General Pattern</span>
          </span>
        `;
      }

      // Tag chips
      const tagsHtml = (n.tags || []).map(tag => `
        <button type="button" onclick="CodeForgeNotes.filterByTag('${escapeHtml(tag)}')" class="inline-flex items-center px-2 py-0.5 rounded text-[11px] font-medium bg-gray-50 text-gray-600 hover:bg-emerald-50 hover:text-emerald-700 border border-gray-200 transition">
          #${escapeHtml(tag)}
        </button>
      `).join('');

      return `
        <div class="bg-white rounded-xl border border-[#e5eae7] p-5 shadow-xs hover:shadow-md transition flex flex-col justify-between space-y-4 group">
          <div class="space-y-3">
            <!-- Header Row -->
            <div class="flex items-center justify-between gap-2">
              <div>${problemBadgeHtml}</div>
              <div class="flex items-center gap-2">
                <span class="text-[11px] text-gray-400 font-medium">${formattedDate}</span>
                <div class="flex items-center gap-1 opacity-80 group-hover:opacity-100 transition">
                  <button onclick="CodeForgeNotes.openEditModal('${n.id}')" class="p-1 rounded hover:bg-gray-100 text-gray-500 hover:text-gray-900 transition" title="Edit Note">
                    <svg class="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z"/></svg>
                  </button>
                  <button onclick="CodeForgeNotes.deleteNote('${n.id}')" class="p-1 rounded hover:bg-red-50 text-gray-400 hover:text-red-600 transition" title="Delete Note">
                    <svg class="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16"/></svg>
                  </button>
                </div>
              </div>
            </div>

            <!-- Title -->
            <h3 class="text-base font-bold text-gray-900 group-hover:text-emerald-900 transition leading-snug">
              ${escapeHtml(n.title)}
            </h3>

            <!-- Content preview -->
            <div class="text-xs text-gray-700 leading-relaxed font-sans space-y-1">
              ${formattedBody}
            </div>
          </div>

          <!-- Tags Footer -->
          <div class="pt-3 border-t border-gray-100 flex flex-wrap items-center gap-1.5">
            ${tagsHtml}
          </div>
        </div>
      `;
    }).join('');
  }

  function filterByTag(tag) {
    activeTag = tag;
    const buttons = document.querySelectorAll('.notes-tag-btn');
    buttons.forEach(btn => {
      const bTag = btn.getAttribute('data-tag');
      if (bTag === tag || (tag !== 'all' && bTag.toLowerCase() === tag.toLowerCase())) {
        btn.className = 'notes-tag-btn px-2.5 py-1 rounded-md text-xs font-semibold bg-emerald-50 text-emerald-800 border border-emerald-200 transition';
      } else {
        btn.className = 'notes-tag-btn px-2.5 py-1 rounded-md text-xs font-medium text-gray-600 hover:bg-gray-100 transition';
      }
    });
    renderNotes();
  }

  function openCreateModal() {
    editingNoteId = null;
    const modal = document.getElementById('note-modal');
    const modalTitle = document.getElementById('note-modal-title');
    const titleInput = document.getElementById('note-title-input');
    const problemSelect = document.getElementById('note-problem-select');
    const tagsInput = document.getElementById('note-tags-input');
    const contentInput = document.getElementById('note-content-input');

    if (!modal) return;
    if (modalTitle) modalTitle.textContent = 'Create New Note';
    if (titleInput) titleInput.value = '';
    if (problemSelect) problemSelect.value = '';
    if (tagsInput) tagsInput.value = '';
    if (contentInput) contentInput.value = '';

    modal.classList.remove('hidden');
    modal.classList.add('flex');
    if (titleInput) titleInput.focus();
  }

  function openEditModal(noteId) {
    const note = notes.find(n => n.id === noteId);
    if (!note) return;

    editingNoteId = noteId;
    const modal = document.getElementById('note-modal');
    const modalTitle = document.getElementById('note-modal-title');
    const titleInput = document.getElementById('note-title-input');
    const problemSelect = document.getElementById('note-problem-select');
    const tagsInput = document.getElementById('note-tags-input');
    const contentInput = document.getElementById('note-content-input');

    if (!modal) return;
    if (modalTitle) modalTitle.textContent = 'Edit Note';
    if (titleInput) titleInput.value = note.title || '';
    if (problemSelect) problemSelect.value = note.problemId ? String(note.problemId) : '';
    if (tagsInput) tagsInput.value = (note.tags || []).join(', ');
    if (contentInput) contentInput.value = note.content || '';

    modal.classList.remove('hidden');
    modal.classList.add('flex');
    if (titleInput) titleInput.focus();
  }

  function closeModal() {
    const modal = document.getElementById('note-modal');
    if (modal) {
      modal.classList.add('hidden');
      modal.classList.remove('flex');
    }
    editingNoteId = null;
  }

  function handleSaveNote(e) {
    if (e) e.preventDefault();

    const titleInput = document.getElementById('note-title-input');
    const problemSelect = document.getElementById('note-problem-select');
    const tagsInput = document.getElementById('note-tags-input');
    const contentInput = document.getElementById('note-content-input');

    const title = (titleInput ? titleInput.value : '').trim();
    const content = (contentInput ? contentInput.value : '').trim();
    if (!title) {
      if (window.CodeForgeApp) CodeForgeApp.showToast('Please enter a note title', 'warn');
      return;
    }

    const problemIdVal = problemSelect ? problemSelect.value : '';
    let problemId = null;
    let problemTitle = null;
    if (problemIdVal) {
      problemId = parseInt(problemIdVal, 10);
      const found = window.CodeForgeData && window.CodeForgeData.problems ? window.CodeForgeData.problems.find(p => p.id === problemId) : null;
      problemTitle = found ? found.title : `Problem #${problemId}`;
    }

    const rawTags = tagsInput ? tagsInput.value : '';
    const tags = rawTags.split(',').map(t => t.trim()).filter(Boolean);

    if (editingNoteId) {
      // Update existing
      const idx = notes.findIndex(n => n.id === editingNoteId);
      if (idx !== -1) {
        notes[idx] = {
          ...notes[idx],
          title,
          problemId,
          problemTitle,
          tags: tags.length > 0 ? tags : ['Notes'],
          content,
          updatedAt: new Date().toISOString()
        };
      }
      if (window.CodeForgeApp) CodeForgeApp.showToast('Note updated successfully!', 'success');
    } else {
      // Create new
      const newNote = {
        id: 'note-' + Date.now(),
        title,
        problemId,
        problemTitle,
        tags: tags.length > 0 ? tags : ['Notes'],
        content,
        updatedAt: new Date().toISOString()
      };
      notes.unshift(newNote);
      if (window.CodeForgeApp) CodeForgeApp.showToast('Note created successfully!', 'success');
    }

    saveNotesToStorage();
    closeModal();
    renderNotes();
  }

  function deleteNote(noteId) {
    const note = notes.find(n => n.id === noteId);
    const title = note ? note.title : 'this note';
    if (!confirm(`Are you sure you want to delete "${title}"?`)) {
      return;
    }

    notes = notes.filter(n => n.id !== noteId);
    saveNotesToStorage();
    renderNotes();
    if (window.CodeForgeApp) CodeForgeApp.showToast('Note deleted.', 'info');
  }

  function bindEvents() {
    // New Note button
    const btnCreate = document.getElementById('btn-create-note');
    if (btnCreate) btnCreate.addEventListener('click', openCreateModal);

    // Modal Close and Cancel buttons
    const btnClose = document.getElementById('btn-close-note-modal');
    if (btnClose) btnClose.addEventListener('click', closeModal);

    const btnCancel = document.getElementById('btn-cancel-note-modal');
    if (btnCancel) btnCancel.addEventListener('click', closeModal);

    // Modal Form submit
    const noteForm = document.getElementById('note-form');
    if (noteForm) {
      noteForm.addEventListener('submit', handleSaveNote);
    }

    // Search input
    const searchInput = document.getElementById('notes-search-input');
    if (searchInput) {
      searchInput.addEventListener('input', (e) => {
        searchQuery = e.target.value;
        renderNotes();
      });
    }

    // Tag filter chips
    const tagContainer = document.getElementById('notes-tag-filters');
    if (tagContainer) {
      tagContainer.addEventListener('click', (e) => {
        const btn = e.target.closest('.notes-tag-btn');
        if (!btn) return;
        const tag = btn.getAttribute('data-tag');
        if (tag) filterByTag(tag);
      });
    }

    // Close modal on backdrop click
    const modal = document.getElementById('note-modal');
    if (modal) {
      modal.addEventListener('click', (e) => {
        if (e.target === modal) closeModal();
      });
    }
  }

  function init() {
    loadNotes();
    populateProblemSelect();
    bindEvents();
    renderNotes();
    updateSidebarCount();
  }

  return {
    init,
    renderNotes,
    openCreateModal,
    openEditModal,
    deleteNote,
    filterByTag,
    closeModal
  };
})();
