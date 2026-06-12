package li.sebastianmueller.planner.controller;

import li.sebastianmueller.planner.dto.*;
import li.sebastianmueller.planner.service.SprintService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/sprints")
@RequiredArgsConstructor
public class SprintController {

    private final SprintService sprintService;

    /**
     * GET /api/v1/sprints
     * Returns all sprints, newest first (highest isoYear + isoWeek).
     */
    @GetMapping
    public ResponseEntity<PagedResponse<SprintSummaryResponse>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size,
                Sort.by(Sort.Direction.DESC, "isoYear", "isoWeek"));
        return ResponseEntity.ok(sprintService.listSprints(pageable));
    }

    /**
     * GET /api/v1/sprints/current
     * Returns the sprint whose date range covers today, 404 if none exists.
     */
    @GetMapping("/current")
    public ResponseEntity<SprintSummaryResponse> getCurrent() {
        return ResponseEntity.ok(sprintService.getCurrentSprint());
    }

    /**
     * POST /api/v1/sprints
     * Creates (or returns existing) sprint for the given isoYear+isoWeek.
     * If body is absent or fields are null, defaults to the current ISO week.
     * Idempotent by isoYear+isoWeek.
     */
    @PostMapping
    public ResponseEntity<SprintSummaryResponse> create(
            @RequestBody(required = false) SprintRequest request) {
        SprintSummaryResponse response = sprintService.createSprint(
                request != null ? request : new SprintRequest());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
