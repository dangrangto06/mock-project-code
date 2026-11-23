// CodeForge AI Chatbot Engine (ForgeBot)
// Fully synchronized with backend /api/chatbot and database history.
// English-only assistant strictly restricted to the CodeForge platform domain.

window.CodeForgeChatbot = (function () {
  'use strict';

  const CONV_ID_KEY = 'codeforge_chat_conv_id';
  let isOpen = false;
  let isTyping = false;
  let isHistoryLoaded = false;

  // Retrieve or create persistent conversation session ID
  function getConversationId() {
    try {
      let id = localStorage.getItem(CONV_ID_KEY);
      if (!id) {
        id = 'conv-' + Date.now() + '-' + Math.random().toString(36).substring(2, 9);
        localStorage.setItem(CONV_ID_KEY, id);
      }
      return id;
    } catch (_) {
      return 'conv-default';
    }
  }

  // Out of scope standard English refusal
  const OUT_OF_SCOPE_REFUSAL =
    "I am ForgeBot, the dedicated AI assistant for the CodeForge platform. " +
    "I am only permitted to answer questions directly related to CodeForge—such as our coding challenges, " +
    "the online IDE, Judge0 code execution sandbox, algorithm study notes, community discussions, user streaks, " +
    "and platform architecture.\n\n" +
    "I cannot assist with queries outside of the CodeForge platform. " +
    "How can I help you with your coding practice on CodeForge today?";

  // Client-side domain keywords
  const IN_SCOPE_KEYWORDS = [
    "codeforge", "forge", "platform", "challenge", "challenges", "problem", "problems",
    "ide", "code", "coding", "editor", "run", "running", "execute", "execution", "judge0",
    "sandbox", "testcase", "test cases", "test case", "sample", "expected", "output", "input",
    "runtime", "memory", "solution", "solutions", "submit", "submission", "submissions", "accepted",
    "streak", "streaks", "daily", "goal", "goals", "weekly goal", "progress", "solved", "acceptance rate",
    "note", "notes", "notebook", "cheatsheet", "tag", "tags", "markdown",
    "community", "discussion", "discussions", "post", "posts", "comment", "comments", "reply", "replies",
    "upvote", "upvotes", "report", "moderator", "admin", "role", "learner", "guest", "auth",
    "login", "sign in", "signup", "sign up", "register", "google", "jwt", "password", "email",
    "two sum", "pair sum", "104", "arrays", "array", "hashing", "hash map", "hash set",
    "two pointers", "sliding window", "binary search", "linked list", "tree", "trees", "graph", "graphs",
    "dynamic programming", "dp", "backtracking", "greedy", "heap", "stack", "stacks", "queue", "queues",
    "big-o", "complexity", "time complexity", "space complexity", "time limit", "memory limit",
    "python", "javascript", "java", "c++", "spring boot", "h2", "sql server", "architecture",
    "focus mode", "theme", "dark mode", "light mode", "shortcut", "cmd+k", "ctrl+k", "random",
    "creator", "create problem", "author", "constraints", "help", "who are you", "what can you do"
  ];

  const OUT_OF_SCOPE_REGEX = /\b(weather|forecast|rain|temperature|president|politics|government|war|election|capital of|population of|recipe|bake|cook|chocolate cake|pizza|movie|actor|celebrity|singer|song|album|football|soccer|fifa|world cup|messi|ronaldo|nba|basketball|crypto|bitcoin|stock market|poem|story|love letter|horoscope|zodiac|medical advice|doctor)\b/i;

  function escapeHtml(str) {
    if (!str) return '';
    return String(str).replace(/[&<>"']/g, ch => (
      { '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[ch]
    ));
  }

  function formatMarkdown(text) {
    if (!text) return '';
    let safe = escapeHtml(text);
    // Code blocks ```code```
    safe = safe.replace(/```([\s\S]*?)```/g, '<pre class="bg-gray-900 text-emerald-400 p-2.5 rounded-lg text-[11px] font-mono my-2 overflow-x-auto border border-gray-800">$1</pre>');
    // Inline code `...`
    safe = safe.replace(/`([^`]+)`/g, '<code class="px-1.5 py-0.5 rounded bg-gray-100 text-emerald-800 font-mono text-[11px] border border-gray-200">$1</code>');
    // Bold **...**
    safe = safe.replace(/\*\*([^*]+)\*\*/g, '<strong class="font-bold text-gray-900">$1</strong>');
    // Bullet points
    safe = safe.replace(/^[•\-*]\s+(.+)$/gm, '<div class="flex items-start gap-1.5 my-1"><span class="text-emerald-600 font-bold">•</span><span>$1</span></div>');
    // Linebreaks
    safe = safe.replace(/\n\n/g, '<div class="h-2"></div>').replace(/\n/g, '<br/>');
    return safe;
  }

  function isQueryInScope(msg) {
    if (!msg || !msg.trim()) return false;
    const lower = msg.toLowerCase().trim();

    if (OUT_OF_SCOPE_REGEX.test(lower)) {
      if (!lower.includes("codeforge")) return false;
    }

    if (/^(hi|hello|hey|greetings|who are you|what can you do|help|what is this platform)[!?.]*$/i.test(lower)) {
      return true;
    }

    for (const kw of IN_SCOPE_KEYWORDS) {
      if (lower.includes(kw)) return true;
    }

    if (lower.includes("how do i") || lower.includes("how to") || lower.includes("where is") || lower.includes("can i")) {
      if (lower.includes("start") || lower.includes("solve") || lower.includes("submit") ||
          lower.includes("test") || lower.includes("practice") || lower.includes("register") ||
          lower.includes("login") || lower.includes("profile") || lower.includes("streak") ||
          lower.includes("notes") || lower.includes("community")) {
        return true;
      }
    }

    return false;
  }

  function getLocalResponse(msg) {
    const lower = msg.toLowerCase().trim();

    if (!isQueryInScope(msg)) {
      return {
        reply: OUT_OF_SCOPE_REFUSAL,
        inScope: false,
        suggestedPrompts: [
          "What challenges are on CodeForge?",
          "How does the online IDE work?",
          "How do I track my streak and goals?",
          "What tech stack powers this website?"
        ]
      };
    }

    let reply = "";
    let relevantRoute = null;
    let suggestions = [];

    if (/who are you|what can you do|introduce yourself/i.test(lower)) {
      reply = "Hello! I am **ForgeBot**, the synchronized AI assistant for **CodeForge**.\n\n" +
        "I am connected to the backend to help you with:\n" +
        "• Exploring our **curated algorithmic challenges** across 6 learning tracks.\n" +
        "• Running, testing, and submitting code in the **online IDE** (Python, JavaScript, Java, C++).\n" +
        "• Utilizing **Learner Notes** to capture invariant patterns and interview takeaways.\n" +
        "• Participating in **Community Discussions** and exploring peer solutions.\n" +
        "• Tracking your **daily streak**, weekly goals, and performance metrics.";
      suggestions = ["Show me the challenges", "How do I run code in the IDE?", "How do Learner Notes work?"];
    } else if (lower.includes("what is codeforge") || lower.includes("about codeforge") || lower.includes("what is this platform")) {
      reply = "**CodeForge** is a high-performance web platform engineered for software developers to master algorithms and conquer technical interviews.\n\n" +
        "**Core Highlights:**\n" +
        "• **100+ Curated Problems**: Covering Arrays, Two Pointers, Sliding Window, Trees, Graphs, and DP.\n" +
        "• **Multi-Language Web IDE**: Write, run, and benchmark code in Python, JavaScript, Java, and C++ with Judge0 sandboxing.\n" +
        "• **Personal Notebook**: Take and link markdown notes to specific problems for rapid review.\n" +
        "• **Developer Community**: Share optimal approaches, upvote solutions, and read real interview loop debriefs.\n" +
        "• **Progress Engine**: Keep your daily streak alive and complete weekly practice targets.";
      relevantRoute = "library";
      suggestions = ["How do I practice problems?", "Tell me about the IDE", "How do streaks work?"];
    } else if (lower.includes("ide") || lower.includes("run code") || lower.includes("execution") || lower.includes("judge0") || lower.includes("language")) {
      reply = "The **CodeForge Web IDE** provides a distraction-free, professional coding workspace:\n\n" +
        "• **Supported Languages**: Python 3.12, JavaScript (Node.js 20), Java 13/21, and C++.\n" +
        "• **Run Code (`▷ Run code`)**: Executes your solution against sample test cases (Case 1 & Case 2) in ~30ms.\n" +
        "• **Submit (`↑ Submit solution`)**: Judges your implementation against full test suites and reports runtime percentiles and memory usage.\n" +
        "• **Focus Mode**: Press the maximize icon to enter distraction-free full-screen coding.\n" +
        "• **Stopwatch**: Live session timer tracks your speed on each challenge.";
      relevantRoute = "ide";
      suggestions = ["What is Problem 104 Pair Sum?", "How do test cases work?", "Go to IDE"];
    } else if (lower.includes("challenge") || lower.includes("problem") || lower.includes("library") || lower.includes("catalog")) {
      reply = "CodeForge features **100+ curated algorithmic challenges** categorized by topic and difficulty:\n\n" +
        "• **Difficulty Levels**: Easy (32+), Medium (14+), and Hard (2+).\n" +
        "• **Key Topics**: Arrays & Hashing, Two Pointers, Sliding Window, Binary Search, Linked Lists, Trees, Graphs, and Dynamic Programming.\n" +
        "• **Search & Filter**: Press `Cmd + K` (or `Ctrl + K`) anytime on the Library page to instantly search problems.\n" +
        "• **Random Picker**: Click `🎲 Pick a random problem` to test your intuition without difficulty bias.";
      relevantRoute = "library";
      suggestions = ["Explain Pair Sum #104", "What is Sliding Window?", "Go to Problem Library"];
    } else if (lower.includes("note") || lower.includes("notebook") || lower.includes("cheatsheet")) {
      reply = "**Learner Notes** is your personal technical interview notebook:\n\n" +
        "• **Problem Linking**: Link your notes directly to a problem (e.g. `#104 Pair Sum`). Clicking the problem badge in your notes opens the Web IDE instantly.\n" +
        "• **Tagging & Filtering**: Organize by tags (`Arrays`, `Sliding Window`, `Dynamic Programming`, `Binary Search`).\n" +
        "• **Markdown Formatting**: Write code blocks with `` `code` ``, bold text with `**bold**`, and formatted bullet points.\n" +
        "• **Persistence**: Your notes persist in your browser (`localStorage`) and sync with your learner profile.";
      relevantRoute = "notes";
      suggestions = ["Open my Notes", "How do I create a note?", "Browse Community"];
    } else if (lower.includes("community") || lower.includes("discussion") || lower.includes("forum") || lower.includes("upvote") || lower.includes("comment")) {
      reply = "The **CodeForge Developer Community** is a collaborative space for engineers:\n\n" +
        "• **Categories**: Filter by *Solutions*, *Questions*, *Interview Prep*, or *Tips & Tricks*.\n" +
        "• **Upvoting**: Upvote helpful explanations to elevate optimal approaches for the community.\n" +
        "• **Discussion Threads**: View in-depth solutions with Big-O analysis and reply with optimizations.\n" +
        "• **Moderation**: Content is monitored by platform Admins with community report flags.";
      relevantRoute = "community";
      suggestions = ["View Community Discussions", "How to share a solution?", "Open Notes"];
    } else if (lower.includes("pair sum") || lower.includes("104") || lower.includes("two sum")) {
      reply = "**Problem #104: Pair Sum (Easy)**\n\n" +
        "• **Description**: Given an array `nums` and target integer `target`, return indices of two numbers that add up to `target`.\n" +
        "• **Optimal Approach**: Use a **1-pass Hash Map**. Store each element's complement `target - num` as you iterate.\n" +
        "• **Complexity**: Time **O(N)**, Space **O(N)**.\n" +
        "• **Common Pitfall**: Do not sort the array first, as sorting loses the original element indices!";
      relevantRoute = "ide";
      suggestions = ["Open Pair Sum in IDE", "What is Sliding Window?", "View Solutions in Community"];
    } else if (lower.includes("streak") || lower.includes("goal") || lower.includes("dashboard") || lower.includes("overview")) {
      reply = "Your **CodeForge Dashboard** tracks your momentum:\n\n" +
        "• **Daily Streak**: Increments every day you solve or submit challenges on CodeForge.\n" +
        "• **Weekly Goal**: Radial tracker displays your weekly target (e.g. 8/10 completed) with Monday-Sunday checkmarks.\n" +
        "• **Acceptance Rate & Time**: Real-time stats on your overall acceptance percentile and hours practiced.\n" +
        "• **Next Challenge Spotlight**: Recommends the optimal next problem to tackle based on your progress.";
      relevantRoute = "dashboard";
      suggestions = ["Go to Dashboard", "Browse Problems", "Explain Pair Sum #104"];
    } else if (lower.includes("tech stack") || lower.includes("architecture") || lower.includes("spring boot") || lower.includes("backend")) {
      reply = "**CodeForge Synchronized Architecture:**\n\n" +
        "• **Backend Layer**: Java 21 + **Spring Boot 3.3.4** REST controllers, JPA/Hibernate, stateless **JWT security**, and H2/SQL Server storage.\n" +
        "• **Code Execution**: Integrated **Judge0 sandbox** executing isolated Python, Java, JS, and C++ code.\n" +
        "• **AI Chatbot Engine**: Spring Boot `/api/chatbot` synchronized with live JPA entities and Gemini LLM support.\n" +
        "• **Frontend Client**: Vanilla modular JavaScript, Tailwind CSS, and JetBrains Mono code editor styling.";
      suggestions = ["What is CodeForge?", "How does the IDE run code?", "View Problems"];
    } else {
      reply = "I am ready to help you with anything on CodeForge! Here are quick guides you can explore:\n\n" +
        "• **Browse Challenges**: Visit the **Problem Library** to select from 100+ algorithmic exercises.\n" +
        "• **Online IDE**: Open the **IDE** to code in Python, JavaScript, Java, or C++ with live test runners.\n" +
        "• **Study Notes**: Record invariant checklists and pattern formulas in **Learner Notes**.\n" +
        "• **Discussions**: Check the **Community** to exchange optimal solutions with other engineers.";
      suggestions = ["Open Problem Library", "How to run code in IDE?", "View Learner Notes", "Community Discussions"];
    }

    return {
      reply,
      inScope: true,
      relevantRoute,
      suggestedPrompts: suggestions
    };
  }

  // Synchronized Query to Backend API
  async function queryChatbot(userMessage) {
    const convId = getConversationId();
    const payload = {
      message: userMessage,
      conversationId: convId,
      contextPage: window.location.hash.replace('#', '') || 'dashboard'
    };

    if (window.CodeForgeAPI && typeof CodeForgeAPI.askChatbot === 'function') {
      const response = await CodeForgeAPI.askChatbot(payload);
      if (response && response.reply) {
        return response;
      }
    }

    // Direct fetch fallback
    try {
      const token = localStorage.getItem('codeforge_token') || sessionStorage.getItem('codeforge_token');
      const headers = { 'Content-Type': 'application/json' };
      if (token) headers['Authorization'] = 'Bearer ' + token;

      const res = await fetch('http://localhost:8080/api/chatbot/ask', {
        method: 'POST',
        headers,
        body: JSON.stringify(payload)
      });
      if (res.ok) {
        return await res.json();
      }
    } catch (_) {}

    return getLocalResponse(userMessage);
  }

  // Load backend message history on first open
  async function loadHistoryFromBackend() {
    if (isHistoryLoaded) return;
    isHistoryLoaded = true;

    const convId = getConversationId();
    let history = null;

    if (window.CodeForgeAPI && typeof CodeForgeAPI.getChatHistory === 'function') {
      history = await CodeForgeAPI.getChatHistory(convId);
    } else {
      try {
        const token = localStorage.getItem('codeforge_token') || sessionStorage.getItem('codeforge_token');
        const headers = {};
        if (token) headers['Authorization'] = 'Bearer ' + token;
        const res = await fetch(`http://localhost:8080/api/chatbot/history?conversationId=${encodeURIComponent(convId)}`, { headers });
        if (res.ok) history = await res.json();
      } catch (_) {}
    }

    const messagesContainer = document.getElementById('chatbot-messages');
    if (!messagesContainer) return;

    if (history && history.messages && history.messages.length > 0) {
      messagesContainer.innerHTML = '';
      history.messages.forEach(m => {
        appendMessage(m.sender, m.message, m.relevantRoute, []);
      });
    } else if (messagesContainer.children.length === 0) {
      // Default greeting if no prior conversation
      appendMessage(
        'bot',
        "👋 Hello! I am **ForgeBot**, the official AI guide for **CodeForge**.\n\n" +
        "I am synchronized with the backend engine to answer questions about the **CodeForge platform**, our **100+ coding challenges**, " +
        "the online IDE, algorithm notes, community discussions, and system architecture.\n\n" +
        "How can I assist your coding journey today?",
        null,
        [
          "How to run code in the IDE?",
          "Tell me about 100 challenges",
          "Explain Pair Sum #104",
          "How do Learner Notes work?",
          "What is the platform's tech stack?"
        ]
      );
    }
  }

  function appendMessage(sender, text, relevantRoute = null, suggestedPrompts = []) {
    const messagesContainer = document.getElementById('chatbot-messages');
    if (!messagesContainer) return;

    const isUser = sender === 'user';
    const msgDiv = document.createElement('div');
    msgDiv.className = `flex gap-2.5 ${isUser ? 'justify-end' : 'justify-start'} animate-fadeIn`;

    const avatarHtml = isUser
      ? ''
      : `<div class="w-7 h-7 rounded-lg bg-emerald-800 text-emerald-300 font-mono font-bold text-xs flex items-center justify-center shrink-0 shadow-xs">
          <svg class="w-4 h-4" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M12 2a2 2 0 0 1 2 2v2a2 2 0 0 1-2 2 2 2 0 0 1-2-2V4a2 2 0 0 1 2-2z"/><rect x="4" y="8" width="16" height="12" rx="2"/><circle cx="9" cy="13" r="1"/><circle cx="15" cy="13" r="1"/><path d="M9 17h6"/></svg>
         </div>`;

    const bubbleBg = isUser
      ? 'bg-emerald-700 text-white rounded-br-none'
      : 'bg-white border border-gray-200 text-gray-800 rounded-bl-none shadow-xs';

    let actionsHtml = '';
    if (relevantRoute && window.CodeForgeApp) {
      actionsHtml = `
        <div class="mt-2.5 pt-2 border-t border-gray-100 flex items-center gap-2">
          <button type="button" onclick="CodeForgeApp.navigate('${relevantRoute}'); CodeForgeChatbot.close();" class="inline-flex items-center gap-1.5 px-3 py-1 rounded-md bg-emerald-50 hover:bg-emerald-100 text-emerald-800 text-xs font-semibold border border-emerald-200 transition">
            <span>Go to ${relevantRoute.charAt(0).toUpperCase() + relevantRoute.slice(1)}</span>
            <svg class="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M14 5l7 7m0 0l-7 7m7-7H3"/></svg>
          </button>
        </div>
      `;
    }

    let promptsHtml = '';
    if (!isUser && suggestedPrompts && suggestedPrompts.length > 0) {
      promptsHtml = `
        <div class="mt-2.5 flex flex-wrap gap-1.5 pt-1">
          ${suggestedPrompts.map(p => `
            <button type="button" onclick="CodeForgeChatbot.sendPrompt('${escapeHtml(p)}')" class="px-2.5 py-1 rounded-md text-[11px] font-medium bg-emerald-50/70 hover:bg-emerald-100 text-emerald-800 border border-emerald-200/70 transition">
              ${escapeHtml(p)}
            </button>
          `).join('')}
        </div>
      `;
    }

    msgDiv.innerHTML = `
      ${avatarHtml}
      <div class="max-w-[85%] space-y-1">
        <div class="p-3.5 rounded-2xl text-xs sm:text-[13px] leading-relaxed font-sans ${bubbleBg}">
          ${isUser ? escapeHtml(text) : formatMarkdown(text)}
          ${actionsHtml}
        </div>
        ${promptsHtml}
      </div>
    `;

    messagesContainer.appendChild(msgDiv);
    messagesContainer.scrollTop = messagesContainer.scrollHeight;
  }

  function showTypingIndicator() {
    const messagesContainer = document.getElementById('chatbot-messages');
    if (!messagesContainer) return;

    removeTypingIndicator();

    const indicator = document.createElement('div');
    indicator.id = 'chatbot-typing-indicator';
    indicator.className = 'flex gap-2.5 justify-start animate-fadeIn';
    indicator.innerHTML = `
      <div class="w-7 h-7 rounded-lg bg-emerald-800 text-emerald-300 font-mono font-bold text-xs flex items-center justify-center shrink-0">
        <svg class="w-4 h-4" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M12 2a2 2 0 0 1 2 2v2a2 2 0 0 1-2 2 2 2 0 0 1-2-2V4a2 2 0 0 1 2-2z"/><rect x="4" y="8" width="16" height="12" rx="2"/><circle cx="9" cy="13" r="1"/><circle cx="15" cy="13" r="1"/><path d="M9 17h6"/></svg>
      </div>
      <div class="p-3 bg-white border border-gray-200 rounded-2xl rounded-bl-none shadow-xs flex items-center gap-1.5">
        <span class="w-1.5 h-1.5 rounded-full bg-emerald-600 animate-bounce"></span>
        <span class="w-1.5 h-1.5 rounded-full bg-emerald-600 animate-bounce [animation-delay:0.2s]"></span>
        <span class="w-1.5 h-1.5 rounded-full bg-emerald-600 animate-bounce [animation-delay:0.4s]"></span>
      </div>
    `;

    messagesContainer.appendChild(indicator);
    messagesContainer.scrollTop = messagesContainer.scrollHeight;
  }

  function removeTypingIndicator() {
    const indicator = document.getElementById('chatbot-typing-indicator');
    if (indicator) indicator.remove();
  }

  async function handleSendMessage(msgText) {
    if (isTyping) return;
    const text = (msgText || '').trim();
    if (!text) return;

    const input = document.getElementById('chatbot-input');
    if (input) input.value = '';

    appendMessage('user', text);
    isTyping = true;
    showTypingIndicator();

    try {
      const response = await queryChatbot(text);
      removeTypingIndicator();
      isTyping = false;
      appendMessage('bot', response.reply, response.relevantRoute, response.suggestedPrompts);
    } catch (_) {
      removeTypingIndicator();
      isTyping = false;
      appendMessage('bot', OUT_OF_SCOPE_REFUSAL);
    }
  }

  function sendPrompt(promptText) {
    handleSendMessage(promptText);
  }

  async function open() {
    isOpen = true;
    const widget = document.getElementById('codeforge-chatbot-widget');
    const badge = document.getElementById('chatbot-unread-dot');
    if (widget) {
      widget.classList.remove('hidden');
      widget.classList.add('flex');
    }
    if (badge) badge.classList.add('hidden');

    const input = document.getElementById('chatbot-input');
    if (input) setTimeout(() => input.focus(), 150);

    // Load backend history
    await loadHistoryFromBackend();
  }

  function close() {
    isOpen = false;
    const widget = document.getElementById('codeforge-chatbot-widget');
    if (widget) {
      widget.classList.add('hidden');
      widget.classList.remove('flex');
    }
  }

  function toggle() {
    if (isOpen) {
      close();
    } else {
      open();
    }
  }

  async function clearHistory() {
    const convId = getConversationId();
    if (window.CodeForgeAPI && typeof CodeForgeAPI.clearChatHistory === 'function') {
      await CodeForgeAPI.clearChatHistory(convId);
    }

    const messagesContainer = document.getElementById('chatbot-messages');
    if (messagesContainer) {
      messagesContainer.innerHTML = '';
    }
    isHistoryLoaded = false;
    await loadHistoryFromBackend();
  }

  function bindEvents() {
    const btnToggle = document.getElementById('btn-chatbot-toggle');
    if (btnToggle) btnToggle.addEventListener('click', toggle);

    const btnClose = document.getElementById('btn-chatbot-close');
    if (btnClose) btnClose.addEventListener('click', close);

    const btnClear = document.getElementById('btn-chatbot-clear');
    if (btnClear) btnClear.addEventListener('click', clearHistory);

    const form = document.getElementById('chatbot-form');
    if (form) {
      form.addEventListener('submit', (e) => {
        e.preventDefault();
        const input = document.getElementById('chatbot-input');
        if (input) handleSendMessage(input.value);
      });
    }

    const input = document.getElementById('chatbot-input');
    if (input) {
      input.addEventListener('keydown', (e) => {
        if (e.key === 'Enter' && !e.shiftKey) {
          e.preventDefault();
          handleSendMessage(input.value);
        }
      });
    }
  }

  function init() {
    bindEvents();
  }

  return {
    init,
    open,
    close,
    toggle,
    sendPrompt,
    clearHistory
  };
})();

// Auto-boot on DOM ready
document.addEventListener('DOMContentLoaded', () => {
  if (window.CodeForgeChatbot) CodeForgeChatbot.init();
});
