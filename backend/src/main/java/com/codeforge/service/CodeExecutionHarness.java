package com.codeforge.service;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Prepares LeetCode-style algorithm snippets for real compilation and execution on Judge0
 * when user code implements a solution function/method rather than a full stdin/stdout {@code main} program.
 *
 * <p>Standalone programs that already define an entry point ({@code public static void main}, {@code int main(},
 * {@code if __name__}, or direct stdin reading) are passed directly to Judge0 without modification.</p>
 */
public final class CodeExecutionHarness {

    private CodeExecutionHarness() {}

    public record PreparedCode(String sourceCode, boolean wrapped) {}

    /**
     * Wraps algorithm challenge snippets with a language-appropriate stdin/stdout driver harness if needed.
     */
    public static PreparedCode prepareForJudge0(String language, String sourceCode, String inputData) {
        if (sourceCode == null || sourceCode.isBlank()) {
            return new PreparedCode(sourceCode, false);
        }
        String trimmed = sourceCode.trim();
        // Preserve deterministic unit-test markers untouched
        if (trimmed.contains("def solution_code") || trimmed.contains("force-wrong")) {
            return new PreparedCode(sourceCode, false);
        }

        String lang = language == null ? "python" : language.trim().toLowerCase();
        return switch (lang) {
            case "python", "python 3", "python3", "py" -> preparePython(sourceCode, inputData);
            case "javascript", "js", "node", "nodejs" -> prepareJavaScript(sourceCode, inputData);
            case "java" -> prepareJava(sourceCode, inputData);
            case "cpp", "c++" -> prepareCpp(sourceCode, inputData);
            default -> new PreparedCode(sourceCode, false);
        };
    }

    /**
     * Normalizes output values for whitespace-tolerant comparison across languages
     * (e.g. {@code "[0,1]"} equals {@code "[0, 1]"}, {@code "True"} equals {@code "true"}).
     */
    public static String normalizeOutput(String output) {
        if (output == null) return "";
        String trimmed = output.trim();
        if (trimmed.equalsIgnoreCase("true") || trimmed.equalsIgnoreCase("false")) {
            return trimmed.toLowerCase();
        }
        // Strip non-string whitespace so JSON/list representations compare uniformly
        StringBuilder sb = new StringBuilder(trimmed.length());
        boolean inQuote = false;
        char quoteChar = 0;
        boolean escaped = false;
        for (int i = 0; i < trimmed.length(); i++) {
            char c = trimmed.charAt(i);
            if (escaped) {
                sb.append(c);
                escaped = false;
                continue;
            }
            if (inQuote) {
                sb.append(c);
                if (c == '\\') escaped = true;
                else if (c == quoteChar) inQuote = false;
                continue;
            }
            if (c == '"' || c == '\'') {
                inQuote = true;
                quoteChar = c;
                sb.append(c);
            } else if (!Character.isWhitespace(c)) {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    /**
     * Checks whether actual stdout matches expected output under normalized formatting rules.
     */
    public static boolean outputsMatch(String actualStdout, String expectedOutput) {
        if (expectedOutput == null) return true;
        if (actualStdout == null) return false;
        return normalizeOutput(actualStdout).equals(normalizeOutput(expectedOutput));
    }

    private static PreparedCode preparePython(String code, String inputData) {
        if (code.contains("if __name__") || code.contains("sys.stdin") || code.contains("input(")) {
            return new PreparedCode(code, false);
        }
        boolean hasDef = Pattern.compile("(?m)^\\s*(?:async\\s+)?def\\s+\\w+\\s*\\(").matcher(code).find();
        if (!hasDef) {
            return new PreparedCode(code, false);
        }

        StringBuilder sb = new StringBuilder();
        sb.append("from typing import List, Optional, Dict, Tuple, Set, Any\n");
        sb.append("import sys, json, ast, inspect\n\n");
        sb.append(code).append("\n\n");
        sb.append("""
                def __cf_parse_args(raw):
                    raw = (raw or "").strip()
                    if not raw:
                        return []
                    parts, buf, depth, quote, esc = [], [], 0, None, False
                    for ch in raw:
                        if esc:
                            buf.append(ch); esc = False; continue
                        if quote:
                            buf.append(ch)
                            if ch == '\\\\': esc = True
                            elif ch == quote: quote = None
                            continue
                        if ch in ('"', "'"):
                            quote = ch; buf.append(ch); continue
                        if ch in '([{': depth += 1; buf.append(ch); continue
                        if ch in ')]}': depth -= 1; buf.append(ch); continue
                        if (ch == ',' or ch == '\\n') and depth == 0:
                            seg = ''.join(buf).strip()
                            if seg: parts.append(seg)
                            buf = []
                            continue
                        buf.append(ch)
                    seg = ''.join(buf).strip()
                    if seg: parts.append(seg)
                    vals = []
                    for p in parts:
                        expr = p.split('=', 1)[1].strip() if ('=' in p and not p.startswith(('"', "'"))) else p
                        try:
                            vals.append(ast.literal_eval(expr))
                        except Exception:
                            try:
                                vals.append(json.loads(expr))
                            except Exception:
                                vals.append(expr)
                    return vals

                def __cf_format(val):
                    if isinstance(val, bool):
                        return 'true' if val else 'false'
                    if isinstance(val, (list, dict)):
                        return json.dumps(val)
                    if val is None:
                        return 'null'
                    return str(val)

                if __name__ == '__main__':
                    __args = __cf_parse_args(sys.stdin.read())
                    __fn = None
                    if 'Solution' in globals() and inspect.isclass(Solution):
                        __inst = Solution()
                        __methods = [getattr(__inst, m) for m in dir(__inst) if not m.startswith('_') and callable(getattr(__inst, m))]
                        for __m in __methods:
                            if len(inspect.signature(__m).parameters) == len(__args):
                                __fn = __m; break
                        if __fn is None and __methods:
                            __fn = __methods[0]
                    if __fn is None:
                        __fns = [v for k, v in list(globals().items())
                                 if not k.startswith('__cf_') and inspect.isfunction(v) and getattr(v, '__module__', None) == '__main__']
                        for __f in reversed(__fns):
                            if len(inspect.signature(__f).parameters) == len(__args):
                                __fn = __f; break
                        if __fn is None and __fns:
                            __fn = __fns[-1]
                    if __fn is not None:
                        __res = __fn(*__args)
                        print(__cf_format(__res))
                """);
        return new PreparedCode(sb.toString(), true);
    }

    private static PreparedCode prepareJavaScript(String code, String inputData) {
        if (code.contains("fs.readFileSync") || code.contains("process.stdin") || code.contains("readline")) {
            return new PreparedCode(code, false);
        }
        List<String> fnNames = new ArrayList<>();
        Matcher fnMatcher = Pattern.compile("\\bfunction\\s+([a-zA-Z_$][a-zA-Z0-9_$]*)\\s*\\(").matcher(code);
        while (fnMatcher.find()) {
            fnNames.add(fnMatcher.group(1));
        }
        Matcher varMatcher = Pattern.compile("\\b(?:const|let|var)\\s+([a-zA-Z_$][a-zA-Z0-9_$]*)\\s*=\\s*(?:function|\\([^)]*\\)\\s*=>|[a-zA-Z_$][a-zA-Z0-9_$]*\\s*=>)").matcher(code);
        while (varMatcher.find()) {
            fnNames.add(varMatcher.group(1));
        }
        if (fnNames.isEmpty()) {
            return new PreparedCode(code, false);
        }

        String candidateList = String.join(", ", fnNames);
        StringBuilder sb = new StringBuilder();
        sb.append("const __fs = require('fs');\n\n");
        sb.append(code).append("\n\n");
        sb.append("""
                function __cfParseArgs(raw) {
                  raw = (raw || '').trim();
                  if (!raw) return [];
                  const parts = [];
                  let buf = '', depth = 0, quote = null, esc = false;
                  for (const ch of raw) {
                    if (esc) { buf += ch; esc = false; continue; }
                    if (quote) {
                      buf += ch;
                      if (ch === '\\\\') esc = true;
                      else if (ch === quote) quote = null;
                      continue;
                    }
                    if (ch === '"' || ch === "'") { quote = ch; buf += ch; continue; }
                    if ('([{'.includes(ch)) { depth++; buf += ch; continue; }
                    if (')]}'.includes(ch)) { depth--; buf += ch; continue; }
                    if ((ch === ',' || ch === '\\n') && depth === 0) {
                      if (buf.trim()) parts.push(buf.trim());
                      buf = ''; continue;
                    }
                    buf += ch;
                  }
                  if (buf.trim()) parts.push(buf.trim());
                  return parts.map(p => {
                    const idx = p.indexOf('=');
                    const expr = (idx >= 0 && !p.startsWith('"') && !p.startsWith("'")) ? p.slice(idx + 1).trim() : p;
                    try { return JSON.parse(expr); } catch (_) { return expr; }
                  });
                }
                const __rawIn = __fs.readFileSync(0, 'utf8');
                const __args = __cfParseArgs(__rawIn);
                """);
        sb.append("const __candidates = [").append(candidateList).append("].filter(f => typeof f === 'function');\n");
        sb.append("const __target = __candidates.find(f => f.length === __args.length) || __candidates[0];\n");
        sb.append("const __res = __target ? __target(...__args) : null;\n");
        sb.append("console.log(__res === undefined ? 'null' : JSON.stringify(__res));\n");
        return new PreparedCode(sb.toString(), true);
    }

    private static PreparedCode prepareJava(String code, String inputData) {
        if (code.contains("static void main")) {
            return new PreparedCode(code, false);
        }
        if (!code.contains("class Solution")) {
            return new PreparedCode(code, false);
        }

        // Remove public modifier on Solution so Main can be the public entry class in Main.java
        String cleanedUserCode = code.replaceAll("\\bpublic\\s+class\\s+Solution\\b", "class Solution");
        StringBuilder sb = new StringBuilder();
        if (!cleanedUserCode.contains("import java.util.")) {
            sb.append("import java.util.*;\n");
        }
        if (!cleanedUserCode.contains("import java.math.")) {
            sb.append("import java.math.*;\n");
        }
        sb.append("import java.io.*;\n");
        sb.append("import java.lang.reflect.*;\n");
        sb.append("import java.nio.charset.StandardCharsets;\n\n");
        sb.append(cleanedUserCode).append("\n\n");
        sb.append("""
                public class Main {
                    public static void main(String[] args) throws Exception {
                        String raw = new String(System.in.readAllBytes(), StandardCharsets.UTF_8).trim();
                        List<String> argExprs = splitTopLevelArgs(raw);
                        Method target = null;
                        for (Method m : Solution.class.getDeclaredMethods()) {
                            if (!m.isSynthetic() && Modifier.isPublic(m.getModifiers()) && m.getParameterCount() == argExprs.size()) {
                                target = m; break;
                            }
                        }
                        if (target == null) {
                            for (Method m : Solution.class.getDeclaredMethods()) {
                                if (!m.isSynthetic() && m.getParameterCount() == argExprs.size()) {
                                    target = m; break;
                                }
                            }
                        }
                        if (target == null) {
                            System.err.println("No matching solution method found in Solution class.");
                            System.exit(1);
                            return;
                        }
                        target.setAccessible(true);
                        Class<?>[] paramTypes = target.getParameterTypes();
                        Object[] parsedArgs = new Object[paramTypes.length];
                        for (int i = 0; i < paramTypes.length; i++) {
                            parsedArgs[i] = convertArg(argExprs.get(i), paramTypes[i]);
                        }
                        Solution sol = new Solution();
                        Object res;
                        try {
                            res = target.invoke(Modifier.isStatic(target.getModifiers()) ? null : sol, parsedArgs);
                        } catch (InvocationTargetException ite) {
                            Throwable cause = ite.getCause() != null ? ite.getCause() : ite;
                            cause.printStackTrace(System.err);
                            System.exit(1);
                            return;
                        }
                        System.out.println(formatResult(res));
                    }

                    private static List<String> splitTopLevelArgs(String raw) {
                        List<String> out = new ArrayList<>();
                        if (raw == null || raw.isEmpty()) return out;
                        StringBuilder buf = new StringBuilder();
                        int depth = 0; char quote = 0; boolean esc = false;
                        for (int i = 0; i < raw.length(); i++) {
                            char c = raw.charAt(i);
                            if (esc) { buf.append(c); esc = false; continue; }
                            if (quote != 0) {
                                buf.append(c);
                                if (c == '\\\\') esc = true;
                                else if (c == quote) quote = 0;
                                continue;
                            }
                            if (c == '"' || c == '\\'') { quote = c; buf.append(c); continue; }
                            if (c == '[' || c == '(' || c == '{') { depth++; buf.append(c); continue; }
                            if (c == ']' || c == ')' || c == '}') { depth--; buf.append(c); continue; }
                            if ((c == ',' || c == '\\n') && depth == 0) {
                                String seg = extractValue(buf.toString().trim());
                                if (!seg.isEmpty()) out.add(seg);
                                buf.setLength(0);
                                continue;
                            }
                            buf.append(c);
                        }
                        String tail = extractValue(buf.toString().trim());
                        if (!tail.isEmpty()) out.add(tail);
                        return out;
                    }

                    private static String extractValue(String seg) {
                        int eq = seg.indexOf('=');
                        if (eq >= 0 && !seg.startsWith("\\"")) {
                            return seg.substring(eq + 1).trim();
                        }
                        return seg;
                    }

                    private static Object convertArg(String expr, Class<?> type) {
                        if (type == int.class || type == Integer.class) return Integer.parseInt(expr.trim());
                        if (type == long.class || type == Long.class) return Long.parseLong(expr.trim());
                        if (type == double.class || type == Double.class) return Double.parseDouble(expr.trim());
                        if (type == boolean.class || type == Boolean.class) return Boolean.parseBoolean(expr.trim());
                        if (type == String.class) return unquote(expr);
                        if (type == int[].class) return parseIntArray(expr);
                        if (type == String[].class) return parseStringList(expr).toArray(new String[0]);
                        if (type == List.class) return expr.trim().startsWith("[\\\"") || expr.trim().startsWith("['")
                                ? parseStringList(expr) : parseIntList(expr);
                        return expr;
                    }

                    private static String unquote(String s) {
                        String t = s.trim();
                        if ((t.startsWith("\\"") && t.endsWith("\\"")) || (t.startsWith("'") && t.endsWith("'"))) {
                            return t.substring(1, t.length() - 1);
                        }
                        return t;
                    }

                    private static int[] parseIntArray(String expr) {
                        List<Integer> list = parseIntList(expr);
                        int[] arr = new int[list.size()];
                        for (int i = 0; i < list.size(); i++) arr[i] = list.get(i);
                        return arr;
                    }

                    private static List<Integer> parseIntList(String expr) {
                        String inner = expr.trim();
                        if (inner.startsWith("[")) inner = inner.substring(1);
                        if (inner.endsWith("]")) inner = inner.substring(0, inner.length() - 1);
                        List<Integer> list = new ArrayList<>();
                        if (inner.trim().isEmpty()) return list;
                        for (String part : inner.split(",")) {
                            if (!part.trim().isEmpty()) list.add(Integer.parseInt(part.trim()));
                        }
                        return list;
                    }

                    private static List<String> parseStringList(String expr) {
                        String inner = expr.trim();
                        if (inner.startsWith("[")) inner = inner.substring(1);
                        if (inner.endsWith("]")) inner = inner.substring(0, inner.length() - 1);
                        List<String> list = new ArrayList<>();
                        if (inner.trim().isEmpty()) return list;
                        for (String part : splitTopLevelArgs(inner)) {
                            list.add(unquote(part));
                        }
                        return list;
                    }

                    private static String formatResult(Object res) {
                        if (res == null) return "null";
                        if (res instanceof int[] arr) return Arrays.toString(arr);
                        if (res instanceof long[] arr) return Arrays.toString(arr);
                        if (res instanceof double[] arr) return Arrays.toString(arr);
                        if (res instanceof boolean[] arr) return Arrays.toString(arr);
                        if (res instanceof String[] arr) {
                            StringBuilder sb = new StringBuilder("[");
                            for (int i = 0; i < arr.length; i++) {
                                if (i > 0) sb.append(", ");
                                sb.append("\\"").append(arr[i]).append("\\"");
                            }
                            return sb.append("]").toString();
                        }
                        if (res instanceof Object[] arr) return Arrays.deepToString(arr);
                        return String.valueOf(res);
                    }
                }
                """);
        return new PreparedCode(sb.toString(), true);
    }

    private static PreparedCode prepareCpp(String code, String inputData) {
        if (code.contains("int main(") || code.contains("void main(")) {
            return new PreparedCode(code, false);
        }
        Matcher methodMatcher = Pattern.compile(
                "(vector<vector<string>>|vector<string>|vector<int>|string|bool|int|double|long long)\\s+([a-zA-Z_][a-zA-Z0-9_]*)\\s*\\(([^)]*)\\)")
                .matcher(code);
        if (!methodMatcher.find()) {
            return new PreparedCode(code, false);
        }
        String returnType = methodMatcher.group(1).trim();
        String methodName = methodMatcher.group(2).trim();
        String paramsStr = methodMatcher.group(3).trim();

        StringBuilder sb = new StringBuilder();
        if (!code.contains("#include <iostream>")) sb.append("#include <iostream>\n");
        if (!code.contains("#include <vector>")) sb.append("#include <vector>\n");
        if (!code.contains("#include <string>")) sb.append("#include <string>\n");
        if (!code.contains("#include <unordered_map>")) sb.append("#include <unordered_map>\n");
        if (!code.contains("#include <unordered_set>")) sb.append("#include <unordered_set>\n");
        if (!code.contains("#include <map>")) sb.append("#include <map>\n");
        if (!code.contains("#include <set>")) sb.append("#include <set>\n");
        if (!code.contains("#include <stack>")) sb.append("#include <stack>\n");
        if (!code.contains("#include <queue>")) sb.append("#include <queue>\n");
        if (!code.contains("#include <algorithm>")) sb.append("#include <algorithm>\n");
        if (!code.contains("#include <sstream>")) sb.append("#include <sstream>\n");
        if (!code.contains("#include <climits>")) sb.append("#include <climits>\n");
        if (!code.contains("using namespace std")) sb.append("using namespace std;\n");
        sb.append("\n").append(code).append("\n\n");

        sb.append("""
                static vector<string> __cfSplitArgs(const string& raw) {
                    vector<string> out; string buf; int depth = 0; char quote = 0; bool esc = false;
                    for (char c : raw) {
                        if (esc) { buf += c; esc = false; continue; }
                        if (quote) {
                            buf += c;
                            if (c == '\\\\') esc = true;
                            else if (c == quote) quote = 0;
                            continue;
                        }
                        if (c == '"' || c == '\\'') { quote = c; buf += c; continue; }
                        if (c == '[' || c == '(' || c == '{') { depth++; buf += c; continue; }
                        if (c == ']' || c == ')' || c == '}') { depth--; buf += c; continue; }
                        if ((c == ',' || c == '\\n') && depth == 0) {
                            size_t eq = buf.find('=');
                            string val = (eq != string::npos && buf.find('"') > eq) ? buf.substr(eq + 1) : buf;
                            size_t s = val.find_first_not_of(" \\t\\r\\n");
                            if (s != string::npos) {
                                size_t e = val.find_last_not_of(" \\t\\r\\n");
                                out.push_back(val.substr(s, e - s + 1));
                            }
                            buf.clear(); continue;
                        }
                        buf += c;
                    }
                    if (!buf.empty()) {
                        size_t eq = buf.find('=');
                        string val = (eq != string::npos && buf.find('"') > eq) ? buf.substr(eq + 1) : buf;
                        size_t s = val.find_first_not_of(" \\t\\r\\n");
                        if (s != string::npos) {
                            size_t e = val.find_last_not_of(" \\t\\r\\n");
                            out.push_back(val.substr(s, e - s + 1));
                        }
                    }
                    return out;
                }
                static vector<int> __cfToVecInt(const string& s) {
                    vector<int> res; string cur;
                    for (char c : s) {
                        if (isdigit(c) || c == '-') cur += c;
                        else if (!cur.empty()) { res.push_back(stoi(cur)); cur.clear(); }
                    }
                    if (!cur.empty()) res.push_back(stoi(cur));
                    return res;
                }
                static string __cfUnquote(const string& s) {
                    if (s.size() >= 2 && ((s.front() == '"' && s.back() == '"') || (s.front() == '\\'' && s.back() == '\\'')))
                        return s.substr(1, s.size() - 2);
                    return s;
                }
                static vector<string> __cfToVecStr(const string& s) {
                    string inner = s;
                    if (inner.size() >= 2 && inner.front() == '[' && inner.back() == ']')
                        inner = inner.substr(1, inner.size() - 2);
                    vector<string> items = __cfSplitArgs(inner);
                    for (auto& it : items) it = __cfUnquote(it);
                    return items;
                }
                int main() {
                    ostringstream __oss; __oss << cin.rdbuf();
                    vector<string> __args = __cfSplitArgs(__oss.str());
                    Solution __sol;
                """);

        List<String> argNames = new ArrayList<>();
        if (!paramsStr.isEmpty()) {
            String[] params = paramsStr.split(",");
            for (int i = 0; i < params.length; i++) {
                String p = params[i].trim();
                String varName = "__p" + i;
                argNames.add(varName);
                String idxExpr = "(__args.size() > " + i + " ? __args[" + i + "] : \"\")";
                if (p.contains("vector<string>")) {
                    sb.append("    vector<string> ").append(varName).append(" = __cfToVecStr(").append(idxExpr).append(");\n");
                } else if (p.contains("vector<int>")) {
                    sb.append("    vector<int> ").append(varName).append(" = __cfToVecInt(").append(idxExpr).append(");\n");
                } else if (p.contains("string")) {
                    sb.append("    string ").append(varName).append(" = __cfUnquote(").append(idxExpr).append(");\n");
                } else if (p.contains("bool")) {
                    sb.append("    bool ").append(varName).append(" = (").append(idxExpr).append(" == \"true\");\n");
                } else if (p.contains("double")) {
                    sb.append("    double ").append(varName).append(" = stod(").append(idxExpr).append(");\n");
                } else {
                    sb.append("    int ").append(varName).append(" = stoi(").append(idxExpr).append(");\n");
                }
            }
        }

        sb.append("    auto __res = __sol.").append(methodName).append("(").append(String.join(", ", argNames)).append(");\n");
        if (returnType.equals("vector<int>")) {
            sb.append("    cout << \"[\"; for (size_t i = 0; i < __res.size(); i++) { if (i) cout << \", \"; cout << __res[i]; } cout << \"]\" << endl;\n");
        } else if (returnType.equals("vector<string>")) {
            sb.append("    cout << \"[\"; for (size_t i = 0; i < __res.size(); i++) { if (i) cout << \", \"; cout << \"\\\"\" << __res[i] << \"\\\"\"; } cout << \"]\" << endl;\n");
        } else if (returnType.equals("vector<vector<string>>")) {
            sb.append("    cout << \"[\"; for (size_t i = 0; i < __res.size(); i++) { if (i) cout << \", \"; cout << \"[\"; for (size_t j = 0; j < __res[i].size(); j++) { if (j) cout << \", \"; cout << \"\\\"\" << __res[i][j] << \"\\\"\"; } cout << \"]\"; } cout << \"]\" << endl;\n");
        } else if (returnType.equals("bool")) {
            sb.append("    cout << (__res ? \"true\" : \"false\") << endl;\n");
        } else {
            sb.append("    cout << __res << endl;\n");
        }
        sb.append("    return 0;\n}\n");
        return new PreparedCode(sb.toString(), true);
    }
}
