// CodeForge IDE & Test Runner Controller

window.CodeForgeIDE = (function () {
  let timerSeconds = 12 * 60 + 48; // 00:12:48
  let timerInterval = null;
  let activeCase = 1;
  let activeConsoleTab = 'results'; // 'cases' | 'results'
  let currentLanguage = 'python';

  function init() {
    startTimer();
    bindEvents();
    renderCodeEditor();
    renderTestCaseView();
  }

  function startTimer() {
    if (timerInterval) clearInterval(timerInterval);
    const timerEl = document.getElementById('ide-timer');
    timerInterval = setInterval(() => {
      timerSeconds++;
      const hrs = Math.floor(timerSeconds / 3600);
      const mins = Math.floor((timerSeconds % 3600) / 60);
      const secs = timerSeconds % 60;
      const formatted = [
        String(hrs).padStart(2, '0'),
        String(mins).padStart(2, '0'),
        String(secs).padStart(2, '0')
      ].join(':');
      if (timerEl) timerEl.textContent = formatted;
    }, 1000);
  }

  function renderCodeEditor() {
    const rawCode = CodeForgeData.problems[0].starterCode[currentLanguage] || CodeForgeData.problems[0].starterCode.python;
    const lines = rawCode.split('\n');
    
    const linesContainer = document.getElementById('editor-line-numbers');
    const codeContainer = document.getElementById('editor-code-content');
    const textarea = document.getElementById('editor-real-input');

    if (!linesContainer || !codeContainer) return;

    if (textarea && textarea.value !== rawCode) {
      textarea.value = rawCode;
    }

    let lineNumbersHtml = '';
    let codeLinesHtml = '';

    lines.forEach((line, idx) => {
      const lineNum = idx + 1;
      const isActive = lineNum === 9; // Line 9 active highlight as in Image 1
      lineNumbersHtml += `<div class="editor-line-number ${isActive ? 'text-emerald-400 font-medium' : ''}">${lineNum}</div>`;
      
      const highlighted = highlightPythonLine(line);
      codeLinesHtml += `<div class="px-3 leading-[1.6rem] font-mono text-xs md:text-sm whitespace-pre ${isActive ? 'editor-line-active' : ''}">${highlighted || ' '}</div>`;
    });

    linesContainer.innerHTML = lineNumbersHtml;
    codeContainer.innerHTML = codeLinesHtml;

    const statsEl = document.getElementById('editor-cursor-stats');
    if (statsEl) {
      const langName = currentLanguage === 'python' ? 'Python 3.12' : currentLanguage === 'javascript' ? 'Node.js 20' : 'C++ 20';
      statsEl.textContent = `Spaces: 4 · UTF-8 · ${langName}`;
    }
  }

  function highlightPythonLine(line) {
    if (!line) return '';
    // Escape HTML first
    let escaped = line
      .replace(/&/g, '&amp;')
      .replace(/</g, '&lt;')
      .replace(/>/g, '&gt;');

    // Comments
    if (escaped.trim().startsWith('#')) {
      return `<span class="syn-comment">${escaped}</span>`;
    }

    // Keyword & def matches
    escaped = escaped.replace(/\b(def|class|return|if|in|for|while|import|from|as|else|elif|try|except)\b/g, '<span class="syn-keyword">$1</span>');
    escaped = escaped.replace(/\b(enumerate|len|range|print|int|str|list|dict|set)\b/g, '<span class="syn-type">$1</span>');
    escaped = escaped.replace(/(#[^<]*)/g, '<span class="syn-comment">$1</span>');
    escaped = escaped.replace(/(\b\d+\b)/g, '<span class="syn-number">$1</span>');

    return escaped;
  }

  function renderTestCaseView() {
    const problem = CodeForgeData.problems[0];
    const caseData = problem.testCases.find(c => c.id === activeCase) || problem.testCases[0];

    // Update case selector tabs
    const case1Btn = document.getElementById('case-btn-1');
    const case2Btn = document.getElementById('case-btn-2');
    
    if (case1Btn && case2Btn) {
      if (activeCase === 1) {
        case1Btn.className = "px-3 py-1 text-xs font-medium rounded-md bg-emerald-50 text-emerald-700 border border-emerald-200 flex items-center gap-1.5 shadow-sm";
        case2Btn.className = "px-3 py-1 text-xs font-medium rounded-md bg-gray-50 text-gray-600 hover:bg-gray-100 border border-gray-200 flex items-center gap-1.5";
      } else {
        case1Btn.className = "px-3 py-1 text-xs font-medium rounded-md bg-gray-50 text-gray-600 hover:bg-gray-100 border border-gray-200 flex items-center gap-1.5";
        case2Btn.className = "px-3 py-1 text-xs font-medium rounded-md bg-emerald-50 text-emerald-700 border border-emerald-200 flex items-center gap-1.5 shadow-sm";
      }
    }

    // Inputs & outputs
    const inputValEl = document.getElementById('test-case-input');
    const outputValEl = document.getElementById('test-case-output');
    const expectedValEl = document.getElementById('test-case-expected');

    if (inputValEl) inputValEl.textContent = caseData.input;
    if (outputValEl) outputValEl.textContent = caseData.yourOutput;
    if (expectedValEl) expectedValEl.textContent = caseData.expected;
  }

  function bindEvents() {
    // Run Code Button
    const runBtn = document.getElementById('btn-run-code');
    if (runBtn) {
      runBtn.addEventListener('click', runCode);
    }

    // Submit Solution Button
    const submitBtn = document.getElementById('btn-submit-solution');
    if (submitBtn) {
      submitBtn.addEventListener('click', submitSolution);
    }

    // Focus Mode Toggle
    const focusBtn = document.getElementById('btn-focus-mode');
    if (focusBtn) {
      focusBtn.addEventListener('click', () => {
        document.body.classList.toggle('focus-mode');
        const isFocus = document.body.classList.contains('focus-mode');
        focusBtn.innerHTML = isFocus
          ? `<svg class="w-4 h-4 text-emerald-600" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"></path></svg> Exit Focus`
          : `<svg class="w-4 h-4 text-gray-500" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 8V4m0 0h4M4 4l5 5m11-1V4m0 0h-4m4 0l-5 5M4 16v4m0 0h4m-4 0l5-5m11 5l-5-5m5 5v-4m0 4h-4"></path></svg> Focus mode`;
      });
    }

    // Case Buttons
    const case1Btn = document.getElementById('case-btn-1');
    const case2Btn = document.getElementById('case-btn-2');
    if (case1Btn) case1Btn.addEventListener('click', () => { activeCase = 1; renderTestCaseView(); });
    if (case2Btn) case2Btn.addEventListener('click', () => { activeCase = 2; renderTestCaseView(); });

    // Console Tabs (Test cases vs Test results)
    const tabCases = document.getElementById('tab-test-cases');
    const tabResults = document.getElementById('tab-test-results');
    const casesView = document.getElementById('view-test-cases');
    const resultsView = document.getElementById('view-test-results');

    if (tabCases && tabResults) {
      tabCases.addEventListener('click', () => {
        activeConsoleTab = 'cases';
        tabCases.className = "pb-2 border-b-2 border-emerald-600 text-emerald-700 font-medium text-xs flex items-center gap-1.5 cursor-pointer";
        tabResults.className = "pb-2 border-b-2 border-transparent text-gray-500 hover:text-gray-700 text-xs flex items-center gap-1.5 cursor-pointer";
        if (casesView) casesView.classList.remove('hidden');
        if (resultsView) resultsView.classList.add('hidden');
      });

      tabResults.addEventListener('click', () => {
        activeConsoleTab = 'results';
        tabResults.className = "pb-2 border-b-2 border-emerald-600 text-emerald-700 font-medium text-xs flex items-center gap-1.5 cursor-pointer";
        tabCases.className = "pb-2 border-b-2 border-transparent text-gray-500 hover:text-gray-700 text-xs flex items-center gap-1.5 cursor-pointer";
        if (resultsView) resultsView.classList.remove('hidden');
        if (casesView) casesView.classList.add('hidden');
      });
    }

    // Language dropdown change
    const langSelect = document.getElementById('ide-language-select');
    if (langSelect) {
      langSelect.addEventListener('change', (e) => {
        currentLanguage = e.target.value;
        renderCodeEditor();
      });
    }

    // Live code textarea typing
    const textarea = document.getElementById('editor-real-input');
    if (textarea) {
      textarea.addEventListener('input', () => {
        CodeForgeData.problems[0].starterCode[currentLanguage] = textarea.value;
        renderCodeEditor();
      });
    }

    // Keyboard shortcut Cmd/Ctrl + Enter to run
    window.addEventListener('keydown', (e) => {
      if ((e.metaKey || e.ctrlKey) && e.key === 'Enter') {
        const ideContainer = document.getElementById('page-ide');
        if (ideContainer && !ideContainer.classList.contains('hidden')) {
          e.preventDefault();
          runCode();
        }
      }
    });
  }

  function runCode() {
    const runBtn = document.getElementById('btn-run-code');
    const originalContent = runBtn.innerHTML;

    runBtn.disabled = true;
    runBtn.innerHTML = `
      <svg class="animate-spin w-3.5 h-3.5 text-gray-700" fill="none" viewBox="0 0 24 24">
        <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
        <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
      </svg>
      <span>Running tests...</span>
    `;

    setTimeout(() => {
      runBtn.disabled = false;
      runBtn.innerHTML = originalContent;

      // Ensure test results tab is active
      const tabResults = document.getElementById('tab-test-results');
      if (tabResults) tabResults.click();

      // Show toast notification
      CodeForgeApp.showToast("All 2 sample test cases passed in 32ms!", "success");
    }, 700);
  }

  function submitSolution() {
    const submitBtn = document.getElementById('btn-submit-solution');
    const originalContent = submitBtn.innerHTML;

    submitBtn.disabled = true;
    submitBtn.innerHTML = `
      <svg class="animate-spin w-4 h-4 text-white" fill="none" viewBox="0 0 24 24">
        <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
        <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
      </svg>
      <span>Judging 35 tests...</span>
    `;

    setTimeout(() => {
      submitBtn.disabled = false;
      submitBtn.innerHTML = originalContent;

      // Trigger Celebration Modal
      showSuccessModal();
    }, 1100);
  }

  function showSuccessModal() {
    const modal = document.getElementById('submission-success-modal');
    if (modal) {
      modal.classList.remove('hidden');
      modal.classList.add('flex');
    }
  }

  return {
    init,
    runCode,
    submitSolution
  };
})();

