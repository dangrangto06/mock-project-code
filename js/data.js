// CodeForge Mock Database & Initial State

window.CodeForgeData = {
  currentUser: {
    name: "Alex Morgan",
    initials: "AM",
    greetingName: "Hieu",
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
      { id: 104, title: "Pair Sum", status: "Accepted", runtime: "32 ms", memory: "17.4 MB", date: "Just now" },
      { id: 106, title: "Valid Anagram", status: "Accepted", runtime: "41 ms", memory: "16.8 MB", date: "Yesterday" },
      { id: 105, title: "Contains Duplicate", status: "Accepted", runtime: "28 ms", memory: "19.2 MB", date: "2 days ago" },
      { id: 110, title: "Valid Parentheses", status: "Accepted", runtime: "19 ms", memory: "16.5 MB", date: "4 days ago" }
    ]
  },

  problems: [
    {
      id: 104,
      title: "Pair Sum",
      difficulty: "Easy",
      topics: ["Arrays", "Hash map"],
      acceptance: "78.4%",
      status: "in-progress", // 'solved', 'in-progress', 'unattempted'
      saved: true,
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
        cpp: `class Solution {
public:
    vector<int> pairSum(vector<int>& nums, int target) {
        unordered_map<int, int> seen;
        for (int i = 0; i < nums.size(); ++i) {
            int complement = target - nums[i];
            if (seen.find(complement) != seen.end()) {
                return {seen[complement], i};
            }
            seen[nums[i]] = i;
        }
        return {};
    }
};`,
        java: `class Solution {
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
          title: "Optimized C++ unordered_map Solution (0ms beats 100%)",
          votes: 412,
          language: "C++",
          preview: "Pre-reserve bucket size to avoid rehash overhead."
        }
      ],
      submissions: [
        { status: "Accepted", runtime: "32 ms", memory: "17.4 MB", language: "Python 3", date: "Oct 28, 2026 14:22" },
        { status: "Accepted", runtime: "38 ms", memory: "17.6 MB", language: "Python 3", date: "Oct 28, 2026 14:18" },
        { status: "Wrong Answer", runtime: "28 ms", memory: "17.3 MB", language: "Python 3", date: "Oct 28, 2026 14:15" }
      ]
    },
    {
      id: 105,
      title: "Contains Duplicate",
      difficulty: "Easy",
      topics: ["Arrays", "Hash set"],
      acceptance: "82.1%",
      status: "solved",
      saved: false,
      isMine: false,
      description: "Given an integer array nums, return true if any value appears at least twice in the array, and return false if every element is distinct."
    },
    {
      id: 106,
      title: "Valid Anagram",
      difficulty: "Easy",
      topics: ["Strings", "Hash map"],
      acceptance: "76.8%",
      status: "solved",
      saved: true,
      isMine: false,
      description: "Given two strings s and t, return true if t is an anagram of s, and false otherwise."
    },
    {
      id: 107,
      title: "Group Anagrams",
      difficulty: "Medium",
      topics: ["Strings", "Sorting"],
      acceptance: "64.2%",
      status: "unattempted",
      saved: false,
      isMine: false,
      description: "Given an array of strings strs, group the anagrams together. You can return the answer in any order."
    },
    {
      id: 108,
      title: "Top K Frequent Elements",
      difficulty: "Medium",
      topics: ["Arrays", "Heap"],
      acceptance: "61.5%",
      status: "unattempted",
      saved: true,
      isMine: false,
      description: "Given an integer array nums and an integer k, return the k most frequent elements. You may return the answer in any order."
    },
    {
      id: 109,
      title: "Product of Array Except Self",
      difficulty: "Medium",
      topics: ["Arrays", "Prefix sum"],
      acceptance: "67.3%",
      status: "unattempted",
      saved: false,
      isMine: false,
      description: "Given an integer array nums, return an array answer such that answer[i] is equal to the product of all the elements of nums except nums[i]."
    },
    {
      id: 110,
      title: "Valid Parentheses",
      difficulty: "Easy",
      topics: ["Strings", "Stacks"],
      acceptance: "74.6%",
      status: "solved",
      saved: false,
      isMine: false,
      description: "Given a string s containing just the characters '(', ')', '{', '}', '[' and ']', determine if the input string is valid."
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
