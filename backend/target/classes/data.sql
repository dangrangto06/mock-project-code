-- Seed Initial Data for CodeForge

-- 1. Insert Initial User: Alex Morgan (Username: alex_morgan, greeting: Hieu)
INSERT INTO users (id, username, email, password_hash, full_name, role, streak_days, solved_count, acceptance_rate, practice_time_hours, created_at)
VALUES (1, 'alex_morgan', 'alex@codeforge.io', '$2a$10$7EqJtq98hPqEX7fNZaFWoOeV2d6m8C6sX1w/2WjH4Uf31hB7uRfa2', 'Alex Morgan', 'Admin', 7, 48, 76.20, 12.50, CURRENT_TIMESTAMP);

-- 2. Insert Problem 104: Pair Sum
INSERT INTO exercises (id, title, difficulty, topics, acceptance_rate, description, constraints, starter_code_python, starter_code_javascript, starter_code_cpp, starter_code_java, time_limit_ms, memory_limit_mb, is_custom, created_at)
VALUES (104, 'Pair Sum', 'Easy', 'Arrays, Hash map', 78.40,
'Given an integer array `nums` and an integer `target`, return the indices of the two numbers that add up to `target`.\n\nYou may assume that each input has exactly one solution. You may not use the same element twice.\n\nReturn the answer in any order.',
'2 <= nums.length <= 10^4\n-10^9 <= nums[i], target <= 10^9\nExactly one valid answer exists.',
'def pair_sum(nums: list[int], target: int) -> list[int]:\n    # Remember each value and its index.\n    seen = {}\n\n    for i, num in enumerate(nums):\n        complement = target - num\n\n        if complement in seen:\n            return [seen[complement], i]\n\n        seen[num] = i\n\n    return []',
'function pairSum(nums, target) {\n    const seen = new Map();\n    for (let i = 0; i < nums.length; i++) {\n        const complement = target - nums[i];\n        if (seen.has(complement)) {\n            return [seen.get(complement), i];\n        }\n        seen.set(nums[i], i);\n    }\n    return [];\n}',
'class Solution {\npublic:\n    vector<int> pairSum(vector<int>& nums, int target) {\n        unordered_map<int, int> seen;\n        for (int i = 0; i < nums.size(); ++i) {\n            int complement = target - nums[i];\n            if (seen.find(complement) != seen.end()) {\n                return {seen[complement], i};\n            }\n            seen[nums[i]] = i;\n        }\n        return {};\n    }\n};',
'class Solution {\n    public int[] pairSum(int[] nums, int target) {\n        Map<Integer, Integer> seen = new HashMap<>();\n        for (int i = 0; i < nums.length; i++) {\n            int complement = target - nums[i];\n            if (seen.containsKey(complement)) {\n                return new int[] { seen.get(complement), i };\n            }\n            seen.put(nums[i], i);\n        }\n        return new int[0];\n    }\n}',
1000, 256, 1, CURRENT_TIMESTAMP);

-- 3. Insert TestCases for Problem 104
INSERT INTO test_cases (exercise_id, case_name, input_data, expected_output, is_sample)
VALUES (104, 'Case 1', 'nums = [2,7,11,15], target = 9', '[0, 1]', 1);

INSERT INTO test_cases (exercise_id, case_name, input_data, expected_output, is_sample)
VALUES (104, 'Case 2', 'nums = [3,2,4], target = 6', '[1, 2]', 1);

-- 4. Insert Other Exercises
INSERT INTO exercises (id, title, difficulty, topics, acceptance_rate, description, time_limit_ms, memory_limit_mb, is_custom, created_at)
VALUES (105, 'Contains Duplicate', 'Easy', 'Arrays, Hash set', 82.10, 'Given an integer array nums, return true if any value appears at least twice in the array, and return false if every element is distinct.', 1000, 256, 0, CURRENT_TIMESTAMP);

INSERT INTO exercises (id, title, difficulty, topics, acceptance_rate, description, time_limit_ms, memory_limit_mb, is_custom, created_at)
VALUES (106, 'Valid Anagram', 'Easy', 'Strings, Hash map', 76.80, 'Given two strings s and t, return true if t is an anagram of s, and false otherwise.', 1000, 256, 0, CURRENT_TIMESTAMP);

INSERT INTO exercises (id, title, difficulty, topics, acceptance_rate, description, time_limit_ms, memory_limit_mb, is_custom, created_at)
VALUES (107, 'Group Anagrams', 'Medium', 'Strings, Sorting', 64.20, 'Given an array of strings strs, group the anagrams together. You can return the answer in any order.', 1000, 256, 0, CURRENT_TIMESTAMP);

INSERT INTO exercises (id, title, difficulty, topics, acceptance_rate, description, time_limit_ms, memory_limit_mb, is_custom, created_at)
VALUES (108, 'Top K Frequent Elements', 'Medium', 'Arrays, Heap', 61.50, 'Given an integer array nums and an integer k, return the k most frequent elements. You may return the answer in any order.', 1000, 256, 0, CURRENT_TIMESTAMP);

INSERT INTO exercises (id, title, difficulty, topics, acceptance_rate, description, time_limit_ms, memory_limit_mb, is_custom, created_at)
VALUES (109, 'Product of Array Except Self', 'Medium', 'Arrays, Prefix sum', 67.30, 'Given an integer array nums, return an array answer such that answer[i] is equal to the product of all the elements of nums except nums[i].', 1000, 256, 0, CURRENT_TIMESTAMP);

INSERT INTO exercises (id, title, difficulty, topics, acceptance_rate, description, time_limit_ms, memory_limit_mb, is_custom, created_at)
VALUES (110, 'Valid Parentheses', 'Easy', 'Strings, Stacks', 74.60, 'Given a string s containing just the characters ''('', '')'', ''{'', ''}'', ''['' and '']'', determine if the input string is valid.', 1000, 256, 0, CURRENT_TIMESTAMP);

