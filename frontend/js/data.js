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
      id: "post-1",
      title: "[Walkthrough] Clean O(N) Python & Java solutions for Pair Sum with step-by-step visual diagrams",
      authorName: "Alex Morgan",
      authorInitials: "AM",
      authorRole: "Admin",
      category: "Solutions",
      problemId: 104,
      problemTitle: "Pair Sum",
      content: "Hey everyone! While solving Pair Sum, the most common trap is sorting the array first (which ruins original indices). Using a single-pass hash map keeps time complexity strictly at O(N) while maintaining indices effortlessly. Here is the annotated solution and complexity analysis.",
      upvotes: 48,
      isUpvoted: false,
      repliesCount: 3,
      createdAt: "3 hours ago",
      replies: [
        {
          id: "rep-1-1",
          authorName: "David Kim",
          authorInitials: "DK",
          authorRole: "Top Contributor",
          content: "Great breakdown! One subtle edge case to watch out for is duplicate values: the complement lookup must check `target - num` before inserting the current index to avoid self-pairing when `target == 2 * num`.",
          createdAt: "2 hours ago",
          upvotes: 8
        },
        {
          id: "rep-1-2",
          authorName: "Sarah Jenkins",
          authorInitials: "SJ",
          authorRole: "Learner",
          content: "The single-pass hash map is so much cleaner than my initial nested loops. Reduced runtime from 1450ms down to 32ms!",
          createdAt: "1 hour ago",
          upvotes: 4
        },
        {
          id: "rep-1-3",
          authorName: "Marcus Vance",
          authorInitials: "MV",
          authorRole: "Learner",
          content: "Thanks Alex! Does this general approach work when the problem asks for all pairs without duplicates?",
          createdAt: "30 mins ago",
          upvotes: 2
        }
      ]
    },
    {
      id: "post-2",
      title: "How do you systematically identify when to use Sliding Window vs Two Pointers?",
      authorName: "Sarah Jenkins",
      authorInitials: "SJ",
      authorRole: "Learner",
      category: "Questions",
      problemId: null,
      problemTitle: null,
      content: "I often get confused during timed contests on whether a problem requires two pointers moving towards each other (like 3Sum / Container with Most Water) or a sliding window moving in the same direction. What mental models or problem keywords do you look for?",
      upvotes: 36,
      isUpvoted: false,
      repliesCount: 2,
      createdAt: "Yesterday",
      replies: [
        {
          id: "rep-2-1",
          authorName: "Alex Morgan",
          authorInitials: "AM",
          authorRole: "Admin",
          content: "Rule of thumb: If the problem asks for a contiguous subarray/substring with some condition (max length, min window, at most k distinct elements), it's almost always a Sliding Window. If the array is sorted and you need pairs or triplets satisfying a sum or area constraint, use Two Pointers from opposite ends.",
          createdAt: "Yesterday",
          upvotes: 19
        },
        {
          id: "rep-2-2",
          authorName: "David Kim",
          authorInitials: "DK",
          authorRole: "Top Contributor",
          content: "Also check the monotonicity property! Sliding window works when expanding the right pointer only increases (or preserves) the condition, and shrinking the left pointer monotonically decreases it.",
          createdAt: "Yesterday",
          upvotes: 11
        }
      ]
    },
    {
      id: "post-3",
      title: "Google L4 Technical Interview Experience — 3 Rounds Algorithms (Tree DFS & Graph BFS)",
      authorName: "David Kim",
      authorInitials: "DK",
      authorRole: "Top Contributor",
      category: "Interview Prep",
      problemId: null,
      problemTitle: null,
      content: "Just completed my onsite loops. Key takeaway: Communication matters just as much as getting the code to run. Make sure to state constraints, edge cases (empty input, duplicates), and talk through space-time complexity before writing the first line of code.",
      upvotes: 85,
      isUpvoted: false,
      repliesCount: 2,
      createdAt: "2 days ago",
      replies: [
        {
          id: "rep-3-1",
          authorName: "Sarah Jenkins",
          authorInitials: "SJ",
          authorRole: "Learner",
          content: "Huge congrats David! Did you get asked about follow-up scaling questions (e.g. what if the graph doesn't fit in memory)?",
          createdAt: "2 days ago",
          upvotes: 5
        },
        {
          id: "rep-3-2",
          authorName: "David Kim",
          authorInitials: "DK",
          authorRole: "Top Contributor",
          content: "Yes! In round 2 with the Graph BFS question, the interviewer asked how to partition the graph across worker machines and avoid duplicated visits with distributed caching.",
          createdAt: "1 day ago",
          upvotes: 14
        }
      ]
    },
    {
      id: "post-4",
      title: "Avoid TLE in Python: Using collections.deque vs list.pop(0)",
      authorName: "Marcus Vance",
      authorInitials: "MV",
      authorRole: "Learner",
      category: "Tips",
      problemId: null,
      problemTitle: null,
      content: "PSA for Python developers: `list.pop(0)` is O(N) because it shifts all remaining elements in memory! In BFS queue traversal, this easily triggers Time Limit Exceeded on larger test inputs. Always use `from collections import deque` and `queue.popleft()` which is strictly O(1).",
      upvotes: 52,
      isUpvoted: false,
      repliesCount: 1,
      createdAt: "3 days ago",
      replies: [
        {
          id: "rep-4-1",
          authorName: "Alex Morgan",
          authorInitials: "AM",
          authorRole: "Admin",
          content: "100% agree. This is one of the top reasons people fail Python test suites on BFS challenges. Bookmark this!",
          createdAt: "3 days ago",
          upvotes: 12
        }
      ]
    }
  ];
})();
