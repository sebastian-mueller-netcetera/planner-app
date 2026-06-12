package li.sebastianmueller.planner.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskResponse {

    private UUID id;
    private String title;
    private String description;
    private String status;
    private UserSummaryResponse assignee;
    private LocalDate dueDate;
    private SprintSummaryResponse sprint;
    private Boolean syncGoogleCalendarA;
    private Boolean syncGoogleCalendarB;
    private OffsetDateTime archivedAt;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private Set<LabelResponse> labels;
    private List<CommentResponse> comments;
}
