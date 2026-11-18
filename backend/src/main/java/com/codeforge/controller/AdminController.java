package com.codeforge.controller;

import com.codeforge.dto.AdminDto;
import com.codeforge.service.AdminService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST controller managing administrator functions, moderation workflows, and user permissions.
 *
 * <p>All endpoints under {@code /api/admin} require the caller to possess the 'ADMIN' role.</p>
 */
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;

    /**
     * Retrieves aggregated administrative statistics.
     */
    @GetMapping("/stats")
    public ResponseEntity<AdminDto.StatsResponse> getStats() {
        return ResponseEntity.ok(adminService.getStats());
    }

    /**
     * Retrieves all items currently awaiting moderation review.
     */
    @GetMapping("/moderation")
    public ResponseEntity<AdminDto.ModerationQueueResponse> getModerationQueue() {
        return ResponseEntity.ok(adminService.getModerationQueue());
    }

    /**
     * Approves a user-submitted coding problem.
     */
    @PostMapping("/problems/{id}/approve")
    public ResponseEntity<Map<String, Object>> approveProblem(@PathVariable Long id) {
        adminService.approveProblem(id);
        return ResponseEntity.ok(Map.of("success", true, "message", "Problem approved successfully."));
    }

    /**
     * Rejects a user-submitted coding problem with reason.
     */
    @PostMapping("/problems/{id}/reject")
    public ResponseEntity<Map<String, Object>> rejectProblem(
            @PathVariable Long id,
            @Valid @RequestBody AdminDto.RejectRequest request) {
        adminService.rejectProblem(id, request.getReason());
        return ResponseEntity.ok(Map.of("success", true, "message", "Problem rejected."));
    }

    /**
     * Approves a community discussion topic.
     */
    @PostMapping("/discussions/{id}/approve")
    public ResponseEntity<Map<String, Object>> approveDiscussion(@PathVariable Long id) {
        adminService.approveDiscussion(id);
        return ResponseEntity.ok(Map.of("success", true, "message", "Discussion approved successfully."));
    }

    /**
     * Rejects a community discussion topic with reason.
     */
    @PostMapping("/discussions/{id}/reject")
    public ResponseEntity<Map<String, Object>> rejectDiscussion(
            @PathVariable Long id,
            @Valid @RequestBody AdminDto.RejectRequest request) {
        adminService.rejectDiscussion(id, request.getReason());
        return ResponseEntity.ok(Map.of("success", true, "message", "Discussion rejected."));
    }

    /**
     * Permanently deletes a community discussion topic from database.
     */
    @DeleteMapping("/discussions/{id}")
    public ResponseEntity<Map<String, Object>> deleteDiscussion(@PathVariable Long id) {
        adminService.deleteDiscussion(id);
        return ResponseEntity.ok(Map.of("success", true, "message", "Discussion deleted from database."));
    }

    /**
     * Dismisses a user report, keeping the content.
     */
    @PostMapping("/reports/{id}/dismiss")
    public ResponseEntity<Map<String, Object>> dismissReport(@PathVariable Long id) {
        adminService.dismissReport(id);
        return ResponseEntity.ok(Map.of("success", true, "message", "Report dismissed."));
    }

    /**
     * Removes the reported discussion or comment from database and resolves the report.
     */
    @PostMapping("/reports/{id}/delete")
    public ResponseEntity<Map<String, Object>> deleteReportedItem(@PathVariable Long id) {
        adminService.deleteReportedItem(id);
        return ResponseEntity.ok(Map.of("success", true, "message", "Content removed from database and report resolved."));
    }

    /**
     * Retrieves directory of all platform users.
     */
    @GetMapping("/users")
    public ResponseEntity<List<AdminDto.AdminUserDto>> getUsers() {
        return ResponseEntity.ok(adminService.getUsers());
    }

    /**
     * Updates an existing user's role.
     */
    @PutMapping("/users/{id}/role")
    public ResponseEntity<Map<String, Object>> updateUserRole(
            @PathVariable Long id,
            @Valid @RequestBody AdminDto.UpdateRoleRequest request) {
        adminService.updateUserRole(id, request.getRole());
        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "User role updated successfully.",
                "role", request.getRole()
        ));
    }
}
