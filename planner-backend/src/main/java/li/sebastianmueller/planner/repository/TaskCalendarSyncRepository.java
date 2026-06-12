package li.sebastianmueller.planner.repository;

import li.sebastianmueller.planner.entity.TaskCalendarSync;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TaskCalendarSyncRepository extends JpaRepository<TaskCalendarSync, UUID> {

    List<TaskCalendarSync> findByTaskId(UUID taskId);

    Optional<TaskCalendarSync> findByTaskIdAndCalendarKey(UUID taskId, String calendarKey);
}
