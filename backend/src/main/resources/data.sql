// CodeForge Mock Database & Initial State

window.CodeForgeData = {
  currentUser: {
    name: "Guest",
    initials: "G",
    greetingName: "there",
    streakDays: 7,
    solvedCount: 48,
    solvedWeek: 8,
    acceptanceRate: "76.2%",
    acceptanceDelta: "+4.1%",
    practiceTime: "12.5h",
    practiceDelta: "+2.4h",
    weeklyGoalDone: 8,
    weeklyGoalTarget: 10,
    weekHistory: [
      { day: "M", done: true },
      { day: "T", done: true },
      { day: "W", done: true },
      { day: "T", done: true },
      { day: "F", done: true },
      { day: "S", done: false },
      { day: "S", done: false }
    ],
    learningPaths: [
      {
        id: "arrays-hashing",
        title: "Arrays & hashing",
        subtitle: "Build your foundations",
        icon: "code",
        completed: 12,
        total: 20,
        pct: 60
      },
      {
        id: "two-pointers",
        title: "Two pointers",
        subtitle: "Solve with less space",
        icon: "git-commit",
        completed: 4,
        total: 12,
        pct: 33
      },
      {
        id: "trees-graphs",
        title: "Trees & graphs",
        subtitle: "Connect the concepts",
        icon: "git-branch",
        completed: 2,
        total: 18,
        pct: 11
      }
    ],
    recentActivity: [
      { id: 1, title: "Pair Sum", status: "Accepted", runtime: "32 ms", memory: "17.4 MB", date: "Just now" },
      { id: 106, title: "Valid Anagram", status: "Accepted", runtime: "41 ms", memory: "16.8 MB", date: "Yesterday" },
      { id: 105, title: "Contains Duplicate", status: "Accepted", runtime: "28 ms", memory: "19.2 MB", date: "2 days ago" },
      { id: 110, title: "Valid Parentheses", status: "Accepted", runtime: "19 ms", memory: "16.5 MB", date: "4 days ago" }
    ]
  },

  problems: [
    {
      id: 1,
      title: "Pair Sum",
      difficulty: "Easy",
      topics: ["Arrays", "Hash map"],
      acceptance: "78.4%",
      status: "unattempted", // 'solved', 'in-progress', 'unattempted'
      saved: false,
      isMine: true,
      author: "Alex Morgan",
      updatedDate: "Oct 28, 2026",
      pathInfo: "Arrays & hashing · Step 13 of 20",
      timeEst: "~15 min",
      description: `Given an integer array \`nums\` and an integer \`target\`, return the indices of the two numbers that add up to \`target\`.

You may assume that each input has exactly one solution. You may not use the same element twice.

Return the answer in any order.`,
      examples: [
        {
          input: "nums = [2,7,11,15], target = 9",
          output: "[0,1]",
          explanation: "Because nums[0] + nums[1] == 2 + 7 == 9, return [0,1]."
        },
        {
          input: "nums = [3,2,4], target = 6",
          output: "[1,2]",
          explanation: "The numbers at indices 1 and 2 add up to 6."
        }
      ],
      constraints: [
        "2 ≤ nums.length ≤ 10⁴",
        "-10⁹ ≤ nums[i], target ≤ 10⁹",
        "Exactly one valid answer exists."
      ],
      starterCode: {
        python: `def pair_sum(nums: list[int], target: int) -> list[int]:
    # Remember each value and its index.
    seen = {}

    for i, num in enumerate(nums):
        complement = target - num

        if complement in seen:
            return [seen[complement], i]

        seen[num] = i

    return []`,
        javascript: `/**
 * @param {number[]} nums
 * @param {number} target
 * @return {number[]}
 */
function pairSum(nums, target) {
    const seen = new Map();
    for (let i = 0; i < nums.length; i++) {
        const complement = target - nums[i];
        if (seen.has(complement)) {
            return [seen.get(complement), i];
        }
        seen.set(nums[i], i);
    }
    return [];
}`,
        typescript: `function pairSum(nums: number[], target: number): number[] {
    const seen: Map<number, number> = new Map();
    for (let i = 0; i < nums.length; i++) {
        const complement = target - nums[i];
        if (seen.has(complement)) {
            return [seen.get(complement)!, i];
        }
        seen.set(nums[i], i);
    }
    return [];
}`,
        java: `import java.util.*;

class Solution {
    public int[] pairSum(int[] nums, int target) {
        Map<Integer, Integer> seen = new HashMap<>();
        for (int i = 0; i < nums.length; i++) {
            int complement = target - nums[i];
            if (seen.containsKey(complement)) {
                return new int[] { seen.get(complement), i };
            }
            seen.put(nums[i], i);
        }
        return new int[0];
    }
}`
      },
      testCases: [
        {
          id: 1,
          name: "Case 1",
          input: "nums = [2,7,11,15], target = 9",
          expected: "[0, 1]",
          yourOutput: "[0, 1]",
          passed: true
        },
        {
          id: 2,
          name: "Case 2",
          input: "nums = [3,2,4], target = 6",
          expected: "[1, 2]",
          yourOutput: "[1, 2]",
          passed: true
        }
      ],
      editorial: {
        title: "One-Pass Hash Table (Intuition & Walkthrough)",
        timeComplexity: "O(n)",
        spaceComplexity: "O(n)",
        body: `### Approach: One-pass Hash Map

While we iterate and insert elements into the hash table, we also look back to check if current element's complement already exists in the table. If it exists, we have found a solution and return its index immediately.

#### Algorithm Steps:
1. Initialize an empty hash map \`seen\` to store numbers as keys and their 0-based indices as values.
2. Iterate through \`nums\` with index \`i\` and value \`num\`.
3. Compute the \`complement = target - num\`.
4. Check if \`complement\` is in \`seen\`:
   - If yes, return \`[seen[complement], i]\`.
   - If no, add \`seen[num] = i\` to \`seen\`.
5. Return empty list if no pair is found.`
      },
      solutions: [
        {
          author: "StefanPochmann",
          title: "Clean Python One-pass Hash Map with comments",
          votes: 1420,
          language: "Python 3",
          preview: "Use a dictionary to remember numbers seen so far."
        },
        {
          author: "neetcode",
          title: "Detailed Video & Visual Explanation (Hash Map O(N))",
          votes: 980,
          language: "General",
          preview: "Why brute force is O(n^2) and how Hash Map trades memory for O(n) time."
        },
        {
          author: "alex_morgan",
          title: "Optimized Java HashMap Solution (1ms beats 100%)",
          votes: 412,
          language: "Java",
          preview: "Pre-size the HashMap to avoid rehash overhead."
        }
      ],
      submissions: [
        { status: "Accepted", runtime: "32 ms", memory: "17.4 MB", language: "Python 3", date: "Oct 28, 2026 14:22" },
        { status: "Accepted", runtime: "38 ms", memory: "17.6 MB", language: "Python 3", date: "Oct 28, 2026 14:18" },
        { status: "Wrong Answer", runtime: "28 ms", memory: "17.3 MB", language: "Python 3", date: "Oct 28, 2026 14:15" }
      ]
    }
  ],

  challengeForm: {
    title: "Pair Sum",
    markdownStatement: `Given an integer array \`nums\` and an integer \`target\`,
return the indices of the two numbers that add up to \`target\`.

You may assume that each input has exactly one solution.
You may not use the same element twice.
Return the answer in any order.

### Constraints
- 2 ≤ nums.length ≤ 10⁴
- -10⁹ ≤ nums[i], target ≤ 10⁹
- Exactly one valid answer exists.`,
    language: "Python 3",
    difficulty: "Easy",
    topics: ["Arrays", "Hash map"],
    timeLimit: "1,000 ms",
    memoryLimit: "256 MB",
    version: "Live version 1.2"
  }
};

