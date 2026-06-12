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
    public void createCurrentWeekSprint() {
        log.info("SprintScheduler: triggering auto-create for current ISO week sprint");
        sprintService.getOrCreateCurrentWeekSprint();
        log.info("SprintScheduler: auto-create complete");
    }
}
