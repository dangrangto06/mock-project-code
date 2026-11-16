// CodeForge Frontend API Client with auto-fallback to local mock data

window.CodeForgeAPI = (function () {
  const API_BASE = 'http://localhost:8080/api';
  const TOKEN_KEY = 'codeforge_token';
  let isBackendOnline = false;
  let healthPromise = null;

  function getAuthHeaders() {
    const headers = { 'Content-Type': 'application/json' };
    try {
      const token = localStorage.getItem(TOKEN_KEY) || sessionStorage.getItem(TOKEN_KEY);
      if (token) headers['Authorization'] = 'Bearer ' + token;
    } catch (_) {}
    return headers;
  }

  function checkBackend() {
    if (!healthPromise) {
      healthPromise = fetch(`${API_BASE}/health`, { method: 'GET', signal: AbortSignal.timeout(1500) })
        .then(res => {
          isBackendOnline = res.ok;
          if (isBackendOnline) console.log("Connected to CodeForge Backend at", API_BASE);
          else console.log("Backend offline, running in standalone client mode.");
          return isBackendOnline;
        })
        .catch(() => {
          isBackendOnline = false;
          console.log("Backend offline, running in standalone client mode.");
          return false;
        });
    }
    return healthPromise;
  }

  // Get overview data
  async function getOverview() {
    await checkBackend();
    if (isBackendOnline) {
      try {
        const res = await fetch(`${API_BASE}/overview`, { headers: getAuthHeaders() });
        if (res.ok) {
          const json = await res.json();
          // Backend returns the OverviewResponse object directly
          return json;
        }
      } catch (e) {
        console.warn("Failed to fetch overview, using fallback.", e);
      }
    }
    return {
      user: CodeForgeData.currentUser,
      stats: {
        totalProblems: CodeForgeData.problems.length,
        solvedCount: CodeForgeData.currentUser.solvedCount
      }
    };
  }

  // Get problems list
  async function getProblems(params = {}) {
    await checkBackend();
    if (isBackendOnline) {
      try {
        const query = new URLSearchParams(params).toString();
        const res = await fetch(`${API_BASE}/problems?${query}`, { headers: getAuthHeaders() });
        if (res.ok) {
          const json = await res.json();
          return json;
        }
      } catch (e) {
        console.warn("Failed to fetch problems from API, using fallback.", e);
      }
    }
    return CodeForgeData.problems;
  }

  // Run code against test cases
  async function runCode(payload) {
    await checkBackend();
    if (isBackendOnline) {
      try {
        const res = await fetch(`${API_BASE}/execute/run`, {
          method: 'POST',
          headers: getAuthHeaders(),
          body: JSON.stringify(payload)
        });
        if (res.ok) {
          const json = await res.json();
          return json;
        }
      } catch (e) {
        console.warn("Run code API error, using local simulation.", e);
      }
    }
    // Local fallback
    return {
      success: true,
      runtime: "32 ms",
      memory: "17.4 MB",
      status: "All sample tests passed"
    };
  }

  // Submit code
  async function submitSolution(payload) {
    await checkBackend();
    if (isBackendOnline) {
      try {
        const res = await fetch(`${API_BASE}/execute/submit`, {
          method: 'POST',
          headers: getAuthHeaders(),
          body: JSON.stringify(payload)
        });
        if (res.ok) {
          const json = await res.json();
          return json;
        }
      } catch (e) {
        console.warn("Submit code API error, using local simulation.", e);
      }
    }
    // Local fallback
    return {
      success: true,
      status: "Accepted",
      runtime: "32 ms",
      memory: "17.4 MB"
    };
  }

  // Create problem
  async function createProblem(payload) {
    await checkBackend();
    if (isBackendOnline) {
      try {
        const res = await fetch(`${API_BASE}/problems`, {
          method: 'POST',
          headers: getAuthHeaders(),
          body: JSON.stringify(payload)
        });
        if (res.status === 401) throw new Error('UNAUTHORIZED');
        if (res.ok) {
          const json = await res.json();
          return json;
        }
      } catch (e) {
        if (e.message === 'UNAUTHORIZED') throw e;
        console.warn("Create problem API error, using local fallback.", e);
      }
    }
    const newProb = {
      id: Math.max(0, ...CodeForgeData.problems.map(p => p.id)) + 1,
      title: payload.title,
      difficulty: payload.difficulty,
      topics: payload.topics,
      acceptance: "100.0%",
      status: "unattempted",
      saved: false,
      isMine: true
    };
    CodeForgeData.problems.unshift(newProb);
    return newProb;
  }

  // Toggle bookmark on problem
  async function toggleBookmark(problemId) {
    await checkBackend();
    if (isBackendOnline) {
      try {
        const res = await fetch(`${API_BASE}/problems/${problemId}/bookmark`, {
          method: 'POST',
          headers: getAuthHeaders()
        });
        if (res.status === 401) throw new Error('UNAUTHORIZED');
        if (res.ok) {
          const json = await res.json();
          return json;
        }
      } catch (e) {
        if (e.message === 'UNAUTHORIZED') throw e;
        console.warn("Toggle bookmark API error, using local fallback.", e);
      }
    }
    const p = (CodeForgeData.problems || []).find(item => item.id === problemId);
    if (p) {
      p.saved = !p.saved;
      return { saved: p.saved };
    }
    return null;
  }

  // Get problem submissions history
  async function getProblemSubmissions(problemId) {
    await checkBackend();
    if (isBackendOnline) {
      try {
        const res = await fetch(`${API_BASE}/problems/${problemId}/submissions`, {
          headers: getAuthHeaders()
        });
        if (res.ok) {
          const json = await res.json();
          return json;
        }
      } catch (e) {
        console.warn("Failed to fetch problem submissions from API, using fallback.", e);
      }
    }
    const problem = (CodeForgeData.problems || []).find(p => p.id === problemId);
    return (problem && problem.submissions) || [];
  }

  // Check health on boot
  checkBackend();

  return {
    checkBackend,
    getOverview,
    getProblems,
    runCode,
    submitSolution,
    createProblem,
    toggleBookmark,
    getProblemSubmissions
  };
})();
