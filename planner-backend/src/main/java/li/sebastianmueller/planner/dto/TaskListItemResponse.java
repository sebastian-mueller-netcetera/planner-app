package li.sebastianmueller.planner.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Set;
import java.util.UUID;

/**
 * Lightweight task summary for list endpoints — excludes comments to keep payloads small.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskListItemResponse {

    private UUID id;
    private String title;
    private String description;
    private String status;
    private UserSummaryResponse assignee;
    private LocalDate dueDate;
    private SprintSummaryResponse sprint;
    private Boolean syncGoogleCalendarA;
    private Boolean syncGoogleCalendarB;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private Set<LabelResponse> labels;
}
