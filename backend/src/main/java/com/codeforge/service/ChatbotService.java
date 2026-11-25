package com.codeforge.service;

import com.codeforge.dto.ChatbotDto.ChatHistoryResponse;
import com.codeforge.dto.ChatbotDto.ChatRequest;
import com.codeforge.dto.ChatbotDto.ChatResponse;
import com.codeforge.dto.ChatbotDto.MessageItem;
import com.codeforge.entity.ChatMessage;
import com.codeforge.entity.Discussion;
import com.codeforge.entity.Exercise;
import com.codeforge.entity.User;
import com.codeforge.repository.ChatMessageRepository;
import com.codeforge.repository.DiscussionRepository;
import com.codeforge.repository.ExerciseRepository;
import com.codeforge.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * CodeForge Synchronized AI Chatbot Engine (ForgeBot).
 *
 * <p>Enforces strict domain boundaries: ForgeBot ONLY answers questions regarding the CodeForge platform
 * in English. Communicates with live database entities (exercises, discussions, user progress) and supports
 * external Generative AI (Gemini / LLM) with database context grounding (RAG).</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ChatbotService {

    private final ChatMessageRepository chatMessageRepository;
    private final ExerciseRepository exerciseRepository;
    private final DiscussionRepository discussionRepository;
    private final UserRepository userRepository;

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${ai.chatbot.api.key:}")
    private String configuredApiKey;

    @Value("${ai.chatbot.model:gemini-1.5-flash}")
    private String modelName;

    /**
     * Standard polite refusal message when a query is outside the CodeForge domain.
     */
    public static final String OUT_OF_SCOPE_REFUSAL =
            "I am ForgeBot, the dedicated AI assistant for the CodeForge platform. " +
            "I am only permitted to answer questions directly related to CodeForge—such as our coding challenges, " +
            "the online IDE, Judge0 code execution sandbox, algorithm study notes, community discussions, user streaks, " +
            "and platform architecture.\n\n" +
            "I cannot assist with queries outside of the CodeForge platform. " +
            "How can I help you with your coding practice on CodeForge today?";

    // Keywords strictly related to CodeForge and its algorithmic learning domain
    private static final Set<String> IN_SCOPE_KEYWORDS = new HashSet<>(Arrays.asList(
            "codeforge", "forge", "platform", "challenge", "challenges", "problem", "problems",
            "ide", "code", "coding", "editor", "run", "running", "execute", "execution", "judge0",
            "compiler", "compile", "playground", "stdin",
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
            "python", "javascript", "java", "c++", "typescript", "rust", "golang", "spring boot", "h2", "sql server", "architecture",
            "focus mode", "theme", "dark mode", "light mode", "shortcut", "cmd+k", "ctrl+k", "random",
            "creator", "create problem", "author", "constraints", "help", "who are you", "what can you do"
    ));

    // Explicit out-of-scope triggers
    private static final List<Pattern> OUT_OF_SCOPE_PATTERNS = Arrays.asList(
            Pattern.compile("(?i)\\b(weather|forecast|rain|temperature)\\b"),
            Pattern.compile("(?i)\\b(president|prime minister|election|politics|government|war|congress)\\b"),
            Pattern.compile("(?i)\\b(capital of|population of|continent of)\\b"),
            Pattern.compile("(?i)\\b(recipe|bake|cook|ingredients|chocolate cake|pizza)\\b"),
            Pattern.compile("(?i)\\b(movie|actor|actress|hollywood|oscar|celebrity|singer|song|album)\\b"),
            Pattern.compile("(?i)\\b(football|soccer|fifa|world cup|messi|ronaldo|nba|basketball)\\b"),
            Pattern.compile("(?i)\\b(bitcoin price|crypto trade|stock market investment|forex)\\b"),
            Pattern.compile("(?i)\\b(write a poem about|write a story about|love letter)\\b"),
            Pattern.compile("(?i)\\b(horoscope|zodiac|astrology)\\b"),
            Pattern.compile("(?i)\\b(medical advice|doctor|cure cancer|symptoms of)\\b")
    );

    /**
     * Determines whether the user query is strictly within the CodeForge domain.
     */
    public boolean isQueryInScope(String query) {
        if (query == null || query.trim().isEmpty()) {
            return false;
        }

        String normalized = query.toLowerCase().trim();

        // Check for explicit out-of-scope patterns first
        for (Pattern pattern : OUT_OF_SCOPE_PATTERNS) {
            if (pattern.matcher(normalized).find()) {
                if (!normalized.contains("codeforge")) {
                    return false;
                }
            }
        }

        // Friendly greetings or platform identity queries
        if (normalized.matches("(?i)^(hi|hello|hey|greetings|good (morning|afternoon|evening)|who are you|what can you do|help|what is this platform)[!?.]*$")) {
            return true;
        }

        // Keyword matches
        for (String kw : IN_SCOPE_KEYWORDS) {
            if (normalized.contains(kw)) {
                return true;
            }
        }

        // Navigational or feature queries
        if (normalized.contains("how do i") || normalized.contains("how to") || normalized.contains("can i") || normalized.contains("where is")) {
            if (normalized.contains("start") || normalized.contains("solve") || normalized.contains("submit") ||
                normalized.contains("test") || normalized.contains("navigate") || normalized.contains("register") ||
                normalized.contains("profile") || normalized.contains("streak") || normalized.contains("account") ||
                normalized.contains("notes") || normalized.contains("community")) {
                return true;
            }
        }

        return false;
    }

    /**
     * Processes a user chat request, synchronizing messages with database history and grounding responses in live data.
     *
     * @param request Inbound message request
     * @param username Authenticated username or null for guest
     * @return ChatResponse with AI reply
     */
    public ChatResponse processChat(ChatRequest request, String username) {
        String userMsg = request.getMessage() != null ? request.getMessage().trim() : "";
        String convId = (request.getConversationId() != null && !request.getConversationId().trim().isEmpty())
                ? request.getConversationId().trim()
                : UUID.randomUUID().toString();

        User user = null;
        if (username != null) {
            user = userRepository.findByUsername(username).orElse(null);
        }

        // 1. Evaluate scope guardrail
        boolean inScope = isQueryInScope(userMsg);

        // Record User Message in Database
        chatMessageRepository.save(ChatMessage.builder()
                .conversationId(convId)
                .user(user)
                .sender("user")
                .message(userMsg)
                .inScope(inScope)
                .createdAt(LocalDateTime.now())
                .build());

        if (!inScope) {
            // Save bot refusal message
            chatMessageRepository.save(ChatMessage.builder()
                    .conversationId(convId)
                    .user(user)
                    .sender("bot")
                    .message(OUT_OF_SCOPE_REFUSAL)
                    .inScope(false)
                    .createdAt(LocalDateTime.now())
                    .build());

            return ChatResponse.builder()
                    .reply(OUT_OF_SCOPE_REFUSAL)
                    .inScope(false)
                    .relevantRoute(null)
                    .conversationId(convId)
                    .suggestedPrompts(Arrays.asList(
                            "What challenges are on CodeForge?",
                            "How does the online IDE work?",
                            "How do I track my streak and goals?",
                            "What tech stack powers this website?"
                    ))
                    .build();
        }

        // 2. Query Live Database Context for AI Grounding (RAG)
        long totalProblems = exerciseRepository.count();
        List<Exercise> sampleExercises = exerciseRepository.findAll().stream().limit(5).collect(Collectors.toList());
        long totalDiscussions = discussionRepository.count();
        List<Discussion> recentDiscussions = discussionRepository.findAllByOrderByCreatedAtDesc().stream().limit(3).collect(Collectors.toList());

        // 3. Generate AI Response (via Generative AI API or Grounded Dynamic Engine)
        String apiKey = getEffectiveApiKey();
        String reply;
        String relevantRoute = null;
        List<String> suggestions = new ArrayList<>();

        if (apiKey != null && !apiKey.isEmpty() && !apiKey.equalsIgnoreCase("YOUR_API_KEY_HERE")) {
            // Call Real LLM API (Google Gemini REST API with Grounded Context)
            reply = callGeminiGenerativeAI(userMsg, user, totalProblems, sampleExercises, totalDiscussions, recentDiscussions, apiKey);
        } else {
            // Grounded High-Performance AI Logic using Live Database State
            GroundedResult grounded = generateGroundedResponse(userMsg, user, totalProblems, sampleExercises, totalDiscussions, recentDiscussions);
            reply = grounded.reply;
            relevantRoute = grounded.relevantRoute;
            suggestions = grounded.suggestions;
        }

        // 4. Save Bot Message in Database History
        chatMessageRepository.save(ChatMessage.builder()
                .conversationId(convId)
                .user(user)
                .sender("bot")
                .message(reply)
                .inScope(true)
                .relevantRoute(relevantRoute)
                .createdAt(LocalDateTime.now())
                .build());

        return ChatResponse.builder()
                .reply(reply)
                .inScope(true)
                .relevantRoute(relevantRoute)
                .suggestedPrompts(suggestions)
                .conversationId(convId)
                .build();
    }

    /**
     * Retrieves synchronized message history for a user or conversation.
     */
    public ChatHistoryResponse getHistory(String conversationId, String username) {
        List<ChatMessage> list;
        User user = null;
        if (username != null) {
            user = userRepository.findByUsername(username).orElse(null);
        }

        if (user != null) {
            list = chatMessageRepository.findByUserOrderByCreatedAtAsc(user);
        } else if (conversationId != null && !conversationId.isEmpty()) {
            list = chatMessageRepository.findByConversationIdOrderByCreatedAtAsc(conversationId);
        } else {
            list = Collections.emptyList();
        }

        List<MessageItem> items = list.stream().map(m -> MessageItem.builder()
                .id(m.getId())
                .sender(m.getSender())
                .message(m.getMessage())
                .inScope(m.isInScope())
                .relevantRoute(m.getRelevantRoute())
                .createdAt(m.getCreatedAt())
                .build()).collect(Collectors.toList());

        return ChatHistoryResponse.builder()
                .conversationId(conversationId)
                .messages(items)
                .build();
    }

    /**
     * Clears synchronized chat history for a session or user.
     */
    public void clearHistory(String conversationId, String username) {
        User user = null;
        if (username != null) {
            user = userRepository.findByUsername(username).orElse(null);
        }

        if (user != null) {
            chatMessageRepository.deleteByUser(user);
        }
        if (conversationId != null && !conversationId.isEmpty()) {
            chatMessageRepository.deleteByConversationId(conversationId);
        }
    }

    private String getEffectiveApiKey() {
        if (configuredApiKey != null && !configuredApiKey.trim().isEmpty()) {
            return configuredApiKey.trim();
        }
        String envKey = System.getenv("GEMINI_API_KEY");
        if (envKey != null && !envKey.trim().isEmpty()) return envKey.trim();
        envKey = System.getenv("AI_CHATBOT_API_KEY");
        if (envKey != null && !envKey.trim().isEmpty()) return envKey.trim();
        return null;
    }

    /**
     * Calls Google Gemini Generative AI REST API with strictly enforced System Prompt and Grounded Context.
     */
    private String callGeminiGenerativeAI(String userQuery, User user, long totalProblems, List<Exercise> sampleExercises,
                                         long totalDiscussions, List<Discussion> recentDiscussions, String apiKey) {
        try {
            String url = "https://generativelanguage.googleapis.com/v1beta/models/" + modelName + ":generateContent?key=" + apiKey;

            StringBuilder contextBuilder = new StringBuilder();
            contextBuilder.append("CodeForge Live Database Context:\n");
            contextBuilder.append("- Total Problems in Catalog: ").append(totalProblems).append("\n");
            contextBuilder.append("- Sample Problems: ");
            for (Exercise e : sampleExercises) {
                contextBuilder.append("#").append(e.getId()).append(" ").append(e.getTitle())
                        .append(" (").append(e.getDifficulty()).append("), ");
            }
            contextBuilder.append("\n- Total Discussions in Community: ").append(totalDiscussions).append("\n");
            if (user != null) {
                contextBuilder.append("- Current Learner: ").append(user.getFullName())
                        .append(" (Username: ").append(user.getUsername())
                        .append(", Streak: ").append(user.getStreakDays() != null ? user.getStreakDays() : 0).append(" days")
                        .append(", Solved: ").append(user.getSolvedCount() != null ? user.getSolvedCount() : 0).append(")\n");
            }

            String systemInstruction =
                    "You are ForgeBot, the specialized AI assistant for the CodeForge algorithmic platform. " +
                    "CRITICAL RULES:\n" +
                    "1. Respond exclusively in English.\n" +
                    "2. You ONLY answer questions about CodeForge: coding challenges, the online IDE, Judge0 sandbox, algorithm study notes, community discussions, streaks, goals, and platform architecture.\n" +
                    "3. If the user asks about ANYTHING outside CodeForge (e.g., weather, politics, general trivia, movies, recipes, non-CodeForge coding), you MUST strictly refuse in English: \"" + OUT_OF_SCOPE_REFUSAL.replace("\"", "\\\"") + "\"\n" +
                    "4. Use the provided live database context to answer accurately.\n" +
                    "5. Format responses with clean Markdown.\n\n" +
                    contextBuilder.toString();

            Map<String, Object> body = new HashMap<>();
            Map<String, Object> contents = new HashMap<>();
            contents.put("role", "user");
            contents.put("parts", Collections.singletonList(Collections.singletonMap("text", userQuery)));
            body.put("contents", Collections.singletonList(contents));

            Map<String, Object> sysMap = new HashMap<>();
            sysMap.put("parts", Collections.singletonList(Collections.singletonMap("text", systemInstruction)));
            body.put("systemInstruction", sysMap);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

            ResponseEntity<Map> response = restTemplate.postForEntity(url, entity, Map.class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                List candidates = (List) response.getBody().get("candidates");
                if (candidates != null && !candidates.isEmpty()) {
                    Map firstCand = (Map) candidates.get(0);
                    Map content = (Map) firstCand.get("content");
                    if (content != null) {
                        List parts = (List) content.get("parts");
                        if (parts != null && !parts.isEmpty()) {
                            Map part = (Map) parts.get(0);
                            return (String) part.get("text");
                        }
                    }
                }
            }
        } catch (Exception ex) {
            log.warn("Gemini AI API call failed or timed out, using grounded engine: {}", ex.getMessage());
        }

        // Fallback to grounded engine if API fails
        GroundedResult fallback = generateGroundedResponse(userQuery, user, totalProblems, sampleExercises, totalDiscussions, recentDiscussions);
        return fallback.reply;
    }

    private static class GroundedResult {
        String reply;
        String relevantRoute;
        List<String> suggestions = new ArrayList<>();

        GroundedResult(String reply, String relevantRoute, List<String> suggestions) {
            this.reply = reply;
            this.relevantRoute = relevantRoute;
            this.suggestions = suggestions;
        }
    }

    /**
     * Synthesizes a response grounded dynamically in real database data and CodeForge features.
     */
    private GroundedResult generateGroundedResponse(String userMsg, User user, long totalProblems,
                                                    List<Exercise> sampleExercises, long totalDiscussions,
                                                    List<Discussion> recentDiscussions) {
        String lower = userMsg.toLowerCase();
        String reply;
        String relevantRoute = null;
        List<String> suggestions = new ArrayList<>();

        if (lower.matches("(?i).*(who are you|what can you do|introduce yourself).*")) {
            reply = "Hello! I am **ForgeBot**, the synchronized AI assistant for **CodeForge**.\n\n" +
                    "I am directly connected to the CodeForge live engine to help you with:\n" +
                    "• Exploring our **" + totalProblems + "+ curated algorithmic challenges** across 6 learning tracks.\n" +
                    "• Running, testing, and submitting code in the **online IDE** (Python, JavaScript, Java, C++).\n" +
                    "• Utilizing **Learner Notes** to capture invariant patterns and interview takeaways.\n" +
                    "• Browsing and engaging in our **" + totalDiscussions + "+ Community Discussions**.\n" +
                    "• Tracking your **daily streak**, weekly goals, and performance metrics.";
            suggestions.addAll(Arrays.asList("Browse the " + totalProblems + " challenges", "How do I run code in the IDE?", "How do Learner Notes work?"));

        } else if (lower.contains("my streak") || lower.contains("my progress") || lower.contains("my account") || lower.contains("how many have i solved")) {
            if (user != null) {
                int streak = user.getStreakDays() != null ? user.getStreakDays() : 0;
                int solved = user.getSolvedCount() != null ? user.getSolvedCount() : 0;
                reply = "Here is your live learner telemetry, **" + user.getFullName() + "**:\n\n" +
                        "• **Current Daily Streak**: 🔥 **" + streak + " days** active\n" +
                        "• **Challenges Solved**: ✅ **" + solved + " / " + totalProblems + "** problems\n" +
                        "• **Acceptance Rate**: 📈 **" + (user.getAcceptanceRate() != null ? user.getAcceptanceRate() + "%" : "76.2%") + "**\n" +
                        "• **Practice Time**: ⏱ **" + (user.getPracticeTimeHours() != null ? user.getPracticeTimeHours() + " hours" : "12.5h") + "**\n\n" +
                        "Keep the momentum going! Would you like to continue solving your next recommended challenge?";
            } else {
                reply = "You are currently browsing as a **Guest**.\n\n" +
                        "To track your daily coding streak, save solved challenge history, and record personal notes, please **Sign In** or create a free learner account!";
                relevantRoute = "login";
            }
            suggestions.addAll(Arrays.asList("Go to Dashboard", "Browse Challenges", "Open IDE"));

        } else if (lower.contains("challenge") || lower.contains("problem") || lower.contains("library") || lower.contains("catalog") || lower.contains("how many problems")) {
            StringBuilder sb = new StringBuilder();
            sb.append("CodeForge currently hosts **").append(totalProblems).append(" curated algorithmic challenges** in the live database:\n\n");
            sb.append("• **Difficulty Tiers**: Easy, Medium, and Hard across key FAANG interview categories.\n");
            sb.append("• **Sample Challenges from Database**:\n");
            for (Exercise e : sampleExercises) {
                sb.append("  - **#").append(e.getId()).append(" ").append(e.getTitle()).append("** — *").append(e.getDifficulty()).append("*\n");
            }
            sb.append("\nUse `Cmd + K` (or `Ctrl + K`) on the Library page to quickly filter by keyword, topic, or difficulty!");
            reply = sb.toString();
            relevantRoute = "library";
            suggestions.addAll(Arrays.asList("Open Pair Sum #104", "How to run code in IDE?", "View Problem Library"));

        } else if (lower.contains("compiler") || lower.contains("playground") || lower.contains("stdin")) {
            reply = "The **CodeForge Judge0 Compiler Playground** lets you compile and run arbitrary standalone programs directly via Judge0 CE:\n\n" +
                    "• **Multi-Language Runtime**: Supports **Python 3, C++ (GCC 14), Java 17, JavaScript (Node 20), C, TypeScript, Go, Rust**, and 70+ Judge0 languages.\n" +
                    "• **Custom Stdin & Expected Output**: Test interactive inputs, CPU time limits, and expected output verification.\n" +
                    "• **Full Telemetry**: Inspect `stdout`, `stderr`, `compile_output`, execution time, memory KB, and raw Judge0 JSON payloads.";
            relevantRoute = "compiler";
            suggestions.addAll(Arrays.asList("How does the online IDE work?", "What is Pair Sum #104?", "Browse Challenges"));

        } else if (lower.contains("ide") || lower.contains("run code") || lower.contains("execution") || lower.contains("judge0") || lower.contains("language")) {
            reply = "The **CodeForge Web IDE** connects directly to our backend execution engine:\n\n" +
                    "• **Sandboxed Compilation**: Powered by Judge0 with sub-second execution in isolated containers.\n" +
                    "• **Supported Languages**: Python 3.12, JavaScript (Node.js 20), Java 13/21, and C++.\n" +
                    "• **Run Code (`▷ Run code`)**: Validates against test cases (Case 1 & Case 2) in ~30ms.\n" +
                    "• **Submit (`↑ Submit solution`)**: Evaluates against hidden edge cases and reports runtime and memory percentiles.\n" +
                    "• **Focus Mode**: Distraction-free full-screen coding view.";
            relevantRoute = "ide";
            suggestions.addAll(Arrays.asList("What is Pair Sum #104?", "Go to IDE", "Browse Challenges"));

        } else if (lower.contains("note") || lower.contains("notebook") || lower.contains("cheatsheet")) {
            reply = "**Learner Notes** is integrated with your practice sessions:\n\n" +
                    "• **Problem Linking**: Link your notes directly to a problem (e.g. `#104 Pair Sum`). Clicking the problem badge in your notes opens the Web IDE instantly.\n" +
                    "• **Tagging & Filtering**: Organize by tags (`Arrays`, `Sliding Window`, `Dynamic Programming`, `Binary Search`).\n" +
                    "• **Markdown Formatting**: Write code blocks with `` `code` ``, bold text with `**bold**`, and formatted bullet points.\n" +
                    "• **Backend Sync**: Your notes persist and synchronize across devices.";
            relevantRoute = "notes";
            suggestions.addAll(Arrays.asList("Open my Notes", "View Problem Library", "Browse Community"));

        } else if (lower.contains("community") || lower.contains("discussion") || lower.contains("forum") || lower.contains("upvote") || lower.contains("comment")) {
            StringBuilder sb = new StringBuilder();
            sb.append("The **CodeForge Developer Community** has **").append(totalDiscussions).append(" live discussions**:\n\n");
            sb.append("• **Categories**: *Solutions*, *Questions*, *Interview Prep*, and *Tips & Tricks*.\n");
            if (!recentDiscussions.isEmpty()) {
                sb.append("• **Recent Hot Topics**:\n");
                for (Discussion d : recentDiscussions) {
                    sb.append("  - **[").append(d.getCategory()).append("]** ").append(d.getTitle()).append(" (").append(d.getUpvotes()).append(" upvotes)\n");
                }
            }
            sb.append("\nYou can participate, upvote optimal solutions, or ask questions!");
            reply = sb.toString();
            relevantRoute = "community";
            suggestions.addAll(Arrays.asList("Open Community", "View Notes", "Go to Library"));

        } else if (lower.contains("pair sum") || lower.contains("104") || lower.contains("two sum")) {
            reply = "**Problem #104: Pair Sum (Easy)**\n\n" +
                    "• **Description**: Given an array `nums` and target integer `target`, return indices of two numbers that add up to `target`.\n" +
                    "• **Optimal Approach**: Use a **1-pass Hash Map**. Store each element's complement `target - num` as you iterate.\n" +
                    "• **Complexity**: Time **O(N)**, Space **O(N)**.\n" +
                    "• **Common Pitfall**: Do not sort the array first, as sorting loses the original element indices!";
            relevantRoute = "ide";
            suggestions.addAll(Arrays.asList("Open Pair Sum in IDE", "What is Sliding Window?", "View Solutions in Community"));

        } else if (lower.contains("tech stack") || lower.contains("architecture") || lower.contains("spring boot") || lower.contains("backend")) {
            reply = "**CodeForge Synchronized Architecture:**\n\n" +
                    "• **Backend Core**: Java 21 + **Spring Boot 3.3.4** REST controllers, JPA/Hibernate, stateless **JWT security**, and H2/SQL Server.\n" +
                    "• **Code Execution**: Integrated **Judge0 sandbox** executing isolated Python, Java, JS, and C++ code.\n" +
                    "• **AI Chatbot Engine**: Spring Boot `/api/chatbot` synchronized with live JPA entities and Gemini LLM support.\n" +
                    "• **Frontend Client**: Vanilla modular JavaScript, Tailwind CSS, and JetBrains Mono code editor styling.";
            suggestions.addAll(Arrays.asList("What is CodeForge?", "How does the IDE run code?", "View Problems"));

        } else {
            reply = "I am ready to help you with anything on CodeForge! Here are quick guides you can explore:\n\n" +
                    "• **Browse Challenges**: Visit the **Problem Library** to select from " + totalProblems + "+ algorithmic exercises.\n" +
                    "• **Online IDE**: Open the **IDE** to code in Python, JavaScript, Java, or C++ with live test runners.\n" +
                    "• **Study Notes**: Record invariant checklists and pattern formulas in **Learner Notes**.\n" +
                    "• **Discussions**: Check the **Community** to exchange optimal solutions with other engineers.";
            suggestions.addAll(Arrays.asList("Open Problem Library", "How to run code in IDE?", "View Learner Notes", "Community Discussions"));
        }

        return new GroundedResult(reply, relevantRoute, suggestions);
    }
}
