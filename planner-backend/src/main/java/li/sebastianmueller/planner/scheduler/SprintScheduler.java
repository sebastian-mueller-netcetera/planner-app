package li.sebastianmueller.planner.scheduler;

import li.sebastianmueller.planner.service.SprintService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Runs every Monday at midnight Europe/Zurich to ensure the current ISO
 * week's sprint exists (idempotent via SprintService.getOrCreateCurrentWeekSprint).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SprintScheduler {

    private final SprintService sprintService;

    @Scheduled(cron = "0 0 0 * * MON", zone = "Europe/Zurich")
    public void ensureUpcomingSprints() {
        log.info("SprintScheduler: ensuring next 4 upcoming sprints exist");
        sprintService.ensureUpcomingSprintsExist();
        log.info("SprintScheduler: upcoming sprint check complete");
    }
}
