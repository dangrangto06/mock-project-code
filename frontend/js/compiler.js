/**
 * @file compiler.js
 * @description CodeForge Online Compiler & Playground powered by Judge0 CE API.
 * @module CodeForgeCompiler
 */

window.CodeForgeCompiler = (function () {
  'use strict';

  const STORAGE_PREFIX = 'codeforge_compiler_code_';
  const STDIN_STORAGE_KEY = 'codeforge_compiler_stdin';

  const DEFAULT_TEMPLATES = {
    python: {
      languageId: 100,
      filename: 'main.py',
      label: 'Python (3.12.5)',
      stdin: '15 27\nCodeForge',
      code: `import sys

def solve():
    lines = sys.stdin.read().strip().splitlines()
    if lines:
        nums = list(map(int, lines[0].split()))
        name = lines[1] if len(lines) > 1 else "Developer"
        print(f"Hello, {name}!")
        print(f"Sum = {sum(nums)}")
    else:
        print("Hello from Judge0 Python Compiler!")

if __name__ == "__main__":
    solve()
`
    },
    cpp: {
      languageId: 105,
      filename: 'main.cpp',
      label: 'C++ (GCC 14.1.0)',
      stdin: '15 27\nCodeForge',
      code: `#include <iostream>
#include <string>
using namespace std;

int main() {
    ios::sync_with_stdio(false);
    cin.tie(nullptr);

    long long a = 0, b = 0;
    string name = "Developer";
    if (cin >> a >> b) {
        cin.ignore();
        getline(cin, name);
        if (name.empty()) name = "Developer";
        cout << "Hello, " << name << "!\\n";
        cout << "Sum = " << (a + b) << "\\n";
    } else {
        cout << "Hello from Judge0 C++ Compiler!\\n";
    }
    return 0;
}
`
    },
    java: {
      languageId: 91,
      filename: 'Main.java',
      label: 'Java (JDK 17.0.6)',
      stdin: '15 27\nCodeForge',
      code: `import java.util.*;

public class Main {
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        if (sc.hasNextLong()) {
            long a = sc.nextLong();
            long b = sc.hasNextLong() ? sc.nextLong() : 0;
            sc.nextLine();
            String name = sc.hasNextLine() ? sc.nextLine().trim() : "Developer";
            if (name.isEmpty()) name = "Developer";
            System.out.println("Hello, " + name + "!");
            System.out.println("Sum = " + (a + b));
        } else {
            System.out.println("Hello from Judge0 Java Compiler!");
        }
    }
}
`
    },
    javascript: {
      languageId: 97,
      filename: 'main.js',
      label: 'JavaScript (Node.js 20.17.0)',
      stdin: '15 27\nCodeForge',
      code: `const fs = require('fs');

const raw = fs.readFileSync(0, 'utf8').trim().split(/\\r?\\n/);
if (raw[0]) {
  const [a, b] = raw[0].trim().split(/\\s+/).map(Number);
  const name = raw[1] ? raw[1].trim() : 'Developer';
  console.log(\`Hello, \${name}!\`);
  console.log(\`Sum = \${(a || 0) + (b || 0)}\`);
} else {
  console.log('Hello from Judge0 Node.js Compiler!');
}
`
    },
    c: {
      languageId: 103,
      filename: 'main.c',
      label: 'C (GCC 14.1.0)',
      stdin: '15 27',
      code: `#include <stdio.h>

int main(void) {
    long long a = 0, b = 0;
    if (scanf("%lld %lld", &a, &b) == 2) {
        printf("Sum = %lld\\n", a + b);
    } else {
        printf("Hello from Judge0 C Compiler!\\n");
    }
    return 0;
}
`
    },
    typescript: {
      languageId: 101,
      filename: 'main.ts',
      label: 'TypeScript (5.6.2)',
      stdin: '15 27',
      code: `import * as fs from 'fs';

const add = (x: number, y: number): number => x + y;
const input = fs.readFileSync(0, 'utf8').trim();
if (input) {
  const [a, b] = input.split(/\\s+/).map(Number);
  console.log(\`Sum = \${add(a || 0, b || 0)}\`);
} else {
  console.log('Hello from Judge0 TypeScript Compiler!');
}
`
    },
    go: {
      languageId: 107,
      filename: 'main.go',
      label: 'Go (1.23.5)',
      stdin: '15 27',
      code: `package main

import "fmt"

func main() {
    var a, b int64
    if _, err := fmt.Scan(&a, &b); err == nil {
        fmt.Printf("Sum = %d\\n", a+b)
    } else {
        fmt.Println("Hello from Judge0 Go Compiler!")
    }
}
`
    },
    rust: {
      languageId: 108,
      filename: 'main.rs',
      label: 'Rust (1.85.0)',
      stdin: '15 27',
      code: `use std::io::{self, Read};

fn main() {
    let mut buf = String::new();
    io::stdin().read_to_string(&mut buf).unwrap();
    let nums: Vec<i64> = buf.split_whitespace().filter_map(|s| s.parse().ok()).collect();
    if nums.len() >= 2 {
        println!("Sum = {}", nums[0] + nums[1]);
    } else {
        println!("Hello from Judge0 Rust Compiler!");
    }
}
`
    }
  };

  let initialized = false;
  let isRunning = false;
  let currentLanguageKey = 'cpp';
  let currentLanguageId = 105;
  let currentLanguageName = 'C++ (GCC 14.1.0)';
  let allLanguages = [];
  let lastCompileResult = null;
  let activeOutputTab = 'output'; // 'output' | 'json'

  function escapeHtml(val) {
    if (val === null || val === undefined) return '';
    return String(val).replace(/[&<>"']/g, ch => (
      { '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[ch]
    ));
  }

  function getFileName(langId, key, name) {
    if (key && DEFAULT_TEMPLATES[key]) return DEFAULT_TEMPLATES[key].filename;
    const lower = (name || '').toLowerCase();
    if (lower.includes('python')) return 'main.py';
    if (lower.includes('c++')) return 'main.cpp';
    if (lower.includes('java (')) return 'Main.java';
    if (lower.includes('javascript') || lower.includes('node')) return 'main.js';
    if (lower.includes('typescript')) return 'main.ts';
    if (lower.includes('c (')) return 'main.c';
    if (lower.includes('go (')) return 'main.go';
    if (lower.includes('rust')) return 'main.rs';
    if (lower.includes('c#')) return 'Program.cs';
    if (lower.includes('kotlin')) return 'Main.kt';
    if (lower.includes('php')) return 'main.php';
    if (lower.includes('ruby')) return 'main.rb';
    if (lower.includes('bash')) return 'script.sh';
    return 'main.txt';
  }

  function getSavedOrDefaultCode(langId, key, name) {
    try {
      const saved = localStorage.getItem(STORAGE_PREFIX + langId);
      if (saved !== null) return saved;
    } catch (_) {}

    if (key && DEFAULT_TEMPLATES[key]) {
      return DEFAULT_TEMPLATES[key].code;
    }
    return `// ${name || 'CodeForge Judge0 Compiler'}\n// Write your standalone program here and click Compile & Run.\n`;
  }

  function saveCodeForLanguage(langId, code) {
    try {
      localStorage.setItem(STORAGE_PREFIX + langId, code);
    } catch (_) {}
  }

  function getLineLanguageFamily() {
    const lower = (currentLanguageName || '').toLowerCase();
    if (currentLanguageKey === 'python' || lower.includes('python') || lower.includes('ruby') || lower.includes('bash')) {
      return 'hash';
    }
    return 'cstyle';
  }

  function highlightLine(line) {
    if (!line) return '';
    const family = getLineLanguageFamily();
    let escaped = line
      .replace(/&/g, '&amp;')
      .replace(/</g, '&lt;')
      .replace(/>/g, '&gt;');

    const commentPrefix = family === 'hash' ? '#' : '//';
    if (escaped.trim().startsWith(commentPrefix)) {
      return `<span class="syn-comment">${escaped}</span>`;
    }
    if (family === 'cstyle' && escaped.trim().startsWith('#')) {
      return `<span class="syn-def">${escaped}</span>`;
    }

    const keywords = family === 'hash'
      ? /\b(def|class|return|if|in|for|while|import|from|as|else|elif|try|except|pass|with|and|or|not|None|True|False)\b/g
      : /\b(import|package|class|struct|public|private|protected|static|final|new|return|if|else|for|while|do|switch|case|break|continue|try|catch|using|namespace|function|const|let|var|fn|mut|use|pub|func|true|false|null|nullptr)\b/g;
    escaped = escaped.replace(keywords, '<span class="syn-keyword">$1</span>');

    const types = family === 'hash'
      ? /\b(print|input|len|range|map|list|dict|set|int|str|float|sum|enumerate)\b/g
      : /\b(int|long|double|float|bool|boolean|char|void|auto|string|String|vector|unordered_map|unordered_set|Map|HashMap|List|Scanner|System|Console|cout|cin|endl|printf|scanf|fmt|println|i64|String)\b/g;
    escaped = escaped.replace(types, '<span class="syn-type">$1</span>');

    escaped = escaped.replace(family === 'hash' ? /(#[^<]*)/g : /(\/\/[^<]*)/g, '<span class="syn-comment">$1</span>');
    escaped = escaped.replace(/(\b\d+\b)/g, '<span class="syn-number">$1</span>');
    return escaped;
  }

  function renderEditor() {
    const textarea = document.getElementById('compiler-code-input');
    const linesContainer = document.getElementById('compiler-line-numbers');
    const codeContainer = document.getElementById('compiler-code-content');
    if (!textarea || !linesContainer || !codeContainer) return;

    const rawCode = textarea.value;
    const lines = rawCode.split('\n');

    let activeLineNum = 1;
    let activeColNum = 1;
    if (typeof textarea.selectionStart === 'number') {
      const uptoCursor = textarea.value.substring(0, textarea.selectionStart);
      const cursorLines = uptoCursor.split('\n');
      activeLineNum = cursorLines.length;
      activeColNum = cursorLines[cursorLines.length - 1].length + 1;
    }

    const lnColEl = document.getElementById('compiler-ln-col');
    if (lnColEl) lnColEl.textContent = `Ln ${activeLineNum}, Col ${activeColNum}`;

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

    const fileEl = document.getElementById('compiler-filename');
    if (fileEl) fileEl.textContent = getFileName(currentLanguageId, currentLanguageKey, currentLanguageName);

    const langIdEl = document.getElementById('compiler-lang-id-badge');
    if (langIdEl) langIdEl.textContent = `language_id: ${currentLanguageId}`;

    const statsEl = document.getElementById('compiler-cursor-stats');
    if (statsEl) statsEl.textContent = `UTF-8 · ${currentLanguageName}`;
  }

  function populateLanguageDropdown(langs) {
    const select = document.getElementById('compiler-language-select');
    if (!select) return;

    const list = Array.isArray(langs) && langs.length > 0 ? langs : [
      { id: 105, name: 'C++ (GCC 14.1.0)', key: 'cpp' },
      { id: 100, name: 'Python (3.12.5)', key: 'python' },
      { id: 91, name: 'Java (JDK 17.0.6)', key: 'java' },
      { id: 97, name: 'JavaScript (Node.js 20.17.0)', key: 'javascript' },
      { id: 103, name: 'C (GCC 14.1.0)', key: 'c' },
      { id: 101, name: 'TypeScript (5.6.2)', key: 'typescript' },
      { id: 107, name: 'Go (1.23.5)', key: 'go' },
      { id: 108, name: 'Rust (1.85.0)', key: 'rust' }
    ];
    allLanguages = list.filter(l => !l.isArchived);

    const flagshipIds = new Set([105, 100, 91, 97, 103, 101, 107, 108]);
    const featured = allLanguages.filter(l => flagshipIds.has(l.id));
    const others = allLanguages.filter(l => !flagshipIds.has(l.id));

    let html = '<optgroup label="Popular Compilers (Judge0 CE)">';
    featured.forEach(l => {
      html += `<option value="${l.id}" data-key="${escapeHtml(l.key || '')}">${escapeHtml(l.name)}</option>`;
    });
    html += '</optgroup>';

    if (others.length > 0) {
      html += '<optgroup label="All Judge0 Languages">';
      others.forEach(l => {
        html += `<option value="${l.id}" data-key="${escapeHtml(l.key || '')}">${escapeHtml(l.name)}</option>`;
      });
      html += '</optgroup>';
    }

    select.innerHTML = html;
    if (allLanguages.some(l => l.id === currentLanguageId)) {
      select.value = String(currentLanguageId);
    } else if (allLanguages.length > 0) {
      currentLanguageId = allLanguages[0].id;
      currentLanguageName = allLanguages[0].name;
      currentLanguageKey = allLanguages[0].key || '';
      select.value = String(currentLanguageId);
    }
  }

  async function refreshEngineStatus() {
    const pill = document.getElementById('compiler-engine-pill');
    if (!pill || !window.CodeForgeAPI || !CodeForgeAPI.getJudge0Status) return;

    try {
      const status = await CodeForgeAPI.getJudge0Status();
      if (status && status.live) {
        pill.className = 'inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full text-[11px] font-semibold bg-emerald-50 text-emerald-700 border border-emerald-200';
        pill.innerHTML = `<span class="w-2 h-2 rounded-full bg-emerald-500 animate-pulse"></span><span>${escapeHtml(status.engine || 'Judge0 CE')} Live${status.version ? ' · v' + escapeHtml(status.version) : ''}</span>`;
      } else {
        pill.className = 'inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full text-[11px] font-semibold bg-amber-50 text-amber-700 border border-amber-200';
        pill.innerHTML = `<span class="w-2 h-2 rounded-full bg-amber-500"></span><span>${escapeHtml(status?.engine || 'Local Fallback')}</span>`;
      }
    } catch (_) {
      pill.className = 'inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full text-[11px] font-semibold bg-gray-100 text-gray-600 border border-gray-200';
      pill.innerHTML = `<span class="w-2 h-2 rounded-full bg-gray-400"></span><span>Offline Mode</span>`;
    }
  }

  function selectLanguage(langId) {
    const prevKey = currentLanguageKey;
    const prevDefaultStdin = (prevKey && DEFAULT_TEMPLATES[prevKey]) ? (DEFAULT_TEMPLATES[prevKey].stdin || '') : '';

    const numericId = parseInt(langId, 10) || 105;
    const found = allLanguages.find(l => l.id === numericId);
    currentLanguageId = numericId;
    currentLanguageName = found ? found.name : `Language #${numericId}`;
    currentLanguageKey = (found && found.key) ? found.key : '';

    const textarea = document.getElementById('compiler-code-input');
    if (textarea) {
      textarea.value = getSavedOrDefaultCode(currentLanguageId, currentLanguageKey, currentLanguageName);
      textarea.selectionStart = textarea.selectionEnd = 0;
    }
    const stdinEl = document.getElementById('compiler-stdin-input');
    if (stdinEl && currentLanguageKey && DEFAULT_TEMPLATES[currentLanguageKey]) {
      const currentStdin = stdinEl.value || '';
      if (!currentStdin.trim() || currentStdin === prevDefaultStdin) {
        stdinEl.value = DEFAULT_TEMPLATES[currentLanguageKey].stdin || '';
      }
    }
    renderEditor();
  }

  function renderOutputPanel(loading = false) {
    const emptyEl = document.getElementById('compiler-output-empty');
    const loadingEl = document.getElementById('compiler-output-loading');
    const bodyEl = document.getElementById('compiler-output-body');
    const jsonEl = document.getElementById('compiler-output-json');
    const telemetryBar = document.getElementById('compiler-telemetry-bar');

    if (!emptyEl || !loadingEl || !bodyEl) return;

    if (loading) {
      emptyEl.classList.add('hidden');
      bodyEl.classList.add('hidden');
      if (jsonEl) jsonEl.classList.add('hidden');
      if (telemetryBar) telemetryBar.classList.add('hidden');
      loadingEl.classList.remove('hidden');
      return;
    }

    loadingEl.classList.add('hidden');

    if (!lastCompileResult) {
      emptyEl.classList.remove('hidden');
      bodyEl.classList.add('hidden');
      if (jsonEl) jsonEl.classList.add('hidden');
      if (telemetryBar) telemetryBar.classList.add('hidden');
      return;
    }

    emptyEl.classList.add('hidden');
    if (telemetryBar) telemetryBar.classList.remove('hidden');

    const res = lastCompileResult;
    const statusBadge = document.getElementById('compiler-status-badge');
    const metricsEl = document.getElementById('compiler-metrics-text');
    const tokenEl = document.getElementById('compiler-token-text');

    if (statusBadge) {
      const ok = !!res.success;
      const isCompileErr = res.statusId === 6;
      const badgeClass = ok
        ? 'inline-flex items-center gap-1.5 px-2.5 py-1 rounded-md text-xs font-bold bg-emerald-50 text-emerald-700 border border-emerald-200'
        : (isCompileErr
          ? 'inline-flex items-center gap-1.5 px-2.5 py-1 rounded-md text-xs font-bold bg-amber-50 text-amber-800 border border-amber-200'
          : 'inline-flex items-center gap-1.5 px-2.5 py-1 rounded-md text-xs font-bold bg-rose-50 text-rose-700 border border-rose-200');
      statusBadge.className = badgeClass;
      statusBadge.textContent = `${res.status || (ok ? 'Accepted' : 'Error')}${res.statusId ? ` (ID: ${res.statusId})` : ''}`;
    }

    if (metricsEl) {
      const exitPart = res.exitCode !== null && res.exitCode !== undefined ? ` · Exit ${res.exitCode}` : '';
      metricsEl.innerHTML = `Time: <strong>${escapeHtml(res.runtime || (res.time ? res.time + 's' : '—'))}</strong> · Memory: <strong>${escapeHtml(res.memory || '—')}</strong>${exitPart} · Engine: <span class="text-emerald-700 font-semibold">${escapeHtml(res.engine || 'Judge0 CE')}</span>`;
    }

    if (tokenEl) {
      tokenEl.textContent = res.token ? `Token: ${res.token}` : 'No token (simulated)';
    }

    if (activeOutputTab === 'json') {
      bodyEl.classList.add('hidden');
      if (jsonEl) {
        jsonEl.classList.remove('hidden');
        jsonEl.innerHTML = `<pre class="text-[11px] font-mono text-gray-800 bg-gray-50 border border-gray-200 rounded-lg p-3 overflow-auto whitespace-pre-wrap">${escapeHtml(JSON.stringify(res, null, 2))}</pre>`;
      }
      return;
    }

    if (jsonEl) jsonEl.classList.add('hidden');
    bodyEl.classList.remove('hidden');

    let sectionsHtml = '';

    if (res.compileOutput && res.compileOutput.trim()) {
      sectionsHtml += `
        <div class="space-y-1">
          <div class="text-[11px] font-bold uppercase tracking-wider text-amber-700 flex items-center gap-1.5">
            <span>Compiler Output (compile_output)</span>
          </div>
          <pre class="p-3 rounded-lg bg-amber-50 border border-amber-200 text-amber-800 font-mono text-xs whitespace-pre-wrap overflow-x-auto">${escapeHtml(res.compileOutput)}</pre>
        </div>
      `;
    }

    if (res.stderr && res.stderr.trim()) {
      sectionsHtml += `
        <div class="space-y-1">
          <div class="text-[11px] font-bold uppercase tracking-wider text-rose-700">Standard Error (stderr)</div>
          <pre class="p-3 rounded-lg bg-rose-50 border border-rose-200 text-rose-700 font-mono text-xs whitespace-pre-wrap overflow-x-auto">${escapeHtml(res.stderr)}</pre>
        </div>
      `;
    }

    if (res.message && res.message.trim()) {
      sectionsHtml += `
        <div class="space-y-1">
          <div class="text-[11px] font-bold uppercase tracking-wider text-gray-500">Judge0 System Message</div>
          <pre class="p-2.5 rounded-lg bg-gray-50 border border-gray-200 text-gray-700 font-mono text-xs whitespace-pre-wrap">${escapeHtml(res.message)}</pre>
        </div>
      `;
    }

    const stdoutText = res.stdout || '';
    sectionsHtml += `
      <div class="space-y-1">
        <div class="text-[11px] font-bold uppercase tracking-wider text-gray-500">Standard Output (stdout)</div>
        <pre class="p-3 rounded-lg bg-gray-50 border border-gray-200 ${stdoutText ? 'text-gray-900' : 'text-gray-400 italic'} font-mono text-xs whitespace-pre-wrap overflow-x-auto min-h-[72px]">${stdoutText ? escapeHtml(stdoutText) : '(empty stdout)'}</pre>
      </div>
    `;

    bodyEl.innerHTML = sectionsHtml;
  }

  function switchOutputTab(tab) {
    activeOutputTab = tab;
    const btnOut = document.getElementById('btn-compiler-tab-output');
    const btnJson = document.getElementById('btn-compiler-tab-json');
    if (btnOut) {
      btnOut.className = tab === 'output'
        ? 'pb-2 pt-2 border-b-2 border-emerald-600 text-emerald-800 font-semibold text-xs cursor-pointer'
        : 'pb-2 pt-2 border-b-2 border-transparent text-gray-500 hover:text-gray-700 font-medium text-xs cursor-pointer';
    }
    if (btnJson) {
      btnJson.className = tab === 'json'
        ? 'pb-2 pt-2 border-b-2 border-emerald-600 text-emerald-800 font-semibold text-xs cursor-pointer'
        : 'pb-2 pt-2 border-b-2 border-transparent text-gray-500 hover:text-gray-700 font-medium text-xs cursor-pointer';
    }
    renderOutputPanel(false);
  }

  async function compileAndRun() {
    if (isRunning) return;
    const textarea = document.getElementById('compiler-code-input');
    const stdinEl = document.getElementById('compiler-stdin-input');
    const expectedEl = document.getElementById('compiler-expected-input');
    const cpuEl = document.getElementById('compiler-cpu-limit');
    const memEl = document.getElementById('compiler-memory-limit');
    const runBtn = document.getElementById('btn-compiler-run');

    const sourceCode = textarea ? textarea.value : '';
    if (!sourceCode.trim()) {
      if (window.CodeForgeApp) CodeForgeApp.showToast('Please enter source code to compile.', 'warn');
      return;
    }

    const originalHtml = runBtn ? runBtn.innerHTML : '';
    isRunning = true;
    if (runBtn) {
      runBtn.disabled = true;
      runBtn.innerHTML = `<span>Compiling...</span>`;
    }
    renderOutputPanel(true);

    try {
      const cpuLimit = cpuEl ? parseFloat(cpuEl.value) : 5.0;
      const memMb = memEl ? parseInt(memEl.value, 10) : 256;
      const expectedVal = expectedEl && expectedEl.value.trim() ? expectedEl.value : null;

      const payload = {
        languageId: currentLanguageId,
        language: currentLanguageKey || currentLanguageName,
        sourceCode,
        stdin: stdinEl ? stdinEl.value : '',
        expectedOutput: expectedVal,
        cpuTimeLimit: isNaN(cpuLimit) ? 5.0 : cpuLimit,
        memoryLimit: isNaN(memMb) ? 256000 : memMb * 1024
      };

      const res = await CodeForgeAPI.compileCode(payload);
      lastCompileResult = res;
      renderOutputPanel(false);

      if (window.CodeForgeApp) {
        CodeForgeApp.showToast(`${res.status || 'Completed'} (${res.runtime || '0 ms'})`, res.success ? 'success' : 'warn');
      }
    } catch (err) {
      lastCompileResult = {
        success: false,
        statusId: 13,
        status: 'Execution Error',
        stderr: err.message || 'Failed to invoke Judge0 compiler endpoint.',
        stdout: '',
        compileOutput: '',
        runtime: '0 ms',
        memory: '—',
        engine: 'Client Error'
      };
      renderOutputPanel(false);
      if (window.CodeForgeApp) CodeForgeApp.showToast(err.message || 'Compiler failed', 'warn');
    } finally {
      isRunning = false;
      if (runBtn) {
        runBtn.disabled = false;
        runBtn.innerHTML = originalHtml;
      }
    }
  }

  function resetToSample() {
    try {
      localStorage.removeItem(STORAGE_PREFIX + currentLanguageId);
    } catch (_) {}
    const tpl = currentLanguageKey && DEFAULT_TEMPLATES[currentLanguageKey]
      ? DEFAULT_TEMPLATES[currentLanguageKey]
      : null;

    const textarea = document.getElementById('compiler-code-input');
    const stdinEl = document.getElementById('compiler-stdin-input');
    if (textarea) {
      textarea.value = tpl ? tpl.code : `// ${currentLanguageName}\n`;
      saveCodeForLanguage(currentLanguageId, textarea.value);
    }
    if (stdinEl && tpl) {
      stdinEl.value = tpl.stdin || '';
      try { localStorage.setItem(STDIN_STORAGE_KEY, stdinEl.value); } catch (_) {}
    }
    renderEditor();
    if (window.CodeForgeApp) CodeForgeApp.showToast('Sample template loaded.', 'info');
  }

  function loadCompileErrorDemo() {
    selectLanguage(105);
    const textarea = document.getElementById('compiler-code-input');
    if (textarea) {
      textarea.value = `#include <iostream>\nusing namespace std;\n\nint main() {\n    // Deliberate syntax error to inspect Judge0 compile_output\n    cout << undeclared_variable << endl;\n    return 0;\n}\n`;
      saveCodeForLanguage(105, textarea.value);
    }
    renderEditor();
    compileAndRun();
  }

  function bindEvents() {
    const langSelect = document.getElementById('compiler-language-select');
    if (langSelect) {
      langSelect.addEventListener('change', (e) => selectLanguage(e.target.value));
    }

    const runBtn = document.getElementById('btn-compiler-run');
    if (runBtn) runBtn.addEventListener('click', compileAndRun);

    const resetBtn = document.getElementById('btn-compiler-reset');
    if (resetBtn) resetBtn.addEventListener('click', resetToSample);

    const demoErrBtn = document.getElementById('btn-compiler-error-demo');
    if (demoErrBtn) demoErrBtn.addEventListener('click', loadCompileErrorDemo);

    const formatBtn = document.getElementById('btn-compiler-format');
    if (formatBtn) {
      formatBtn.addEventListener('click', () => {
        const textarea = document.getElementById('compiler-code-input');
        if (!textarea) return;
        textarea.value = textarea.value
          .split('\n')
          .map(l => l.replace(/\t/g, '    ').replace(/\s+$/, ''))
          .join('\n');
        saveCodeForLanguage(currentLanguageId, textarea.value);
        renderEditor();
        if (window.CodeForgeApp) CodeForgeApp.showToast('Code formatted.', 'info');
      });
    }

    const clearOutBtn = document.getElementById('btn-compiler-clear-output');
    if (clearOutBtn) {
      clearOutBtn.addEventListener('click', () => {
        lastCompileResult = null;
        renderOutputPanel(false);
      });
    }

    const clearStdinBtn = document.getElementById('btn-compiler-clear-stdin');
    if (clearStdinBtn) {
      clearStdinBtn.addEventListener('click', () => {
        const stdinEl = document.getElementById('compiler-stdin-input');
        if (stdinEl) {
          stdinEl.value = '';
          try { localStorage.setItem(STDIN_STORAGE_KEY, ''); } catch (_) {}
        }
      });
    }

    const tabOut = document.getElementById('btn-compiler-tab-output');
    const tabJson = document.getElementById('btn-compiler-tab-json');
    if (tabOut) tabOut.addEventListener('click', () => switchOutputTab('output'));
    if (tabJson) tabJson.addEventListener('click', () => switchOutputTab('json'));

    const stdinEl = document.getElementById('compiler-stdin-input');
    if (stdinEl) {
      try {
        const savedStdin = localStorage.getItem(STDIN_STORAGE_KEY);
        if (savedStdin !== null) stdinEl.value = savedStdin;
      } catch (_) {}
      stdinEl.addEventListener('input', () => {
        try { localStorage.setItem(STDIN_STORAGE_KEY, stdinEl.value); } catch (_) {}
      });
    }

    const textarea = document.getElementById('compiler-code-input');
    if (textarea) {
      textarea.addEventListener('input', () => {
        saveCodeForLanguage(currentLanguageId, textarea.value);
        renderEditor();
      });
      textarea.addEventListener('click', renderEditor);
      textarea.addEventListener('keyup', renderEditor);
      textarea.addEventListener('scroll', () => {
        const codeContainer = document.getElementById('compiler-code-content');
        const linesContainer = document.getElementById('compiler-line-numbers');
        if (codeContainer) {
          codeContainer.scrollTop = textarea.scrollTop;
          codeContainer.scrollLeft = textarea.scrollLeft;
        }
        if (linesContainer) linesContainer.scrollTop = textarea.scrollTop;
      });
      textarea.addEventListener('keydown', (e) => {
        if (e.key === 'Tab') {
          e.preventDefault();
          const start = textarea.selectionStart;
          const end = textarea.selectionEnd;
          textarea.value = textarea.value.substring(0, start) + '    ' + textarea.value.substring(end);
          textarea.selectionStart = textarea.selectionEnd = start + 4;
          saveCodeForLanguage(currentLanguageId, textarea.value);
          renderEditor();
        }
      });
    }

    window.addEventListener('keydown', (e) => {
      if ((e.metaKey || e.ctrlKey) && e.key === 'Enter') {
        const page = document.getElementById('page-compiler');
        if (page && !page.classList.contains('hidden')) {
          e.preventDefault();
          compileAndRun();
        }
      }
    });
  }

  async function init() {
    if (!initialized) {
      initialized = true;
      bindEvents();
      populateLanguageDropdown([]);
      selectLanguage(currentLanguageId);
      if (window.CodeForgeAPI && CodeForgeAPI.getCompilerLanguages) {
        try {
          const langs = await CodeForgeAPI.getCompilerLanguages();
          populateLanguageDropdown(langs);
        } catch (_) {}
      }
    }
    refreshEngineStatus();
    renderEditor();
  }

  return {
    init,
    compileAndRun,
    selectLanguage,
    resetToSample
  };
})();
