package com.codeforge.controller;

import com.codeforge.dto.NoteDto;
import com.codeforge.service.NoteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST controller managing personal learner notes.
 *
 * <p>All endpoints under {@code /api/notes} require authentication.</p>
 */
@RestController
@RequestMapping("/api/notes")
@RequiredArgsConstructor
public class NoteController {

    private final NoteService noteService;

    /**
     * Retrieves all notes for the authenticated user, ordered by updatedAt descending.
     */
    @GetMapping
    public ResponseEntity<List<NoteDto.Response>> getNotes(Authentication authentication) {
        String username = authentication != null ? authentication.getName() : null;
        return ResponseEntity.ok(noteService.getNotes(username));
    }

    /**
     * Creates a new personal note for the authenticated user.
     */
    @PostMapping
    public ResponseEntity<NoteDto.Response> createNote(
            @Valid @RequestBody NoteDto.Request request,
            Authentication authentication) {
        String username = authentication != null ? authentication.getName() : null;
        NoteDto.Response created = noteService.createNote(request, username);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * Updates an existing note.
     */
    @PutMapping("/{id}")
    public ResponseEntity<NoteDto.Response> updateNote(
            @PathVariable Long id,
            @Valid @RequestBody NoteDto.Request request,
            Authentication authentication) {
        String username = authentication != null ? authentication.getName() : null;
        NoteDto.Response updated = noteService.updateNote(id, request, username);
        return ResponseEntity.ok(updated);
    }

    /**
     * Deletes a note by ID.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deleteNote(
            @PathVariable Long id,
            Authentication authentication) {
        String username = authentication != null ? authentication.getName() : null;
        noteService.deleteNote(id, username);
        return ResponseEntity.ok(Map.of("success", true, "message", "Note deleted successfully"));
    }
}

