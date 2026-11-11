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

/**
 * REST controller managing the CodeForge algorithm problem library.
 *
 * <p>Exposes endpoints under {@code /api/problems} to search, filter, retrieve, and author
 * algorithmic challenges. Enriches problem payloads with contextual status for authenticated callers.</p>
 */
@RestController
@RequestMapping("/api/problems")
@RequiredArgsConstructor
public class ExerciseController {

    /** Service layer handling problem retrieval, search algorithms, and authoring. */
    private final ExerciseService exerciseService;

    /**
     * Retrieves all problems matching optional multi-criteria filters.
     *
     * <p>HTTP Endpoint: {@code GET /api/problems}</p>
     *
     * @param search         Optional substring query matching problem title or ID.
     * @param difficulty     Optional difficulty tier filter ("Easy", "Medium", "Hard").
     * @param topic          Optional category topic filter (e.g. "Arrays", "Hash map").
     * @param tab            Optional view tab ("all", "saved", "my").
     * @param authentication Security context representing the calling user.
     * @return {@link ResponseEntity} with HTTP 200 OK and list of {@link ExerciseDto} objects.
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
     * Selects and returns a random problem from the catalog for the "Pick Random" feature.
     *
     * <p>HTTP Endpoint: {@code GET /api/problems/random}</p>
     *
     * @param authentication Security context representing the calling user.
     * @return {@link ResponseEntity} with HTTP 200 OK and random {@link ExerciseDto}, or 404 if no problems exist.
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
     * Retrieves complete problem statement and metadata for a specific exercise ID.
     *
     * <p>HTTP Endpoint: {@code GET /api/problems/{id}}</p>
     *
     * @param id             The unique problem ID to fetch.
     * @param authentication Security context representing the calling user.
     * @return {@link ResponseEntity} with HTTP 200 OK and {@link ExerciseDto}, or 404 if not found.
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
     * Creates a new custom algorithm challenge authored by the authenticated user.
     *
     * <p>HTTP Endpoint: {@code POST /api/problems}</p>
     * <p>Requires a valid Bearer token authentication in the security context.</p>
     *
     * @param request        The problem creation parameters (title, statement, difficulty, topics, etc.).
     * @param authentication Security context representing the authenticated author.
     * @return {@link ResponseEntity} with HTTP 201 Created and the created {@link ExerciseDto}, or 401 if unauthorized.
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
