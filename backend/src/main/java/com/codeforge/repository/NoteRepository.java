package com.codeforge.repository;

import com.codeforge.entity.Note;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for {@link Note} entities.
 */
@Repository
public interface NoteRepository extends JpaRepository<Note, Long> {

    /**
     * Finds all notes authored by the specified user, ordered by most recently updated.
     *
     * @param userId The ID of the owner user.
     * @return List of matching {@link Note} entities.
     */
    List<Note> findByUserIdOrderByUpdatedAtDesc(Long userId);

    /**
     * Finds a single note by its ID and user ID, ensuring access control verification.
     *
     * @param id     The note primary key.
     * @param userId The owner user ID.
     * @return An {@link Optional} containing the note if owned by the user.
     */
    Optional<Note> findByIdAndUserId(Long id, Long userId);
}