// ---------------------------------------------------------------------------
// Practice library catalog: 100 challenges in total (Pair Sum above + 99 below).
// Every challenge starts as "unattempted" because the learner has not solved
// anything yet. Entries use the format "Title|Topic,Topic".
// ---------------------------------------------------------------------------
(function buildProblemCatalog() {
  const RAW = {
    Easy: [
      "Contains Duplicate|Arrays,Hash set",
      "Valid Anagram|Strings,Hash map",
      "Valid Parentheses|Strings,Stacks",
      "Binary Search|Arrays,Binary search",
      "Best Time to Buy and Sell Stock|Arrays,Greedy",
      "Valid Palindrome|Strings,Two pointers",
      "Reverse String|Strings,Two pointers",
      "Merge Sorted Array|Arrays,Two pointers",
      "Move Zeroes|Arrays,Two pointers",
      "Majority Element|Arrays,Hash map",
      "Single Number|Arrays,Bit manipulation",
      "Missing Number|Arrays,Math",
      "Intersection of Two Arrays|Arrays,Hash set",
      "First Unique Character in a String|Strings,Hash map",
      "Ransom Note|Strings,Hash map",
      "Isomorphic Strings|Strings,Hash map",
      "Roman to Integer|Strings,Math",
      "Longest Common Prefix|Strings,Arrays",
      "Plus One|Arrays,Math",
      "Remove Duplicates from Sorted Array|Arrays,Two pointers",
      "Climbing Stairs|Dynamic programming,Math",
      "Fizz Buzz|Math,Strings",
      "Reverse Linked List|Linked list,Recursion",
      "Merge Two Sorted Lists|Linked list,Recursion",
      "Linked List Cycle|Linked list,Two pointers",
      "Maximum Depth of Binary Tree|Trees,Recursion",
      "Invert Binary Tree|Trees,Recursion",
      "Symmetric Tree|Trees,Recursion",
      "Min Stack|Stacks,Design",
      "Implement Queue using Stacks|Stacks,Design",
      "Happy Number|Math,Hash set",
      "Power of Two|Math,Bit manipulation",
      "Pascal's Triangle|Arrays,Dynamic programming"
    ],
    Medium: [
      "Group Anagrams|Strings,Sorting",
      "Top K Frequent Elements|Arrays,Heap",
      "Product of Array Except Self|Arrays,Prefix sum",
      "Longest Consecutive Sequence|Arrays,Hash set",
      "3Sum|Arrays,Two pointers",
      "Container With Most Water|Arrays,Two pointers",
      "Two Sum II - Sorted Input|Arrays,Two pointers",
      "Longest Substring Without Repeating Characters|Strings,Sliding window",
      "Longest Repeating Character Replacement|Strings,Sliding window",
      "Permutation in String|Strings,Sliding window",
      "Search in Rotated Sorted Array|Arrays,Binary search",
      "Find Minimum in Rotated Sorted Array|Arrays,Binary search",
      "Koko Eating Bananas|Arrays,Binary search",
      "Time Based Key-Value Store|Hash map,Binary search",
      "Daily Temperatures|Arrays,Stacks",
      "Car Fleet|Arrays,Stacks",
      "Generate Parentheses|Strings,Backtracking",
      "Evaluate Reverse Polish Notation|Stacks,Math",
      "Maximum Subarray|Arrays,Dynamic programming",
      "Jump Game|Arrays,Greedy",
      "Jump Game II|Arrays,Greedy",
      "Merge Intervals|Intervals,Sorting",
      "Insert Interval|Intervals,Arrays",
      "Non-overlapping Intervals|Intervals,Greedy",
      "Subsets|Backtracking,Arrays",
      "Permutations|Backtracking,Arrays",
      "Combination Sum|Backtracking,Arrays",
      "Word Search|Matrix,Backtracking",
      "Number of Islands|Graphs,Matrix",
      "Clone Graph|Graphs,Hash map",
      "Course Schedule|Graphs,Topological sort",
      "Pacific Atlantic Water Flow|Graphs,Matrix",
      "Rotting Oranges|Graphs,Queues",
      "Add Two Numbers|Linked list,Math",
      "Remove Nth Node From End of List|Linked list,Two pointers",
      "Reorder List|Linked list,Two pointers",
      "Copy List with Random Pointer|Linked list,Hash map",
      "Lowest Common Ancestor of a BST|Trees,Recursion",
      "Binary Tree Level Order Traversal|Trees,Queues",
      "Validate Binary Search Tree|Trees,Recursion",
      "Kth Smallest Element in a BST|Trees,Recursion",
      "Coin Change|Dynamic programming,Arrays",
      "House Robber|Dynamic programming,Arrays",
      "Longest Increasing Subsequence|Dynamic programming,Binary search",
      "Unique Paths|Dynamic programming,Math",
      "Decode Ways|Dynamic programming,Strings",
      "Word Break|Dynamic programming,Strings",
      "Partition Equal Subset Sum|Dynamic programming,Arrays",
      "K Closest Points to Origin|Heap,Sorting",
      "Kth Largest Element in an Array|Heap,Sorting"
    ],
    Hard: [
      "Trapping Rain Water|Arrays,Two pointers",
      "Minimum Window Substring|Strings,Sliding window",
      "Sliding Window Maximum|Arrays,Sliding window",
      "Median of Two Sorted Arrays|Arrays,Binary search",
      "Largest Rectangle in Histogram|Arrays,Stacks",
      "Merge k Sorted Lists|Linked list,Heap",
      "Reverse Nodes in k-Group|Linked list,Recursion",
      "Word Ladder|Graphs,Queues",
      "Longest Valid Parentheses|Strings,Dynamic programming",
      "Edit Distance|Dynamic programming,Strings",
      "Regular Expression Matching|Dynamic programming,Strings",
      "N-Queens|Backtracking,Matrix",
      "Binary Tree Maximum Path Sum|Trees,Recursion",
      "Serialize and Deserialize Binary Tree|Trees,Design",
      "Find Median from Data Stream|Heap,Design",
      "Alien Dictionary|Graphs,Topological sort"
    ]
  };

  // Acceptance-rate ranges per difficulty (values are stable across reloads).
  const RANGE = { Easy: [62, 86], Medium: [42, 68], Hard: [26, 52] };

  function stableAcceptance(title, difficulty) {
    let h = 0;
    for (let i = 0; i < title.length; i++) h = (h * 31 + title.charCodeAt(i)) >>> 0;
    const [lo, hi] = RANGE[difficulty];
    return (lo + (h % ((hi - lo) * 10 + 1)) / 10).toFixed(1) + "%";
  }

  // Mix difficulties so every page of the table looks like a real curriculum.
  const queues = { Easy: RAW.Easy.slice(), Medium: RAW.Medium.slice(), Hard: RAW.Hard.slice() };
  const PATTERN = ["Easy", "Medium", "Easy", "Medium", "Medium", "Hard"];
  const ordered = [];
  let i = 0;
  while (queues.Easy.length + queues.Medium.length + queues.Hard.length > 0) {
    let want = PATTERN[i++ % PATTERN.length];
    if (!queues[want].length) want = ["Medium", "Easy", "Hard"].find(d => queues[d].length);
    ordered.push({ difficulty: want, raw: queues[want].shift() });
  }

  ordered.forEach((entry, idx) => {
    const [title, topics] = entry.raw.split("|");
    CodeForgeData.problems.push({
      id: idx + 2, // Pair Sum is #1
      title,
      difficulty: entry.difficulty,
      topics: topics.split(","),
      acceptance: stableAcceptance(title, entry.difficulty),
      status: "unattempted",
      saved: false,
      isMine: false
    });
  });

  // Learner's Personal Notes initial mock data
  CodeForgeData.initialNotes = [
    {
      id: "note-1",
      title: "Two Sum: 1-Pass Hash Map Pattern",
      problemId: 104,
      problemTitle: "Pair Sum",
      tags: ["Arrays", "Hash map", "Optimization"],
      content: "Key takeaway: Instead of checking all pairs O(N²), store each element's complement `target - num` as we iterate. Lookup in hash map is O(1) average. Time complexity: O(N), Space complexity: O(N).",
      updatedAt: "2026-11-14T10:15:00Z"
    },
    {
      id: "note-2",
      title: "Sliding Window: Invariant Checklist",
      problemId: null,
      problemTitle: "General Template",
      tags: ["Sliding Window", "Two pointers", "Cheatsheet"],
      content: "1. Expand right pointer `r` to absorb elements into window.\n2. When window invariant is violated, contract left pointer `l`.\n3. Update answer either on valid state or after contraction.\nClassic applications: Longest Substring Without Repeating Characters, Minimum Window Substring.",
      updatedAt: "2026-11-13T16:30:00Z"
    },
    {
      id: "note-3",
      title: "Dynamic Programming: 5-Step Framework",
      problemId: null,
      problemTitle: "General Template",
      tags: ["Dynamic Programming", "Recursion", "Framework"],
      content: "Step 1: State definition (what does `dp[i]` represent in plain English?).\nStep 2: Base cases (initial row/column values).\nStep 3: Recurrence relation (choice between take or skip).\nStep 4: Computation order (bottom-up direction).\nStep 5: Space optimization (roll array if only previous row is needed).",
      updatedAt: "2026-11-12T11:45:00Z"
    },
    {
      id: "note-4",
      title: "Binary Search Edge Cases: Off-by-one Prevention",
      problemId: null,
      problemTitle: "General Template",
      tags: ["Binary Search", "Edge cases"],
      content: "Always use `mid = left + (right - left) / 2` to avoid 32-bit integer overflow.\nWhen updating bounds: if `left = mid`, then `mid` calculation must round up: `mid = left + (right - left + 1) / 2` to prevent infinite loops when `right - left == 1`.",
      updatedAt: "2026-11-11T08:20:00Z"
    }
  ];

  // Learner's Community Discussions initial mock data
  CodeForgeData.initialDiscussions = [
    {
      id: 1,
      title: "Optimal O(N) Hash Map approach vs Sorting Two-Pointer for Pair Sum",
      authorName: "David Kim",
      authorInitials: "DK",
      authorRole: "Top Contributor",
      category: "Solutions",
      problemId: 1,
      problemTitle: "Pair Sum",
      content: "While sorting takes O(N log N) time and alters element indices, a single-pass hash map achieves optimal O(N) time and O(N) space. Store the complement `target - num` as you iterate.\n\nWatch out for edge cases with negative integers and when `complement == num`!",
      upvotes: 24,
      isUpvoted: false,
      repliesCount: 6,
      createdAt: "24 days ago",
      moderationStatus: "APPROVED",
      replies: [
        {
          id: 1,
          authorName: "Sarah Jenkins",
          authorInitials: "SJ",
          authorRole: "Learner",
          content: "Great write-up David! Did you measure the memory overhead of the Map in Java? Sometimes Primitive IntIntMap helps on huge inputs.",
          createdAt: "24 days ago"
        },
        {
          id: 2,
          authorName: "Priya Natarajan",
          authorInitials: "PN",
          authorRole: "Learner",
          content: "Sorting + Two pointers is still super valuable when the question asks to return the values instead of indices and space is constrained to O(1)!",
          createdAt: "24 days ago"
        },
        {
          id: 3,
          authorName: "Daniel Alves",
          authorInitials: "DA",
          authorRole: "Learner",
          content: "100% agreed. In a live interview, proposing brute force O(N^2) first, then optimizing to Hash Map O(N) shows clear progression.",
          createdAt: "24 days ago"
        },
        {
          id: 4,
          authorName: "Ethan Walker",
          authorInitials: "EW",
          authorRole: "Learner",
          content: "Don't forget to handle integer overflow if target and numbers can be close to Integer.MAX_VALUE in other languages.",
          createdAt: "24 days ago"
        },
        {
          id: 5,
          authorName: "Emma Novak",
          authorInitials: "EN",
          authorRole: "Learner",
          content: "Clean and straightforward explanation! The single pass check `seen.containsKey(complement)` before inserting the current element is key.",
          createdAt: "24 days ago"
        },
        {
          id: 6,
          authorName: "Zoe Carter",
          authorInitials: "ZC",
          authorRole: "Learner",
          content: "Bookmarked! This is the canonical problem for understanding Hash Map complement lookup patterns.",
          createdAt: "24 days ago"
        }
      ]
    },
    {
      id: 2,
      title: "How to properly shrink the sliding window without redundant scans?",
      authorName: "Sarah Jenkins",
      authorInitials: "SJ",
      authorRole: "Learner",
      category: "Questions",
      problemId: 3,
      problemTitle: "Longest Substring Without Repeating Characters",
      content: "When encountering a duplicate character in the sliding window, is it cleaner to advance the `left` pointer one-by-one in a while loop, or store character indices in a map and jump `left = max(left, lastSeenIndex + 1)` directly?",
      upvotes: 18,
      isUpvoted: false,
      repliesCount: 6,
      createdAt: "23 days ago",
      moderationStatus: "APPROVED",
      replies: [
        {
          id: 7,
          authorName: "David Kim",
          authorInitials: "DK",
          authorRole: "Top Contributor",
          content: "Storing the last seen index is strictly fewer iterations! Just remember `left = max(left, lastSeen[c] + 1)` so `left` never moves backward.",
          createdAt: "23 days ago"
        },
        {
          id: 8,
          authorName: "Marcus Vance",
          authorInitials: "MV",
          authorRole: "Learner",
          content: "Both run in O(N) amortized time, but the index map approach avoids unnecessary while iterations. Much cleaner in production code.",
          createdAt: "23 days ago"
        },
        {
          id: 9,
          authorName: "Liam O'Connor",
          authorInitials: "LO",
          authorRole: "Learner",
          content: "In C++ and Java, using an `int[128]` array for ASCII characters instead of `HashMap` gives an enormous speed boost on LeetCode tests.",
          createdAt: "23 days ago"
        },
        {
          id: 10,
          authorName: "Hana Suzuki",
          authorInitials: "HS",
          authorRole: "Learner",
          content: "I used to forget the `Math.max(left, ...)` condition and spent 30 minutes debugging why the pointer moved left. Great question!",
          createdAt: "23 days ago"
        },
        {
          id: 11,
          authorName: "Noah Becker",
          authorInitials: "NB",
          authorRole: "Learner",
          content: "The while loop approach with a `HashSet` is often easier to explain verbally to an interviewer under pressure.",
          createdAt: "23 days ago"
        },
        {
          id: 12,
          authorName: "Ryan Bennett",
          authorInitials: "RB",
          authorRole: "Learner",
          content: "Both are valid, but interviewers love when you mention that the while loop visits each character at most twice (2N total operations).",
          createdAt: "23 days ago"
        }
      ]
    },
    {
      id: 3,
      title: "Avoid TLE in Python: collections.deque vs list.pop(0) in BFS traversals",
      authorName: "Marcus Vance",
      authorInitials: "MV",
      authorRole: "Learner",
      category: "Tips",
      problemId: null,
      problemTitle: null,
      content: "PSA for Python developers: `list.pop(0)` is strictly O(N) because it shifts all remaining elements in memory! In tree and graph BFS traversals, this easily causes Time Limit Exceeded.\n\nAlways use `from collections import deque` and `queue.popleft()` which runs in guaranteed O(1) time.",
      upvotes: 35,
      isUpvoted: false,
      repliesCount: 6,
      createdAt: "22 days ago",
      moderationStatus: "APPROVED",
      replies: [
        {
          id: 13,
          authorName: "Alex Morgan",
          authorInitials: "AM",
          authorRole: "Admin",
          content: "Excellent reminder Marcus. This is one of the top subtle mistakes candidates make in Python live coding rounds.",
          createdAt: "22 days ago"
        },
        {
          id: 14,
          authorName: "Priya Natarajan",
          authorInitials: "PN",
          authorRole: "Learner",
          content: "I learned this the hard way on Word Ladder! Switched to `deque` and runtime dropped from 1800ms to 95ms.",
          createdAt: "22 days ago"
        },
        {
          id: 15,
          authorName: "Mia Thompson",
          authorInitials: "MT",
          authorRole: "Learner",
          content: "Same concept in JavaScript: `Array.prototype.shift()` is O(N). For large queues, use a pointer index or a custom ring buffer.",
          createdAt: "22 days ago"
        },
        {
          id: 16,
          authorName: "Omar Haddad",
          authorInitials: "OH",
          authorRole: "Learner",
          content: "In Java, also avoid `java.util.Vector` and `Stack`. Prefer `ArrayDeque` as it is non-synchronized and faster than `LinkedList`.",
          createdAt: "22 days ago"
        },
        {
          id: 17,
          authorName: "Chloe Martin",
          authorInitials: "CM",
          authorRole: "Learner",
          content: "Thanks for sharing! Adding this tip to my Python interview cheat sheet.",
          createdAt: "22 days ago"
        },
        {
          id: 18,
          authorName: "Lucas Ferreira",
          authorInitials: "LF",
          authorRole: "Learner",
          content: "A great follow-up is explaining why `deque` uses a doubly linked list of fixed-size memory blocks under the hood in CPython.",
          createdAt: "22 days ago"
        }
      ]
    },
    {
      id: 4,
      title: "FAANG Onsite Interview Debrief: Top 5 Patterns asked in 2026",
      authorName: "Priya Natarajan",
      authorInitials: "PN",
      authorRole: "Learner",
      category: "Interview Prep",
      problemId: null,
      problemTitle: null,
      content: "Just completed onsite loops at Google and Amazon. Over 5 technical rounds, 80% of algorithmic questions boiled down to these 5 archetypes:\n1. Sliding Window (dynamic & fixed size)\n2. Monotonic Stack (Next Greater Element)\n3. Top-K using Min/Max Heap\n4. Tree DFS with bottom-up path aggregations\n5. Topological Sort (Kahn's BFS algorithm)\n\nMastering these 5 patterns gave me immense confidence!",
      upvotes: 42,
      isUpvoted: false,
      repliesCount: 6,
      createdAt: "21 days ago",
      moderationStatus: "APPROVED",
      replies: [
        {
          id: 19,
          authorName: "David Kim",
          authorInitials: "DK",
          authorRole: "Top Contributor",
          content: "Huge congratulations Priya! Did any round ask follow-up questions about distributed scaling or memory limits?",
          createdAt: "21 days ago"
        },
        {
          id: 20,
          authorName: "Sarah Jenkins",
          authorInitials: "SJ",
          authorRole: "Learner",
          content: "Thanks for the summary! Monotonic stack has been popping up so frequently recently in stock and histogram problems.",
          createdAt: "21 days ago"
        },
        {
          id: 21,
          authorName: "Daniel Alves",
          authorInitials: "DA",
          authorRole: "Learner",
          content: "Did you practice mostly LeetCode Mediums or Hard? How much did behavioral (LP / Googleyness) weigh into the loops?",
          createdAt: "21 days ago"
        },
        {
          id: 22,
          authorName: "Isabella Rossi",
          authorInitials: "IR",
          authorRole: "Learner",
          content: "Super encouraging post! Did you write tests on the whiteboard / Google Doc or did the interviewers provide test runners?",
          createdAt: "21 days ago"
        },
        {
          id: 23,
          authorName: "Grace Lee",
          authorInitials: "GL",
          authorRole: "Learner",
          content: "Topological sort is definitely an Amazon favorite for task scheduler and package dependency problems.",
          createdAt: "21 days ago"
        },
        {
          id: 24,
          authorName: "Sofia Petrova",
          authorInitials: "SP",
          authorRole: "Learner",
          content: "Bookmarked! Focusing on patterns rather than memorizing individual problems is 100% the right strategy.",
          createdAt: "21 days ago"
        }
      ]
    },
    {
      id: 5,
      title: "Why Greedy Two-Pointer correctly guarantees Maximum Area in Container With Most Water",
      authorName: "Liam O'Connor",
      authorInitials: "LO",
      authorRole: "Learner",
      category: "Solutions",
      problemId: 11,
      problemTitle: "Container With Most Water",
      content: "A common doubt: Why are we sure that moving the pointer at the shorter height will never miss a better solution with an inner line?\n\nMathematical proof: Area is bounded by `min(height[l], height[r]) * (r - l)`. If we moved the taller pointer inwards, width decreases AND height can never exceed the shorter pointer. Thus area is strictly smaller! Moving the shorter line is the ONLY move that could possibly increase area.",
      upvotes: 29,
      isUpvoted: false,
      repliesCount: 6,
      createdAt: "20 days ago",
      moderationStatus: "APPROVED",
      replies: [
        {
          id: 25,
          authorName: "Marcus Vance",
          authorInitials: "MV",
          authorRole: "Learner",
          content: "This mathematical explanation is the clearest I've read. Explaining this proof in an interview shows true mastery.",
          createdAt: "20 days ago"
        },
        {
          id: 26,
          authorName: "Mia Thompson",
          authorInitials: "MT",
          authorRole: "Learner",
          content: "Brilliant deduction! The proof by contradiction makes it crystal clear why no potential maximal area is skipped.",
          createdAt: "20 days ago"
        },
        {
          id: 27,
          authorName: "Ethan Walker",
          authorInitials: "EW",
          authorRole: "Learner",
          content: "I love problems where the greedy choice has an intuitive mathematical invariant. O(N) time with O(1) space.",
          createdAt: "20 days ago"
        },
        {
          id: 28,
          authorName: "Noah Becker",
          authorInitials: "NB",
          authorRole: "Learner",
          content: "Do you have recommendations for similar problems that rely on eliminating sub-optimal search spaces with two pointers?",
          createdAt: "20 days ago"
        },
        {
          id: 29,
          authorName: "Emma Novak",
          authorInitials: "EN",
          authorRole: "Learner",
          content: "Trapping Rain Water uses a very similar leftMax / rightMax invariant! Highly recommend trying that next.",
          createdAt: "20 days ago"
        },
        {
          id: 30,
          authorName: "Aiden Murphy",
          authorInitials: "AM",
          authorRole: "Learner",
          content: "Crisp and concise. This helped me finally understand why we discard the shorter line.",
          createdAt: "20 days ago"
        }
      ]
    },
    {
      id: 6,
      title: "Best strategy to avoid duplicate triplets in 3Sum without using an expensive Set?",
      authorName: "Mia Thompson",
      authorInitials: "MT",
      authorRole: "Learner",
      category: "Questions",
      problemId: 15,
      problemTitle: "3Sum",
      content: "I keep generating duplicate triplets in 3Sum when the input contains multiple identical numbers. What is the cleanest condition to skip duplicates for both the outer anchor index and the two inner two-pointer indices after finding a valid sum?",
      upvotes: 16,
      isUpvoted: false,
      repliesCount: 6,
      createdAt: "19 days ago",
      moderationStatus: "APPROVED",
      replies: [
        {
          id: 31,
          authorName: "Sarah Jenkins",
          authorInitials: "SJ",
          authorRole: "Learner",
          content: "After sorting, for outer loop: `if (i > 0 && nums[i] == nums[i-1]) continue;`. That prevents using the same anchor twice!",
          createdAt: "19 days ago"
        },
        {
          id: 32,
          authorName: "Priya Natarajan",
          authorInitials: "PN",
          authorRole: "Learner",
          content: "For inner pointers, once `nums[l] + nums[r] == -nums[i]`: increment `l` while `nums[l] == nums[l-1]` and decrement `r` while `nums[r] == nums[r+1]`.",
          createdAt: "19 days ago"
        },
        {
          id: 33,
          authorName: "Hana Suzuki",
          authorInitials: "HS",
          authorRole: "Learner",
          content: "Crucial point: always compare with `i-1` and NOT `i+1`, otherwise you would skip valid triplets like `[-1, -1, 2]`!",
          createdAt: "19 days ago"
        },
        {
          id: 34,
          authorName: "Chloe Martin",
          authorInitials: "CM",
          authorRole: "Learner",
          content: "Sorting takes O(N log N) which is dominated by the O(N^2) two-pointer search, so sorting is basically free here.",
          createdAt: "19 days ago"
        },
        {
          id: 35,
          authorName: "Ryan Bennett",
          authorInitials: "RB",
          authorRole: "Learner",
          content: "Using a `Set<List<Integer>>` causes huge memory allocations and can trigger TLE. The pointer skip technique is strictly better.",
          createdAt: "19 days ago"
        },
        {
          id: 36,
          authorName: "Zoe Carter",
          authorInitials: "ZC",
          authorRole: "Learner",
          content: "Thanks everyone! This exact question tripped me up in a phone screen last week.",
          createdAt: "19 days ago"
        }
      ]
    },
    {
      id: 7,
      title: "Clean Stack solution in Java using modern Switch Expressions and Deque",
      authorName: "Daniel Alves",
      authorInitials: "DA",
      authorRole: "Learner",
      category: "Solutions",
      problemId: 20,
      problemTitle: "Valid Parentheses",
      content: "A neat trick for Valid Parentheses: whenever you see an opening bracket `(`, `[`, `{`, push the corresponding closing bracket `)`, `]`, `}` onto the stack!\n\nThen when you encounter a closing character, you only need to check `if (stack.isEmpty() || stack.pop() != c) return false;`. No complicated lookup map needed!",
      upvotes: 31,
      isUpvoted: false,
      repliesCount: 6,
      createdAt: "18 days ago",
      moderationStatus: "APPROVED",
      replies: [
        {
          id: 37,
          authorName: "David Kim",
          authorInitials: "DK",
          authorRole: "Top Contributor",
          content: "That trick is so elegant! Pushing the expected closing bracket reduces the matching condition to a single equality check.",
          createdAt: "18 days ago"
        },
        {
          id: 38,
          authorName: "Liam O'Connor",
          authorInitials: "LO",
          authorRole: "Learner",
          content: "And at the end, don't forget `return stack.isEmpty();` to catch leftover opening brackets like `(((`.",
          createdAt: "18 days ago"
        },
        {
          id: 39,
          authorName: "Omar Haddad",
          authorInitials: "OH",
          authorRole: "Learner",
          content: "In Java, always use `Deque<Character> stack = new ArrayDeque<>();`. Never use `java.util.Stack` because of synchronized lock overhead.",
          createdAt: "18 days ago"
        },
        {
          id: 40,
          authorName: "Grace Lee",
          authorInitials: "GL",
          authorRole: "Learner",
          content: "In C++, `std::string` can act as a stack with `push_back` and `pop_back` to avoid heap allocations completely!",
          createdAt: "18 days ago"
        },
        {
          id: 41,
          authorName: "Lucas Ferreira",
          authorInitials: "LF",
          authorRole: "Learner",
          content: "What is the time complexity if string length is odd? Quick early return: `if (s.length() % 2 != 0) return false;`!",
          createdAt: "18 days ago"
        },
        {
          id: 42,
          authorName: "Sofia Petrova",
          authorInitials: "SP",
          authorRole: "Learner",
          content: "Loved the early exit check for odd lengths. Saves unnecessary stack operations on invalid test cases.",
          createdAt: "18 days ago"
        }
      ]
    },
    {
      id: 8,
      title: "System Design for Backend Engineers: Transitioning from DSA to Architecture",
      authorName: "Hana Suzuki",
      authorInitials: "HS",
      authorRole: "Learner",
      category: "Interview Prep",
      problemId: null,
      problemTitle: null,
      content: "Once you feel comfortable with LeetCode Medium algorithms, how do you structure your System Design prep? Which resources helped you understand caching layers (Redis), DB partitioning/sharding, message brokers (Kafka/RabbitMQ), and CAP theorem tradeoffs most intuitively?",
      upvotes: 38,
      isUpvoted: false,
      repliesCount: 6,
      createdAt: "17 days ago",
      moderationStatus: "APPROVED",
      replies: [
        {
          id: 43,
          authorName: "Alex Morgan",
          authorInitials: "AM",
          authorRole: "Admin",
          content: "Start with Alex Xu's System Design Interview Volume 1 & 2. Practice back-of-the-envelope calculations for QPS and storage.",
          createdAt: "17 days ago"
        },
        {
          id: 44,
          authorName: "Marcus Vance",
          authorInitials: "MV",
          authorRole: "Learner",
          content: "Martin Kleppmann's 'Designing Data-Intensive Applications' (DDIA) is the gold standard for deep dive into storage engines and replication.",
          createdAt: "17 days ago"
        },
        {
          id: 45,
          authorName: "Mia Thompson",
          authorInitials: "MT",
          authorRole: "Learner",
          content: "Focus on real-world scenarios: URL shortener (hashing/collisions), Rate limiter (Token bucket/Redis), and Chat service (WebSockets).",
          createdAt: "17 days ago"
        },
        {
          id: 46,
          authorName: "Isabella Rossi",
          authorInitials: "IR",
          authorRole: "Learner",
          content: "Don't just memorize designs. Learn the trade-offs: Why choose NoSQL vs SQL? What happens during network partitions?",
          createdAt: "17 days ago"
        },
        {
          id: 47,
          authorName: "Noah Becker",
          authorInitials: "NB",
          authorRole: "Learner",
          content: "Mock interviews with peers are critical. In 45 minutes you have to drive the conversation from requirements to high-level to deep-dive.",
          createdAt: "17 days ago"
        },
        {
          id: 48,
          authorName: "Ryan Bennett",
          authorInitials: "RB",
          authorRole: "Learner",
          content: "Always ask clarifying questions in the first 5 minutes: Read-heavy vs Write-heavy ratio, Latency SLA, and Consistency requirements.",
          createdAt: "17 days ago"
        }
      ]
    },
    {
      id: 9,
      title: "Bit Manipulation Tricks that frequently show up in coding interviews",
      authorName: "Omar Haddad",
      authorInitials: "OH",
      authorRole: "Learner",
      category: "Tips",
      problemId: null,
      problemTitle: null,
      content: "Quick bit manipulation reference:\n- `n & (n - 1)` : Clears the lowest set bit (Brian Kernighan's algorithm for counting bits in O(k) steps)\n- `n & -n` : Isolates the lowest set bit (due to two's complement arithmetic)\n- `x ^ y` : Finds bits that differ\n- `x ^ x == 0` and `x ^ 0 == x` : Finds the single non-duplicate number in an array\n\nThese turn O(32) iterations into instant O(1) bitwise operations!",
      upvotes: 27,
      isUpvoted: false,
      repliesCount: 6,
      createdAt: "16 days ago",
      moderationStatus: "APPROVED",
      replies: [
        {
          id: 49,
          authorName: "Priya Natarajan",
          authorInitials: "PN",
          authorRole: "Learner",
          content: "Brian Kernighan's trick `n &= (n - 1)` is pure magic. Used it to solve Counting Bits in optimal O(N) time.",
          createdAt: "16 days ago"
        },
        {
          id: 50,
          authorName: "Daniel Alves",
          authorInitials: "DA",
          authorRole: "Learner",
          content: "Also `(n & (n - 1)) == 0 && n > 0` checks if a number is a power of two in exactly one line of code!",
          createdAt: "16 days ago"
        },
        {
          id: 51,
          authorName: "Ethan Walker",
          authorInitials: "EW",
          authorRole: "Learner",
          content: "Two's complement `-n = ~n + 1` is why `n & -n` works so cleanly to extract the rightmost 1-bit. Great refresher.",
          createdAt: "16 days ago"
        },
        {
          id: 52,
          authorName: "Lucas Ferreira",
          authorInitials: "LF",
          authorRole: "Learner",
          content: "Single Number II (where every element appears 3 times except one) is an awesome follow-up using bit counters.",
          createdAt: "16 days ago"
        },
        {
          id: 53,
          authorName: "Emma Novak",
          authorInitials: "EN",
          authorRole: "Learner",
          content: "Watch out for operator precedence in C++ and Java! Bitwise operators like `&` have lower precedence than `==`.",
          createdAt: "16 days ago"
        },
        {
          id: 54,
          authorName: "Aiden Murphy",
          authorInitials: "AM",
          authorRole: "Learner",
          content: "Always wrap bitwise expressions in parentheses: `(n & 1) == 0`. Saved me so many compiler bugs!",
          createdAt: "16 days ago"
        }
      ]
    },
    {
      id: 10,
      title: "Intuitive logic to decide which half is sorted in Rotated Binary Search",
      authorName: "Isabella Rossi",
      authorInitials: "IR",
      authorRole: "Learner",
      category: "Solutions",
      problemId: 33,
      problemTitle: "Search in Rotated Sorted Array",
      content: "The key insight for Search in Rotated Sorted Array:\nAt least one half is ALWAYS sorted!\n- If `nums[left] <= nums[mid]`: The left half is sorted.\n  - Target is in left half if `nums[left] <= target < nums[mid]`.\n  - Otherwise target must be in right half.\n- Else: The right half is sorted.\n  - Target is in right half if `nums[mid] < target <= nums[right]`.\n  - Otherwise target must be in left half.\n\nStrictly O(log N) runtime with O(1) space.",
      upvotes: 25,
      isUpvoted: false,
      repliesCount: 6,
      createdAt: "15 days ago",
      moderationStatus: "APPROVED",
      replies: [
        {
          id: 55,
          authorName: "David Kim",
          authorInitials: "DK",
          authorRole: "Top Contributor",
          content: "Breaking it down by 'which half is guaranteed sorted' makes this problem so much easier than trying to locate the pivot first.",
          createdAt: "15 days ago"
        },
        {
          id: 56,
          authorName: "Liam O'Connor",
          authorInitials: "LO",
          authorRole: "Learner",
          content: "Be careful with duplicate elements (Search in Rotated Sorted Array II) where `nums[left] == nums[mid] == nums[right]` degrades to O(N).",
          createdAt: "15 days ago"
        },
        {
          id: 57,
          authorName: "Hana Suzuki",
          authorInitials: "HS",
          authorRole: "Learner",
          content: "The `<=` condition vs `<` is where most people get off-by-one errors. Checking the closed boundary on `left` or `right` is essential.",
          createdAt: "15 days ago"
        },
        {
          id: 58,
          authorName: "Chloe Martin",
          authorInitials: "CM",
          authorRole: "Learner",
          content: "I used to find the min index first with binary search and then do a second binary search. This one-pass method is twice as clean!",
          createdAt: "15 days ago"
        },
        {
          id: 59,
          authorName: "Grace Lee",
          authorInitials: "GL",
          authorRole: "Learner",
          content: "Can confirm, this exact question was my first round technical screening at Microsoft. Nailed it thanks to this pattern!",
          createdAt: "15 days ago"
        },
        {
          id: 60,
          authorName: "Zoe Carter",
          authorInitials: "ZC",
          authorRole: "Learner",
          content: "Saved! Clean logic that eliminates messy edge cases.",
          createdAt: "15 days ago"
        }
      ]
    },
    {
      id: 11,
      title: "Two Pointers vs Monotonic Stack for Trapping Rain Water: Which is preferred?",
      authorName: "Ethan Walker",
      authorInitials: "EW",
      authorRole: "Learner",
      category: "Questions",
      problemId: 42,
      problemTitle: "Trapping Rain Water",
      content: "I solved Trapping Rain Water using a Monotonic Decreasing Stack in O(N) time and O(N) space. But interviewers often ask to optimize to O(1) space using two pointers (`leftMax` and `rightMax`).\n\nHow do you explain the transition between both techniques smoothly in a live interview?",
      upvotes: 22,
      isUpvoted: false,
      repliesCount: 6,
      createdAt: "14 days ago",
      moderationStatus: "APPROVED",
      replies: [
        {
          id: 61,
          authorName: "Sarah Jenkins",
          authorInitials: "SJ",
          authorRole: "Learner",
          content: "Start by explaining the stack horizontal-layering approach: stack computes water by horizontal bars bounded by left and right walls.",
          createdAt: "14 days ago"
        },
        {
          id: 62,
          authorName: "Marcus Vance",
          authorInitials: "MV",
          authorRole: "Learner",
          content: "Then explain: 'We can also calculate water vertically column-by-column: water[i] = min(maxL, maxR) - height[i]'. That naturally motivates two pointers!",
          createdAt: "14 days ago"
        },
        {
          id: 63,
          authorName: "Mia Thompson",
          authorInitials: "MT",
          authorRole: "Learner",
          content: "Because we only need `min(maxL, maxR)`, whichever side has the smaller max determines the water level regardless of the other side!",
          createdAt: "14 days ago"
        },
        {
          id: 64,
          authorName: "Omar Haddad",
          authorInitials: "OH",
          authorRole: "Learner",
          content: "That insight: 'If leftMax < rightMax, the water at left is determined solely by leftMax' is the eureka moment interviewers look for.",
          createdAt: "14 days ago"
        },
        {
          id: 65,
          authorName: "Noah Becker",
          authorInitials: "NB",
          authorRole: "Learner",
          content: "The two-pointer solution is only 15 lines of code and O(1) extra space. Unbeatable.",
          createdAt: "14 days ago"
        },
        {
          id: 66,
          authorName: "Sofia Petrova",
          authorInitials: "SP",
          authorRole: "Learner",
          content: "Practicing both is great because Monotonic Stack generalizes better to Largest Rectangle in Histogram.",
          createdAt: "14 days ago"
        }
      ]
    },
    {
      id: 12,
      title: "Kadane's Algorithm: Understanding dynamic state transition vs Divide & Conquer",
      authorName: "Chloe Martin",
      authorInitials: "CM",
      authorRole: "Learner",
      category: "Solutions",
      problemId: 53,
      problemTitle: "Maximum Subarray",
      content: "Kadane's algorithm is the pinnacle of elegant 1D Dynamic Programming:\n`currentMax = max(nums[i], currentMax + nums[i])`\nAt each index, you make a binary choice: either extend the running contiguous subarray, or abandon it and start fresh from `nums[i]`.\n\nFollow-up question: How do you return the actual starting and ending indices of the maximal subarray?",
      upvotes: 33,
      isUpvoted: false,
      repliesCount: 6,
      createdAt: "13 days ago",
      moderationStatus: "APPROVED",
      replies: [
        {
          id: 67,
          authorName: "Priya Natarajan",
          authorInitials: "PN",
          authorRole: "Learner",
          content: "To track indices: keep `start = 0`, `end = 0`, `tempStart = 0`. Whenever `currentMax < 0`, set `tempStart = i + 1`!",
          createdAt: "13 days ago"
        },
        {
          id: 68,
          authorName: "Daniel Alves",
          authorInitials: "DA",
          authorRole: "Learner",
          content: "When `currentMax + nums[i] > globalMax`, update `globalMax`, `start = tempStart`, and `end = i`.",
          createdAt: "13 days ago"
        },
        {
          id: 69,
          authorName: "Isabella Rossi",
          authorInitials: "IR",
          authorRole: "Learner",
          content: "The Divide & Conquer O(N log N) approach using Segment Tree concepts is also a frequent follow-up for streaming/range query updates.",
          createdAt: "13 days ago"
        },
        {
          id: 70,
          authorName: "Lucas Ferreira",
          authorInitials: "LF",
          authorRole: "Learner",
          content: "What if all numbers in the array are negative? Kadane's handles this seamlessly by picking the single least-negative number.",
          createdAt: "13 days ago"
        },
        {
          id: 71,
          authorName: "Ryan Bennett",
          authorInitials: "RB",
          authorRole: "Learner",
          content: "This is often asked as the base problem before Maximum Product Subarray (which requires tracking both min and max due to negative signs).",
          createdAt: "13 days ago"
        },
        {
          id: 72,
          authorName: "Aiden Murphy",
          authorInitials: "AM",
          authorRole: "Learner",
          content: "Short, intuitive, and optimal O(N) time with O(1) auxiliary space.",
          createdAt: "13 days ago"
        }
      ]
    },
    {
      id: 13,
      title: "Handling Recursion Stack Overflow in Deep Trees and Graphs",
      authorName: "Noah Becker",
      authorInitials: "NB",
      authorRole: "Learner",
      category: "Tips",
      problemId: null,
      problemTitle: null,
      content: "When traversing deep graphs or skewed trees with depth N >= 10^5, standard recursive DFS will trigger `StackOverflowError` in Java/C++ or exceed `sys.getrecursionlimit()` in Python.\n\nKey mitigation strategies:\n1. Convert recursion to an iterative loop with an explicit `ArrayDeque` allocated on the heap.\n2. In Python, `sys.setrecursionlimit(200000)` works for competitions but is discouraged in production.\n3. Use BFS or Morris Traversal for O(1) space tree walks.",
      upvotes: 20,
      isUpvoted: false,
      repliesCount: 6,
      createdAt: "12 days ago",
      moderationStatus: "APPROVED",
      replies: [
        {
          id: 73,
          authorName: "Alex Morgan",
          authorInitials: "AM",
          authorRole: "Admin",
          content: "Spot on Noah. In production systems, heap space is gigabytes while stack space is typically just 1-2 megabytes.",
          createdAt: "12 days ago"
        },
        {
          id: 74,
          authorName: "Liam O'Connor",
          authorInitials: "LO",
          authorRole: "Learner",
          content: "Morris Traversal is mind-blowing when you first see it. Re-wiring temporary right child pointers to predecessors achieves O(1) space!",
          createdAt: "12 days ago"
        },
        {
          id: 75,
          authorName: "Hana Suzuki",
          authorInitials: "HS",
          authorRole: "Learner",
          content: "For graph DFS with cycles, iterative DFS also makes it easier to track visited state and path back-tracing.",
          createdAt: "12 days ago"
        },
        {
          id: 76,
          authorName: "Ethan Walker",
          authorInitials: "EW",
          authorRole: "Learner",
          content: "Iterative tree traversal with simulated call-stack is a favorite Google interview follow-up.",
          createdAt: "12 days ago"
        },
        {
          id: 77,
          authorName: "Emma Novak",
          authorInitials: "EN",
          authorRole: "Learner",
          content: "Good reminder to never rely on `sys.setrecursionlimit` during interviews unless explicitly permitted by the interviewer.",
          createdAt: "12 days ago"
        },
        {
          id: 78,
          authorName: "Zoe Carter",
          authorInitials: "ZC",
          authorRole: "Learner",
          content: "Always mention stack depth when discussing the space complexity of recursive DFS: O(H) where H can be O(N) in the worst case.",
          createdAt: "12 days ago"
        }
      ]
    },
    {
      id: 14,
      title: "Mock Interview Checklist: What interviewers silently evaluate during the 45 mins",
      authorName: "Grace Lee",
      authorInitials: "GL",
      authorRole: "Learner",
      category: "Interview Prep",
      problemId: null,
      problemTitle: null,
      content: "A rubric based on shadowing senior FAANG interviewers:\n1. **Clarification (3-5 mins)**: Do you check empty inputs, duplicates, range of values, and mutable inputs?\n2. **Communication (ongoing)**: Never stay silent for >30 seconds. Think out loud.\n3. **Brute Force first (5 mins)**: State the naive approach and its complexity to set a baseline.\n4. **Optimization & Dry Run**: Manually trace a sample input through your logic before writing code.\n5. **Clean Code**: Meaningful variable names, modular helpers, and zero syntax flubs.\n6. **Complexity**: Stating time AND space complexity accurately without waiting to be prompted.",
      upvotes: 45,
      isUpvoted: false,
      repliesCount: 6,
      createdAt: "11 days ago",
      moderationStatus: "APPROVED",
      replies: [
        {
          id: 79,
          authorName: "David Kim",
          authorInitials: "DK",
          authorRole: "Top Contributor",
          content: "Point #4 is massive. Tracing your algorithm with a small input on whiteboard catches 90% of off-by-one errors before running.",
          createdAt: "11 days ago"
        },
        {
          id: 80,
          authorName: "Marcus Vance",
          authorInitials: "MV",
          authorRole: "Learner",
          content: "Agree on point #3! Jumping straight to complex code without agreeing on the approach with the interviewer is the #1 rejection reason.",
          createdAt: "11 days ago"
        },
        {
          id: 81,
          authorName: "Mia Thompson",
          authorInitials: "MT",
          authorRole: "Learner",
          content: "Also remember: interviewers want to see how you respond to hints. Treat hints as collaborative nudges, not failures.",
          createdAt: "11 days ago"
        },
        {
          id: 82,
          authorName: "Omar Haddad",
          authorInitials: "OH",
          authorRole: "Learner",
          content: "Great checklist Grace! Printing this out and reviewing before every mock interview session.",
          createdAt: "11 days ago"
        },
        {
          id: 83,
          authorName: "Chloe Martin",
          authorInitials: "CM",
          authorRole: "Learner",
          content: "Time management is everything. If you are not writing code by minute 20, you will likely run out of time to test.",
          createdAt: "11 days ago"
        },
        {
          id: 84,
          authorName: "Sofia Petrova",
          authorInitials: "SP",
          authorRole: "Learner",
          content: "Don't forget to test edge cases at the end: null/empty array, single element, all duplicates, and negative numbers.",
          createdAt: "11 days ago"
        }
      ]
    },
    {
      id: 15,
      title: "Single pass O(N) time and O(1) space tracking min price so far in Stock Buy/Sell",
      authorName: "Lucas Ferreira",
      authorInitials: "LF",
      authorRole: "Learner",
      category: "Solutions",
      problemId: 121,
      problemTitle: "Best Time to Buy and Sell Stock",
      content: "For Best Time to Buy and Sell Stock I:\nYou only need to maintain `minPrice = min(minPrice, price)` and calculate potential profit `maxProfit = max(maxProfit, price - minPrice)` as you iterate.\n\nThis simple problem forms the bedrock for:\n- Stock II: Infinite transactions (accumulate every positive slope `prices[i] - prices[i-1]`)\n- Stock with Cooldown / Fee: State machine DP with `buy` and `sell` states.",
      upvotes: 26,
      isUpvoted: false,
      repliesCount: 6,
      createdAt: "10 days ago",
      moderationStatus: "APPROVED",
      replies: [
        {
          id: 85,
          authorName: "Sarah Jenkins",
          authorInitials: "SJ",
          authorRole: "Learner",
          content: "The connection from Stock I greedy tracking to Stock with Cooldown state machine DP is such a great learning journey.",
          createdAt: "10 days ago"
        },
        {
          id: 86,
          authorName: "Priya Natarajan",
          authorInitials: "PN",
          authorRole: "Learner",
          content: "In Stock II, many candidates overcomplicate with DP when greedy slope addition is literally 3 lines of code!",
          createdAt: "10 days ago"
        },
        {
          id: 87,
          authorName: "Daniel Alves",
          authorInitials: "DA",
          authorRole: "Learner",
          content: "Another perspective: It is essentially finding the maximum subarray sum on the array of price differences!",
          createdAt: "10 days ago"
        },
        {
          id: 88,
          authorName: "Isabella Rossi",
          authorInitials: "IR",
          authorRole: "Learner",
          content: "Super clear summary Lucas. This is the ultimate beginner friendly problem to build dynamic tracking intuition.",
          createdAt: "10 days ago"
        },
        {
          id: 89,
          authorName: "Grace Lee",
          authorInitials: "GL",
          authorRole: "Learner",
          content: "Kadane's algorithm and Stock I are practically twin problems when viewed through running difference lenses.",
          createdAt: "10 days ago"
        },
        {
          id: 90,
          authorName: "Aiden Murphy",
          authorInitials: "AM",
          authorRole: "Learner",
          content: "Clean, concise, and optimal. Great stepping stone for DP series.",
          createdAt: "10 days ago"
        }
      ]
    },
    {
      id: 16,
      title: "Space optimization from O(N) array to two rolling variables in 1D DP",
      authorName: "Emma Novak",
      authorInitials: "EN",
      authorRole: "Learner",
      category: "Questions",
      problemId: 198,
      problemTitle: "House Robber",
      content: "In House Robber: `dp[i] = max(dp[i-1], dp[i-2] + nums[i])`.\nBecause `dp[i]` only looks back 2 steps, we can discard the entire array and keep just `prev1` and `prev2`, dropping auxiliary space from O(N) to O(1).\n\nDoes this rolling variable optimization apply universally whenever recurrences have a constant lookback window?",
      upvotes: 21,
      isUpvoted: false,
      repliesCount: 6,
      createdAt: "9 days ago",
      moderationStatus: "APPROVED",
      replies: [
        {
          id: 91,
          authorName: "David Kim",
          authorInitials: "DK",
          authorRole: "Top Contributor",
          content: "Yes! Exactly like Fibonacci numbers and Climbing Stairs. Any linear recurrence `f(n) = g(f(n-1), ..., f(n-k))` only needs `k` variables.",
          createdAt: "9 days ago"
        },
        {
          id: 92,
          authorName: "Liam O'Connor",
          authorInitials: "LO",
          authorRole: "Learner",
          content: "Even in 2D DP (like Unique Paths or Edit Distance), if row `i` only depends on row `i-1`, you can reduce space from O(M*N) to O(N) using two rows!",
          createdAt: "9 days ago"
        },
        {
          id: 93,
          authorName: "Hana Suzuki",
          authorInitials: "HS",
          authorRole: "Learner",
          content: "And if you update in the correct order (e.g. reverse iteration in 0/1 Knapsack), you can even reduce to a single 1D array!",
          createdAt: "9 days ago"
        },
        {
          id: 94,
          authorName: "Ethan Walker",
          authorInitials: "EW",
          authorRole: "Learner",
          content: "Interviewers always ask for space optimization after you write the 1D table. Showing the progression from O(N) to O(1) gets top marks.",
          createdAt: "9 days ago"
        },
        {
          id: 95,
          authorName: "Noah Becker",
          authorInitials: "NB",
          authorRole: "Learner",
          content: "Just be careful: if the problem asks to reconstruct the actual path of houses robbed, you must retain the full array or back-pointers.",
          createdAt: "9 days ago"
        },
        {
          id: 96,
          authorName: "Ryan Bennett",
          authorInitials: "RB",
          authorRole: "Learner",
          content: "House Robber II (circular houses) is a fantastic follow-up: run the linear solution twice, once for `[0..n-2]` and once for `[1..n-1]`!",
          createdAt: "9 days ago"
        }
      ]
    },
    {
      id: 17,
      title: "BFS vs DFS vs Disjoint Set Union (Union-Find) for grid connected components",
      authorName: "Ryan Bennett",
      authorInitials: "RB",
      authorRole: "Learner",
      category: "Solutions",
      problemId: 200,
      problemTitle: "Number of Islands",
      content: "Three approaches compared for Number of Islands:\n1. **DFS**: Fewest lines of code, modifies grid in-place (`grid[r][c] = '0'`), risk of recursion stack overflow on 1000x1000 grids.\n2. **BFS**: Iterative with queue, memory bounded by perimeter of island (min(M, N)), zero recursion risk.\n3. **Union-Find (DSU)**: Best for dynamic scenarios (e.g. Number of Islands II where land cells are added over time via a stream).",
      upvotes: 34,
      isUpvoted: false,
      repliesCount: 6,
      createdAt: "8 days ago",
      moderationStatus: "APPROVED",
      replies: [
        {
          id: 97,
          authorName: "Marcus Vance",
          authorInitials: "MV",
          authorRole: "Learner",
          content: "Mentioning Union-Find as the optimal choice for the dynamic/streaming follow-up is an instant hire signal in interviews.",
          createdAt: "8 days ago"
        },
        {
          id: 98,
          authorName: "Mia Thompson",
          authorInitials: "MT",
          authorRole: "Learner",
          content: "For BFS, remember to mark the cell as visited *immediately upon pushing to queue*, not upon popping! Otherwise duplicate nodes flood the queue.",
          createdAt: "8 days ago"
        },
        {
          id: 99,
          authorName: "Omar Haddad",
          authorInitials: "OH",
          authorRole: "Learner",
          content: "That queue deduplication tip is huge! Marking on pop instead of push causes memory to explode to O(M*N) instead of O(min(M,N)).",
          createdAt: "8 days ago"
        },
        {
          id: 100,
          authorName: "Chloe Martin",
          authorInitials: "CM",
          authorRole: "Learner",
          content: "Also in-place modification of `grid` might not be allowed in production if input is immutable. Always ask the interviewer first.",
          createdAt: "8 days ago"
        },
        {
          id: 101,
          authorName: "Lucas Ferreira",
          authorInitials: "LF",
          authorRole: "Learner",
          content: "Union-Find with path compression and union by rank has nearly O(1) amortized operations via the inverse Ackermann function α(N).",
          createdAt: "8 days ago"
        },
        {
          id: 102,
          authorName: "Zoe Carter",
          authorInitials: "ZC",
          authorRole: "Learner",
          content: "Outstanding comparison Ryan. Covering DFS, BFS, and DSU tradeoffs shows broad algorithmic maturity.",
          createdAt: "8 days ago"
        }
      ]
    },
    {
      id: 18,
      title: "Iterative pointer reversal without losing node references in Reverse Linked List",
      authorName: "Sofia Petrova",
      authorInitials: "SP",
      authorRole: "Learner",
      category: "Tips",
      problemId: 206,
      problemTitle: "Reverse Linked List",
      content: "The foolproof 4-step pointer rotation for reversing a singly linked list:\n```java\nListNode nextTemp = curr.next; // 1. Save next\ncurr.next = prev;              // 2. Reverse link\nprev = curr;                   // 3. Step prev forward\ncurr = nextTemp;               // 4. Step curr forward\n```\nAlways return `prev` as the new head (since `curr` ends at null)!\nDrawing the 3 pointers on paper completely prevents null pointer errors.",
      upvotes: 28,
      isUpvoted: false,
      repliesCount: 6,
      createdAt: "7 days ago",
      moderationStatus: "APPROVED",
      replies: [
        {
          id: 103,
          authorName: "Sarah Jenkins",
          authorInitials: "SJ",
          authorRole: "Learner",
          content: "The 4-step rhythm: Save next -> Reverse -> Advance prev -> Advance curr. Once you memorize this rhythm, you never forget.",
          createdAt: "7 days ago"
        },
        {
          id: 104,
          authorName: "Priya Natarajan",
          authorInitials: "PN",
          authorRole: "Learner",
          content: "The recursive approach is also great practice: `ListNode newHead = reverseList(head.next); head.next.next = head; head.next = null;`.",
          createdAt: "7 days ago"
        },
        {
          id: 105,
          authorName: "Daniel Alves",
          authorInitials: "DA",
          authorRole: "Learner",
          content: "Recursive uses O(N) call stack space though, so iterative with O(1) space is always the industry standard.",
          createdAt: "7 days ago"
        },
        {
          id: 106,
          authorName: "Isabella Rossi",
          authorInitials: "IR",
          authorRole: "Learner",
          content: "Reverse Linked List II (reversing between index left and right) builds directly on this using a dummy head node.",
          createdAt: "7 days ago"
        },
        {
          id: 107,
          authorName: "Grace Lee",
          authorInitials: "GL",
          authorRole: "Learner",
          content: "Reverse Nodes in k-Group is the ultimate final boss for this pattern. Mastering this base technique is crucial.",
          createdAt: "7 days ago"
        },
        {
          id: 108,
          authorName: "Aiden Murphy",
          authorInitials: "AM",
          authorRole: "Learner",
          content: "Always draw the pointers! Visualizing `prev`, `curr`, and `nextTemp` makes linked list bugs impossible to hide.",
          createdAt: "7 days ago"
        }
      ]
    },
    {
      id: 19,
      title: "O(N) time without using the division operator via prefix and suffix products",
      authorName: "Aiden Murphy",
      authorInitials: "AM",
      authorRole: "Learner",
      category: "Solutions",
      problemId: 238,
      problemTitle: "Product of Array Except Self",
      content: "The challenge: Solve in O(N) time WITHOUT using the division operator `/`.\n\nOptimal two-pass approach:\n1. Initialize `ans` array where `ans[i]` contains the product of all elements to the left of `i`.\n2. Iterate backwards maintaining a running `rightProduct` variable, multiplying `ans[i] *= rightProduct`, and updating `rightProduct *= nums[i]`.\n\nResult: Exactly O(N) time and O(1) extra space (excluding output array)!",
      upvotes: 37,
      isUpvoted: false,
      repliesCount: 6,
      createdAt: "6 days ago",
      moderationStatus: "APPROVED",
      replies: [
        {
          id: 109,
          authorName: "David Kim",
          authorInitials: "DK",
          authorRole: "Top Contributor",
          content: "The right-to-left rolling accumulator is so smart because it avoids allocating a separate suffix product array.",
          createdAt: "6 days ago"
        },
        {
          id: 110,
          authorName: "Liam O'Connor",
          authorInitials: "LO",
          authorRole: "Learner",
          content: "What if division was allowed? Then you'd have to deal with zero elements: single zero vs multiple zeros. Much messier!",
          createdAt: "6 days ago"
        },
        {
          id: 111,
          authorName: "Hana Suzuki",
          authorInitials: "HS",
          authorRole: "Learner",
          content: "Yes! If there are two or more zeros, all elements become zero. The prefix/suffix product naturally handles zeros with zero special casing.",
          createdAt: "6 days ago"
        },
        {
          id: 112,
          authorName: "Ethan Walker",
          authorInitials: "EW",
          authorRole: "Learner",
          content: "This question tests whether you can break a composite computation into symmetric sub-problems: `prefix[i-1] * suffix[i+1]`.",
          createdAt: "6 days ago"
        },
        {
          id: 113,
          authorName: "Noah Becker",
          authorInitials: "NB",
          authorRole: "Learner",
          content: "One of the most frequently asked problems at Meta and Apple. Clean, deterministic, and no edge-case landmines.",
          createdAt: "6 days ago"
        },
        {
          id: 114,
          authorName: "Emma Novak",
          authorInitials: "EN",
          authorRole: "Learner",
          content: "Clear and elegant explanation Aiden. Bookmarking for interview revision!",
          createdAt: "6 days ago"
        }
      ]
    },
    {
      id: 20,
      title: "Top-down memoized recursion vs Bottom-up tabulation for Coin Change",
      authorName: "Zoe Carter",
      authorInitials: "ZC",
      authorRole: "Learner",
      category: "Questions",
      problemId: 322,
      problemTitle: "Coin Change",
      content: "In Coin Change (unbounded knapsack), what are the practical performance tradeoffs between top-down DP with memoization vs bottom-up tabulation?\n\nWhy does bottom-up array iteration frequently benchmark 2-3x faster than memoized recursion in Java and C++?",
      upvotes: 23,
      isUpvoted: false,
      repliesCount: 6,
      createdAt: "5 days ago",
      moderationStatus: "APPROVED",
      replies: [
        {
          id: 115,
          authorName: "Alex Morgan",
          authorInitials: "AM",
          authorRole: "Admin",
          content: "Bottom-up avoids function call overhead (call frame setup/teardown) and benefits heavily from CPU cache locality (sequential memory access).",
          createdAt: "5 days ago"
        },
        {
          id: 116,
          authorName: "Marcus Vance",
          authorInitials: "MV",
          authorRole: "Learner",
          content: "In top-down, hash map lookup or jumping around non-contiguous array indices causes frequent CPU L1/L2 cache misses.",
          createdAt: "5 days ago"
        },
        {
          id: 117,
          authorName: "Mia Thompson",
          authorInitials: "MT",
          authorRole: "Learner",
          content: "Bottom-up also prevents recursion stack overflow on large target amounts (e.g. amount = 10000 with coin = 1).",
          createdAt: "5 days ago"
        },
        {
          id: 118,
          authorName: "Omar Haddad",
          authorInitials: "OH",
          authorRole: "Learner",
          content: "Top-down has one advantage though: it only visits reachable subproblems. If many amounts are unreachable, top-down computes fewer states.",
          createdAt: "5 days ago"
        },
        {
          id: 119,
          authorName: "Chloe Martin",
          authorInitials: "CM",
          authorRole: "Learner",
          content: "For Coin Change, initializing `dp` array with `amount + 1` (a sentinel value larger than any valid answer) makes the min check super clean.",
          createdAt: "5 days ago"
        },
        {
          id: 120,
          authorName: "Ryan Bennett",
          authorInitials: "RB",
          authorRole: "Learner",
          content: "Great discussion Zoe! Starting with top-down to discover the recurrence and then converting to bottom-up is the textbook workflow.",
          createdAt: "5 days ago"
        }
      ]
    }
  ];
})();
