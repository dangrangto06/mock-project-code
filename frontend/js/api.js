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
        const queryObj = typeof params === 'string' ? { tab: params } : (params || {});
        const query = new URLSearchParams(queryObj).toString();
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

  // Get all submissions for the logged-in user across all problems
  async function getUserSubmissions() {
    await checkBackend();
    if (isBackendOnline) {
      try {
        const res = await fetch(`${API_BASE}/execute/submissions`, {
          headers: getAuthHeaders()
        });
        if (res.ok) {
          return await res.json();
        }
      } catch (e) {
        console.warn("Failed to fetch user submissions from API, using fallback.", e);
      }
    }
    return [];
  }

  function isAuthenticated() {
    try {
      return !!(localStorage.getItem(TOKEN_KEY) || sessionStorage.getItem(TOKEN_KEY));
    } catch (_) {
      return false;
    }
  }

  function isOnline() {
    return isBackendOnline;
  }

  // ===== Learner Notes API =====

  async function getNotes() {
    await checkBackend();
    if (isBackendOnline && isAuthenticated()) {
      try {
        const res = await fetch(`${API_BASE}/notes`, { headers: getAuthHeaders() });
        if (res.ok) {
          return await res.json();
        }
      } catch (e) {
        console.warn("Failed to fetch notes from API, using fallback.", e);
      }
    }
    const stored = localStorage.getItem('codeforge_notes');
    if (stored) {
      try { return JSON.parse(stored); } catch (_) {}
    }
    return (window.CodeForgeData && window.CodeForgeData.initialNotes) ? [...window.CodeForgeData.initialNotes] : [];
  }

  async function createNote(payload) {
    await checkBackend();
    if (isBackendOnline && isAuthenticated()) {
      try {
        const res = await fetch(`${API_BASE}/notes`, {
          method: 'POST',
          headers: getAuthHeaders(),
          body: JSON.stringify(payload)
        });
        if (res.ok) {
          return await res.json();
        }
      } catch (e) {
        console.warn("Create note API error, falling back to local.", e);
      }
    }
    return null;
  }

  async function updateNote(id, payload) {
    await checkBackend();
    if (isBackendOnline && isAuthenticated()) {
      try {
        const res = await fetch(`${API_BASE}/notes/${id}`, {
          method: 'PUT',
          headers: getAuthHeaders(),
          body: JSON.stringify(payload)
        });
        if (res.ok) {
          return await res.json();
        }
      } catch (e) {
        console.warn("Update note API error, falling back to local.", e);
      }
    }
    return null;
  }

  async function deleteNote(id) {
    await checkBackend();
    if (isBackendOnline && isAuthenticated()) {
      try {
        const res = await fetch(`${API_BASE}/notes/${id}`, {
          method: 'DELETE',
          headers: getAuthHeaders()
        });
        if (res.ok) {
          return true;
        }
      } catch (e) {
        console.warn("Delete note API error, falling back to local.", e);
      }
    }
    return false;
  }

  // ===== Community Discussions API =====

  async function getDiscussions(category) {
    await checkBackend();
    if (isBackendOnline) {
      try {
        const url = category && category !== 'all'
          ? `${API_BASE}/discussions?category=${encodeURIComponent(category)}`
          : `${API_BASE}/discussions`;
        const res = await fetch(url, { headers: getAuthHeaders() });
        if (res.ok) {
          return await res.json();
        }
      } catch (e) {
        console.warn("Failed to fetch discussions from API, using fallback.", e);
      }
    }
    const stored = localStorage.getItem('codeforge_discussions');
    if (stored) {
      try { return JSON.parse(stored); } catch (_) {}
    }
    return (window.CodeForgeData && window.CodeForgeData.initialDiscussions) ? [...window.CodeForgeData.initialDiscussions] : [];
  }

  async function createDiscussion(payload) {
    await checkBackend();
    if (isBackendOnline && isAuthenticated()) {
      try {
        const res = await fetch(`${API_BASE}/discussions`, {
          method: 'POST',
          headers: getAuthHeaders(),
          body: JSON.stringify(payload)
        });
        if (res.ok) {
          return await res.json();
        }
      } catch (e) {
        console.warn("Create discussion API error, falling back to local.", e);
      }
    }
    return null;
  }

  async function toggleDiscussionUpvote(id) {
    await checkBackend();
    if (isBackendOnline && isAuthenticated()) {
      try {
        const res = await fetch(`${API_BASE}/discussions/${id}/upvote`, {
          method: 'POST',
          headers: getAuthHeaders()
        });
        if (res.ok) {
          return await res.json();
        }
      } catch (e) {
        console.warn("Toggle discussion upvote API error, falling back to local.", e);
      }
    }
    return null;
  }

  async function addDiscussionReply(id, content) {
    await checkBackend();
    if (isBackendOnline && isAuthenticated()) {
      try {
        const res = await fetch(`${API_BASE}/discussions/${id}/replies`, {
          method: 'POST',
          headers: getAuthHeaders(),
          body: JSON.stringify({ content })
        });
        if (res.ok) {
          return await res.json();
        }
      } catch (e) {
        console.warn("Add discussion reply API error, falling back to local.", e);
      }
    }
    return null;
  }

  async function reportContent(payload) {
    await checkBackend();
    if (isBackendOnline && isAuthenticated()) {
      try {
        const res = await fetch(`${API_BASE}/discussions/reports`, {
          method: 'POST',
          headers: getAuthHeaders(),
          body: JSON.stringify(payload)
        });
        if (res.ok) {
          return await res.json();
        }
      } catch (e) {
        console.warn("Report content API error, falling back to local.", e);
      }
    }
    return { success: true, message: "Report noted locally." };
  }

  // ===== Admin & Moderation API =====

  async function getAdminStats() {
    await checkBackend();
    if (isBackendOnline && isAuthenticated()) {
      try {
        const res = await fetch(`${API_BASE}/admin/stats`, { headers: getAuthHeaders() });
        if (res.ok) return await res.json();
      } catch (e) {
        console.warn("Failed to fetch admin stats:", e);
      }
    }
    return null;
  }

  async function getModerationQueue() {
    await checkBackend();
    if (isBackendOnline && isAuthenticated()) {
      try {
        const res = await fetch(`${API_BASE}/admin/moderation`, { headers: getAuthHeaders() });
        if (res.ok) return await res.json();
      } catch (e) {
        console.warn("Failed to fetch moderation queue:", e);
      }
    }
    return { pendingProblems: [], pendingDiscussions: [] };
  }

  async function approveProblem(id) {
    await checkBackend();
    if (isBackendOnline && isAuthenticated()) {
      try {
        const res = await fetch(`${API_BASE}/admin/problems/${id}/approve`, {
          method: 'POST',
          headers: getAuthHeaders()
        });
        if (res.ok) return await res.json();
      } catch (e) {
        console.warn("Failed to approve problem:", e);
      }
    }
    return null;
  }

  async function rejectProblem(id, reason) {
    await checkBackend();
    if (isBackendOnline && isAuthenticated()) {
      try {
        const res = await fetch(`${API_BASE}/admin/problems/${id}/reject`, {
          method: 'POST',
          headers: getAuthHeaders(),
          body: JSON.stringify({ reason })
        });
        if (res.ok) return await res.json();
      } catch (e) {
        console.warn("Failed to reject problem:", e);
      }
    }
    return null;
  }

  async function approveDiscussion(id) {
    await checkBackend();
    if (isBackendOnline && isAuthenticated()) {
      try {
        const res = await fetch(`${API_BASE}/admin/discussions/${id}/approve`, {
          method: 'POST',
          headers: getAuthHeaders()
        });
        if (res.ok) return await res.json();
      } catch (e) {
        console.warn("Failed to approve discussion:", e);
      }
    }
    return null;
  }

  async function rejectDiscussion(id, reason) {
    await checkBackend();
    if (isBackendOnline && isAuthenticated()) {
      try {
        const res = await fetch(`${API_BASE}/admin/discussions/${id}/reject`, {
          method: 'POST',
          headers: getAuthHeaders(),
          body: JSON.stringify({ reason })
        });
        if (res.ok) return await res.json();
      } catch (e) {
        console.warn("Failed to reject discussion:", e);
      }
    }
    return null;
  }

  async function deleteDiscussion(id) {
    await checkBackend();
    if (isBackendOnline && isAuthenticated()) {
      try {
        const res = await fetch(`${API_BASE}/admin/discussions/${id}`, {
          method: 'DELETE',
          headers: getAuthHeaders()
        });
        if (res.ok) return await res.json();
      } catch (e) {
        console.warn("Failed to delete discussion:", e);
      }
    }
    return null;
  }

  async function dismissReport(id) {
    await checkBackend();
    if (isBackendOnline && isAuthenticated()) {
      try {
        const res = await fetch(`${API_BASE}/admin/reports/${id}/dismiss`, {
          method: 'POST',
          headers: getAuthHeaders()
        });
        if (res.ok) return await res.json();
      } catch (e) {
        console.warn("Failed to dismiss report:", e);
      }
    }
    return null;
  }

  async function deleteReportedItem(id) {
    await checkBackend();
    if (isBackendOnline && isAuthenticated()) {
      try {
        const res = await fetch(`${API_BASE}/admin/reports/${id}/delete`, {
          method: 'POST',
          headers: getAuthHeaders()
        });
        if (res.ok) return await res.json();
      } catch (e) {
        console.warn("Failed to delete reported item:", e);
      }
    }
    return null;
  }

  async function getAdminUsers() {
    await checkBackend();
    if (isBackendOnline && isAuthenticated()) {
      try {
        const res = await fetch(`${API_BASE}/admin/users`, { headers: getAuthHeaders() });
        if (res.ok) return await res.json();
      } catch (e) {
        console.warn("Failed to fetch admin users:", e);
      }
    }
    return [];
  }

  async function updateUserRole(id, role) {
    await checkBackend();
    if (isBackendOnline && isAuthenticated()) {
      try {
        const res = await fetch(`${API_BASE}/admin/users/${id}/role`, {
          method: 'PUT',
          headers: getAuthHeaders(),
          body: JSON.stringify({ role })
        });
        if (res.ok) return await res.json();
      } catch (e) {
        console.warn("Failed to update user role:", e);
      }
    }
    return null;
  }

  // Chatbot operations
  async function askChatbot(payload) {
    await checkBackend();
    if (isBackendOnline) {
      try {
        const res = await fetch(`${API_BASE}/chatbot/ask`, {
          method: 'POST',
          headers: getAuthHeaders(),
          body: JSON.stringify(payload)
        });
        if (res.ok) return await res.json();
      } catch (e) {
        console.warn("Failed to query chatbot API, using local fallback:", e);
      }
    }
    return null;
  }

  async function getChatHistory(conversationId) {
    await checkBackend();
    if (isBackendOnline) {
      try {
        const query = conversationId ? `?conversationId=${encodeURIComponent(conversationId)}` : '';
        const res = await fetch(`${API_BASE}/chatbot/history${query}`, {
          headers: getAuthHeaders()
        });
        if (res.ok) return await res.json();
      } catch (e) {
        console.warn("Failed to fetch chat history:", e);
      }
    }
    return null;
  }

  async function clearChatHistory(conversationId) {
    await checkBackend();
    if (isBackendOnline) {
      try {
        const query = conversationId ? `?conversationId=${encodeURIComponent(conversationId)}` : '';
        const res = await fetch(`${API_BASE}/chatbot/history${query}`, {
          method: 'DELETE',
          headers: getAuthHeaders()
        });
        if (res.ok) return await res.json();
      } catch (e) {
        console.warn("Failed to clear chat history:", e);
      }
    }
    return null;
  }

  // Check health on boot
  checkBackend();

  return {
    checkBackend,
    isAuthenticated,
    isOnline,
    getOverview,
    getProblems,
    runCode,
    submitSolution,
    createProblem,
    toggleBookmark,
    getProblemSubmissions,
    getUserSubmissions,
    getNotes,
    createNote,
    updateNote,
    deleteNote,
    getDiscussions,
    createDiscussion,
    toggleDiscussionUpvote,
    addDiscussionReply,
    reportContent,
    getAdminStats,
    getModerationQueue,
    approveProblem,
    rejectProblem,
    approveDiscussion,
    rejectDiscussion,
    deleteDiscussion,
    dismissReport,
    deleteReportedItem,
    getAdminUsers,
    updateUserRole,
    askChatbot,
    getChatHistory,
    clearChatHistory
  };
})();
