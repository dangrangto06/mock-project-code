/**
 * @file ide.js
 * @description CodeForge In-Browser IDE & Interactive Test Runner Controller.
 * @module CodeForgeIDE
 */

window.CodeForgeIDE = (function () {
  'use strict';

  /** Practice session duration stopwatch counter in seconds */
  let timerSeconds = 0;

  /** Interval timer handle for the stopwatch */
  let timerInterval = null;

  /** Currently selected test case index (0-based) in the console pane */
  let activeCaseIndex = 0;

  /** Active console tab view: 'cases' (Testcase) | 'results' (Test Result) */
  let activeConsoleTab = 'cases';

  /** Currently active programming language identifier ('python' | 'javascript' | 'java') */
  let currentLanguage = 'python';

  /** Stores the latest Run / Submit result for the currently loaded problem */
  let lastRunResult = null;

  /** Stores user-edited code per problemId and language so original starter templates can be reset */
  const userCodeStore = {};

  /** Stores custom test case inputs per problemId */
  const customCasesStore = {};

  const LANGUAGE_LABELS = { python: 'Python 3.12', javascript: 'Node.js 20', java: 'Java 17', cpp: 'C++ (GCC 14)' };
  const FILE_NAMES = { python: 'solution.py', javascript: 'solution.js', java: 'Solution.java', cpp: 'solution.cpp' };

  function escapeHtml(val) {
    if (val === null || val === undefined) return '';
    return String(val).replace(/[&<>"']/g, ch => (
      { '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[ch]
    ));
  }

  function getProblemList() {
    const all = (window.CodeForgeData && Array.isArray(CodeForgeData.problems)) ? CodeForgeData.problems : [];
    const visible = all.filter(p => !p.moderationStatus || p.moderationStatus === 'APPROVED' || p.isMine);
    return visible.length > 0 ? visible : all;
  }

  function getCurrentProblem() {
    const id = window.CodeForgeCurrentProblemId;
    const list = getProblemList();
    if (id) {
      const match = list.find(p => Number(p.id) === Number(id));
      if (match) return match;
    }
    return list[0] || {
      id: 104,
      title: "Pair Sum",
      difficulty: "Easy",
      topics: ["Arrays", "Hash map"],
      description: "Given an integer array nums and an integer target, return indices of the two numbers that add up to target.",
      starterCode: { python: "def pair_sum(nums: list[int], target: int) -> list[int]:\n    seen = {}\n    for i, num in enumerate(nums):\n        complement = target - num\n        if complement in seen:\n            return [seen[complement], i]\n        seen[num] = i\n    return []\n" },
      testCases: []
    };
  }

  function getDefaultStarterTemplate(problem, lang) {
    if (problem && problem.id === 104) {
      if (lang === 'python') {
        if (typeof problem.starterCode === 'string' && problem.starterCode.trim()) return problem.starterCode;
        if (problem.starterCode && typeof problem.starterCode === 'object' && problem.starterCode.python) return problem.starterCode.python;
        return `def pair_sum(nums: list[int], target: int) -> list[int]:\n    # Remember each value and its index.\n    seen = {}\n\n    for i, num in enumerate(nums):\n        complement = target - num\n\n        if complement in seen:\n            return [seen[complement], i]\n\n        seen[num] = i\n\n    return []\n`;
      }
      if (lang === 'javascript') {
        return `function pairSum(nums, target) {\n    const seen = new Map();\n    for (let i = 0; i < nums.length; i++) {\n        const complement = target - nums[i];\n        if (seen.has(complement)) {\n            return [seen.get(complement), i];\n        }\n        seen.set(nums[i], i);\n    }\n    return [];\n}\n`;
      }
      if (lang === 'java') {
        return `class Solution {\n    public int[] pairSum(int[] nums, int target) {\n        Map<Integer, Integer> seen = new HashMap<>();\n        for (int i = 0; i < nums.length; i++) {\n            int complement = target - nums[i];\n            if (seen.containsKey(complement)) {\n                return new int[] { seen.get(complement), i };\n            }\n            seen.put(nums[i], i);\n        }\n        return new int[0];\n    }\n}\n`;
      }
      if (lang === 'cpp') {
        return `class Solution {\npublic:\n    vector<int> pairSum(vector<int>& nums, int target) {\n        unordered_map<int, int> seen;\n        for (int i = 0; i < nums.size(); ++i) {\n            int complement = target - nums[i];\n            if (seen.find(complement) != seen.end()) {\n                return {seen[complement], i};\n            }\n            seen[nums[i]] = i;\n        }\n        return {};\n    }\n};\n`;
      }
    }

    if (problem && problem.starterCode) {
      if (typeof problem.starterCode === 'string' && problem.starterCode.trim() && lang === 'python' && problem.starterCode.trim() !== '# Write your solution here') {
        return problem.starterCode;
      }
      if (typeof problem.starterCode === 'object' && problem.starterCode[lang] && problem.starterCode[lang].trim() !== '# Write your solution here') {
        return problem.starterCode[lang];
      }
    }

    const fnName = problem && problem.title
      ? problem.title.toLowerCase().replace(/[^a-z0-9]+/g, '_').replace(/^_|_$/g, '') || 'solve'
      : 'solution';

    if (lang === 'python') return `def ${fnName}(nums, target):\n    # Write your solution here\n    pass\n`;
    if (lang === 'javascript') return `function ${fnName}(nums, target) {\n    // Write your solution here\n}\n`;
    if (lang === 'java') return `class Solution {\n    public int[] ${fnName}(int[] nums, int target) {\n        // Write your solution here\n        return new int[]{};\n    }\n}\n`;
    if (lang === 'cpp') return `class Solution {\npublic:\n    vector<int> ${fnName}(vector<int>& nums, int target) {\n        // Write your solution here\n        return {};\n    }\n};\n`;
    return `// Write your solution here\n`;
  }

  function getActiveCode(problem, lang) {
    const pid = problem ? problem.id : 104;
    if (userCodeStore[pid] && typeof userCodeStore[pid][lang] === 'string') {
      return userCodeStore[pid][lang];
    }
    return getDefaultStarterTemplate(problem, lang);
  }

  function setActiveCode(problem, lang, code) {
    const pid = problem ? problem.id : 104;
    if (!userCodeStore[pid]) userCodeStore[pid] = {};
    userCodeStore[pid][lang] = code;
  }

  function getProblemSpecificDefaultCases(problem) {
    const pid = problem ? Number(problem.id) : 104;
    const map = {
      104: [
        { id: 1, name: "Case 1", input: "nums = [2,7,11,15], target = 9", expected: "[0, 1]", explanation: "Because nums[0] + nums[1] == 2 + 7 == 9, return [0, 1]." },
        { id: 2, name: "Case 2", input: "nums = [3,2,4], target = 6", expected: "[1, 2]", explanation: "The numbers at indices 1 and 2 add up to 6." }
      ],
      105: [
        { id: 1, name: "Case 1", input: "nums = [1,2,3,1]", expected: "true", explanation: "The element 1 occurs at indices 0 and 3." },
        { id: 2, name: "Case 2", input: "nums = [1,2,3,4]", expected: "false", explanation: "All elements are distinct." }
      ],
      106: [
        { id: 1, name: "Case 1", input: "s = \"anagram\", t = \"nagaram\"", expected: "true", explanation: "Both strings contain the exact same character frequencies." },
        { id: 2, name: "Case 2", input: "s = \"rat\", t = \"car\"", expected: "false", explanation: "Character 't' is not in \"car\"." }
      ],
      107: [
        { id: 1, name: "Case 1", input: "strs = [\"eat\",\"tea\",\"tan\",\"ate\",\"nat\",\"bat\"]", expected: "[[\"bat\"],[\"nat\",\"tan\"],[\"ate\",\"eat\",\"tea\"]]", explanation: "Anagrams share the same sorted character signature." },
        { id: 2, name: "Case 2", input: "strs = [\"\"]", expected: "[[\"\"]]", explanation: "Single empty string forms one group." }
      ],
      108: [
        { id: 1, name: "Case 1", input: "nums = [1,1,1,2,2,3], k = 2", expected: "[1, 2]", explanation: "1 appears 3 times and 2 appears 2 times." },
        { id: 2, name: "Case 2", input: "nums = [1], k = 1", expected: "[1]", explanation: "1 is the only element." }
      ],
      109: [
        { id: 1, name: "Case 1", input: "nums = [1,2,3,4]", expected: "[24,12,8,6]", explanation: "Prefix and suffix products combined in O(n) time." },
        { id: 2, name: "Case 2", input: "nums = [-1,1,0,-3,3]", expected: "[0,0,9,0,0]", explanation: "All products except index 2 include 0." }
      ],
      110: [
        { id: 1, name: "Case 1", input: "s = \"()[]{}\"", expected: "true", explanation: "Every opening bracket is closed by the same type in order." },
        { id: 2, name: "Case 2", input: "s = \"(]\"", expected: "false", explanation: "Mismatched closing bracket." }
      ]
    };
    return map[pid] || [
      { id: 1, name: "Case 1", input: "nums = [2,7,11,15], target = 9", expected: "[0, 1]" },
      { id: 2, name: "Case 2", input: "nums = [3,2,4], target = 6", expected: "[1, 2]" }
    ];
  }

  function getSampleTestCases(problem) {
    const pid = problem ? problem.id : 104;
    if (customCasesStore[pid] && customCasesStore[pid].length > 0) {
      return customCasesStore[pid];
    }
    const baseCases = (problem && Array.isArray(problem.testCases) && problem.testCases.length > 0)
      ? problem.testCases.map((tc, i) => ({
          id: tc.id || (i + 1),
          name: tc.name || tc.caseName || `Case ${i + 1}`,
          input: tc.input || tc.inputData || 'nums = [2,7,11,15], target = 9',
          expected: tc.expected || tc.expectedOutput || '[0, 1]',
          explanation: tc.explanation || ''
        }))
      : getProblemSpecificDefaultCases(problem);
    customCasesStore[pid] = baseCases;
    return baseCases;
  }

  function renderProblemExamples(problem) {
    const container = document.getElementById('ide-problem-examples');
    if (!container) return;
    const cases = getSampleTestCases(problem).slice(0, 3);
    container.innerHTML = cases.map((c, idx) => `
      <div class="space-y-2">
        <div class="text-xs font-bold text-gray-900">Example ${idx + 1}</div>
        <div class="p-3.5 bg-[#f6f8f7] rounded-lg border border-[#e5eae7] text-xs font-mono space-y-1">
          <div><span class="text-gray-500">Input:</span> ${escapeHtml(c.input)}</div>
          <div><span class="text-gray-500">Output:</span> ${escapeHtml(c.expected)}</div>
        </div>
        ${c.explanation ? `<p class="text-xs text-gray-500">${escapeHtml(c.explanation)}</p>` : ''}
      </div>
    `).join('');
  }

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
    const problem = getCurrentProblem();
    const rawCode = getActiveCode(problem, currentLanguage);
    const lines = rawCode.split('\n');

    const linesContainer = document.getElementById('editor-line-numbers');
    const codeContainer = document.getElementById('editor-code-content');
    const textarea = document.getElementById('editor-real-input');

    if (!linesContainer || !codeContainer) return;

    if (textarea && textarea.value !== rawCode) {
      textarea.value = rawCode;
    }

    // Calculate active cursor line and column from textarea.selectionStart
    let activeLineNum = Math.min(lines.length, 2);
    let activeColNum = 1;
    if (textarea && typeof textarea.selectionStart === 'number') {
      const uptoCursor = textarea.value.substring(0, textarea.selectionStart);
      const cursorLines = uptoCursor.split('\n');
      activeLineNum = cursorLines.length;
      activeColNum = cursorLines[cursorLines.length - 1].length + 1;
    }

    const lnColEl = document.getElementById('editor-ln-col');
    if (lnColEl) {
      lnColEl.textContent = `Ln ${activeLineNum}, Col ${activeColNum}`;
    }

    let lineNumbersHtml = '';
    let codeLinesHtml = '';

    lines.forEach((line, idx) => {
      const lineNum = idx + 1;
      const isActive = lineNum === activeLineNum;
      lineNumbersHtml += `<div class="editor-line-number ${isActive ? 'text-emerald-400 font-semibold' : ''}">${lineNum}</div>`;
      const highlighted = highlightLine(line);
      codeLinesHtml += `<div class="px-3 leading-[1.6rem] h-[1.6rem] font-mono text-xs md:text-sm whitespace-pre ${isActive ? 'editor-line-active' : ''}">${highlighted || ' '}</div>`;
    });

    linesContainer.innerHTML = lineNumbersHtml;
    codeContainer.innerHTML = codeLinesHtml;

    const statsEl = document.getElementById('editor-cursor-stats');
    if (statsEl) {
      const langName = LANGUAGE_LABELS[currentLanguage] || currentLanguage;
      statsEl.textContent = `Spaces: 4 · UTF-8 · ${langName}`;
    }
    const fileEl = document.getElementById('editor-filename');
    if (fileEl) fileEl.textContent = FILE_NAMES[currentLanguage] || 'solution.txt';
  }

  function highlightLine(line) {
    if (!line) return '';
    const isCStyle = currentLanguage === 'java' || currentLanguage === 'javascript' || currentLanguage === 'cpp';
    let escaped = line
      .replace(/&/g, '&amp;')
      .replace(/</g, '&lt;')
      .replace(/>/g, '&gt;');

    const commentStart = isCStyle ? '//' : '#';
    if (escaped.trim().startsWith(commentStart)) {
      return `<span class="syn-comment">${escaped}</span>`;
    }
    if (currentLanguage === 'cpp' && escaped.trim().startsWith('#')) {
      return `<span class="syn-def">${escaped}</span>`;
    }

    const keywords = isCStyle
      ? /\b(import|class|struct|public|private|static|new|return|if|else|for|while|break|continue|try|catch|final|function|const|let|var|using|namespace|auto|true|false)\b/g
      : /\b(def|class|return|if|in|for|while|import|from|as|else|elif|try|except|pass)\b/g;
    escaped = escaped.replace(keywords, '<span class="syn-keyword">$1</span>');

    const types = isCStyle
      ? /\b(int|long|double|bool|boolean|void|String|Integer|Map|HashMap|List|ArrayList|Set|HashSet|vector|string|unordered_map|unordered_set|cout|cin|endl)\b/g
      : /\b(enumerate|len|range|print|int|str|list|dict|set)\b/g;
    escaped = escaped.replace(types, '<span class="syn-type">$1</span>');
    escaped = escaped.replace(isCStyle ? /(\/\/[^<]*)/g : /(#[^<]*)/g, '<span class="syn-comment">$1</span>');
    escaped = escaped.replace(/(\b\d+\b)/g, '<span class="syn-number">$1</span>');

    return escaped;
  }

  /**
   * Renders the Testcase tab (pill buttons Case 1, Case 2, + Custom and editable input).
   */
  function renderTestCaseView() {
    const problem = getCurrentProblem();
    const cases = getSampleTestCases(problem);
    if (activeCaseIndex >= cases.length) activeCaseIndex = 0;

    const pillsBar = document.getElementById('testcase-pills-bar');
    if (pillsBar) {
      const pillsHtml = cases.map((c, idx) => {
        const isActive = idx === activeCaseIndex;
        const btnClass = isActive
          ? "px-3 py-1 text-xs font-semibold rounded-md bg-emerald-50 text-emerald-800 border border-emerald-200 flex items-center gap-1.5 shadow-xs"
          : "px-3 py-1 text-xs font-medium rounded-md bg-gray-50 text-gray-600 hover:bg-gray-100 border border-gray-200 flex items-center gap-1.5";
        return `
          <button type="button" data-tc-index="${idx}" class="testcase-pill-btn ${btnClass}">
            <span>${escapeHtml(c.name || `Case ${idx + 1}`)}</span>
          </button>
        `;
      }).join('');

      pillsBar.innerHTML = pillsHtml + `
        <button id="btn-add-custom-case" type="button" title="Add custom testcase" class="px-2.5 py-1 text-xs font-bold rounded-md bg-gray-50 text-gray-500 hover:text-emerald-700 hover:bg-emerald-50 border border-gray-200 transition">
          +
        </button>
      `;

      pillsBar.querySelectorAll('.testcase-pill-btn').forEach(btn => {
        btn.addEventListener('click', () => {
          activeCaseIndex = parseInt(btn.getAttribute('data-tc-index'), 10) || 0;
          renderTestCaseView();
        });
      });

      const addBtn = document.getElementById('btn-add-custom-case');
      if (addBtn) {
        addBtn.addEventListener('click', () => {
          if (cases.length >= 5) {
            if (window.CodeForgeApp) CodeForgeApp.showToast("Maximum of 5 sample testcases reached.", "info");
            return;
          }
          const last = cases[cases.length - 1] || { input: "nums = [1,2], target = 3", expected: "[0, 1]" };
          cases.push({
            id: cases.length + 1,
            name: `Case ${cases.length + 1}`,
            input: last.input,
            expected: last.expected
          });
          activeCaseIndex = cases.length - 1;
          renderTestCaseView();
        });
      }
    }

    const currentCase = cases[activeCaseIndex] || cases[0];
    const customInputEl = document.getElementById('testcase-custom-input');
    const expectedPreviewEl = document.getElementById('testcase-expected-preview');
    if (customInputEl && currentCase) {
      customInputEl.value = currentCase.input || '';
    }
    if (expectedPreviewEl && currentCase) {
      expectedPreviewEl.textContent = currentCase.expected || '[0, 1]';
    }
  }

  /**
   * Renders the Test Result tab (empty state before running, loading state during run, or full verdict + per-case I/O).
   */
  function renderTestResultView(isLoading = false) {
    const emptyEl = document.getElementById('test-result-empty');
    const loadingEl = document.getElementById('test-result-loading');
    const contentEl = document.getElementById('test-result-content');

    if (!emptyEl || !loadingEl || !contentEl) return;

    if (isLoading) {
      emptyEl.classList.add('hidden');
      contentEl.classList.add('hidden');
      loadingEl.classList.remove('hidden');
      return;
    }

    loadingEl.classList.add('hidden');

    if (!lastRunResult) {
      emptyEl.classList.remove('hidden');
      contentEl.classList.add('hidden');
      return;
    }

    emptyEl.classList.add('hidden');
    contentEl.classList.remove('hidden');

    const isSuccess = !!lastRunResult.success;
    const statusText = lastRunResult.status || (isSuccess ? 'Accepted' : 'Wrong Answer');
    const resultsList = Array.isArray(lastRunResult.results) && lastRunResult.results.length > 0
      ? lastRunResult.results
      : getSampleTestCases(getCurrentProblem()).map((c, idx) => ({
          id: c.id || (idx + 1),
          name: c.name || `Case ${idx + 1}`,
          input: c.input,
          expected: c.expected,
          yourOutput: isSuccess ? c.expected : 'null',
          passed: isSuccess
        }));

    if (activeCaseIndex >= resultsList.length) activeCaseIndex = 0;

    const passedCount = lastRunResult.passedCount != null
      ? lastRunResult.passedCount
      : (lastRunResult.testsPassed != null ? lastRunResult.testsPassed : resultsList.filter(r => r.passed).length);
    const totalCount = lastRunResult.totalCount != null
      ? lastRunResult.totalCount
      : (lastRunResult.totalTests != null ? lastRunResult.totalTests : resultsList.length);

    // 1. Verdict Header Badge
    const verdictBadge = document.getElementById('test-result-verdict-badge');
    if (verdictBadge) {
      if (isSuccess) {
        verdictBadge.className = "flex items-center gap-2 text-sm font-bold text-emerald-700";
        verdictBadge.innerHTML = `
          <svg class="w-4 h-4 text-emerald-600" fill="none" stroke="currentColor" viewBox="0 0 24 24"><circle cx="12" cy="12" r="9" stroke-width="2"></circle><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2.5" d="M9 12l2 2 4-4"></path></svg>
          <span>${escapeHtml(statusText)}</span>
        `;
      } else {
        verdictBadge.className = "flex items-center gap-2 text-sm font-bold text-rose-600";
        verdictBadge.innerHTML = `
          <svg class="w-4 h-4 text-rose-600" fill="none" stroke="currentColor" viewBox="0 0 24 24"><circle cx="12" cy="12" r="9" stroke-width="2"></circle><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2.5" d="M15 9l-6 6m0-6l6 6"></path></svg>
          <span>${escapeHtml(statusText)}</span>
        `;
      }
    }

    // 2. Metrics Row
    const metricsEl = document.getElementById('test-result-metrics');
    if (metricsEl) {
      const engineText = lastRunResult.engine ? ` &middot; Engine: <span class="font-semibold text-emerald-700">${escapeHtml(lastRunResult.engine)}</span>` : '';
      metricsEl.innerHTML = `
        <span class="${isSuccess ? 'text-emerald-700' : 'text-rose-600'} font-bold">${passedCount} / ${totalCount}</span> testcases passed
        &middot; Runtime: <span class="font-semibold text-gray-800">${escapeHtml(lastRunResult.runtime || '32 ms')}</span>
        &middot; Memory: <span class="font-semibold text-gray-800">${escapeHtml(lastRunResult.memory || '17.0 MB')}</span>${engineText}
      `;
    }

    // 3. Interactive Case Tabs
    const tabsContainer = document.getElementById('test-result-case-tabs');
    if (tabsContainer) {
      tabsContainer.innerHTML = resultsList.map((r, idx) => {
        const isSelected = idx === activeCaseIndex;
        const casePassed = !!r.passed;
        const dotHtml = casePassed
          ? `<span class="text-emerald-600 font-bold text-xs">&check;</span>`
          : `<span class="text-rose-600 font-bold text-xs">&times;</span>`;
        const btnClass = isSelected
          ? (casePassed
              ? "px-3 py-1 text-xs font-semibold rounded-md bg-emerald-50 text-emerald-800 border border-emerald-200 flex items-center gap-1.5 shadow-xs"
              : "px-3 py-1 text-xs font-semibold rounded-md bg-rose-50 text-rose-700 border border-rose-200 flex items-center gap-1.5 shadow-xs")
          : "px-3 py-1 text-xs font-medium rounded-md bg-gray-50 text-gray-600 hover:bg-gray-100 border border-gray-200 flex items-center gap-1.5";
        return `
          <button type="button" data-res-index="${idx}" class="result-case-tab-btn ${btnClass}">
            ${dotHtml}
            <span>${escapeHtml(r.name || `Case ${idx + 1}`)}</span>
          </button>
        `;
      }).join('');

      tabsContainer.querySelectorAll('.result-case-tab-btn').forEach(btn => {
        btn.addEventListener('click', () => {
          activeCaseIndex = parseInt(btn.getAttribute('data-res-index'), 10) || 0;
          renderTestResultView(false);
        });
      });
    }

    // 4. Populate Input, Output, Expected boxes for the selected case
    const selectedCase = resultsList[activeCaseIndex] || resultsList[0];
    const inputValEl = document.getElementById('test-case-input');
    const outputValEl = document.getElementById('test-case-output');
    const outputBoxEl = document.getElementById('test-case-output-box');
    const expectedValEl = document.getElementById('test-case-expected');

    if (selectedCase) {
      if (inputValEl) inputValEl.textContent = selectedCase.input || '—';
      if (outputValEl) {
        const displayOut = (lastRunResult.compileOutput && !selectedCase.yourOutput)
          ? lastRunResult.compileOutput
          : (selectedCase.yourOutput !== undefined && selectedCase.yourOutput !== null ? selectedCase.yourOutput : 'null');
        outputValEl.textContent = displayOut;
        outputValEl.className = selectedCase.passed
          ? "text-emerald-800 font-bold break-all"
          : "text-rose-700 font-bold break-all whitespace-pre-wrap";
      }
      if (outputBoxEl) {
        outputBoxEl.className = selectedCase.passed
          ? "p-2.5 rounded-lg bg-emerald-50 border border-emerald-200 space-y-1"
          : "p-2.5 rounded-lg bg-rose-50 border border-rose-200 space-y-1";
      }
      if (expectedValEl) expectedValEl.textContent = selectedCase.expected || '—';
    }
  }

  function switchConsoleTab(tab) {
    activeConsoleTab = tab;
    const tabCases = document.getElementById('tab-test-cases');
    const tabResults = document.getElementById('tab-test-results');
    const casesView = document.getElementById('view-test-cases');
    const resultsView = document.getElementById('view-test-results');

    if (tab === 'cases') {
      if (tabCases) tabCases.className = "pb-2 pt-2 border-b-2 border-emerald-600 text-emerald-800 font-semibold text-xs flex items-center gap-1.5 cursor-pointer";
      if (tabResults) tabResults.className = "pb-2 pt-2 border-b-2 border-transparent text-gray-500 hover:text-gray-700 text-xs font-medium flex items-center gap-1.5 cursor-pointer";
      if (casesView) casesView.classList.remove('hidden');
      if (resultsView) resultsView.classList.add('hidden');
      renderTestCaseView();
    } else {
      if (tabResults) tabResults.className = "pb-2 pt-2 border-b-2 border-emerald-600 text-emerald-800 font-semibold text-xs flex items-center gap-1.5 cursor-pointer";
      if (tabCases) tabCases.className = "pb-2 pt-2 border-b-2 border-transparent text-gray-500 hover:text-gray-700 text-xs font-medium flex items-center gap-1.5 cursor-pointer";
      if (resultsView) resultsView.classList.remove('hidden');
      if (casesView) casesView.classList.add('hidden');
    }
  }

  function renderEditorialForProblem(problem) {
    const titleEl = document.getElementById('ide-editorial-title');
    const bodyEl = document.getElementById('ide-editorial-body');
    if (!bodyEl) return;

    const topics = (problem && problem.topics && problem.topics.length > 0) ? problem.topics.join(', ') : 'Hash Map & Single-Pass Iteration';
    if (titleEl) {
      titleEl.textContent = `Optimal Approach for ${problem.title || 'Challenge'}`;
    }

    bodyEl.innerHTML = `
      <div class="p-4 rounded-xl bg-gray-50 border border-gray-200 space-y-2">
        <div class="text-xs font-bold text-gray-900">1. Intuition &amp; Core Pattern (${escapeHtml(topics)})</div>
        <p class="text-xs text-gray-600 leading-relaxed">
          A brute-force nested loop checks every pair in <code>O(n²)</code> time. By maintaining an auxiliary lookup structure (such as a Hash Map or Set) as we iterate through the input once, we can verify complement or membership conditions in <code>O(1)</code> amortized time per element.
        </p>
      </div>

      <div class="p-4 rounded-xl bg-gray-50 border border-gray-200 space-y-2">
        <div class="text-xs font-bold text-gray-900">2. Step-by-Step Algorithm</div>
        <ol class="list-decimal pl-4 space-y-1.5 text-xs text-gray-600">
          <li>Initialize an empty dictionary/map <code>seen</code> to store previously visited values and their indices.</li>
          <li>Iterate through each element <code>num</code> at index <code>i</code>.</li>
          <li>Check whether the target complement already exists in <code>seen</code>. If found, return the pair immediately.</li>
          <li>Otherwise, record <code>seen[num] = i</code> and continue scanning.</li>
        </ol>
      </div>

      <div class="grid grid-cols-2 gap-3">
        <div class="p-3.5 rounded-xl bg-emerald-50 border border-emerald-200">
          <div class="text-[10px] font-bold uppercase tracking-wider text-emerald-700">Time Complexity</div>
          <div class="text-sm font-bold font-mono text-gray-900 mt-0.5">O(n)</div>
          <div class="text-[11px] text-gray-500 mt-0.5">Single pass over input with O(1) hash lookups</div>
        </div>
        <div class="p-3.5 rounded-xl bg-emerald-50 border border-emerald-200">
          <div class="text-[10px] font-bold uppercase tracking-wider text-emerald-700">Space Complexity</div>
          <div class="text-sm font-bold font-mono text-gray-900 mt-0.5">O(n)</div>
          <div class="text-[11px] text-gray-500 mt-0.5">At most n elements stored in the hash table</div>
        </div>
      </div>
    `;
  }

  async function loadProblem(problemId) {
    if (problemId) {
      window.CodeForgeCurrentProblemId = parseInt(problemId, 10) || problemId;
    }
    const id = window.CodeForgeCurrentProblemId || 104;
    window.CodeForgeCurrentProblemId = id;

    let problem = (CodeForgeData.problems || []).find(p => Number(p.id) === Number(id));

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

    // Reset run result when opening a problem so user sees clean Testcase tab & "You must run your code first"
    lastRunResult = null;
    activeCaseIndex = 0;
    timerSeconds = 0;

    // Update Prev / Next counter in subheader
    const list = getProblemList();
    const currentIdx = list.findIndex(p => Number(p.id) === Number(problem.id));
    const counterEl = document.getElementById('ide-problem-counter');
    if (counterEl) {
      const pos = currentIdx >= 0 ? currentIdx + 1 : 1;
      counterEl.textContent = `${pos} / ${Math.max(1, list.length)}`;
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

    // 3. Difficulty & Topic Pills
    const d = (problem.difficulty || 'Medium').toLowerCase();
    const diffClass = d === 'easy'
      ? 'px-2.5 py-0.5 rounded-full text-xs font-medium bg-emerald-50 text-emerald-700 border border-emerald-200'
      : (d === 'hard'
        ? 'px-2.5 py-0.5 rounded-full text-xs font-medium bg-rose-50 text-rose-700 border border-rose-200'
        : 'px-2.5 py-0.5 rounded-full text-xs font-medium bg-amber-50 text-amber-700 border border-amber-200');

    const pillsContainer = document.getElementById('ide-problem-pills');
    if (pillsContainer) {
      const topics = problem.topics || [];
      const topicsHtml = topics.map(t =>
        `<span class="px-2.5 py-0.5 rounded-full text-xs font-medium bg-gray-100 text-gray-700 border border-gray-200">${escapeHtml(t)}</span>`
      ).join(' ');
      pillsContainer.innerHTML = `
        <span id="ide-problem-difficulty" class="${diffClass}">${escapeHtml(problem.difficulty || 'Medium')}</span>
        ${topicsHtml}
        ${problem.isMine ? '<span class="px-2.5 py-0.5 rounded-full text-xs font-medium bg-indigo-50 text-indigo-700 border border-indigo-200">Yours</span>' : ''}
      `;
    }

    // 4. Bookmark Button Icon
    const bookmarkBtn = document.getElementById('ide-btn-bookmark');
    if (bookmarkBtn) {
      bookmarkBtn.innerHTML = problem.saved
        ? `<svg class="w-5 h-5 fill-emerald-600 text-emerald-600" viewBox="0 0 24 24"><path d="M5 5a2 2 0 012-2h10a2 2 0 012 2v16l-7-3.5L5 21V5z"></path></svg>`
        : `<svg class="w-5 h-5 fill-none stroke-current" viewBox="0 0 24 24" stroke-width="2"><path stroke-linecap="round" stroke-linejoin="round" d="M5 5a2 2 0 012-2h10a2 2 0 012 2v16l-7-3.5L5 21V5z"></path></svg>`;
    }

    // 5. Meta
    const metaEl = document.getElementById('ide-problem-meta');
    if (metaEl) {
      metaEl.textContent = `${problem.author || 'CodeForge'} · Updated ${problem.updatedDate || 'Recently'}`;
    }

    // 6. Description & Constraints
    const descEl = document.getElementById('ide-problem-description');
    if (descEl) {
      const descText = problem.description || 'Solve the problem as specified and return the expected result.';
      descEl.innerHTML = descText
        .split('\n\n')
        .map(p => `<p>${escapeHtml(p).replace(/`([^`]+)`/g, '<code class="px-1 py-0.5 rounded bg-gray-100 font-mono text-xs text-emerald-800">$1</code>')}</p>`)
        .join('');
    }

    const constraintsEl = document.getElementById('ide-problem-constraints');
    if (constraintsEl) {
      const cText = problem.constraints || "1 <= input.length <= 10^4\nTime complexity target: O(n)";
      const lines = cText.split('\n').filter(l => l.trim().length > 0);
      constraintsEl.innerHTML = lines.map(c => `<li>${escapeHtml(c.replace(/^[*-]\s*/, ''))}</li>`).join('');
    }

    // 7. Dynamic Examples & Editorial
    renderProblemExamples(problem);
    renderEditorialForProblem(problem);

    // 8. Re-render code editor and console
    renderCodeEditor();
    renderTestCaseView();
    renderTestResultView(false);
    switchConsoleTab('cases');
    switchIdeTab('description');
  }

  /**
   * Switches the left IDE pane view between 'description', 'editorial', and 'submissions'.
   */
  async function switchIdeTab(tab) {
    const tabDesc = document.getElementById('ide-tab-description');
    const tabEdit = document.getElementById('ide-tab-editorial');
    const tabSub = document.getElementById('ide-tab-submissions');
    const viewDesc = document.getElementById('ide-view-description');
    const viewEdit = document.getElementById('ide-view-editorial');
    const viewSub = document.getElementById('ide-view-submissions');

    const inactiveClass = "pb-2.5 border-b-2 border-transparent hover:text-gray-800 cursor-pointer";
    const activeClass = "pb-2.5 border-b-2 border-emerald-600 text-emerald-800 font-semibold cursor-pointer";

    if (tabDesc) tabDesc.className = tab === 'description' ? activeClass : inactiveClass;
    if (tabEdit) tabEdit.className = tab === 'editorial' ? activeClass : inactiveClass;
    if (tabSub) tabSub.className = tab === 'submissions' ? activeClass : inactiveClass;

    if (viewDesc) viewDesc.classList.toggle('hidden', tab !== 'description');
    if (viewEdit) viewEdit.classList.toggle('hidden', tab !== 'editorial');
    if (viewSub) viewSub.classList.toggle('hidden', tab !== 'submissions');

    if (tab === 'submissions') {
      loadSubmissionsTab();
    }
  }

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

  function bindEvents() {
    // Run & Submit buttons
    const runBtn = document.getElementById('btn-run-code');
    if (runBtn) runBtn.addEventListener('click', runCode);

    const submitBtn = document.getElementById('btn-submit-solution');
    if (submitBtn) submitBtn.addEventListener('click', submitSolution);

    // Left pane tabs (Description, Editorial, Submissions)
    const tabDesc = document.getElementById('ide-tab-description');
    const tabEdit = document.getElementById('ide-tab-editorial');
    const tabSub = document.getElementById('ide-tab-submissions');
    if (tabDesc) tabDesc.addEventListener('click', () => switchIdeTab('description'));
    if (tabEdit) tabEdit.addEventListener('click', () => switchIdeTab('editorial'));
    if (tabSub) tabSub.addEventListener('click', () => switchIdeTab('submissions'));

    // + Note button inside IDE
    const addNoteBtn = document.getElementById('ide-btn-add-note');
    if (addNoteBtn) {
      addNoteBtn.addEventListener('click', () => {
        if (window.CodeForgeNotes && CodeForgeNotes.openCreateModal) {
          CodeForgeNotes.openCreateModal();
          const selectEl = document.getElementById('note-problem-select');
          if (selectEl && window.CodeForgeCurrentProblemId) {
            selectEl.value = String(window.CodeForgeCurrentProblemId);
          }
        }
      });
    }

    // Prev / Next problem buttons in IDE subheader
    const prevBtn = document.getElementById('ide-btn-prev-problem');
    const nextBtn = document.getElementById('ide-btn-next-problem');
    if (prevBtn) {
      prevBtn.addEventListener('click', () => {
        const list = getProblemList();
        if (list.length === 0) return;
        const idx = list.findIndex(p => Number(p.id) === Number(window.CodeForgeCurrentProblemId));
        const prevIdx = idx > 0 ? idx - 1 : list.length - 1;
        loadProblem(list[prevIdx].id);
      });
    }
    if (nextBtn) {
      nextBtn.addEventListener('click', () => {
        const list = getProblemList();
        if (list.length === 0) return;
        const idx = list.findIndex(p => Number(p.id) === Number(window.CodeForgeCurrentProblemId));
        const nextIdx = (idx >= 0 && idx < list.length - 1) ? idx + 1 : 0;
        loadProblem(list[nextIdx].id);
      });
    }

    // Focus Mode Toggle
    const toggleFocusMode = () => {
      document.body.classList.toggle('focus-mode');
      const isFocus = document.body.classList.contains('focus-mode');
      const focusBtn = document.getElementById('btn-focus-mode');
      if (focusBtn) {
        focusBtn.innerHTML = isFocus
          ? `<svg class="w-4 h-4 text-emerald-600" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"></path></svg> Exit Focus`
          : `<svg class="w-4 h-4 text-gray-500" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 8V4m0 0h4M4 4l5 5m11-1V4m0 0h-4m4 0l-5 5M4 16v4m0 0h4m-4 0l5-5m11 5l-5-5m5 5v-4m0 4h-4"></path></svg> Focus mode`;
      }
    };

    const focusBtn = document.getElementById('btn-focus-mode');
    if (focusBtn) focusBtn.addEventListener('click', toggleFocusMode);

    // Editor toolbar buttons: Reset code, Format code, Expand
    const resetCodeBtn = document.getElementById('btn-ide-reset-code');
    if (resetCodeBtn) {
      resetCodeBtn.addEventListener('click', () => {
        const p = getCurrentProblem();
        if (userCodeStore[p.id]) {
          delete userCodeStore[p.id][currentLanguage];
        }
        renderCodeEditor();
        if (window.CodeForgeApp) CodeForgeApp.showToast("Code reset to default starter template.", "info");
      });
    }

    const formatCodeBtn = document.getElementById('btn-ide-format-code');
    if (formatCodeBtn) {
      formatCodeBtn.addEventListener('click', () => {
        const p = getCurrentProblem();
        const current = getActiveCode(p, currentLanguage);
        const formatted = current
          .split('\n')
          .map(line => line.replace(/\t/g, '    ').replace(/\s+$/, ''))
          .join('\n');
        setActiveCode(p, currentLanguage, formatted);
        renderCodeEditor();
        if (window.CodeForgeApp) CodeForgeApp.showToast("Code formatted.", "info");
      });
    }

    const expandBtn = document.getElementById('btn-ide-expand');
    if (expandBtn) expandBtn.addEventListener('click', toggleFocusMode);

    // Console collapse/expand button
    const toggleConsoleBtn = document.getElementById('btn-toggle-console');
    if (toggleConsoleBtn) {
      toggleConsoleBtn.addEventListener('click', () => {
        const consolePane = document.getElementById('ide-bottom-console');
        if (!consolePane) return;
        if (consolePane.classList.contains('h-64')) {
          consolePane.classList.remove('h-64');
          consolePane.classList.add('h-9');
        } else {
          consolePane.classList.remove('h-9');
          consolePane.classList.add('h-64');
        }
      });
    }

    // Console Tabs switching (Testcase vs Test Result)
    const tabCases = document.getElementById('tab-test-cases');
    const tabResults = document.getElementById('tab-test-results');
    if (tabCases) tabCases.addEventListener('click', () => switchConsoleTab('cases'));
    if (tabResults) tabResults.addEventListener('click', () => {
      switchConsoleTab('results');
      renderTestResultView(false);
    });

    // Editable custom input in Testcase tab
    const customInputEl = document.getElementById('testcase-custom-input');
    if (customInputEl) {
      customInputEl.addEventListener('input', (e) => {
        const cases = getSampleTestCases(getCurrentProblem());
        if (cases[activeCaseIndex]) {
          cases[activeCaseIndex].input = e.target.value;
        }
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

    // Live code input typing, cursor tracking, Tab indentation, and scroll synchronization
    const textarea = document.getElementById('editor-real-input');
    if (textarea) {
      textarea.addEventListener('input', () => {
        const p = getCurrentProblem();
        setActiveCode(p, currentLanguage, textarea.value);
        renderCodeEditor();
      });

      textarea.addEventListener('click', () => renderCodeEditor());
      textarea.addEventListener('keyup', () => renderCodeEditor());

      textarea.addEventListener('scroll', () => {
        const codeContainer = document.getElementById('editor-code-content');
        const linesContainer = document.getElementById('editor-line-numbers');
        if (codeContainer) {
          codeContainer.scrollTop = textarea.scrollTop;
          codeContainer.scrollLeft = textarea.scrollLeft;
        }
        if (linesContainer) {
          linesContainer.scrollTop = textarea.scrollTop;
        }
      });

      textarea.addEventListener('keydown', (e) => {
        if (e.key === 'Tab') {
          e.preventDefault();
          const start = textarea.selectionStart;
          const end = textarea.selectionEnd;
          const val = textarea.value;
          textarea.value = val.substring(0, start) + '    ' + val.substring(end);
          textarea.selectionStart = textarea.selectionEnd = start + 4;
          const p = getCurrentProblem();
          setActiveCode(p, currentLanguage, textarea.value);
          renderCodeEditor();
        }
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
   * Executes code against sample test cases and displays execution metrics in Test Result tab.
   */
  async function runCode() {
    const runBtn = document.getElementById('btn-run-code');
    const originalContent = runBtn ? runBtn.innerHTML : '';
    const textarea = document.getElementById('editor-real-input');
    const code = textarea ? textarea.value : '';
    const problemId = window.CodeForgeCurrentProblemId || 104;

    if (runBtn) {
      runBtn.disabled = true;
      runBtn.innerHTML = `<span>Running...</span>`;
    }

    // Expand console if collapsed & switch to Test Result tab with loading spinner
    const consolePane = document.getElementById('ide-bottom-console');
    if (consolePane && consolePane.classList.contains('h-9')) {
      consolePane.classList.remove('h-9');
      consolePane.classList.add('h-64');
    }
    switchConsoleTab('results');
    renderTestResultView(true);

    try {
      const sampleCases = getSampleTestCases(getCurrentProblem()).map(c => ({
        name: c.name,
        input: c.input,
        expected: c.expected
      }));
      const res = await CodeForgeAPI.runCode({
        problemId,
        language: currentLanguage,
        code,
        customInput: '',
        customCases: sampleCases
      });
      lastRunResult = res;
      renderTestResultView(false);
      if (window.CodeForgeApp) {
        CodeForgeApp.showToast(`${res.status || 'Done'} (${res.runtime || '32 ms'})`, res.success ? "success" : "warn");
      }
    } finally {
      if (runBtn) {
        runBtn.disabled = false;
        runBtn.innerHTML = originalContent;
      }
    }
  }

  /**
   * Submits code to backend judge and refreshes library problem status upon success.
   */
  async function submitSolution() {
    const submitBtn = document.getElementById('btn-submit-solution');
    const originalContent = submitBtn ? submitBtn.innerHTML : '';
    const textarea = document.getElementById('editor-real-input');
    const code = textarea ? textarea.value : '';
    const problemId = window.CodeForgeCurrentProblemId || 104;

    if (submitBtn) {
      submitBtn.disabled = true;
      submitBtn.innerHTML = `<span>Judging...</span>`;
    }

    const consolePane = document.getElementById('ide-bottom-console');
    if (consolePane && consolePane.classList.contains('h-9')) {
      consolePane.classList.remove('h-9');
      consolePane.classList.add('h-64');
    }
    switchConsoleTab('results');
    renderTestResultView(true);

    try {
      const res = await CodeForgeAPI.submitSolution({
        problemId,
        language: currentLanguage,
        code
      });

      lastRunResult = {
        success: !!res.success,
        status: res.status || (res.success ? 'Accepted' : 'Wrong Answer'),
        passedCount: res.testsPassed,
        totalCount: res.totalTests,
        runtime: res.runtime || '32 ms',
        memory: res.memory || '17.0 MB',
        engine: res.engine,
        compileOutput: res.compileOutput
      };
      renderTestResultView(false);

      if (res && res.success) {
        showSuccessModal();

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

        const currentProb = (CodeForgeData.problems || []).find(p => Number(p.id) === Number(problemId));
        if (currentProb) {
          currentProb.status = 'solved';
        }
        const statusHeaderEl = document.getElementById('ide-header-problem-status');
        if (statusHeaderEl) {
          statusHeaderEl.textContent = 'Solved';
          statusHeaderEl.className = 'px-2 py-0.5 rounded text-[11px] font-medium bg-emerald-50 text-emerald-700 border border-emerald-200';
        }
      } else {
        if (window.CodeForgeApp) {
          CodeForgeApp.showToast(res?.status || res?.message || "Wrong Answer", "warn");
        }
      }
    } finally {
      if (submitBtn) {
        submitBtn.disabled = false;
        submitBtn.innerHTML = originalContent;
      }
    }
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
    submitSolution,
    loadProblem
  };
})();
