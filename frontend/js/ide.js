/**
 * @file ide.js
 * @description CodeForge In-Browser IDE & Interactive Test Runner Controller.
 * @module CodeForgeIDE
 *
 * <p>Architectural Role:</p>
 * Orchestrates the full-screen coding workspace view (`#page-ide`). Manages the code editor pane,
 * syntax highlighting, dynamic line numbering, language selection switches, practice session stopwatch,
 * test case tabs (Case 1 vs Case 2), console output panels, focus mode toggle, and code execution /
 * submission actions.
 */

window.CodeForgeIDE = (function () {
  'use strict';

  /** Practice session duration stopwatch counter in seconds (initialized to 12m 48s) */
  let timerSeconds = 12 * 60 + 48;

  /** Interval timer handle for the stopwatch */
  let timerInterval = null;

  /** Currently selected test case ID in the console pane */
  let activeCase = 1;

  /** Active console tab view: 'cases' (Test cases) | 'results' (Test results) */
  let activeConsoleTab = 'results';

  /** Currently active programming language identifier ('python' | 'javascript' | 'java') */
  let currentLanguage = 'python';

  /** Status-bar labels. Java matches the Judge0 runner used by the backend (OpenJDK 13). */
  const LANGUAGE_LABELS = { python: 'Python 3.12', javascript: 'Node.js 20', java: 'Java 13' };
  const FILE_NAMES = { python: 'solution.py', javascript: 'solution.js', java: 'Solution.java' };

  /**
   * Initializes the IDE controller: starts timer, binds UI events, and renders editor & test cases.
   */
  function init() {
    startTimer();
    bindEvents();
    renderCodeEditor();
    renderTestCaseView();
  }

  /**
   * Starts the practice session stopwatch and formats elapsed time into HH:MM:SS.
   */
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

  /**
   * Renders code editor content, line numbers, and active cursor line indicators.
   *
   * <p>Reads the starter code corresponding to the selected language from {@link CodeForgeData},
   * populates the underlying textarea, generates styled line number gutters, and applies
   * syntax highlighting to the visible presentation layer.</p>
   */
  function renderCodeEditor() {
    const rawCode = CodeForgeData.problems[0].starterCode[currentLanguage] || CodeForgeData.problems[0].starterCode.python;
    const lines = rawCode.split('\n');
    
    const linesContainer = document.getElementById('editor-line-numbers');
    const codeContainer = document.getElementById('editor-code-content');
    const textarea = document.getElementById('editor-real-input');

    if (!linesContainer || !codeContainer) return;

    // Sync raw source code into textarea for user input
    if (textarea && textarea.value !== rawCode) {
      textarea.value = rawCode;
    }

    let lineNumbersHtml = '';
    let codeLinesHtml = '';

    // Build line gutters and formatted code DOM
    lines.forEach((line, idx) => {
      const lineNum = idx + 1;
      const isActive = lineNum === 9; // Line 9 active highlight for initial visual focus
      lineNumbersHtml += `<div class="editor-line-number ${isActive ? 'text-emerald-400 font-medium' : ''}">${lineNum}</div>`;
      
      const highlighted = highlightLine(line);
      codeLinesHtml += `<div class="px-3 leading-[1.6rem] font-mono text-xs md:text-sm whitespace-pre ${isActive ? 'editor-line-active' : ''}">${highlighted || ' '}</div>`;
    });

    linesContainer.innerHTML = lineNumbersHtml;
    codeContainer.innerHTML = codeLinesHtml;

    // Update bottom status bar with language and formatting metadata
    const statsEl = document.getElementById('editor-cursor-stats');
    if (statsEl) {
      const langName = LANGUAGE_LABELS[currentLanguage] || currentLanguage;
      statsEl.textContent = `Spaces: 4 · UTF-8 · ${langName}`;
    }
    const fileEl = document.getElementById('editor-filename');
    if (fileEl) fileEl.textContent = FILE_NAMES[currentLanguage] || 'solution.txt';
  }

  /**
   * Applies syntax highlighting spans to a line of code using regular expression matching.
   *
   * <p>Escapes HTML characters to prevent XSS, then highlights language keywords,
   * built-in types, comments, and numeric literals.</p>
   *
   * @param {string} line - Single line of source code.
   * @returns {string} HTML markup with syntax highlighting classes.
   */
  function highlightLine(line) {
    if (!line) return '';
    const isJava = currentLanguage === 'java';
    // Escape HTML characters
    let escaped = line
      .replace(/&/g, '&amp;')
      .replace(/</g, '&lt;')
      .replace(/>/g, '&gt;');

    // Highlight full-line comments
    const commentStart = isJava ? '//' : '#';
    if (escaped.trim().startsWith(commentStart)) {
      return `<span class="syn-comment">${escaped}</span>`;
    }

    // Highlight reserved keywords
    const keywords = isJava
      ? /\b(import|class|public|private|static|new|return|if|else|for|while|break|continue|try|catch|final)\b/g
      : /\b(def|class|return|if|in|for|while|import|from|as|else|elif|try|except)\b/g;
    escaped = escaped.replace(keywords, '<span class="syn-keyword">$1</span>');
    // Highlight standard types and built-in functions
    const types = isJava
      ? /\b(int|boolean|void|String|Integer|Map|HashMap|List|ArrayList|Set|HashSet)\b/g
      : /\b(enumerate|len|range|print|int|str|list|dict|set)\b/g;
    escaped = escaped.replace(types, '<span class="syn-type">$1</span>');
    // Highlight trailing comments
    escaped = escaped.replace(isJava ? /(\/\/[^<]*)/g : /(#[^<]*)/g, '<span class="syn-comment">$1</span>');
    // Highlight numeric literals
    escaped = escaped.replace(/(\b\d+\b)/g, '<span class="syn-number">$1</span>');

    return escaped;
  }

  /**
   * Renders the test case runner view based on the active test case selection.
   */
  function renderTestCaseView() {
    const problem = CodeForgeData.problems[0];
    const caseData = problem.testCases.find(c => c.id === activeCase) || problem.testCases[0];

    // Toggle active styling between Case 1 and Case 2 buttons
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

    // Populate test case parameters in the console DOM
    const inputValEl = document.getElementById('test-case-input');
    const outputValEl = document.getElementById('test-case-output');
    const expectedValEl = document.getElementById('test-case-expected');

    if (inputValEl) inputValEl.textContent = caseData.input;
    if (outputValEl) outputValEl.textContent = caseData.yourOutput;
    if (expectedValEl) expectedValEl.textContent = caseData.expected;
  }

  /**
   * Binds interactive UI event listeners across the IDE interface.
   */
  function bindEvents() {
    // Run Code Button click
    const runBtn = document.getElementById('btn-run-code');
    if (runBtn) {
      runBtn.addEventListener('click', runCode);
    }

    // Submit Solution Button click
    const submitBtn = document.getElementById('btn-submit-solution');
    if (submitBtn) {
      submitBtn.addEventListener('click', submitSolution);
    }

    // Focus Mode Toggle (collapses outer layout to maximize editor space)
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

    // Case selection buttons
    const case1Btn = document.getElementById('case-btn-1');
    const case2Btn = document.getElementById('case-btn-2');
    if (case1Btn) case1Btn.addEventListener('click', () => { activeCase = 1; renderTestCaseView(); });
    if (case2Btn) case2Btn.addEventListener('click', () => { activeCase = 2; renderTestCaseView(); });

    // Console Tabs switching (Test cases vs Test results)
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

    // Live code input typing synchronization
    const textarea = document.getElementById('editor-real-input');
    if (textarea) {
      textarea.addEventListener('input', () => {
        CodeForgeData.problems[0].starterCode[currentLanguage] = textarea.value;
        renderCodeEditor();
      });
    }

    // Keyboard shortcut: Cmd/Ctrl + Enter triggers Run Code
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

  /**
   * Executes code against sample test cases and displays execution metrics.
   *
   * <p>Simulates background compilation and sandbox dispatch, updates the button state
   * with an animated spinner, and switches the console tab to display results.</p>
   */
  function runCode() {
    const runBtn = document.getElementById('btn-run-code');
    const originalContent = runBtn.innerHTML;

    // Indicate processing state
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

  /**
   * Submits solution code for full judging across all hidden and public test cases.
   *
   * <p>Simulates judging lifecycle, updates UI with animated status, and launches
   * the celebration success modal upon completion.</p>
   */
  function submitSolution() {
    const submitBtn = document.getElementById('btn-submit-solution');
    const originalContent = submitBtn.innerHTML;

    // Indicate judging state
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

  /**
   * Displays the celebration modal acknowledging an Accepted solution.
   */
  function showSuccessModal() {
    const modal = document.getElementById('submission-success-modal');
    if (modal) {
      modal.classList.remove('hidden');
      modal.classList.add('flex');
    }
  }

  // Expose public API interface
  return {
    init,
    runCode,
    submitSolution
  };
})();
