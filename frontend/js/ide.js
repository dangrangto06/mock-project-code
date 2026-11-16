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

  function escapeHtml(val) {
    if (val === null || val === undefined) return '';
    return String(val).replace(/[&<>"']/g, ch => (
      { '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[ch]
    ));
  }

  function getCurrentProblem() {
    const id = window.CodeForgeCurrentProblemId;
    if (id && Array.isArray(CodeForgeData.problems)) {
      const match = CodeForgeData.problems.find(p => p.id === id);
      if (match) return match;
    }
    return (CodeForgeData.problems && CodeForgeData.problems[0]) || {
      id: 104,
      title: "Pair Sum",
      difficulty: "Easy",
      description: "Given an integer array nums and an integer target, return indices of the two numbers that add up to target.",
      starterCode: { python: "# Write your solution here\n" },
      testCases: []
    };
  }

  function getStarterCode(problem, lang) {
    if (!problem) return '# Write your solution here\n';
    if (problem.starterCode && typeof problem.starterCode === 'object') {
      return problem.starterCode[lang] || problem.starterCode.python || Object.values(problem.starterCode)[0] || '# Write your solution here\n';
    }
    if (typeof problem.starterCode === 'string') {
      return problem.starterCode;
    }
    if (lang === 'python') return `def solution():\n    pass\n`;
    if (lang === 'javascript') return `function solution() {\n    // your code\n}\n`;
    if (lang === 'java') return `class Solution {\n    public void solution() {\n    }\n}\n`;
    return `// Write code here\n`;
  }

  /**
   * Initializes the IDE controller: starts timer, binds UI events, and renders editor & test cases.
   */
  function init() {
    startTimer();
    bindEvents();
    if (window.CodeForgeCurrentProblemId) {
      loadProblem(window.CodeForgeCurrentProblemId);
    } else {
      renderCodeEditor();
      renderTestCaseView();
    }
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
   */
  function renderCodeEditor() {
    const problem = getCurrentProblem();
    const rawCode = getStarterCode(problem, currentLanguage);
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
   */
  function highlightLine(line) {
    if (!line) return '';
    const isJava = currentLanguage === 'java';
    let escaped = line
      .replace(/&/g, '&amp;')
      .replace(/</g, '&lt;')
      .replace(/>/g, '&gt;');

    const commentStart = isJava ? '//' : '#';
    if (escaped.trim().startsWith(commentStart)) {
      return `<span class="syn-comment">${escaped}</span>`;
    }

    const keywords = isJava
      ? /\b(import|class|public|private|static|new|return|if|else|for|while|break|continue|try|catch|final)\b/g
      : /\b(def|class|return|if|in|for|while|import|from|as|else|elif|try|except)\b/g;
    escaped = escaped.replace(keywords, '<span class="syn-keyword">$1</span>');

    const types = isJava
      ? /\b(int|boolean|void|String|Integer|Map|HashMap|List|ArrayList|Set|HashSet)\b/g
      : /\b(enumerate|len|range|print|int|str|list|dict|set)\b/g;
    escaped = escaped.replace(types, '<span class="syn-type">$1</span>');
    escaped = escaped.replace(isJava ? /(\/\/[^<]*)/g : /(#[^<]*)/g, '<span class="syn-comment">$1</span>');
    escaped = escaped.replace(/(\b\d+\b)/g, '<span class="syn-number">$1</span>');

    return escaped;
  }

  /**
   * Renders the test case runner view based on the active test case selection.
   */
  function renderTestCaseView() {
    const problem = getCurrentProblem();
    const testCases = (problem.testCases && problem.testCases.length > 0)
      ? problem.testCases
      : [
          {
            id: 1,
            name: "Case 1",
            input: "nums = [2,7,11,15], target = 9",
            expected: "[0, 1]",
            yourOutput: "[0, 1]"
          },
          {
            id: 2,
            name: "Case 2",
            input: "nums = [3,2,4], target = 6",
            expected: "[1, 2]",
            yourOutput: "[1, 2]"
          }
        ];

    const caseData = testCases.find(c => c.id === activeCase) || testCases[0];

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

    if (inputValEl) inputValEl.textContent = caseData.input || caseData.inputData || '';
    if (outputValEl) outputValEl.textContent = caseData.yourOutput || '';
    if (expectedValEl) expectedValEl.textContent = caseData.expected || caseData.expectedOutput || '';
  }

  /**
   * Loads and displays problem details in the IDE workspace.
   *
   * @param {number|string} problemId - Identifier of the problem to load.
   */
  async function loadProblem(problemId) {
    if (problemId) {
      window.CodeForgeCurrentProblemId = parseInt(problemId, 10) || problemId;
    }
    const id = window.CodeForgeCurrentProblemId;
    let problem = (CodeForgeData.problems || []).find(p => p.id === id);

    // If problem not found in local cache or statement empty, fetch from API
    if (!problem || !problem.description) {
      try {
        const res = await fetch(`http://localhost:8080/api/problems/${id}`);
        if (res.ok) {
          const fetched = await res.json();
          if (problem) {
            Object.assign(problem, fetched);
          } else {
            problem = fetched;
            if (Array.isArray(CodeForgeData.problems)) {
              CodeForgeData.problems.unshift(problem);
            }
          }
        }
      } catch (_) {}
    }

    if (!problem) {
      problem = getCurrentProblem();
    }

    // 1. Header Problem Title & Status
    const headerTitleEl = document.getElementById('ide-header-problem-title');
    if (headerTitleEl) headerTitleEl.textContent = `${problem.id}. ${problem.title}`;
    const statusHeaderEl = document.getElementById('ide-header-problem-status');
    if (statusHeaderEl) {
      const isSolved = problem.status === 'solved';
      const isInProg = problem.status === 'in-progress';
      statusHeaderEl.textContent = isSolved ? 'Solved' : (isInProg ? 'In progress' : 'Unattempted');
      statusHeaderEl.className = isSolved
        ? 'px-2 py-0.5 rounded text-[11px] font-medium bg-emerald-50 text-emerald-700 border border-emerald-200'
        : (isInProg
          ? 'px-2 py-0.5 rounded text-[11px] font-medium bg-amber-50 text-amber-700 border border-amber-200'
          : 'px-2 py-0.5 rounded text-[11px] font-medium bg-gray-100 text-gray-600 border border-gray-200');
    }

    // 2. Main Title
    const titleEl = document.getElementById('ide-problem-title');
    if (titleEl) titleEl.textContent = `${problem.id}. ${problem.title}`;

    // 3. Difficulty Pill
    const diffEl = document.getElementById('ide-problem-difficulty');
    if (diffEl) {
      diffEl.textContent = problem.difficulty || 'Medium';
      const d = (problem.difficulty || '').toLowerCase();
      diffEl.className = d === 'easy'
        ? 'px-2.5 py-0.5 rounded-full text-xs font-medium bg-emerald-50 text-emerald-700 border border-emerald-200'
        : (d === 'hard'
          ? 'px-2.5 py-0.5 rounded-full text-xs font-medium bg-rose-50 text-rose-700 border border-rose-200'
          : 'px-2.5 py-0.5 rounded-full text-xs font-medium bg-amber-50 text-amber-700 border border-amber-200');
    }

    // 4. Topic Pills
    const pillsContainer = document.getElementById('ide-problem-pills');
    if (pillsContainer) {
      const topics = problem.topics || [];
      const topicsHtml = topics.map(t =>
        `<span class="px-2.5 py-0.5 rounded-full text-xs font-medium bg-gray-100 text-gray-700 border border-gray-200">${escapeHtml(t)}</span>`
      ).join(' ');
      pillsContainer.innerHTML = `
        <span id="ide-problem-difficulty" class="${diffEl ? diffEl.className : ''}">${escapeHtml(problem.difficulty || 'Medium')}</span>
        ${topicsHtml}
        ${problem.isMine ? '<span class="px-2.5 py-0.5 rounded-full text-xs font-medium bg-indigo-50 text-indigo-700 border border-indigo-200">My problem</span>' : ''}
      `;
    }

    // 5. Bookmark Button Icon
    const bookmarkBtn = document.getElementById('ide-btn-bookmark');
    if (bookmarkBtn) {
      bookmarkBtn.innerHTML = problem.saved
        ? `<svg class="w-5 h-5 fill-emerald-600 text-emerald-600" viewBox="0 0 24 24"><path d="M5 5a2 2 0 012-2h10a2 2 0 012 2v16l-7-3.5L5 21V5z"></path></svg>`
        : `<svg class="w-5 h-5 fill-none stroke-current" viewBox="0 0 24 24" stroke-width="2"><path stroke-linecap="round" stroke-linejoin="round" d="M5 5a2 2 0 012-2h10a2 2 0 012 2v16l-7-3.5L5 21V5z"></path></svg>`;
    }

    // 6. Meta
    const metaEl = document.getElementById('ide-problem-meta');
    if (metaEl) {
      metaEl.textContent = `${problem.author || 'CodeForge'} · Updated ${problem.updatedDate || 'Recently'}`;
    }

    // 7. Description & Constraints
    const descEl = document.getElementById('ide-problem-description');
    if (descEl && problem.description) {
      descEl.innerHTML = problem.description
        .split('\n\n')
        .map(p => `<p>${escapeHtml(p).replace(/`([^`]+)`/g, '<code class="px-1 py-0.5 rounded bg-gray-100 font-mono text-xs text-emerald-800">$1</code>')}</p>`)
        .join('');
    }

    const constraintsEl = document.getElementById('ide-problem-constraints');
    if (constraintsEl && problem.constraints) {
      const lines = problem.constraints.split('\n').filter(l => l.trim().length > 0);
      constraintsEl.innerHTML = lines.map(c => `<li>${escapeHtml(c.replace(/^[*-]\s*/, ''))}</li>`).join('');
    }

    // 8. Re-render code editor with matching starter code and test cases
    renderCodeEditor();
    renderTestCaseView();

    // Default view to Description tab
    switchIdeTab('description');
  }

  /**
   * Switches the left IDE pane view between 'description' and 'submissions'.
   */
  async function switchIdeTab(tab) {
    const tabDesc = document.getElementById('ide-tab-description');
    const tabSub = document.getElementById('ide-tab-submissions');
    const viewDesc = document.getElementById('ide-view-description');
    const viewSub = document.getElementById('ide-view-submissions');

    if (tab === 'submissions') {
      if (tabDesc) tabDesc.className = "pb-2.5 border-b-2 border-transparent hover:text-gray-800 cursor-pointer";
      if (tabSub) tabSub.className = "pb-2.5 border-b-2 border-emerald-600 text-emerald-800 font-semibold cursor-pointer";
      if (viewDesc) viewDesc.classList.add('hidden');
      if (viewSub) viewSub.classList.remove('hidden');

      loadSubmissionsTab();
    } else {
      if (tabDesc) tabDesc.className = "pb-2.5 border-b-2 border-emerald-600 text-emerald-800 font-semibold cursor-pointer";
      if (tabSub) tabSub.className = "pb-2.5 border-b-2 border-transparent hover:text-gray-800 cursor-pointer";
      if (viewDesc) viewDesc.classList.remove('hidden');
      if (viewSub) viewSub.classList.add('hidden');
    }
  }

  /**
   * Loads user submissions history for current problem in the submissions tab.
   */
  async function loadSubmissionsTab() {
    const listEl = document.getElementById('ide-submissions-list');
    const countEl = document.getElementById('ide-submissions-count');
    if (!listEl) return;

    listEl.innerHTML = `<div class="py-4 text-center text-gray-400">Loading submissions...</div>`;

    const problemId = window.CodeForgeCurrentProblemId || 104;
    let submissions = [];
    if (window.CodeForgeAPI && window.CodeForgeAPI.getProblemSubmissions) {
      try {
        submissions = await CodeForgeAPI.getProblemSubmissions(problemId);
      } catch (e) {
        console.warn("Failed to load problem submissions:", e);
      }
    }

    if (countEl) countEl.textContent = `${submissions.length} submission${submissions.length === 1 ? '' : 's'}`;

    if (!submissions || submissions.length === 0) {
      listEl.innerHTML = `<div class="py-6 text-center text-gray-400">No submissions for this problem yet. Submit your solution to see history!</div>`;
      return;
    }

    listEl.innerHTML = submissions.map(s => {
      const isAccepted = s.status === 'Accepted';
      const badge = isAccepted
        ? `<span class="px-2 py-0.5 rounded text-[11px] font-medium bg-emerald-50 text-emerald-700 border border-emerald-200">Accepted</span>`
        : `<span class="px-2 py-0.5 rounded text-[11px] font-medium bg-rose-50 text-rose-700 border border-rose-200">${escapeHtml(s.status || 'Wrong Answer')}</span>`;

      return `
        <div class="py-3 flex items-center justify-between hover:bg-gray-50 px-2 rounded transition">
          <div class="flex items-center gap-3">
            ${badge}
            <span class="text-xs text-gray-700 font-medium">${escapeHtml(s.language || 'code')}</span>
            <span class="text-gray-400 text-[11px]">${escapeHtml(s.date || '')}</span>
          </div>
          <div class="flex items-center gap-4 text-xs text-gray-500 font-mono">
            <span class="text-emerald-700">${escapeHtml(s.runtime || (s.runtimeMs ? s.runtimeMs + ' ms' : '—'))}</span>
            <span>${escapeHtml(s.memory || (s.memoryMb ? s.memoryMb + ' MB' : '—'))}</span>
          </div>
        </div>
      `;
    }).join('');
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

    // Left pane tabs (Description vs Submissions)
    const tabDesc = document.getElementById('ide-tab-description');
    const tabSub = document.getElementById('ide-tab-submissions');
    if (tabDesc) tabDesc.addEventListener('click', () => switchIdeTab('description'));
    if (tabSub) tabSub.addEventListener('click', () => switchIdeTab('submissions'));

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
        const p = getCurrentProblem();
        if (!p.starterCode || typeof p.starterCode !== 'object') {
          p.starterCode = {};
        }
        p.starterCode[currentLanguage] = textarea.value;
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
   */
  async function runCode() {
    const runBtn = document.getElementById('btn-run-code');
    const originalContent = runBtn.innerHTML;
    const textarea = document.getElementById('editor-real-input');
    const code = textarea ? textarea.value : '';
    const problemId = window.CodeForgeCurrentProblemId || 104;

    runBtn.disabled = true;
    runBtn.innerHTML = `<span>Running tests...</span>`;

    try {
      const res = await CodeForgeAPI.runCode({
        problemId,
        language: currentLanguage,
        code,
        customInput: ''
      });
      const tabResults = document.getElementById('tab-test-results');
      if (tabResults) tabResults.click();
      CodeForgeApp.showToast(`${res.status || 'Done'} (${res.runtime || '32ms'})`, res.success ? "success" : "warn");
    } finally {
      runBtn.disabled = false;
      runBtn.innerHTML = originalContent;
    }
  }

  /**
   * Submits code to backend judge and refreshes library problem status upon success.
   */
  async function submitSolution() {
    const submitBtn = document.getElementById('btn-submit-solution');
    const originalContent = submitBtn.innerHTML;
    const textarea = document.getElementById('editor-real-input');
    const code = textarea ? textarea.value : '';
    const problemId = window.CodeForgeCurrentProblemId || 104;

    submitBtn.disabled = true;
    submitBtn.innerHTML = `<span>Judging...</span>`;

    try {
      const res = await CodeForgeAPI.submitSolution({
        problemId,
        language: currentLanguage,
        code
      });
      if (res && res.success) {
        showSuccessModal();

        // Refresh problems from backend so the status updates to 'solved'
        if (window.CodeForgeAPI) {
          CodeForgeAPI.getProblems().then(apiProblems => {
            if (Array.isArray(apiProblems) && apiProblems.length > 0) {
              CodeForgeData.problems = apiProblems.map(p => ({
                ...p,
                starterCode: p.starterCode || { python: '# Write your solution here\n' }
              }));
              if (window.CodeForgeApp && window.CodeForgeApp.renderProblemTable) {
                window.CodeForgeApp.renderProblemTable();
              }
            }
          });
        }

        // Also update local problem status immediately
        const currentProb = (CodeForgeData.problems || []).find(p => p.id === problemId);
        if (currentProb) {
          currentProb.status = 'solved';
        }
        const statusHeaderEl = document.getElementById('ide-header-problem-status');
        if (statusHeaderEl) {
          statusHeaderEl.textContent = 'Solved';
          statusHeaderEl.className = 'px-2 py-0.5 rounded text-[11px] font-medium bg-emerald-50 text-emerald-700 border border-emerald-200';
        }
      } else {
        CodeForgeApp.showToast(res?.message || "Wrong Answer", "warn");
      }
    } finally {
      submitBtn.disabled = false;
      submitBtn.innerHTML = originalContent;
    }
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
    submitSolution,
    loadProblem
  };
})();
