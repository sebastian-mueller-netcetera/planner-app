package li.sebastianmueller.planner.dto;

import lombok.Data;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Data
public class TaskRequest {

    private String title;

    private String description;

    private String status;

    private UUID assigneeId;

    private LocalDate dueDate;

    private UUID sprintId;

    private Boolean syncGoogleCalendarA;

    private Boolean syncGoogleCalendarB;

    private Set<UUID> labelIds;
}
