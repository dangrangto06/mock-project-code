// CodeForge Challenge Creator & Markdown Editor

window.CodeForgeEditor = (function () {
  let isPreviewMode = false;
  let activeDifficulty = 'Easy';
  let tags = ['Arrays', 'Hash map'];

  function init() {
    bindEvents();
    renderTags();
  }

  function renderTags() {
    const container = document.getElementById('editor-tag-chips');
    if (!container) return;

    container.innerHTML = tags.map(tag => `
      <span class="inline-flex items-center gap-1 px-2.5 py-1 rounded-md text-xs font-medium bg-emerald-50 text-emerald-800 border border-emerald-200">
        ${tag}
        <button type="button" data-tag="${tag}" class="btn-remove-tag text-emerald-600 hover:text-emerald-900 focus:outline-none">
          <svg class="w-3 h-3" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"></path></svg>
        </button>
      </span>
    `).join('');

    // Attach click listeners to remove tag buttons
    container.querySelectorAll('.btn-remove-tag').forEach(btn => {
      btn.addEventListener('click', (e) => {
        const tagToRemove = btn.getAttribute('data-tag');
        tags = tags.filter(t => t !== tagToRemove);
        renderTags();
      });
    });
  }

  function addTag(tagName) {
    const trimmed = tagName.trim();
    if (trimmed && !tags.includes(trimmed) && tags.length < 5) {
      tags.push(trimmed);
      renderTags();
    }
  }

  function renderMarkdownPreview(rawMarkdown) {
    // Lightweight markdown parser for challenge statement preview
    let html = rawMarkdown
      .replace(/&/g, '&amp;')
      .replace(/</g, '&lt;')
      .replace(/>/g, '&gt;')
      .replace(/^### (.*$)/gim, '<h3 class="text-base font-semibold text-gray-900 mt-4 mb-2">$1</h3>')
      .replace(/^## (.*$)/gim, '<h2 class="text-lg font-bold text-gray-900 mt-4 mb-2">$1</h2>')
      .replace(/^# (.*$)/gim, '<h1 class="text-xl font-bold text-gray-900 mt-4 mb-2">$1</h1>')
      .replace(/\*\*(.*)\*\*/gim, '<strong class="font-semibold text-gray-900">$1</strong>')
      .replace(/\*(.*)\*/gim, '<em class="italic">$1</em>')
      .replace(/`([^`]+)`/gim, '<code class="px-1.5 py-0.5 rounded bg-gray-100 font-mono text-xs text-emerald-700 border border-gray-200">$1</code>')
      .replace(/^\- (.*$)/gim, '<li class="ml-4 list-disc text-gray-700 text-sm">$1</li>')
      .replace(/\n\n/gim, '<p class="my-2.5 text-sm text-gray-700 leading-relaxed"></p>')
      .replace(/\n/gim, '<br />');

    return html;
  }

  function bindEvents() {
    // Markdown tab toggle Write vs Preview
    const tabWrite = document.getElementById('tab-edit-write');
    const tabPreview = document.getElementById('tab-edit-preview');
    const textarea = document.getElementById('problem-statement-input');
    const previewArea = document.getElementById('problem-statement-preview');

    if (tabWrite && tabPreview && textarea && previewArea) {
      tabWrite.addEventListener('click', () => {
        isPreviewMode = false;
        tabWrite.className = "px-3 py-1 text-xs font-semibold rounded bg-white text-gray-800 shadow-sm border border-gray-200";
        tabPreview.className = "px-3 py-1 text-xs font-medium text-gray-500 hover:text-gray-800";
        textarea.classList.remove('hidden');
        previewArea.classList.add('hidden');
      });

      tabPreview.addEventListener('click', () => {
        isPreviewMode = true;
        tabPreview.className = "px-3 py-1 text-xs font-semibold rounded bg-white text-gray-800 shadow-sm border border-gray-200";
        tabWrite.className = "px-3 py-1 text-xs font-medium text-gray-500 hover:text-gray-800";
        previewArea.innerHTML = renderMarkdownPreview(textarea.value);
        textarea.classList.add('hidden');
        previewArea.classList.remove('hidden');
      });
    }

    // Markdown toolbar action helpers
    const toolbarButtons = {
      'btn-fmt-bold': { wrap: '**' },
      'btn-fmt-italic': { wrap: '*' },
      'btn-fmt-code': { wrap: '`' },
      'btn-fmt-link': { prefix: '[', suffix: '](url)' },
      'btn-fmt-list': { prefix: '\n- ' }
    };

    Object.keys(toolbarButtons).forEach(id => {
      const btn = document.getElementById(id);
      if (btn && textarea) {
        btn.addEventListener('click', () => {
          const cfg = toolbarButtons[id];
          const start = textarea.selectionStart;
          const end = textarea.selectionEnd;
          const selected = textarea.value.substring(start, end);

          let replacement = '';
          if (cfg.wrap) {
            replacement = `${cfg.wrap}${selected || 'text'}${cfg.wrap}`;
          } else if (cfg.prefix && cfg.suffix) {
            replacement = `${cfg.prefix}${selected || 'text'}${cfg.suffix}`;
          } else if (cfg.prefix) {
            replacement = `${cfg.prefix}${selected || 'item'}`;
          }

          textarea.setRangeText(replacement, start, end, 'end');
          textarea.focus();
        });
      }
    });

    // Tag input on Enter or Comma
    const tagInput = document.getElementById('tag-input-field');
    if (tagInput) {
      tagInput.addEventListener('keydown', (e) => {
        if (e.key === 'Enter' || e.key === ',') {
          e.preventDefault();
          addTag(tagInput.value);
          tagInput.value = '';
        }
      });
    }

    // Difficulty buttons toggle
    const diffButtons = document.querySelectorAll('.btn-diff-choice');
    diffButtons.forEach(btn => {
      btn.addEventListener('click', () => {
        diffButtons.forEach(b => {
          b.className = "btn-diff-choice flex-1 py-1.5 text-xs font-medium rounded-md text-gray-600 bg-white border border-gray-200 hover:bg-gray-50 text-center transition";
        });
        const choice = btn.getAttribute('data-diff');
        activeDifficulty = choice;
        if (choice === 'Easy') {
          btn.className = "btn-diff-choice flex-1 py-1.5 text-xs font-medium rounded-md bg-emerald-50 text-emerald-800 border border-emerald-300 font-semibold text-center";
        } else if (choice === 'Medium') {
          btn.className = "btn-diff-choice flex-1 py-1.5 text-xs font-medium rounded-md bg-amber-50 text-amber-800 border border-amber-300 font-semibold text-center";
        } else {
          btn.className = "btn-diff-choice flex-1 py-1.5 text-xs font-medium rounded-md bg-red-50 text-red-800 border border-red-300 font-semibold text-center";
        }
      });
    });

    // Save Draft Button
    const saveDraftBtn = document.getElementById('btn-save-draft');
    if (saveDraftBtn) {
      saveDraftBtn.addEventListener('click', () => {
        saveDraftBtn.innerHTML = `
          <svg class="animate-spin w-3.5 h-3.5 text-gray-600" fill="none" viewBox="0 0 24 24"><circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle><path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path></svg>
          Saving...
        `;
        setTimeout(() => {
          saveDraftBtn.innerHTML = `Save draft`;
          const saveStatus = document.getElementById('editor-save-status');
          if (saveStatus) saveStatus.textContent = "Last saved just now";
          CodeForgeApp.showToast("Draft saved successfully!", "info");
        }, 500);
      });
    }

    // Publish Changes Button
    const publishBtn = document.getElementById('btn-publish-changes');
    if (publishBtn) {
      publishBtn.addEventListener('click', () => {
        publishBtn.innerHTML = `
          <svg class="animate-spin w-4 h-4 text-white" fill="none" viewBox="0 0 24 24"><circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle><path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path></svg>
          Publishing...
        `;
        setTimeout(() => {
          publishBtn.innerHTML = `<svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7"></path></svg> Published`;
          CodeForgeApp.showToast("Problem published to the CodeForge library!", "success");
          setTimeout(() => {
            publishBtn.innerHTML = `↗ Publish changes`;
          }, 2000);
        }, 700);
      });
    }

    // Top Preview button opens the problem solving IDE
    const previewProblemBtn = document.getElementById('btn-preview-problem');
    if (previewProblemBtn) {
      previewProblemBtn.addEventListener('click', () => {
        CodeForgeApp.navigate('ide');
      });
    }
  }

  return {
    init
  };
})();

