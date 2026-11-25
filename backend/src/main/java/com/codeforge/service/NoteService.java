package com.codeforge.service;

import com.codeforge.dto.NoteDto;
import com.codeforge.entity.Note;
import com.codeforge.entity.User;
import com.codeforge.exception.ApiException;
import com.codeforge.repository.NoteRepository;
import com.codeforge.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service managing personal notes and algorithmic takeaways.
 */
@Service
@RequiredArgsConstructor
public class NoteService {

    private final NoteRepository noteRepository;
    private final UserRepository userRepository;

    private User resolveUser(String username) {
        if (username == null || username.isBlank() || "anonymousUser".equals(username)) {
            throw ApiException.unauthorized("Authentication required");
        }
        return userRepository.findByUsername(username)
                .or(() -> userRepository.findByEmail(username))
                .orElseThrow(() -> ApiException.unauthorized("User account not found"));
    }

    /**
     * Retrieves all notes for the authenticated user ordered by updatedAt
     * descending.
     */
    @Transactional(readOnly = true)
    public List<NoteDto.Response> getNotes(String username) {
        User user = resolveUser(username);
        return noteRepository.findByUserIdOrderByUpdatedAtDesc(user.getId())
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    /**
     * Creates a new note for the authenticated user.
     */
    @Transactional
    public NoteDto.Response createNote(NoteDto.Request request, String username) {
        User user = resolveUser(username);

        Note note = Note.builder()
                .user(user)
                .title(request.getTitle())
                .problemId(request.getProblemId())
                .problemTitle(request.getProblemTitle())
                .tags(NoteDto.serializeTags(request.getTags()))
                .content(request.getContent() != null ? request.getContent() : "")
                .updatedAt(LocalDateTime.now())
                .build();

        note = noteRepository.save(note);
        return toDto(note);
    }

    /**
     * Updates an existing note ensuring ownership.
     */
    @Transactional
    public NoteDto.Response updateNote(Long id, NoteDto.Request request, String username) {
        User user = resolveUser(username);

        Note note = noteRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Note not found: " + id));

        if (!note.getUser().getId().equals(user.getId())) {
            throw ApiException.forbidden("You do not have permission to modify this note");
        }

        note.setTitle(request.getTitle());
        note.setProblemId(request.getProblemId());
        note.setProblemTitle(request.getProblemTitle());
        note.setTags(NoteDto.serializeTags(request.getTags()));
        if (request.getContent() != null) {
            note.setContent(request.getContent());
        }
        note.setUpdatedAt(LocalDateTime.now());

        note = noteRepository.save(note);
        return toDto(note);
    }

    /**
     * Deletes a note ensuring ownership.
     */
    @Transactional
    public void deleteNote(Long id, String username) {
        User user = resolveUser(username);

        Note note = noteRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Note not found: " + id));

        if (!note.getUser().getId().equals(user.getId())) {
            throw ApiException.forbidden("You do not have permission to delete this note");
        }

        noteRepository.delete(note);
    }

    public NoteDto.Response toDto(Note note) {
        return NoteDto.Response.builder()
                .id(note.getId())
                .title(note.getTitle())
                .problemId(note.getProblemId())
                .problemTitle(note.getProblemTitle())
                .tags(NoteDto.parseTags(note.getTags()))
                .content(note.getContent())
                .updatedAt(note.getUpdatedAt())
                .build();
    }
}