package com.codeforge;

import com.codeforge.dto.ExecutionDto;
import com.codeforge.service.CodeExecutionHarness;
import com.codeforge.service.Judge0Service;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.any;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
@Transactional
class CompilerJudge0Tests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private Judge0Service judge0Service;

    @Test
    @DisplayName("Judge0Service: encodes Base64, polls async token when In Queue (id=1), and MIME-decodes output")
    void testJudge0Base64AndPolling() {
        RestTemplate mockRest = Mockito.mock(RestTemplate.class);
        RestTemplate originalRest = (RestTemplate) ReflectionTestUtils.getField(judge0Service, "restTemplate");
        Boolean originalLive = (Boolean) ReflectionTestUtils.getField(judge0Service, "liveEnabled");
        try {
            ReflectionTestUtils.setField(judge0Service, "liveEnabled", true);
            judge0Service.setRestTemplate(mockRest);

            // Simulate POST returning status id=1 (In Queue) with token
            Map<String, Object> queuedBody = Map.of(
                    "token", "tok-judge0-123",
                    "status", Map.of("id", 1, "description", "In Queue")
            );
            Mockito.when(mockRest.exchange(
                    contains("/submissions?base64_encoded=true&wait=true"),
                    eq(HttpMethod.POST),
                    any(HttpEntity.class),
                    any(ParameterizedTypeReference.class)
            )).thenReturn(ResponseEntity.ok(queuedBody));

            // Simulate Judge0 Ruby-style Base64 with embedded newline
            String b64Stdout = Base64.getEncoder().encodeToString("Hello\nJudge0!".getBytes(StandardCharsets.UTF_8));
            String b64WithNewline = b64Stdout.substring(0, 4) + "\n" + b64Stdout.substring(4);

            Map<String, Object> completedBody = Map.of(
                    "token", "tok-judge0-123",
                    "status", Map.of("id", 3, "description", "Accepted"),
                    "stdout", b64WithNewline,
                    "time", "0.015",
                    "memory", 4096
            );
            Mockito.when(mockRest.exchange(
                    contains("/submissions/tok-judge0-123?base64_encoded=true"),
                    eq(HttpMethod.GET),
                    any(HttpEntity.class),
                    any(ParameterizedTypeReference.class)
            )).thenReturn(ResponseEntity.ok(completedBody));

            Map<String, Object> res = judge0Service.compileAndExecute(
                    100, "python", "print('Hello\\nJudge0!')", "", null, 5.0, 256000);

            assertEquals("tok-judge0-123", res.get("token"));
            assertEquals("Hello\nJudge0!", res.get("stdout"));
            assertEquals("0.015", res.get("time"));
            assertEquals(4096, res.get("memory"));
            assertEquals("Judge0 CE", res.get("engine"));
        } finally {
            judge0Service.setRestTemplate(originalRest);
            ReflectionTestUtils.setField(judge0Service, "liveEnabled", originalLive);
        }
    }

    @Test
    @DisplayName("Compiler API: POST /api/execute/compile returns decoded compile_output on Compilation Error (id=6)")
    void testCompileEndpoint_CompilationError() throws Exception {
        RestTemplate mockRest = Mockito.mock(RestTemplate.class);
        RestTemplate originalRest = (RestTemplate) ReflectionTestUtils.getField(judge0Service, "restTemplate");
        Boolean originalLive = (Boolean) ReflectionTestUtils.getField(judge0Service, "liveEnabled");
        try {
            ReflectionTestUtils.setField(judge0Service, "liveEnabled", true);
            judge0Service.setRestTemplate(mockRest);

            String compileErr = Base64.getEncoder().encodeToString(
                    "main.cpp:2:5: error: 'foo' was not declared".getBytes(StandardCharsets.UTF_8));
            Map<String, Object> ceBody = Map.of(
                    "token", "tok-ce-999",
                    "status", Map.of("id", 6, "description", "Compilation Error"),
                    "compile_output", compileErr,
                    "time", "0.000",
                    "memory", 0
            );
            Mockito.when(mockRest.exchange(
                    contains("/submissions?base64_encoded=true&wait=true"),
                    eq(HttpMethod.POST),
                    any(HttpEntity.class),
                    any(ParameterizedTypeReference.class)
            )).thenReturn(ResponseEntity.ok(ceBody));

            ExecutionDto.CompileRequest req = ExecutionDto.CompileRequest.builder()
                    .languageId(105)
                    .sourceCode("int main() { foo(); }")
                    .stdin("")
                    .build();

            mockMvc.perform(post("/api/execute/compile")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success", is(false)))
                    .andExpect(jsonPath("$.statusId", is(6)))
                    .andExpect(jsonPath("$.status", is("Compilation Error")))
                    .andExpect(jsonPath("$.compileOutput", containsString("not declared")))
                    .andExpect(jsonPath("$.token", is("tok-ce-999")))
                    .andExpect(jsonPath("$.languageId", is(105)));
        } finally {
            judge0Service.setRestTemplate(originalRest);
            ReflectionTestUtils.setField(judge0Service, "liveEnabled", originalLive);
        }
    }

    @Test
    @DisplayName("Compiler API: empty sourceCode returns 400 Bad Request")
    void testCompileEndpoint_EmptySourceBadRequest() throws Exception {
        ExecutionDto.CompileRequest req = ExecutionDto.CompileRequest.builder()
                .language("python")
                .sourceCode("   ")
                .build();

        mockMvc.perform(post("/api/execute/compile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Compiler API: GET /api/execute/languages and /api/execute/judge0/status return valid payloads")
    void testCompilerMetadataEndpoints() throws Exception {
        mockMvc.perform(get("/api/execute/languages"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(5))))
                .andExpect(jsonPath("$[0].id").isNumber())
                .andExpect(jsonPath("$[0].name").isString());

        mockMvc.perform(get("/api/execute/judge0/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.engine").isString());
    }

    @Test
    @DisplayName("Run API: accepts customCases from IDE console without persisting submissions")
    void testRunWithCustomCases() throws Exception {
        ExecutionDto.RunRequest req = ExecutionDto.RunRequest.builder()
                .problemId(104L)
                .language("python")
                .code("def pair_sum(nums, target):\n    seen = {}\n    for i, n in enumerate(nums):\n        if target - n in seen:\n            return [seen[target - n], i]\n        seen[n] = i\n    return []")
                .customCases(List.of(
                        ExecutionDto.CustomTestCaseInput.builder()
                                .name("Custom 1")
                                .input("nums = [10, 20, 30], target = 50")
                                .expected("[1, 2]")
                                .build()
                ))
                .build();

        mockMvc.perform(post("/api/execute/run")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.passedCount", is(1)))
                .andExpect(jsonPath("$.results[0].name", is("Custom 1")));
    }

    @Test
    @DisplayName("CodeExecutionHarness: wraps LeetCode methods cleanly and leaves standalone main() raw")
    void testCodeExecutionHarness() {
        // Python challenge method -> wrapped
        CodeExecutionHarness.PreparedCode pyWrapped = CodeExecutionHarness.prepareForJudge0(
                "python", "def pair_sum(nums, target):\n    return [0, 1]", "nums = [2,7], target = 9");
        assertTrue(pyWrapped.wrapped());
        assertTrue(pyWrapped.sourceCode().contains("__cf_parse_args"));

        // Python standalone script -> raw
        CodeExecutionHarness.PreparedCode pyStandalone = CodeExecutionHarness.prepareForJudge0(
                "python", "if __name__ == '__main__':\n    print(input())", "hello");
        assertFalse(pyStandalone.wrapped());

        // Java Solution class -> wrapped with Main
        CodeExecutionHarness.PreparedCode javaWrapped = CodeExecutionHarness.prepareForJudge0(
                "java", "class Solution { public int[] pairSum(int[] nums, int target) { return new int[]{0,1}; } }", "nums=[2,7], target=9");
        assertTrue(javaWrapped.wrapped());
        assertTrue(javaWrapped.sourceCode().contains("public class Main"));

        // Java standalone Main -> raw
        CodeExecutionHarness.PreparedCode javaStandalone = CodeExecutionHarness.prepareForJudge0(
                "java", "public class Main { public static void main(String[] args) { System.out.println(42); } }", "");
        assertFalse(javaStandalone.wrapped());

        // C++ Solution class -> wrapped with headers & int main()
        CodeExecutionHarness.PreparedCode cppWrapped = CodeExecutionHarness.prepareForJudge0(
                "cpp", "class Solution { public: vector<int> pairSum(vector<int>& nums, int target) { return {0,1}; } };", "nums=[2,7], target=9");
        assertTrue(cppWrapped.wrapped());
        assertTrue(cppWrapped.sourceCode().contains("int main()"));

        // Normalized output comparison
        assertTrue(CodeExecutionHarness.outputsMatch("[0,1]", "[0, 1]"));
        assertTrue(CodeExecutionHarness.outputsMatch("True\n", "true"));
        assertFalse(CodeExecutionHarness.outputsMatch("[0, 2]", "[0, 1]"));

        // JavaScript multi-function and quoted '=' safety
        CodeExecutionHarness.PreparedCode jsWrapped = CodeExecutionHarness.prepareForJudge0(
                "javascript", "function isAnagram(s, t) { return true; }\nfunction helper() { return 0; }", "s = \"a=b\", t = \"b=a\"");
        assertTrue(jsWrapped.wrapped());
        assertTrue(jsWrapped.sourceCode().contains("__candidates"));

        // Python quoted '=' safety
        assertTrue(pyWrapped.sourceCode().contains("not p.startswith"));
    }

    @Test
    @DisplayName("Judge0Service: falls back to async POST + polls token with status 0 when wait=true returns 400")
    void testJudge0Async201FallbackOnWait400() {
        RestTemplate mockRest = Mockito.mock(RestTemplate.class);
        RestTemplate originalRest = (RestTemplate) ReflectionTestUtils.getField(judge0Service, "restTemplate");
        Boolean originalLive = (Boolean) ReflectionTestUtils.getField(judge0Service, "liveEnabled");
        try {
            ReflectionTestUtils.setField(judge0Service, "liveEnabled", true);
            judge0Service.setRestTemplate(mockRest);

            Mockito.when(mockRest.exchange(
                    contains("/submissions?base64_encoded=true&wait=true"),
                    eq(HttpMethod.POST),
                    any(HttpEntity.class),
                    any(ParameterizedTypeReference.class)
            )).thenThrow(HttpClientErrorException.create(HttpStatus.BAD_REQUEST, "wait disabled", HttpHeaders.EMPTY, new byte[0], null));

            // 201 async create response contains only token (no status object -> statusId=0)
            Mockito.when(mockRest.exchange(
                    eq("https://ce.judge0.com/submissions?base64_encoded=true"),
                    eq(HttpMethod.POST),
                    any(HttpEntity.class),
                    any(ParameterizedTypeReference.class)
            )).thenReturn(ResponseEntity.status(HttpStatus.CREATED).body(Map.of("token", "tok-async-201")));

            String b64Out = Base64.getEncoder().encodeToString("OK".getBytes(StandardCharsets.UTF_8));
            Mockito.when(mockRest.exchange(
                    contains("/submissions/tok-async-201?base64_encoded=true"),
                    eq(HttpMethod.GET),
                    any(HttpEntity.class),
                    any(ParameterizedTypeReference.class)
            )).thenReturn(ResponseEntity.ok(Map.of(
                    "token", "tok-async-201",
                    "status", Map.of("id", 3, "description", "Accepted"),
                    "stdout", b64Out,
                    "time", "0.020",
                    "memory", 2048
            )));

            Map<String, Object> res = judge0Service.compileAndExecute(
                    100, "python", "print('OK')", "", null, 5.0, 256000);

            assertEquals("tok-async-201", res.get("token"));
            assertEquals("OK", res.get("stdout"));
            assertEquals("Judge0 CE", res.get("engine"));
        } finally {
            judge0Service.setRestTemplate(originalRest);
            ReflectionTestUtils.setField(judge0Service, "liveEnabled", originalLive);
        }
    }
}
