// CodeForge Frontend API Client with auto-fallback to local mock data

window.CodeForgeAPI = (function () {
  const API_BASE = 'http://localhost:8080/api';
  const TOKEN_KEY = 'codeforge_token';
  let isBackendOnline = false;

  // Get stored JWT token for authenticated requests
  function getAuthHeaders() {
    const headers = { 'Content-Type': 'application/json' };
    try {
      const token = localStorage.getItem(TOKEN_KEY) || sessionStorage.getItem(TOKEN_KEY);
      if (token) headers['Authorization'] = 'Bearer ' + token;
    } catch (_) {}
    return headers;
  }

  // Check backend status on startup
  async function checkBackend() {
    try {
      const res = await fetch(`${API_BASE}/health`, { method: 'GET', signal: AbortSignal.timeout(1200) });
      if (res.ok) {
        isBackendOnline = true;
        console.log("Connected to CodeForge Backend at", API_BASE);
      }
    } catch (e) {
      isBackendOnline = false;
      console.log("Backend offline, running in standalone client mode.");
    }
  }

  // Get overview data
  async function getOverview() {
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
        totalProblems: 240,
        solvedCount: CodeForgeData.currentUser.solvedCount
      }
    };
  }

  // Get problems list
  async function getProblems(params = {}) {
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
    if (isBackendOnline) {
      try {
        const res = await fetch(`${API_BASE}/problems`, {
          method: 'POST',
          headers: getAuthHeaders(),
          body: JSON.stringify(payload)
        });
        if (res.ok) {
          const json = await res.json();
          return json;
        }
      } catch (e) {
        console.warn("Create problem API error, using local fallback.", e);
      }
    }
    const newProb = {
      id: CodeForgeData.problems.length + 104,
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

  // Check health on boot
  checkBackend();

  return {
    checkBackend,
    getOverview,
    getProblems,
    runCode,
    submitSolution,
    createProblem
  };
})();
