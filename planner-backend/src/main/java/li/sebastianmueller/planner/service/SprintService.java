package li.sebastianmueller.planner.service;

import li.sebastianmueller.planner.dto.*;
import li.sebastianmueller.planner.entity.Sprint;
import li.sebastianmueller.planner.repository.SprintRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.IsoFields;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Slf4j
@Service
@RequiredArgsConstructor
public class SprintService {

    private static final ZoneId ZURICH = ZoneId.of("Europe/Zurich");

    private final SprintRepository sprintRepository;
    private final TaskService taskService;

    // ─── Public API ───────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public SprintSummaryResponse getCurrentSprint() {
        return sprintRepository.findFirstByStatus("ACTIVE")
                .map(this::toResponse)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "No active sprint found"));
    }

    /**
     * Idempotent: returns existing sprint for the current ISO week, or creates one.
     * Used by the scheduler and by manual POST /api/v1/sprints.
     */
    @Transactional
    public SprintSummaryResponse getOrCreateCurrentWeekSprint() {
        LocalDate today = LocalDate.now(ZURICH);
        int isoYear = today.get(IsoFields.WEEK_BASED_YEAR);
        int isoWeek = today.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR);
        return findOrCreate(isoYear, isoWeek);
    }

    /**
     * Returns all sprints paged, newest first (caller supplies Pageable with sort).
     */
    @Transactional(readOnly = true)
    public PagedResponse<SprintSummaryResponse> listSprints(Pageable pageable) {
        Page<Sprint> page = sprintRepository.findAll(pageable);
        List<SprintSummaryResponse> content = page.getContent().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
        return PagedResponse.<SprintSummaryResponse>builder()
                .content(content)
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .last(page.isLast())
                .build();
    }

    /**
     * Creates a sprint for the given ISO year/week (idempotent).
     * If isoYear/isoWeek are absent in the request, defaults to the current week.
     */
    @Transactional
    public SprintSummaryResponse createSprint(SprintRequest request) {
        if (request.getIsoYear() != null && request.getIsoWeek() != null) {
            return findOrCreate(request.getIsoYear(), request.getIsoWeek());
        }
        return getOrCreateCurrentWeekSprint();
    }

    /**
     * Returns tasks belonging to the given sprint, paged (page 0, size 100, asc createdAt).
     */
    @Transactional(readOnly = true)
    public PagedResponse<TaskListItemResponse> getTasksForSprint(UUID sprintId) {
        sprintRepository.findById(sprintId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Sprint not found"));
        return taskService.list(null, null, null, null, sprintId, null,
                0, 100, "createdAt", "asc");
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private SprintSummaryResponse findOrCreate(int isoYear, int isoWeek) {
        return sprintRepository.findByIsoYearAndIsoWeek(isoYear, isoWeek)
                .map(this::toResponse)
                .orElseGet(() -> toResponse(createSprintForWeek(isoYear, isoWeek)));
    }

    private Sprint createSprintForWeek(int isoYear, int isoWeek) {
        // Derive Monday of the target ISO week from a reference date in that week
        LocalDate anyDayInWeek = LocalDate.now(ZURICH)
                .with(IsoFields.WEEK_BASED_YEAR, isoYear)
                .with(IsoFields.WEEK_OF_WEEK_BASED_YEAR, isoWeek);
        LocalDate monday = anyDayInWeek.with(DayOfWeek.MONDAY);
        LocalDate sunday = monday.plusDays(6);

        String name = String.format("%d - Woche %02d", isoYear, isoWeek);
        log.info("Creating sprint '{}' ({} – {})", name, monday, sunday);

        Sprint sprint = Sprint.builder()
                .name(name)
                .isoYear(isoYear)
                .isoWeek(isoWeek)
                .startDate(monday)
                .endDate(sunday)
                .status("UPCOMING")
                .build();
        return sprintRepository.save(sprint);
    }

    @Transactional
    public SprintSummaryResponse activateSprint(UUID id) {
        Sprint sprint = sprintRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Sprint not found"));
        sprint.setStatus("ACTIVE");
        return toResponse(sprintRepository.save(sprint));
    }

    @Transactional(readOnly = true)
    public List<SprintSummaryResponse> listUpcoming() {
        return sprintRepository.findByStatusOrderByStartDateAsc("UPCOMING")
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public void ensureUpcomingSprintsExist() {
        LocalDate nextMonday = LocalDate.now(ZURICH)
                .with(DayOfWeek.MONDAY)
                .plusWeeks(1);
        IntStream.range(0, 4).forEach(offset -> {
            LocalDate weekDay = nextMonday.plusWeeks(offset);
            int isoYear = weekDay.get(IsoFields.WEEK_BASED_YEAR);
            int isoWeek = weekDay.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR);
            findOrCreate(isoYear, isoWeek);
        });
        log.info("SprintService: ensured upcoming sprints for next 4 ISO weeks");
    }

    public SprintSummaryResponse toResponse(Sprint sprint) {
        return SprintSummaryResponse.builder()
                .id(sprint.getId())
                .name(sprint.getName())
                .isoYear(sprint.getIsoYear())
                .isoWeek(sprint.getIsoWeek())
                .startDate(sprint.getStartDate())
                .endDate(sprint.getEndDate())
                .status(sprint.getStatus())
                .build();
    }
}
