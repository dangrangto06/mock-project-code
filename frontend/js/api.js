/**
 * @file api.js
 * @description CodeForge Frontend REST API Client with automatic fallback to mock client data.
 * @module CodeForgeAPI
 *
 * <p>Architectural Role:</p>
 * Encapsulates all asynchronous HTTP network interactions between the frontend browser application
 * and the Spring Boot backend REST endpoints (running on {@code http://localhost:8080/api}).
 *
 * <p>Resilience & Offline Architecture:</p>
 * Automatically detects backend availability via a lightweight health check ping at startup.
 * If the Spring Boot service is reachable, live REST calls are executed with JWT authorization headers;
 * if offline or unreachable, calls degrade gracefully to in-memory mock datasets in {@link CodeForgeData},
 * allowing uninterrupted UI demonstration, testing, and exploration.
 */

window.CodeForgeAPI = (function () {
  'use strict';

  /** Base URL for all backend REST endpoints */
  const API_BASE = 'http://localhost:8080/api';

  /** Web Storage key under which the JWT bearer token is cached */
  const TOKEN_KEY = 'codeforge_token';

  /** Internal state flag tracking whether the backend API responded to health checks */
  let isBackendOnline = false;

  /**
   * Retrieves authorization headers with the stored Bearer JWT token if available.
   *
   * <p>Inspects both {@code localStorage} (persistent across sessions) and
   * {@code sessionStorage} (single browser session lifetime).</p>
   *
   * @returns {Object<string, string>} HTTP headers object including Content-Type and optional Authorization.
   */
  function getAuthHeaders() {
    const headers = { 'Content-Type': 'application/json' };
    try {
      // Check persistent storage first, then fall back to session storage
      const token = localStorage.getItem(TOKEN_KEY) || sessionStorage.getItem(TOKEN_KEY);
      if (token) {
        headers['Authorization'] = 'Bearer ' + token;
      }
    } catch (_) {
      // Storage access blocked or restricted by browser security policies
    }
    return headers;
  }

  /**
   * Pings the backend health endpoint to detect service availability on startup.
   *
   * <p>Employs an AbortSignal timeout of 1200ms to avoid blocking frontend rendering
   * if the server is offline or unreachable.</p>
   *
   * @async
   * @returns {Promise<void>} Resolves when status check completes.
   */
  async function checkBackend() {
    try {
      const res = await fetch(`${API_BASE}/health`, {
        method: 'GET',
        signal: AbortSignal.timeout(1200) // Fast timeout for immediate startup responsiveness
      });
      if (res.ok) {
        isBackendOnline = true;
        console.log("Connected to CodeForge Backend at", API_BASE);
      }
    } catch (e) {
      isBackendOnline = false;
      console.log("Backend offline, running in standalone client mode.");
    }
  }

  /**
   * Fetches dashboard overview metrics for the current user.
   *
   * @async
   * @returns {Promise<Object>} Aggregated metrics object containing user summary, next challenge,
   *                            weekly goal progress, and recent activity.
   */
  async function getOverview() {
    if (isBackendOnline) {
      try {
        const res = await fetch(`${API_BASE}/overview`, { headers: getAuthHeaders() });
        if (res.ok) {
          const json = await res.json();
          // Backend returns the OverviewResponse DTO object directly
          return json;
        }
      } catch (e) {
        console.warn("Failed to fetch overview, using fallback.", e);
      }
    }
    // Return static mock fallback when running offline
    return {
      user: CodeForgeData.currentUser,
      stats: {
        totalProblems: 240,
        solvedCount: CodeForgeData.currentUser.solvedCount
      }
    };
  }

  /**
   * Fetches the problem catalog with optional query filters.
   *
   * @async
   * @param {Object} [params={}] - Query filtering parameters (search, difficulty, topic, tab).
   * @returns {Promise<Array<Object>>} List of problem summary objects.
   */
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
    // Return local mock problems list if server is unavailable
    return CodeForgeData.problems;
  }

  /**
   * Sends solution code to the sandbox execution engine for a dry-run test.
   *
   * @async
   * @param {Object} payload - Run request details.
   * @param {number} payload.problemId - Target problem ID.
   * @param {string} payload.language - Code language identifier (e.g. 'python', 'javascript').
   * @param {string} payload.code - Solution source code.
   * @param {string} [payload.customInput] - Custom standard input for the execution.
   * @returns {Promise<Object>} Execution result detailing output, runtime, and memory metrics.
   */
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
    // Standalone fallback result for offline testing
    return {
      success: true,
      runtime: "32 ms",
      memory: "17.4 MB",
      status: "All sample tests passed"
    };
  }

  /**
   * Submits solution code for comprehensive judging against all test cases.
   *
   * @async
   * @param {Object} payload - Submission payload.
   * @param {number} payload.problemId - Target problem ID.
   * @param {string} payload.language - Code language identifier.
   * @param {string} payload.code - Solution source code.
   * @returns {Promise<Object>} Submission verdict detailing tests passed, percentiles, and message.
   */
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
    // Standalone fallback result for offline testing
    return {
      success: true,
      status: "Accepted",
      runtime: "32 ms",
      memory: "17.4 MB"
    };
  }

  /**
   * Creates a new custom algorithm challenge authored by the current user.
   *
   * @async
   * @param {Object} payload - Problem definition details.
   * @param {string} payload.title - Problem title.
   * @param {string} payload.difficulty - Target difficulty ("Easy", "Medium", "Hard").
   * @param {Array<string>} payload.topics - Category topic keywords.
   * @param {string} payload.statement - Problem statement in Markdown.
   * @returns {Promise<Object>} Created problem object.
   */
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

    // In-memory fallback: prepend newly created problem to local mock problems array
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

  // Initiate initial health check ping on module evaluation
  checkBackend();

  // Expose public API interface
  return {
    checkBackend,
    getOverview,
    getProblems,
    runCode,
    submitSolution,
    createProblem
  };
})();
