package com.codeforge.controller;

import com.codeforge.dto.ExerciseDto;
import com.codeforge.service.ExerciseService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/problems")
@RequiredArgsConstructor
public class ExerciseController {

    private final ExerciseService exerciseService;

    /**
     * GET /api/problems
     * List problems with optional filters: search, difficulty, topic, tab.
     */
    @GetMapping
    public ResponseEntity<List<ExerciseDto>> getAll(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String difficulty,
            @RequestParam(required = false) String topic,
            @RequestParam(required = false) String tab,
            Authentication authentication) {

        String username = authentication != null ? authentication.getName() : null;
        return ResponseEntity.ok(exerciseService.getAll(search, difficulty, topic, tab, username));
    }

    /**
     * GET /api/problems/random
     * Returns a random problem.
     */
    @GetMapping("/random")
    public ResponseEntity<?> getRandom(Authentication authentication) {
        String username = authentication != null ? authentication.getName() : null;
        ExerciseDto detail = exerciseService.getRandom(username);
        if (detail == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "No problems available"));
        }
        return ResponseEntity.ok(detail);
    }

    /**
     * GET /api/problems/{id}
     * Returns full problem detail.
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getById(@PathVariable Long id, Authentication authentication) {
        String username = authentication != null ? authentication.getName() : null;
        return exerciseService.getById(id, username)
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "Problem not found: " + id)));
    }

    /**
     * POST /api/problems
     * Create a new problem (authenticated users only).
     */
    @PostMapping
    public ResponseEntity<?> create(
            @RequestBody ExerciseDto.CreateExerciseRequest request,
            Authentication authentication) {

        if (authentication == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Authentication required"));
        }
        ExerciseDto created = exerciseService.create(request, authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }
}
